package com.livelingo.android

import android.content.Context
import java.util.Locale

/**
 * Explicit, user-defined local themes for personally authored phrase notes.
 *
 * The phrase deck stores the actual user text. This index only persists
 * SHA-256 phrase identities with one bounded theme label; never a transcript,
 * machine translation, model output, account identifier or audio.
 */
internal class PrivatePhraseTopics(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "tuddy_private_phrases_v1", Context.MODE_PRIVATE
    )
    private val deck = PrivatePhraseDeck(context)

    companion object {
        const val TOPICS_KEY = "user_phrase_topics_v1"
        const val ASSIGNMENTS_KEY = "user_phrase_topic_assignments_v1"
        const val MAX_TOPICS = 12
        const val MAX_PHRASES = 80
        private const val MAX_NAME_LENGTH = 30
    }

    private fun validName(value: String): Boolean =
        value.length in 1..MAX_NAME_LENGTH && value == value.trim() &&
            value.none { character ->
                character == '|' || Character.isISOControl(character) ||
                    Character.isSurrogate(character) ||
                    (Character.getType(character) == Character.FORMAT.toInt() &&
                        character != '\u200C')
            }

    private fun readTopics(): Set<String>? = try {
        prefs.getStringSet(TOPICS_KEY, emptySet())?.toSet()?.takeIf { values ->
            values.size <= MAX_TOPICS && values.all(::validName) &&
                values.map { it.lowercase(Locale.ROOT) }.toSet().size == values.size
        }
    } catch (_: RuntimeException) { null }

    /**
     * Each row is fingerprint|theme; malformed or duplicate records are
     * refused rather than letting stale data silently change topic ownership.
     */
    private fun readAssignments(): Map<String, String>? {
        return try {
        val rows = prefs.getStringSet(ASSIGNMENTS_KEY, emptySet())?.toSet()
            ?: return null
        if (rows.size > MAX_PHRASES) return null
        val parsed = rows.map { raw ->
            val separator = raw.indexOf('|')
            if (separator != 64 || raw.indexOf('|', separator + 1) != -1) return null
            val fingerprint = raw.substring(0, separator)
            val topic = raw.substring(separator + 1)
            if (fingerprint.any { it !in "0123456789abcdef" } ||
                !validName(topic)) return null
            fingerprint to topic
        }
        if (parsed.map { it.first }.toSet().size != parsed.size) return null
        parsed.toMap()
        } catch (_: RuntimeException) { null }
    }

    private fun rows(assignments: Map<String, String>): Set<String> =
        assignments.map { (identity, topic) -> "$identity|$topic" }.toSet()

    @Synchronized
    fun topics(): List<String> = readTopics()?.sortedWith(String.CASE_INSENSITIVE_ORDER)
        ?: emptyList()

    @Synchronized
    fun topicFor(note: PersonalPhrase): String? {
        val topics = readTopics() ?: return null
        val assignments = readAssignments() ?: return null
        return assignments[PrivatePhraseShortcuts.fingerprint(note)]?.takeIf { it in topics }
    }

    @Synchronized
    fun counts(notes: List<PersonalPhrase>): Map<String, Int> {
        val topics = readTopics() ?: return emptyMap()
        val assignments = readAssignments() ?: return emptyMap()
        return notes.mapNotNull { phrase ->
            assignments[PrivatePhraseShortcuts.fingerprint(phrase)]?.takeIf { it in topics }
        }.groupingBy { it }.eachCount()
    }

    @Synchronized
    fun add(rawName: String): Boolean {
        val name = rawName.trim()
        val existing = readTopics() ?: return false
        if (!validName(name) || existing.size >= MAX_TOPICS ||
            existing.any { it.equals(name, ignoreCase = true) }) return false
        return prefs.edit().putStringSet(TOPICS_KEY, existing + name).commit()
    }

    @Synchronized
    fun rename(oldName: String, rawName: String): Boolean {
        val name = rawName.trim()
        val existing = readTopics() ?: return false
        val assignments = readAssignments() ?: return false
        if (oldName !in existing || !validName(name) ||
            existing.any { it != oldName && it.equals(name, ignoreCase = true) }) return false
        val renamed = assignments.mapValues { (_, topic) ->
            if (topic == oldName) name else topic
        }
        return prefs.edit()
            .putStringSet(TOPICS_KEY, existing - oldName + name)
            .putStringSet(ASSIGNMENTS_KEY, rows(renamed))
            .commit()
    }

    @Synchronized
    fun delete(name: String): Boolean {
        val existing = readTopics() ?: return false
        val assignments = readAssignments() ?: return false
        if (name !in existing) return false
        return prefs.edit()
            .putStringSet(TOPICS_KEY, existing - name)
            .putStringSet(ASSIGNMENTS_KEY, rows(assignments.filterValues { it != name }))
            .commit()
    }

    /** An assignment is made only for a note currently saved in this app. */
    @Synchronized
    fun assign(note: PersonalPhrase, topic: String?): Boolean {
        val topics = readTopics() ?: return false
        val assignments = readAssignments() ?: return false
        val notes = deck.entries() ?: return false
        if (note !in notes || (topic != null && topic !in topics)) return false
        val identity = PrivatePhraseShortcuts.fingerprint(note)
        val available = notes.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
        val next = assignments.filterKeys { it in available }.toMutableMap()
        if (topic == null) next.remove(identity) else next[identity] = topic
        if (next.size > MAX_PHRASES) return false
        return prefs.edit().putStringSet(ASSIGNMENTS_KEY, rows(next)).commit()
    }
}
