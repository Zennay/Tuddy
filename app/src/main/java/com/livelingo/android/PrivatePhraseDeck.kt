package com.livelingo.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Personal human-authored phrase notes for underserved language previews.
 * These are NOT ML Kit translations, reviewed dictionary facts or ASR results.
 *
 * Persistence is opt-in per phrase; Android app-private SharedPreferences and
 * allowBackup=false. No server, network, audio or automatic transcript storage.
 */
internal data class PersonalPhrase(
    val language: String,
    val phrase: String,
    val meaningLanguage: String,
    val ownMeaning: String
)

internal data class DuePersonalPhrase(val phrase: PersonalPhrase, val stage: Int, val dueDay: Long)

internal class PrivatePhraseDeck(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "tuddy_private_phrases_v1", Context.MODE_PRIVATE
    )

    private companion object {
        const val KEY = "user_authored"
        const val REVIEW_OPT_IN = "private_phrase_review_enabled"
        const val REVIEW_ROWS = "private_phrase_review_rows_v1"
        val REVIEW_INTERVALS = listOf(1L, 2L, 4L, 7L, 14L, 30L)
        const val MAX_ROWS = 80
        const val MAX_JSON = 50000
        val LANGUAGES = setOf("kmr", "ckb", "ps")
        val MEANING_LANGUAGES = setOf("nl", "en")
    }

    private fun humanText(value: String, max: Int): Boolean {
        if (value.isBlank() || value.length > max || value != value.trim()) return false
        return value.none { ch ->
            Character.isISOControl(ch) ||
                (Character.getType(ch) == Character.FORMAT.toInt() && ch != '\u200C') ||
                Character.isSurrogate(ch) || ch == '\u2028' || ch == '\u2029'
        }
    }

    private fun valid(record: PersonalPhrase): Boolean =
        record.language in LANGUAGES &&
            record.meaningLanguage in MEANING_LANGUAGES &&
            humanText(record.phrase, 120) &&
            humanText(record.ownMeaning, 180)

    private fun parseEntries(array: JSONArray): List<PersonalPhrase>? {
        if (array.length() > MAX_ROWS) return null
        val seen = mutableSetOf<String>()
        val entries = mutableListOf<PersonalPhrase>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.length() != 4 ||
                listOf("language", "phrase", "meaningLanguage", "ownMeaning").any {
                    obj.opt(it) !is String
                }) return null
            val record = PersonalPhrase(
                obj.getString("language"), obj.getString("phrase"),
                obj.getString("meaningLanguage"), obj.getString("ownMeaning")
            )
            if (!valid(record)) return null
            val key = record.language + "|" + record.phrase.lowercase(Locale.ROOT)
            if (!seen.add(key)) return null
            entries.add(record)
        }
        return entries
    }

    private fun decode(): List<PersonalPhrase>? {
        val raw = try { prefs.getString(KEY, null) } catch (_: RuntimeException) { return null }
        if (raw == null) return emptyList()
        if (raw.length > MAX_JSON) return null
        return try { parseEntries(JSONArray(raw)) } catch (_: Exception) { null }
    }


    /** Only SHA-256 identities and due-day integers; no duplicate phrase text. */
    private fun reviewRows(): Map<String, Pair<Int, Long>> {
        val raw = try {
            prefs.getStringSet(REVIEW_ROWS, emptySet())?.toSet().orEmpty()
        } catch (_: RuntimeException) { return emptyMap() }
        if (raw.size > MAX_ROWS) return emptyMap()
        return raw.mapNotNull { entry ->
            val parts = entry.split('|')
            if (parts.size != 3 || parts[0].length != 64 ||
                parts[0].any { it !in "0123456789abcdef" }) return@mapNotNull null
            val stage = parts[1].toIntOrNull() ?: return@mapNotNull null
            val due = parts[2].toLongOrNull() ?: return@mapNotNull null
            if (stage !in 0..5 || due !in 0L..365000L) return@mapNotNull null
            parts[0] to (stage to due)
        }.toMap()
    }

    private fun encodeReviews(records: Map<String, Pair<Int, Long>>): Set<String> =
        records.map { (hash, p) -> "$hash|${p.first}|${p.second}" }.toSet()

    @Synchronized
    fun reviewsEnabled(): Boolean = try {
        prefs.getBoolean(REVIEW_OPT_IN, false)
    } catch (_: RuntimeException) { false }

    @Synchronized
    fun enableReviews(): Boolean = prefs.edit().putBoolean(REVIEW_OPT_IN, true).commit()

    @Synchronized
    fun disableReviews(): Boolean = prefs.edit()
        .remove(REVIEW_OPT_IN).remove(REVIEW_ROWS).commit()

    @Synchronized
    fun scheduledReviews(): List<DuePersonalPhrase>? {
        val phrases = entries() ?: return null
        if (!reviewsEnabled()) return emptyList()
        val today = java.time.LocalDate.now().toEpochDay()
        val stored = reviewRows()
        return phrases.map { phrase ->
            val record = stored[PrivatePhraseShortcuts.fingerprint(phrase)]
            DuePersonalPhrase(phrase, record?.first ?: 0, record?.second ?: today)
        }.sortedWith(compareBy(DuePersonalPhrase::dueDay,
            { it.phrase.language }, { it.phrase.phrase }))
    }

    /** Self-rated only, and only for an unchanged phrase present in this deck. */
    @Synchronized
    fun rateScheduledPhrase(phrase: PersonalPhrase, remembered: Boolean): Boolean {
        if (!reviewsEnabled()) return false
        val current = decode() ?: return false
        if (phrase !in current) return false
        val id = PrivatePhraseShortcuts.fingerprint(phrase)
        val validHashes = current.map(PrivatePhraseShortcuts::fingerprint).toSet()
        val records = reviewRows().filterKeys { it in validHashes }.toMutableMap()
        val stage = if (remembered) ((records[id]?.first ?: 0) + 1).coerceAtMost(5) else 0
        val due = java.time.LocalDate.now().plusDays(REVIEW_INTERVALS[stage]).toEpochDay()
        records[id] = stage to due
        return prefs.edit().putStringSet(REVIEW_ROWS, encodeReviews(records)).commit()
    }

    private fun toJson(entries: List<PersonalPhrase>): JSONArray {
        val array = JSONArray()
        for (entry in entries) {
            array.put(JSONObject()
                .put("language", entry.language)
                .put("phrase", entry.phrase)
                .put("meaningLanguage", entry.meaningLanguage)
                .put("ownMeaning", entry.ownMeaning))
        }
        return array
    }

    private fun write(entries: List<PersonalPhrase>, resetReview: Boolean = false): Boolean {
        if (entries.size > MAX_ROWS || entries.any { !valid(it) }) return false
        if (entries.map {
            it.language + "|" + it.phrase.lowercase(Locale.ROOT)
        }.toSet().size != entries.size) return false
        val value = toJson(entries).toString()
        if (value.length > MAX_JSON) return false
        val allowedPins = entries.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
        // Only identical human notes retain an old due date. Correcting the
        // meaning resets that card rather than implying the new answer was learned.
        val previous = decode().orEmpty().associateBy(PrivatePhraseShortcuts::fingerprint)
        val allowedReviews = if (resetReview) emptyMap() else {
            reviewRows().filterKeys { hash ->
                val current = entries.firstOrNull {
                    PrivatePhraseShortcuts.fingerprint(it) == hash
                }
                current != null && previous[hash] == current
            }
        }
        val storedPins = runCatching {
            prefs.getStringSet(PrivatePhraseShortcuts.KEY, emptySet())?.toSet().orEmpty()
        }.getOrDefault(emptySet())
        // An edit/delete/backup replacement cannot leave a stale bookmark that
        // later resurrects when an old phrase is imported back into the deck.
        return try {
            prefs.edit()
                .putString(KEY, value)
                .putStringSet(PrivatePhraseShortcuts.KEY, storedPins.intersect(allowedPins))
                .putStringSet(REVIEW_ROWS, encodeReviews(allowedReviews))
                .commit()
        } catch (_: RuntimeException) { false }
    }

    @Synchronized
    fun entries(): List<PersonalPhrase>? = decode()?.sortedWith(
        compareBy(PersonalPhrase::language, PersonalPhrase::phrase)
    )

    /** Adding an existing phrase intentionally replaces only its own note. */
    @Synchronized
    fun save(language: String, phrase: String, meaningLanguage: String, ownMeaning: String): Boolean {
        val record = PersonalPhrase(language, phrase.trim(), meaningLanguage, ownMeaning.trim())
        if (!valid(record)) return false
        val current = decode() ?: return false
        val next = current.filterNot {
            it.language == record.language && it.phrase.equals(record.phrase, ignoreCase = true)
        } + record
        return write(next)
    }

    /**
     * Replace one existing user-written sentence with one atomic preferences
     * write. The old record snapshot must still match; duplicate language/phrase
     * identities are rejected by the same whole-deck validator as normal saves.
     */
    @Synchronized
    fun edit(original: PersonalPhrase, replacement: PersonalPhrase): Boolean {
        val clean = replacement.copy(
            phrase = replacement.phrase.trim(),
            ownMeaning = replacement.ownMeaning.trim()
        )
        if (!valid(clean)) return false
        val current = decode() ?: return false
        val index = current.indexOfFirst {
            it.language == original.language && it.phrase == original.phrase
        }
        if (index < 0 || current[index] != original) return false
        val changed = current.toMutableList()
        changed[index] = clean
        return write(changed)
    }

    @Synchronized
    fun remove(language: String, phrase: String): Boolean {
        val current = decode() ?: return false
        val next = current.filterNot {
            it.language == language && it.phrase == phrase
        }
        return next.size < current.size && write(next)
    }

    /**
     * Portable TSV matches the strict self-authored import format byte-for-byte.
     * Only user notes are included; no transcripts, model outputs or cloud calls.
     */
    @Synchronized
    fun exportTsvSnapshot(): ByteArray? {
        val saved = entries() ?: return null
        if (saved.isEmpty()) return null
        val text = buildString {
            append("language\tphrase\tmeaningLanguage\townMeaning\n")
            saved.forEach { row ->
                append(row.language).append('\t')
                append(row.phrase).append('\t')
                append(row.meaningLanguage).append('\t')
                append(row.ownMeaning).append('\n')
            }
        }
        return text.toByteArray(Charsets.UTF_8).takeIf { bytes ->
            bytes.size <= MAX_JSON && previewTsvImport(bytes) == saved
        }
    }

    /** User-chosen Storage Access Framework export; never background/silent. */
    @Synchronized
    fun exportSnapshot(): ByteArray? {
        val saved = decode() ?: return null
        if (saved.isEmpty()) return null
        val document = JSONObject()
            .put("format", "tuddy-private-phrases-v1")
            .put("entries", toJson(saved))
        val bytes = (document.toString() + "\n").toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= MAX_JSON }
    }

    /** Strict UTF-8, version, schema and duplicate checking before import UI. */
    fun previewImport(bytes: ByteArray): List<PersonalPhrase>? {
        if (bytes.isEmpty() || bytes.size > MAX_JSON) return null
        val source = bytes.toString(Charsets.UTF_8)
        if (!source.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return null
        return try {
            val root = JSONObject(source)
            if (root.length() != 2 ||
                root.opt("format") != "tuddy-private-phrases-v1" ||
                root.opt("entries") !is JSONArray) return null
            val records = parseEntries(root.getJSONArray("entries")) ?: return null
            records.takeIf { it.isNotEmpty() }
        } catch (_: Exception) { null }
    }

    /**
     * Mandatory header, user-written text only, no language inference.
     * UTF-8 TSV rows: language, phrase, meaningLanguage, ownMeaning.
     * Parsing only; UI controls the separate merge/replace confirmation.
     */
    fun previewTsvImport(bytes: ByteArray): List<PersonalPhrase>? {
        if (bytes.isEmpty() || bytes.size > MAX_JSON) return null
        val decoded = bytes.toString(Charsets.UTF_8)
        if (!decoded.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return null
        val lines = decoded.removePrefix("\uFEFF").split('\n')
            .dropLastWhile { it.isEmpty() }
        if (lines.size !in 2..(MAX_ROWS + 1) ||
            lines.first().removeSuffix("\r") !=
                "language\tphrase\tmeaningLanguage\townMeaning"
        ) return null

        val seen = HashSet<String>()
        val result = ArrayList<PersonalPhrase>()
        for (line in lines.drop(1)) {
            val fields = line.removeSuffix("\r").split('\t')
            if (fields.size != 4) return null
            val row = PersonalPhrase(
                fields[0], fields[1], fields[2], fields[3]
            )
            val id = row.language + "|" + row.phrase.lowercase(Locale.ROOT)
            if (!valid(row) || !seen.add(id)) return null
            result.add(row)
        }
        return result.takeIf { it.isNotEmpty() }
    }

    /**
     * Explicit, non-destructive merge of a user-selected note backup.
     * A local entry always wins on duplicate language+phrase identity; imported
     * meanings cannot silently overwrite personal edits. Either the whole
     * merged deck is committed or nothing changes (e.g. capacity exceeded).
     */
    @Synchronized
    fun mergeImported(records: List<PersonalPhrase>): Boolean {
        if (records.isEmpty() || records.size > MAX_ROWS || records.any { !valid(it) })
            return false
        fun identity(record: PersonalPhrase) =
            record.language + "|" + record.phrase.lowercase(Locale.ROOT)
        if (records.map(::identity).toSet().size != records.size) return false
        val current = decode() ?: return false
        val owned = current.map(::identity).toSet()
        val additions = records.filterNot { identity(it) in owned }
        if (current.size + additions.size > MAX_ROWS) return false
        return if (additions.isEmpty()) true else write(current + additions)
    }

    /** Runs only after a separate, visible replacement confirmation. */
    @Synchronized
    fun replaceImported(records: List<PersonalPhrase>): Boolean = write(records, resetReview = true)

    @Synchronized
    fun clear(): Boolean = try {
        prefs.edit().remove(KEY).remove(PrivatePhraseShortcuts.KEY)
            .remove(REVIEW_OPT_IN).remove(REVIEW_ROWS).commit()
    }
    catch (_: RuntimeException) { false }
}
