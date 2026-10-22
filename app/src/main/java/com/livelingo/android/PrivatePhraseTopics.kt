package com.livelingo.android

import android.content.Context
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Explicit, user-defined local themes for personally authored phrase notes.
 *
 * The phrase deck stores the actual user text. This index only persists
 * SHA-256 phrase identities with one bounded theme label; never a transcript,
 * machine translation, model output, account identifier or audio.
 */
/** Preview exists only in RAM until the user expressly confirms replacement. */
internal data class PrivatePhraseTopicBackup(
    val names: List<String>,
    val assignments: Map<String, String>
)

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
        const val MAX_BACKUP_BYTES = 16_384
        private const val BACKUP_FORMAT = "tuddy-private-phrase-topics-v1"
        private const val MAX_NAME_LENGTH = 30
    }

    private fun validName(value: String): Boolean =
        value.length in 1..MAX_NAME_LENGTH && value == value.trim() &&
            !value.startsWith("__") &&
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


    /** The caller chooses an external Android document. Do not include phrases. */
    @Synchronized
    fun exportSnapshot(): ByteArray? {
        val names = readTopics() ?: return null
        val assignments = readAssignments() ?: return null
        val stored = deck.entries() ?: return null
        if (names.isEmpty()) return null
        val live = stored.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
        val root = JSONObject().put("format", BACKUP_FORMAT)
        val namesArray = JSONArray()
        names.sortedWith(String.CASE_INSENSITIVE_ORDER).forEach { namesArray.put(it) }
        root.put("topics", namesArray)
        val pairs = JSONArray()
        assignments.toSortedMap().forEach { (id, name) ->
            if (id in live && name in names) pairs.put(
                JSONObject().put("identity", id).put("topic", name)
            )
        }
        root.put("assignments", pairs)
        val bytes = (root.toString() + "\n").toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= MAX_BACKUP_BYTES }
    }

    /**
     * Parse a chosen backup without changing local notes. A matching phrase
     * must already be restored before its topic assignment is eligible.
     */
    @Synchronized
    fun previewImport(bytes: ByteArray): PrivatePhraseTopicBackup? {
        if (bytes.isEmpty() || bytes.size > MAX_BACKUP_BYTES) return null
        val source = bytes.toString(Charsets.UTF_8)
        if (!source.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return null
        val notes = deck.entries() ?: return null
        val live = notes.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
        return try {
            val root = JSONObject(source)
            if (root.length() != 3 || root.opt("format") != BACKUP_FORMAT ||
                root.opt("topics") !is JSONArray ||
                root.opt("assignments") !is JSONArray) return null
            val topicsArray = root.getJSONArray("topics")
            val assignedArray = root.getJSONArray("assignments")
            if (topicsArray.length() !in 1..MAX_TOPICS ||
                assignedArray.length() > MAX_PHRASES) return null
            val names = mutableListOf<String>()
            for (i in 0 until topicsArray.length()) {
                val name = topicsArray.opt(i) as? String ?: return null
                if (!validName(name) ||
                    names.any { it.equals(name, ignoreCase = true) }) return null
                names.add(name)
            }
            val pairs = mutableMapOf<String, String>()
            for (i in 0 until assignedArray.length()) {
                val objectValue = assignedArray.opt(i) as? JSONObject ?: return null
                if (objectValue.length() != 2 ||
                    objectValue.opt("identity") !is String ||
                    objectValue.opt("topic") !is String) return null
                val id = objectValue.getString("identity")
                val name = objectValue.getString("topic")
                if (id.length != 64 || id.any { it !in "0123456789abcdef" } ||
                    id !in live || name !in names || id in pairs) return null
                pairs[id] = name
            }
            PrivatePhraseTopicBackup(names, pairs)
        } catch (_: Exception) { null }
    }


    /** Snapshot the user's actual notes when opening the restore preview. */
    @Synchronized
    fun notesSnapshot(): List<PersonalPhrase>? = deck.entries()?.toList()

    /**
     * Show every local note affected by a user-chosen topic backup, not just
     * counts or opaque hashes. This is transient in-app preview only.
     */
    @Synchronized
    fun previewAssignments(candidate: PrivatePhraseTopicBackup): List<Pair<PersonalPhrase, String>>? {
        val stored = deck.entries() ?: return null
        val indexed = stored.associateBy { PrivatePhraseShortcuts.fingerprint(it) }
        if (indexed.size != stored.size) return null
        if (candidate.assignments.any { (hash, topic) ->
                hash !in indexed || topic !in candidate.names
            }) return null
        return candidate.assignments.map { (hash, topic) ->
            indexed.getValue(hash) to topic
        }.sortedWith(compareBy({ it.second }, { it.first.language }, { it.first.phrase }))
    }

    /**
     * Reject a stale restore if either existing topic assignments or any
     * user-authored note changed since the complete preview was displayed.
     * This never overwrites a newer local topic edit behind the user's back.
     */
    @Synchronized
    fun replaceImportedIfUnchanged(
        candidate: PrivatePhraseTopicBackup,
        expectedTopics: ByteArray?,
        expectedNotes: List<PersonalPhrase>
    ): Boolean {
        val currentTopics = exportSnapshot()
        if (currentTopics == null && expectedTopics != null ||
            currentTopics != null && expectedTopics == null) return false
        if (currentTopics != null && expectedTopics != null &&
            !currentTopics.contentEquals(expectedTopics)) return false
        if (deck.entries() != expectedNotes) return false
        return replaceImported(candidate)
    }

    /** Replace only after the UI shows counts and a separate confirmation. */
    @Synchronized
    fun replaceImported(candidate: PrivatePhraseTopicBackup): Boolean {
        if (candidate.names.size !in 1..MAX_TOPICS ||
            candidate.names.any { !validName(it) } ||
            candidate.names.map { it.lowercase(Locale.ROOT) }.toSet().size !=
                candidate.names.size ||
            candidate.assignments.size > MAX_PHRASES) return false
        val stored = deck.entries() ?: return false
        val live = stored.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
        if (candidate.assignments.any { (id, topic) ->
                id.length != 64 || id.any { it !in "0123456789abcdef" } ||
                    id !in live || topic !in candidate.names
            }) return false
        return prefs.edit()
            .putStringSet(TOPICS_KEY, candidate.names.toSet())
            .putStringSet(ASSIGNMENTS_KEY, rows(candidate.assignments))
            .commit()
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

    /**
     * Deliberate consolidation of two locally authored topic groups.
     * Every source assignment moves to the existing destination in one
     * preferences transaction. No phrase text, favorites or due dates change.
     * Refuse a stale dialog preview rather than merging a newer edit.
     */
    @Synchronized
    fun mergeIfUnchanged(
        source: String,
        destination: String,
        expectedTopics: ByteArray,
        expectedNotes: List<PersonalPhrase>
    ): Boolean {
        if (source == destination) return false
        val before = exportSnapshot() ?: return false
        if (!before.contentEquals(expectedTopics) || deck.entries() != expectedNotes) {
            return false
        }
        val names = readTopics() ?: return false
        val assignments = readAssignments() ?: return false
        if (source !in names || destination !in names) return false
        val reassigned = assignments.mapValues { (_, topic) ->
            if (topic == source) destination else topic
        }
        return try {
            prefs.edit()
                .putStringSet(TOPICS_KEY, names - source)
                .putStringSet(ASSIGNMENTS_KEY, rows(reassigned))
                .commit()
        } catch (_: RuntimeException) { false }
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

    /**
     * Atomic, user-confirmed group reassignment of existing personal notes.
     * Never writes phrases or changes their meanings, bookmarks or due dates.
     * A stale UI snapshot or corrupt storage fails without partial writes.
     */
    @Synchronized
    fun assignMany(notes: List<PersonalPhrase>, topic: String?): Boolean {
        if (notes.isEmpty() || notes.size > MAX_PHRASES) return false
        val availableTopics = readTopics() ?: return false
        val assigned = readAssignments() ?: return false
        if (topic != null && topic !in availableTopics) return false
        val stored = deck.entries() ?: return false
        val byIdentity = stored.associateBy { PrivatePhraseShortcuts.fingerprint(it) }
        if (byIdentity.size != stored.size) return false
        val selected = notes.map { PrivatePhraseShortcuts.fingerprint(it) }
        if (selected.toSet().size != selected.size) return false
        // Equality includes the user's own meaning. Editing a phrase during a
        // long-open confirmation must not accidentally retarget another record.
        if (notes.any { byIdentity[PrivatePhraseShortcuts.fingerprint(it)] != it })
            return false
        val next = assigned.filterKeys { it in byIdentity }.toMutableMap()
        for (identity in selected) {
            if (topic == null) next.remove(identity) else next[identity] = topic
        }
        if (next.size > MAX_PHRASES) return false
        return try {
            prefs.edit().putStringSet(ASSIGNMENTS_KEY, rows(next)).commit()
        } catch (_: RuntimeException) { false }
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
