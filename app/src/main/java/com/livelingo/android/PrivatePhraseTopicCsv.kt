package com.livelingo.android

import android.content.Context
import java.util.Locale

/**
 * Editable, manually exchanged phrasebook format with one optional theme per row.
 * A separate five-column header prevents accidentally treating the fourth column
 * of the existing, theme-less CSV format as a theme.
 *
 * Files are never read automatically, written without an SAF choice, or used for
 * translation/recognition. The UI owns all user consent and persistence.
 */
internal object PrivatePhraseTopicCsv {
    private const val HEADER = "language,phrase,meaningLanguage,ownMeaning,topic"
    private const val BASIC_HEADER = "language,phrase,meaningLanguage,ownMeaning"
    private const val MAX_BYTES = 50_000
    private const val MAX_ROWS = 80
    private const val MAX_TOPICS = 12

    private fun validTopic(name: String): Boolean =
        name.length in 1..30 && name == name.trim() &&
            !name.startsWith("__") && name.none { character ->
                character == '|' || Character.isISOControl(character) ||
                    Character.isSurrogate(character) ||
                    (Character.getType(character) == Character.FORMAT.toInt() &&
                        character != '\u200C')
            }

    private fun cell(value: String): String =
        if (value.contains(',') || value.contains('"')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    /** Strict one-line CSV decoder; commas/escaped quotes, but no embedded newline. */
    private fun columns(line: String): List<String>? {
        val parts = ArrayList<String>(5)
        var offset = 0
        while (true) {
            val cell = StringBuilder()
            if (offset < line.length && line[offset] == '"') {
                offset++
                var closed = false
                while (offset < line.length) {
                    val character = line[offset++]
                    if (character == '"') {
                        if (offset < line.length && line[offset] == '"') {
                            cell.append('"')
                            offset++
                        } else {
                            closed = true
                            break
                        }
                    } else cell.append(character)
                }
                if (!closed || offset < line.length && line[offset] != ',') return null
            } else {
                while (offset < line.length && line[offset] != ',') {
                    val character = line[offset++]
                    if (character == '"') return null
                    cell.append(character)
                }
            }
            parts.add(cell.toString())
            if (parts.size > 5) return null
            if (offset == line.length) break
            offset++ // Exactly one comma; a trailing comma is a valid empty topic.
        }
        return parts
    }

    /** Read only; all entries and topic links must be valid before a preview. */
    fun preview(bytes: ByteArray, deck: PrivatePhraseDeck): PrivatePhraseArchive? {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
        val source = bytes.toString(Charsets.UTF_8)
        if (!source.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return null
        val lines = source.removePrefix("\uFEFF").split('\n')
            .dropLastWhile { it.isEmpty() }.map { it.removeSuffix("\r") }
        if (lines.size !in 2..(MAX_ROWS + 1) || lines.first() != HEADER) return null

        val allColumns = lines.drop(1).map { columns(it) ?: return null }
        if (allColumns.any { it.size != 5 }) return null
        // Let the existing personal phrase engine validate all text, language
        // tags, code points, duplicate phrase identities and row limits.
        val fourColumns = buildString {
            append(BASIC_HEADER).append('\n')
            allColumns.forEach { row ->
                append(row.take(4).joinToString(",") { cell(it) }).append('\n')
            }
        }.toByteArray(Charsets.UTF_8)
        val notes = deck.previewCsvImport(fourColumns) ?: return null
        if (notes.size != allColumns.size) return null

        val names = mutableListOf<String>()
        val links = mutableMapOf<String, String>()
        for (i in notes.indices) {
            val theme = allColumns[i][4]
            if (theme.isEmpty()) continue
            if (!validTopic(theme)) return null
            if (names.none { it == theme }) {
                if (names.any { it.equals(theme, ignoreCase = true) }) return null
                names.add(theme)
                if (names.size > MAX_TOPICS) return null
            }
            val hash = PrivatePhraseShortcuts.fingerprint(notes[i])
            if (links.put(hash, theme) != null) return null
        }
        return PrivatePhraseArchive(notes, names, links)
    }

    /**
     * Only a deliberate, complete personal backup can generate this CSV.
     * Re-parse exact bytes to verify the record ordering and topic mapping.
     */
    fun snapshot(
        context: Context,
        deck: PrivatePhraseDeck,
        selection: List<PersonalPhrase>? = null
    ): Pair<ByteArray, PrivatePhraseArchive>? {
        val originalBytes = deck.exportArchiveSnapshot(context) ?: return null
        val original = deck.previewArchiveImport(originalBytes) ?: return null
        if (original.notes.isEmpty()) return null
        // Optional subset never invents or silently broadens the requested
        // private export. The original local phrases must match exactly.
        val selected = if (selection == null) original.notes else {
            if (selection.isEmpty() || selection.size > MAX_ROWS ||
                selection.toSet().size != selection.size ||
                !original.notes.containsAll(selection)) return null
            val permitted = selection.toSet()
            original.notes.filter { it in permitted }
        }
        if (selected.isEmpty()) return null
        val allowedIds = selected.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
        val selectedAssignments = original.assignments.filterKeys { it in allowedIds }

        val output = buildString {
            append(HEADER).append('\n')
            selected.forEach { note ->
                val theme = original.assignments[PrivatePhraseShortcuts.fingerprint(note)].orEmpty()
                append(
                    listOf(
                        note.language, note.phrase, note.meaningLanguage,
                        note.ownMeaning, theme
                    ).joinToString(",") { cell(it) }
                )
                append('\n')
            }
        }.toByteArray(Charsets.UTF_8)
        if (output.size > MAX_BYTES) return null
        val parsed = preview(output, deck) ?: return null
        if (parsed.notes != selected || parsed.assignments != selectedAssignments) return null
        return output to parsed
    }
}
