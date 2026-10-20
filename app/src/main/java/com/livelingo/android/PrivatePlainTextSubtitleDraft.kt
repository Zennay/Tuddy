package com.livelingo.android

import java.util.Locale

/**
 * User-authored line, sentence or explicitly paired text cue draft with
 * chosen synthetic durations. These are NOT speech timestamps, word alignment, or translations.
 * Only used after deliberate manual input; no audio, clipboard, or I/O.
 */
internal enum class PlainSubtitleSplitMode { LINES, SENTENCES, PAIRED_COLUMNS, TIMED_PAIRED_COLUMNS, PAIRED_TEXTBOXES }

internal data class PairedSubtitleInputInspection(
    val sourceRows: Int,
    val meaningRows: Int,
    val message: String?,
)

internal object PrivatePlainTextSubtitleDraft {
    private const val MAX_BYTES = 262_144
    private const val MAX_CUES = 500
    private const val MAX_TIME = 359_999_999L
    val durationOptions = listOf(2_000L, 3_500L, 5_000L, 7_000L)
    val startOptions = listOf(0L, 5_000L, 10_000L, 30_000L, 60_000L)

    private fun pairedRows(text: String): List<String> {
        val normalized = text.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n')
        // An ordinary final newline is not a separate entry; any other empty
        // row is still reported, never silently discarded.
        return if (normalized.isEmpty()) emptyList()
        else normalized.removeSuffix("\n").split('\n')
    }

    /** UI-visible reason if two manually authored lists cannot align. */
    fun inspectPaired(source: String, meaning: String): PairedSubtitleInputInspection {
        val originalRows = pairedRows(source)
        val meaningRows = pairedRows(meaning)
        val count = PairedSubtitleInputInspection(originalRows.size, meaningRows.size, null)
        if (originalRows.isEmpty() || meaningRows.isEmpty()) return count.copy(
            message = "Vul beide vakken met ten minste één regel.")
        if (originalRows.size > MAX_CUES || meaningRows.size > MAX_CUES)
            return count.copy(message = "Maximaal 500 tekstparen per concept.")
        if (originalRows.size != meaningRows.size)
            return count.copy(message = "Aantal regels verschilt: " +
                "${originalRows.size} bronregels tegenover ${meaningRows.size} betekenissen. " +
                "Vul de ontbrekende tekst in, verwijder geen lege regels automatisch.")
        for (i in originalRows.indices) {
            val first = originalRows[i].trim()
            val second = meaningRows[i].trim()
            if (first.isBlank() || second.isBlank())
                return count.copy(message = "Regel ${i + 1} bevat een leeg " +
                    (if (first.isBlank()) "bronveld." else "betekenisveld."))
            if (first.length > 800 || second.length > 800)
                return count.copy(message = "Regel ${i + 1} is langer dan 800 tekens.")
            if (listOf(first, second).any { value ->
                    "-->" in value || value.any { ch -> ch.isISOControl() }
                }) return count.copy(message = "Regel ${i + 1} heeft " +
                "ongeldige controletekens of een tijdcodepijl.")
        }
        return count
    }

    private fun label(milliseconds: Long): String =
        String.format(Locale.ROOT, "%02d:%02d:%02d,%03d",
            milliseconds / 3_600_000L,
            milliseconds / 60_000L % 60,
            milliseconds / 1_000L % 60,
            milliseconds % 1_000L)

    fun preview(
        raw: String,
        startMillis: Long,
        durationMillis: Long,
        splitMode: PlainSubtitleSplitMode = PlainSubtitleSplitMode.LINES,
        secondaryRaw: String = ""
    ): List<PrivateSubtitleCue>? {
        if (startMillis !in startOptions || durationMillis !in durationOptions) return null
        val bytes = raw.toByteArray(Charsets.UTF_8)
        val secondaryBytes = secondaryRaw.toByteArray(Charsets.UTF_8)
        if (bytes.isEmpty() || bytes.size + secondaryBytes.size > MAX_BYTES ||
            bytes.toString(Charsets.UTF_8) != raw ||
            secondaryBytes.toString(Charsets.UTF_8) != secondaryRaw) return null
        val normalized = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        if (splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES) {
            // Both lists are checked in the same order as the on-screen
            // diagnostic: no invented, skipped or reordered translations.
            if (inspectPaired(raw, secondaryRaw).message != null) return null
            val originals = pairedRows(raw)
            val meanings = pairedRows(secondaryRaw)
            val cues = originals.indices.map { index ->
                val source = originals[index].trim()
                val meaning = meanings[index].trim()
                val begin = startMillis + index.toLong() * durationMillis
                val end = begin + durationMillis
                if (end > MAX_TIME || end <= begin) return null
                PrivateSubtitleCue(begin, end, label(begin), label(end),
                    source + "\n" + meaning)
            }
            if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
                PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
                return null
            return cues
        }
        if (splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS) {
            // Four manually supplied columns: start, end, source, own meaning.
            // Unlike the estimated routes, timestamps are copied exactly.
            val rawRows = normalized.lines().filter { it.isNotBlank() }
            if (rawRows.isEmpty() || rawRows.size > MAX_CUES) return null
            val cues = rawRows.map { row ->
                val columns = row.split('\t')
                if (columns.size != 4) return null
                val begin = PrivateSubtitleTimelineNavigation.timestamp(columns[0])
                    ?: return null
                val end = PrivateSubtitleTimelineNavigation.timestamp(columns[1])
                    ?: return null
                if (begin < 0L || end > MAX_TIME || begin >= end) return null
                val source = columns[2].trim()
                val meaning = columns[3].trim()
                if (source.isEmpty() || meaning.isEmpty() ||
                    source.length > 800 || meaning.length > 800 ||
                    listOf(source, meaning).any { value ->
                        "-->" in value || value.any { ch ->
                            ch == '\u0000' || ch.isISOControl()
                        }
                    }) return null
                PrivateSubtitleCue(begin, end, label(begin), label(end),
                    source + "\n" + meaning)
            }
            if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
                PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
                return null
            return cues
        }
        // Never classify natural sentences as recognized speech: this merely
        // suggests cut points at punctuation followed by whitespace.
        val rows = when (splitMode) {
            PlainSubtitleSplitMode.LINES -> normalized.lines()
                .map { it.trim() }.filter { it.isNotEmpty() }
            PlainSubtitleSplitMode.SENTENCES -> normalized.trim()
                .replace(Regex("""[ \t\n]+"""), " ")
                .split(Regex("""(?<=[.!?。！？؟])\s+"""))
                .map { it.trim() }.filter { it.isNotEmpty() }
            PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS -> return null // handled above
            PlainSubtitleSplitMode.PAIRED_TEXTBOXES -> return null // handled above
            PlainSubtitleSplitMode.PAIRED_COLUMNS -> normalized.lines()
                .map { it.trim() }.filter { it.isNotEmpty() }.map { row ->
                    val columns = row.split('\t')
                    // Require exactly two explicit, nonempty author-supplied
                    // fields. Do not invent a missing translation or split
                    // language text heuristically.
                    if (columns.size != 2) return null
                    val first = columns[0].trim()
                    val second = columns[1].trim()
                    if (first.isEmpty() || second.isEmpty() ||
                        first.length > 800 || second.length > 800) return null
                    first + "\n" + second
                }
        }
        if (rows.isEmpty() || rows.size > MAX_CUES ||
            rows.first().startsWith("WEBVTT", ignoreCase = true) ||
            rows.any { row ->
                row.length > 2400 || row.lines().size > 2 ||
                row.lines().any { it.length > 800 } || "-->" in row ||
                row.any { it == '\u0000' || (it.isISOControl() && it != '\n') }
            }) return null
        val cues = rows.mapIndexed { index, row ->
            val begin = startMillis + index.toLong() * durationMillis
            val end = begin + durationMillis
            if (end > MAX_TIME || end <= begin) return null
            PrivateSubtitleCue(begin, end, label(begin), label(end), row)
        }
        // Reuse the same portable SRT/VTT export boundaries as the editor.
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null) return null
        return cues
    }
}
