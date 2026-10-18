package com.livelingo.android

import android.content.Context
import com.livelingo.android.model.GlossaryEntry
import com.livelingo.android.model.LocalGlossary
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Explicitly saved, app-private, offline vocabulary. Never stores a transcript,
 * source sentence, provider output, user identity, recording, or definition.
 * Every persisted word must still resolve in the curated local glossary.
 *
 * This is separate from the in-RAM conversation and opt-in transcript export.
 * AndroidManifest allowBackup=false prevents platform backup of this store.
 */
internal data class SavedWord(
    val language: String,
    val word: String,
    val entry: GlossaryEntry
)

/** Optional local review metadata for explicitly saved glossary words; never speech content. */
internal data class ScheduledWordReview(val saved: SavedWord, val dueDay: Long, val stage: Int)

internal class PrivateVocabularyDeck(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "tuddy_private_vocabulary_v1", Context.MODE_PRIVATE
    )

    private companion object {
        const val ENTRIES = "saved_curated_word_keys"
        const val REVIEW_ON = "scheduled_review_opt_in"
        const val REVIEW_ROWS = "scheduled_review_rows_v1"
        const val MAX_REVIEW_STAGE = 5
        val REVIEW_INTERVAL_DAYS = listOf(1L, 2L, 4L, 7L, 14L, 30L)
        const val MAX_ENTRIES = 120
        const val MAX_BACKUP_BYTES = 16_384
        val CODES = setOf("nl", "en", "de", "fr", "es")
    }

    private fun canonical(language: String, word: String): String? {
        val code = language.trim().lowercase(Locale.ROOT).replace('_', '-').substringBefore('-')
        val normalized = word.trim().trim('\'', '’', '-').lowercase(Locale.ROOT)
        if (code !in CODES || normalized.isEmpty() || normalized.length > 40 ||
            normalized.contains('|') || LocalGlossary.lookup(code, normalized) == null) return null
        return "$code|$normalized"
    }

    private fun valid(raw: String): SavedWord? {
        if (raw.length > 48 || raw.count { it == '|' } != 1) return null
        val code = raw.substringBefore('|')
        val word = raw.substringAfter('|')
        if (canonical(code, word) != raw) return null
        return SavedWord(code, word, LocalGlossary.lookup(code, word) ?: return null)
    }

    private fun stored(): Set<String> = try {
        // Never modify a preferences-backed Set in place.
        preferences.getStringSet(ENTRIES, emptySet())?.toSet().orEmpty()
            .filterTo(mutableSetOf()) { valid(it) != null }
    } catch (_: RuntimeException) {
        emptySet()
    }


    /** Each stored row is language|word|stage|epoch-day, never a definition or transcript.
     * An invalid row is ignored; it is never trusted as an arbitrary saved-word key. */
    private fun reviewRows(): Map<String, Pair<Int, Long>> {
        val raw = try {
            preferences.getStringSet(REVIEW_ROWS, emptySet())?.toSet().orEmpty()
        } catch (_: RuntimeException) { return emptyMap() }
        if (raw.size > MAX_ENTRIES) return emptyMap()
        return raw.mapNotNull { row ->
            val parts = row.split('|')
            if (parts.size != 4) return@mapNotNull null
            val key = "${parts[0]}|${parts[1]}"
            val stage = parts[2].toIntOrNull() ?: return@mapNotNull null
            val due = parts[3].toLongOrNull() ?: return@mapNotNull null
            if (valid(key) == null || stage !in 0..MAX_REVIEW_STAGE ||
                due !in 0L..365000L) return@mapNotNull null
            key to (stage to due)
        }.toMap()
    }

    private fun encodedReviewRows(rows: Map<String, Pair<Int, Long>>): Set<String> =
        rows.map { (key, value) -> "$key|${value.first}|${value.second}" }.toSet()

    @Synchronized
    fun reviewsEnabled(): Boolean =
        try { preferences.getBoolean(REVIEW_ON, false) } catch (_: RuntimeException) { false }

    /** The user opts in separately from saving a word. No timers, notifications or network. */
    @Synchronized
    fun enableReviews(): Boolean =
        preferences.edit().putBoolean(REVIEW_ON, true).commit()

    /** Erase the complete optional review history without touching saved words. */
    @Synchronized
    fun disableReviews(): Boolean =
        preferences.edit().remove(REVIEW_ON).remove(REVIEW_ROWS).commit()

    @Synchronized
    fun scheduledReviews(): List<ScheduledWordReview> {
        if (!reviewsEnabled()) return emptyList()
        val rows = reviewRows()
        val today = java.time.LocalDate.now().toEpochDay()
        return entries().map { word ->
            val memory = rows["${word.language}|${word.word}"]
            ScheduledWordReview(word, memory?.second ?: today, memory?.first ?: 0)
        }.sortedWith(compareBy(ScheduledWordReview::dueDay,
            { it.saved.language }, { it.saved.word }))
    }

    /**
     * Manual reveal + self-assessment only. Hard cards return tomorrow; remembered
     * cards progressively return after 2/4/7/14/30 days. No skill claims.
     */
    @Synchronized
    fun rateScheduledWord(language: String, word: String, remembered: Boolean): Boolean {
        if (!reviewsEnabled()) return false
        val key = canonical(language, word) ?: return false
        if (key !in stored()) return false
        val rows = reviewRows().filterKeys { it in stored() }.toMutableMap()
        val stage = if (remembered)
            ((rows[key]?.first ?: 0) + 1).coerceAtMost(MAX_REVIEW_STAGE) else 0
        val due = java.time.LocalDate.now().plusDays(REVIEW_INTERVAL_DAYS[stage]).toEpochDay()
        rows[key] = stage to due
        return preferences.edit().putStringSet(REVIEW_ROWS, encodedReviewRows(rows)).commit()
    }

    @Synchronized
    fun entries(): List<SavedWord> =
        stored().mapNotNull(::valid).sortedWith(compareBy(SavedWord::language, SavedWord::word))

    @Synchronized
    fun contains(language: String, word: String): Boolean =
        canonical(language, word)?.let { it in stored() } ?: false

    @Synchronized
    fun save(language: String, word: String): Boolean {
        val key = canonical(language, word) ?: return false
        val current = stored()
        if (key in current) return true
        if (current.size >= MAX_ENTRIES) return false
        return preferences.edit().putStringSet(ENTRIES, current + key).commit()
    }

    @Synchronized
    fun remove(language: String, word: String): Boolean {
        val key = canonical(language, word) ?: return false
        val current = stored()
        if (key !in current) return false
        return preferences.edit()
            .putStringSet(ENTRIES, current - key)
            .putStringSet(REVIEW_ROWS, encodedReviewRows(reviewRows() - key))
            .commit()
    }

    /**
     * Back up only canonical glossary keys: no definitions, session sentences,
     * user identity, speech, audio or provider-generated language are included.
     * The caller decides if and where Android should create a document.
     */
    @Synchronized
    fun exportSnapshot(): ByteArray? {
        val keys = stored().sorted()
        if (keys.isEmpty() || keys.size > MAX_ENTRIES) return null
        val root = JSONObject()
            .put("format", "tuddy-curated-word-keys-v1")
            .put("keys", JSONArray(keys))
        val bytes = (root.toString() + "\n").toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= MAX_BACKUP_BYTES }
    }

    /**
     * Entire external backup is admitted before replacement is offered.
     * Backed-up keys must still belong to the app's curated local glossary.
     */
    fun previewImport(bytes: ByteArray): List<String>? {
        if (bytes.isEmpty() || bytes.size > MAX_BACKUP_BYTES) return null
        val source = bytes.toString(Charsets.UTF_8)
        if (!source.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return null
        return try {
            val root = JSONObject(source)
            if (root.length() != 2 ||
                root.opt("format") != "tuddy-curated-word-keys-v1" ||
                root.opt("keys") !is JSONArray) return null
            val array = root.getJSONArray("keys")
            if (array.length() !in 1..MAX_ENTRIES) return null
            val seen = HashSet<String>(array.length())
            for (index in 0 until array.length()) {
                val key = array.opt(index) as? String ?: return null
                if (valid(key) == null || !seen.add(key)) return null
            }
            seen.sorted()
        } catch (_: Exception) {
            null
        }
    }

    /** Replace only after the UI explicitly shows count and asks confirmation. */
    @Synchronized
    fun replaceImported(keys: List<String>): Boolean {
        if (keys.isEmpty() || keys.size > MAX_ENTRIES ||
            keys.any { valid(it) == null } || keys.toSet().size != keys.size) return false
        return try {
            preferences.edit().putStringSet(ENTRIES, keys.toSet())
                .remove(REVIEW_ROWS).commit()
        } catch (_: RuntimeException) {
            false
        }
    }

    @Synchronized
    fun clear(): Boolean = preferences.edit()
        .remove(ENTRIES).remove(REVIEW_ROWS).remove(REVIEW_ON).commit()
}
