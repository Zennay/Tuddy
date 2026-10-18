package com.livelingo.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Human-written dictionary notes, not a translated/verified model output.
 *
 * A word and its meaning persist only when the user explicitly saves them.
 * Never scan or persist conversation text automatically. Android backup is
 * disabled at the application level; data remains in app-private preferences.
 */
internal data class OwnWord(val language: String, val word: String, val ownMeaning: String)
internal data class ScheduledOwnWord(val entry: OwnWord, val dueDay: Long, val stage: Int)

internal class PrivateOwnWordDeck(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "tuddy_own_word_notes_v1", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY = "own_word_notes"
        private const val REVIEW_OPT_IN = "own_word_review_enabled_v1"
        private const val REVIEW_DATA = "own_word_review_rows_v1"
        private val INTERVAL_DAYS = listOf(1L, 2L, 4L, 7L, 14L, 30L)
        const val MAX_ENTRIES = 120
        const val MAX_BACKUP_BYTES = 50_000
        private const val MAX_JSON = 50_000
        val SUPPORTED_CODES: Set<String> =
            (TargetCatalog.codes + listOf("kmr", "ckb", "ps")).toSet()
        private fun cleanCode(raw: String): String =
            raw.trim().replace('_', '-').substringBefore('-').lowercase(Locale.ROOT)

        private fun safe(value: String, max: Int, allowSpaces: Boolean): Boolean =
            value.isNotBlank() && value.length <= max && value == value.trim() &&
                (allowSpaces || value.none { it.isWhitespace() }) &&
                value.none { ch ->
                    Character.isISOControl(ch) || Character.isSurrogate(ch) ||
                        (Character.getType(ch) == Character.FORMAT.toInt() && ch != '\u200C') ||
                        ch == '\u2028' || ch == '\u2029'
                }

        private fun valid(value: OwnWord): Boolean =
            value.language in SUPPORTED_CODES &&
                safe(value.word, 48, allowSpaces = false) &&
                safe(value.ownMeaning, 180, allowSpaces = true)

        private fun key(item: OwnWord) =
            item.language + "|" + item.word.lowercase(Locale.ROOT)
    }

    private fun read(): List<OwnWord>? {
        val raw = try { prefs.getString(KEY, null) } catch (_: RuntimeException) { return null }
        if (raw == null) return emptyList()
        if (raw.length > MAX_JSON) return null
        return try {
            val array = JSONArray(raw)
            if (array.length() > MAX_ENTRIES) return null
            val seen = HashSet<String>()
            val items = ArrayList<OwnWord>(array.length())
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                if (row.length() != 3 ||
                    row.opt("language") !is String ||
                    row.opt("word") !is String ||
                    row.opt("ownMeaning") !is String
                ) return null
                val item = OwnWord(
                    row.getString("language"), row.getString("word"),
                    row.getString("ownMeaning")
                )
                if (!valid(item) || !seen.add(key(item))) return null
                items.add(item)
            }
            items
        } catch (_: Exception) { null }
    }

    /** No second copy of user words; keys are 64-char SHA-256 fingerprints. */
    private fun fingerprint(item: OwnWord): String {
        val input = (item.language + "\u0000" + item.word.lowercase(Locale.ROOT))
            .toByteArray(Charsets.UTF_8)
        return java.security.MessageDigest.getInstance("SHA-256").digest(input)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun reviewRows(): Map<String, Pair<Int, Long>> {
        val data = try {
            prefs.getStringSet(REVIEW_DATA, emptySet())?.toSet().orEmpty()
        } catch (_: RuntimeException) { return emptyMap() }
        if (data.size > MAX_ENTRIES) return emptyMap()
        return data.mapNotNull { row ->
            val parts = row.split('|')
            if (parts.size != 3 || parts[0].length != 64 ||
                parts[0].any { it !in "0123456789abcdef" }) return@mapNotNull null
            val stage = parts[1].toIntOrNull() ?: return@mapNotNull null
            val due = parts[2].toLongOrNull() ?: return@mapNotNull null
            if (stage !in 0..5 || due !in 0L..365000L) return@mapNotNull null
            parts[0] to (stage to due)
        }.toMap()
    }

    private fun encodeReviews(records: Map<String, Pair<Int, Long>>): Set<String> =
        records.map { (hash, value) -> "$hash|${value.first}|${value.second}" }.toSet()

    @Synchronized
    fun reviewsEnabled(): Boolean = try { prefs.getBoolean(REVIEW_OPT_IN, false) }
        catch (_: RuntimeException) { false }

    @Synchronized
    fun enableReviews(): Boolean = prefs.edit().putBoolean(REVIEW_OPT_IN, true).commit()

    @Synchronized
    fun disableReviews(): Boolean = prefs.edit()
        .remove(REVIEW_OPT_IN).remove(REVIEW_DATA).commit()

    @Synchronized
    fun scheduledReviews(): List<ScheduledOwnWord>? {
        val notes = entries() ?: return null
        if (!reviewsEnabled()) return emptyList()
        val today = java.time.LocalDate.now().toEpochDay()
        val rows = reviewRows()
        return notes.map { note ->
            val record = rows[fingerprint(note)]
            ScheduledOwnWord(note, record?.second ?: today, record?.first ?: 0)
        }.sortedWith(compareBy(ScheduledOwnWord::dueDay,
            { it.entry.language }, { it.entry.word }))
    }

    @Synchronized
    fun rateScheduledWord(note: OwnWord, remembered: Boolean): Boolean {
        if (!reviewsEnabled()) return false
        val current = read() ?: return false
        if (note !in current) return false
        val hash = fingerprint(note)
        val allowed = current.map(::fingerprint).toSet()
        val records = reviewRows().filterKeys { it in allowed }.toMutableMap()
        val stage = if (remembered) ((records[hash]?.first ?: 0) + 1).coerceAtMost(5) else 0
        records[hash] = stage to java.time.LocalDate.now()
            .plusDays(INTERVAL_DAYS[stage]).toEpochDay()
        return prefs.edit().putStringSet(REVIEW_DATA, encodeReviews(records)).commit()
    }

    private fun write(items: List<OwnWord>, resetReviews: Boolean = false): Boolean {
        if (items.size > MAX_ENTRIES || items.any { !valid(it) } ||
            items.map(::key).toSet().size != items.size) return false
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject()
                .put("language", item.language)
                .put("word", item.word)
                .put("ownMeaning", item.ownMeaning))
        }
        val json = array.toString()
        if (json.length > MAX_JSON) return false
        // A changed personal definition must be practised again rather than
        // reusing the interval for a different answer. Deletions also prune.
        val previous = read().orEmpty().associateBy(::fingerprint)
        val retained = if (resetReviews) emptyMap() else {
            reviewRows().filterKeys { hash ->
                items.any { note ->
                    fingerprint(note) == hash && previous[hash] == note
                }
            }
        }
        return try { prefs.edit()
            .putString(KEY, json)
            .putStringSet(REVIEW_DATA, encodeReviews(retained))
            .commit() }
        catch (_: RuntimeException) { false }
    }

    @Synchronized
    fun entries(): List<OwnWord>? =
        read()?.sortedWith(compareBy(OwnWord::language, OwnWord::word))

    @Synchronized
    fun find(languageTag: String?, word: String): OwnWord? {
        if (languageTag == null) return null
        val code = cleanCode(languageTag)
        val canonical = word.trim().trim('\'', '’', '-').lowercase(Locale.ROOT)
        return read()?.firstOrNull {
            it.language == code && it.word.lowercase(Locale.ROOT) == canonical
        }
    }

    /** Explicit save replaces only the same user-authored language + word key. */
    @Synchronized
    fun save(languageTag: String, word: String, meaning: String): Boolean {
        val candidate = OwnWord(
            cleanCode(languageTag), word.trim().trim('\'', '’', '-'), meaning.trim()
        )
        if (!valid(candidate)) return false
        val current = read() ?: return false
        return write(current.filterNot { key(it) == key(candidate) } + candidate)
    }

    @Synchronized
    fun remove(entry: OwnWord): Boolean {
        val current = read() ?: return false
        if (entry !in current) return false
        return write(current - entry)
    }

    /**
     * Deliberately exported own meanings, not reviewed translations.
     * TSV can be re-imported using the existing strict, bounded parser.
     */
    @Synchronized
    fun exportTsvSnapshot(): ByteArray? {
        val saved = entries() ?: return null
        if (saved.isEmpty()) return null
        val text = buildString {
            append("language\tword\tmeaning\n")
            saved.forEach { row ->
                append(row.language).append('\t')
                append(row.word).append('\t')
                append(row.ownMeaning).append('\n')
            }
        }
        return text.toByteArray(Charsets.UTF_8).takeIf { bytes ->
            bytes.size <= MAX_BACKUP_BYTES && previewTsvImport(bytes) == saved
        }
    }

    /**
     * Explicit user-selected JSON backup. No transcript/audio/provider data
     * is included, only the user's word, language and their own meaning.
     */
    @Synchronized
    fun exportSnapshot(): ByteArray? {
        val owned = read() ?: return null
        if (owned.isEmpty()) return null
        val array = JSONArray()
        owned.sortedWith(compareBy(OwnWord::language, OwnWord::word)).forEach { item ->
            array.put(JSONObject()
                .put("language", item.language)
                .put("word", item.word)
                .put("ownMeaning", item.ownMeaning))
        }
        val root = JSONObject()
            .put("format", "tuddy-own-words-v1")
            .put("entries", array)
        return (root.toString() + "\n").toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BACKUP_BYTES }
    }

    /** Reject wrong versions, malformed UTF-8, duplicate words or huge files. */
    fun previewImport(bytes: ByteArray): List<OwnWord>? {
        if (bytes.isEmpty() || bytes.size > MAX_BACKUP_BYTES) return null
        val source = bytes.toString(Charsets.UTF_8)
        if (!source.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return null
        return try {
            val root = JSONObject(source)
            if (root.length() != 2 ||
                root.opt("format") != "tuddy-own-words-v1" ||
                root.opt("entries") !is JSONArray) return null
            val array = root.getJSONArray("entries")
            if (array.length() !in 1..MAX_ENTRIES) return null
            val seen = HashSet<String>()
            val entries = ArrayList<OwnWord>(array.length())
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                if (row.length() != 3 || row.opt("language") !is String ||
                    row.opt("word") !is String || row.opt("ownMeaning") !is String
                ) return null
                val item = OwnWord(
                    row.getString("language"), row.getString("word"),
                    row.getString("ownMeaning")
                )
                if (!valid(item) || !seen.add(key(item))) return null
                entries.add(item)
            }
            entries
        } catch (_: Exception) { null }
    }

    /**
     * User-selected UTF-8 TSV import for manually maintained vocabulary lists.
     * The header is mandatory: language<TAB>word<TAB>meaning. No guessing from
     * CSV, URLs, transcript text, screenshots or translated model output.
     * Never writes anything until the UI separately confirms merge/replace.
     */
    fun previewTsvImport(bytes: ByteArray): List<OwnWord>? {
        if (bytes.isEmpty() || bytes.size > MAX_BACKUP_BYTES) return null
        val raw = bytes.toString(Charsets.UTF_8)
        if (!raw.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return null
        val text = raw.removePrefix("\uFEFF")
        val lines = text.split('\n').dropLastWhile { it.isEmpty() }
        if (lines.size !in 2..(MAX_ENTRIES + 1)) return null
        val header = lines.first().removeSuffix("\r")
        if (header != "language\tword\tmeaning") return null
        val seen = HashSet<String>()
        val records = ArrayList<OwnWord>()
        for (line in lines.drop(1)) {
            val columns = line.removeSuffix("\r").split('\t')
            if (columns.size != 3) return null
            val word = OwnWord(
                language = cleanCode(columns[0]),
                word = columns[1],
                ownMeaning = columns[2]
            )
            if (!valid(word) || !seen.add(key(word))) return null
            records.add(word)
        }
        return records.takeIf { it.isNotEmpty() }
    }

    /** Merge only previously unseen keys: local edits always win. */
    @Synchronized
    fun mergeImported(imported: List<OwnWord>): Boolean {
        if (imported.isEmpty() || imported.size > MAX_ENTRIES ||
            imported.any { !valid(it) } ||
            imported.map(::key).toSet().size != imported.size) return false
        val current = read() ?: return false
        val owned = current.map(::key).toSet()
        val additions = imported.filterNot { key(it) in owned }
        if (current.size + additions.size > MAX_ENTRIES) return false
        return if (additions.isEmpty()) true else write(current + additions)
    }

    /** Overwrite only after a second, informed user confirmation in the UI. */
    @Synchronized
    fun replaceImported(imported: List<OwnWord>): Boolean =
        if (imported.isEmpty()) false else write(imported, resetReviews = true)

    @Synchronized
    fun clear(): Boolean = try { prefs.edit()
        .remove(KEY).remove(REVIEW_OPT_IN).remove(REVIEW_DATA).commit() }
    catch (_: RuntimeException) { false }
}
