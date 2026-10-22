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

/** One user-owned, self-contained offline transfer of phrases and their themes. */
internal data class PrivatePhraseArchive(
    val notes: List<PersonalPhrase>,
    val topics: List<String>,
    val assignments: Map<String, String>
)

internal data class DuePersonalPhrase(val phrase: PersonalPhrase, val stage: Int, val dueDay: Long)

/** Exact app-private preference state of the personal phrasebook, held only in RAM. */
internal data class PrivatePhraseRollback(val entries: Map<String, Any>)

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

    private fun humanText(value: String, max: Int): Boolean =
        PrivatePhraseTextPolicy.valid(value, max)

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
        val validHashes = current.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
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
        val previous = decode().orEmpty().associateBy { PrivatePhraseShortcuts.fingerprint(it) }
        val allowedReviews = if (resetReview) emptyMap() else {
            reviewRows().filterKeys { hash ->
                val current = entries.firstOrNull {
                    PrivatePhraseShortcuts.fingerprint(it) == hash
                }
                current != null && previous[hash] == current
            }
        }
        val savedTopics = runCatching {
            prefs.getStringSet(PrivatePhraseTopics.ASSIGNMENTS_KEY, emptySet())
                ?.toSet().orEmpty()
        }.getOrDefault(emptySet())
        // A deleted or renamed phrase must never regain an old topic if an
        // identical phrase is imported much later. Export excludes topic data.
        val allowedTopics = if (resetReview) emptySet() else savedTopics.filterTo(mutableSetOf()) {
            row -> row.length > 65 && row.substring(0, 64) in allowedPins &&
                row[64] == '|'
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
                .putStringSet(PrivatePhraseTopics.ASSIGNMENTS_KEY, allowedTopics)
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
     * New-note UI must never silently replace a previously authored meaning.
     * The existence check and write share this synchronized transaction; other
     * explicit replacement/import routes keep their independent consent flow.
     */
    @Synchronized
    fun saveNew(language: String, phrase: String, meaningLanguage: String, ownMeaning: String): Boolean {
        val record = PersonalPhrase(language, phrase.trim(), meaningLanguage, ownMeaning.trim())
        if (!valid(record)) return false
        val current = decode() ?: return false
        if (current.any {
                it.language == record.language && it.phrase.equals(record.phrase, ignoreCase = true)
            }) return false
        return write(current + record)
    }

    /**
     * Explicit whole-batch adoption of user-authored study notes. There is
     * exactly one preferences transaction, never a loop of partial saves.
     * All notes must be new, valid and unique against the current deck.
     */
    @Synchronized
    fun saveNewBatch(batch: List<PersonalPhrase>): Boolean {
        if (batch.size !in 1..10 || batch.any { !valid(it) }) return false
        fun key(note: PersonalPhrase): String =
            note.language + "|" + note.phrase.lowercase(Locale.ROOT)
        val incoming = batch.map(::key)
        if (incoming.toSet().size != incoming.size) return false
        val current = decode() ?: return false
        if (current.size + batch.size > MAX_ROWS) return false
        val storedKeys = current.map(::key).toSet()
        if (incoming.any { it in storedKeys }) return false
        return write(current + batch)
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
     * Explicit bulk removal from a user-reviewed selection of immutable own
     * phrases. Unlike looping over remove(), this writes all affected phrases,
     * favorites, schedules and topic assignments atomically or not at all.
     * Prevents a stale dialog from deleting notes edited since its preview.
     */
    @Synchronized
    fun removeMany(snapshot: List<PersonalPhrase>): Boolean {
        if (snapshot.isEmpty() || snapshot.size > MAX_ROWS) return false
        val selected = snapshot.toSet()
        if (selected.size != snapshot.size) return false
        val current = decode() ?: return false
        if (!current.containsAll(selected)) return false
        val remaining = current.filterNot { it in selected }
        if (current.size - remaining.size != selected.size) return false
        return write(remaining)
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

    /**
     * Export only an explicitly previewed selection, never an implicit full deck.
     * A stale or modified selected note fails closed instead of silently
     * publishing a different entry. The returned bytes can be re-imported by
     * the ordinary strict TSV importer without inference or transformation.
     */
    @Synchronized
    fun exportTsvSelectionSnapshot(selection: List<PersonalPhrase>): ByteArray? {
        if (selection.isEmpty() || selection.size > MAX_ROWS ||
            selection.toSet().size != selection.size) return null
        val saved = entries() ?: return null
        if (!saved.containsAll(selection)) return null
        val selected = selection.toSet()
        val rows = saved.filter { it in selected }
        if (rows.size != selection.size) return null
        val text = buildString {
            append("language\tphrase\tmeaningLanguage\townMeaning\n")
            rows.forEach { note ->
                append(note.language).append('\t')
                append(note.phrase).append('\t')
                append(note.meaningLanguage).append('\t')
                append(note.ownMeaning).append('\n')
            }
        }
        return text.toByteArray(Charsets.UTF_8).takeIf { bytes ->
            bytes.size <= MAX_JSON && previewTsvImport(bytes) == rows
        }
    }

    /**
     * Export only a consciously selected subset as user-editable CSV; never
     * silently include the rest of a private phrase collection.
     * Round-trip through our strict CSV parser before offering document bytes.
     * No topics, review dates, favorites, transcripts or model output are included.
     */
    @Synchronized
    fun exportCsvSelectionSnapshot(selection: List<PersonalPhrase>): ByteArray? {
        if (selection.isEmpty() || selection.size > MAX_ROWS ||
            selection.toSet().size != selection.size) return null
        val saved = entries() ?: return null
        if (!saved.containsAll(selection)) return null
        val selected = selection.toSet()
        val rows = saved.filter { it in selected }
        if (rows.size != selection.size) return null

        fun csvCell(value: String): String =
            if (value.contains(',') || value.contains('"')) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else value
        val text = buildString {
            append("language,phrase,meaningLanguage,ownMeaning\n")
            rows.forEach { note ->
                append(csvCell(note.language)).append(',')
                append(csvCell(note.phrase)).append(',')
                append(csvCell(note.meaningLanguage)).append(',')
                append(csvCell(note.ownMeaning)).append('\n')
            }
        }
        return text.toByteArray(Charsets.UTF_8).takeIf { bytes ->
            bytes.size <= MAX_JSON && previewCsvImport(bytes) == rows
        }
    }

    private fun validArchiveTopic(value: String): Boolean =
        value.length in 1..30 && value == value.trim() &&
            !value.startsWith("__") && value.none { character ->
                character == '|' || Character.isISOControl(character) ||
                    Character.isSurrogate(character) ||
                    (Character.getType(character) == Character.FORMAT.toInt() &&
                        character != '\u200C')
            }

    private fun archiveObject(archive: PrivatePhraseArchive): JSONObject {
        val topics = JSONArray()
        archive.topics.forEach { topics.put(it) }
        val links = JSONArray()
        archive.assignments.toSortedMap().forEach { (id, name) ->
            links.put(JSONObject().put("identity", id).put("topic", name))
        }
        return JSONObject().put("format", "tuddy-private-phrase-archive-v1")
            .put("entries", toJson(archive.notes))
            .put("topics", topics).put("assignments", links)
    }

    /**
     * One deliberately requested portable snapshot; includes only previously
     * user-authored notes, theme labels and SHA-256 note identities. Favorites,
     * audio, transcripts and review schedules are never included.
     */
    @Synchronized
    fun exportArchiveSnapshot(context: Context): ByteArray? {
        val notes = decode() ?: return null
        if (notes.isEmpty()) return null
        val themes = PrivatePhraseTopics(context)
        val names = themes.topics()
        val existing = themes.exportSnapshot()
        if (names.isNotEmpty() && existing == null) return null
        val linked = if (existing == null) emptyMap() else {
            val root = runCatching {
                JSONObject(existing.toString(Charsets.UTF_8))
            }.getOrNull() ?: return null
            val array = root.optJSONArray("assignments") ?: return null
            val mapped = mutableMapOf<String, String>()
            for (i in 0 until array.length()) {
                val pair = array.optJSONObject(i) ?: return null
                mapped[pair.optString("identity")] = pair.optString("topic")
            }
            mapped
        }
        val snapshot = PrivatePhraseArchive(notes, names, linked)
        val bytes = (archiveObject(snapshot).toString() + "\n")
            .toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= 70_000 && previewArchiveImport(it) == snapshot }
    }

    /** Parse an entire portable archive without changing local preferences. */
    fun previewArchiveImport(bytes: ByteArray): PrivatePhraseArchive? {
        if (bytes.isEmpty() || bytes.size > 70_000) return null
        val decoded = bytes.toString(Charsets.UTF_8)
        if (!decoded.toByteArray(Charsets.UTF_8).contentEquals(bytes) ||
            !StrictJsonKeyGuard.accepts(decoded)) return null
        return try {
            val root = JSONObject(decoded)
            if (root.length() != 4 ||
                root.opt("format") != "tuddy-private-phrase-archive-v1" ||
                root.opt("entries") !is JSONArray ||
                root.opt("topics") !is JSONArray ||
                root.opt("assignments") !is JSONArray) return null
            val notes = parseEntries(root.getJSONArray("entries")) ?: return null
            if (notes.isEmpty()) return null
            val rawNames = root.getJSONArray("topics")
            if (rawNames.length() > PrivatePhraseTopics.MAX_TOPICS) return null
            val names = mutableListOf<String>()
            for (i in 0 until rawNames.length()) {
                val name = rawNames.opt(i) as? String ?: return null
                if (!validArchiveTopic(name) ||
                    names.any { it.equals(name, ignoreCase = true) }) return null
                names.add(name)
            }
            val hashes = notes.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
            if (hashes.size != notes.size) return null
            val rawLinks = root.getJSONArray("assignments")
            if (rawLinks.length() > MAX_ROWS) return null
            val linked = mutableMapOf<String, String>()
            for (i in 0 until rawLinks.length()) {
                val row = rawLinks.optJSONObject(i) ?: return null
                if (row.length() != 2 ||
                    row.opt("identity") !is String ||
                    row.opt("topic") !is String) return null
                val hash = row.getString("identity")
                val topic = row.getString("topic")
                if (hash.length != 64 ||
                    hash.any { it !in "0123456789abcdef" } ||
                    hash !in hashes || topic !in names || hash in linked) return null
                linked[hash] = topic
            }
            PrivatePhraseArchive(notes, names, linked)
        } catch (_: Exception) { null }
    }

    /**
     * One atomic replacement for a user-approved file. A stale preview, newer
     * local phrase, changed theme or malformed payload refuses the whole write.
     * Bookmarks and schedules are cleared (not misattributed to new meanings).
     */
    @Synchronized
    fun replaceArchiveIfUnchanged(
        context: Context,
        candidate: PrivatePhraseArchive,
        expectedNotes: List<PersonalPhrase>?,
        expectedThemes: ByteArray?
    ): Boolean {
        if (expectedNotes == null || entries() != expectedNotes) return false
        val currentThemes = PrivatePhraseTopics(context).exportSnapshot()
        if ((currentThemes == null) != (expectedThemes == null) ||
            currentThemes != null && expectedThemes != null &&
            !currentThemes.contentEquals(expectedThemes)) return false
        val archiveBytes = (archiveObject(candidate).toString() + "\n")
            .toByteArray(Charsets.UTF_8)
        if (archiveBytes.size > 70_000 ||
            previewArchiveImport(archiveBytes) != candidate) return false
        val value = toJson(candidate.notes).toString()
        if (value.length > MAX_JSON) return false
        val assignments = candidate.assignments.map { (hash, topic) ->
            hash + "|" + topic
        }.toSet()
        return try {
            prefs.edit().putString(KEY, value)
                .putStringSet(PrivatePhraseTopics.TOPICS_KEY, candidate.topics.toSet())
                .putStringSet(PrivatePhraseTopics.ASSIGNMENTS_KEY, assignments)
                .remove(PrivatePhraseShortcuts.KEY)
                .remove(REVIEW_OPT_IN).remove(REVIEW_ROWS)
                .commit()
        } catch (_: RuntimeException) { false }
    }

    /**
     * Non-destructive offline import. Existing user-written meanings, themes,
     * favorite pins and spaced-repetition dates win by default. Only unseen
     * language+phrase identities are added unless the user explicitly selected
     * existing exact-source notes for meaning correction in the complete preview.
     * Correcting a meaning resets only that card's due date, since the old
     * answer can no longer count as learned. Topic/favorite identities survive.
     * A single SharedPreferences transaction writes every change or none.
     */
    @Synchronized
    fun mergeArchiveIfUnchanged(
        context: Context,
        candidate: PrivatePhraseArchive,
        expectedNotes: List<PersonalPhrase>?,
        expectedThemes: ByteArray?,
        correctedIds: Set<String> = emptySet(),
        retopicIds: Set<String> = emptySet()
    ): Boolean {
        if (expectedNotes == null || entries() != expectedNotes) return false
        val themeDeck = PrivatePhraseTopics(context)
        val currentThemes = themeDeck.exportSnapshot()
        if ((currentThemes == null) != (expectedThemes == null) ||
            currentThemes != null && expectedThemes != null &&
            !currentThemes.contentEquals(expectedThemes)) return false

        // Reject any synthetic or stale caller input, not just files from the UI.
        val bytes = (archiveObject(candidate).toString() + "\n")
            .toByteArray(Charsets.UTF_8)
        if (bytes.size > 70_000 || previewArchiveImport(bytes) != candidate) return false
        val current = decode() ?: return false
        fun identity(note: PersonalPhrase) =
            note.language + "|" + note.phrase.lowercase(Locale.ROOT)
        val existingByIdentity = current.associateBy(::identity)
        if (existingByIdentity.size != current.size) return false
        val additions = candidate.notes.filterNot { identity(it) in existingByIdentity }
        val incomingByIdentity = candidate.notes.associateBy(::identity)
        if (incomingByIdentity.size != candidate.notes.size) return false

        // Fingerprints bind to the exact original source text, never a
        // case-insensitive approximate match. All opted-in corrections must
        // exist both locally and in the already validated candidate.
        val replacements = mutableMapOf<String, PersonalPhrase>()
        for (incoming in candidate.notes) {
            val id = PrivatePhraseShortcuts.fingerprint(incoming)
            if (id !in correctedIds) continue
            val currentNote = existingByIdentity[identity(incoming)] ?: return false
            if (currentNote.language != incoming.language ||
                currentNote.phrase != incoming.phrase ||
                currentNote == incoming || id in replacements) return false
            replacements[id] = incoming
        }
        if (replacements.keys != correctedIds || correctedIds.size > MAX_ROWS) return false

        val retopicNotes = candidate.notes.filter { note ->
            PrivatePhraseShortcuts.fingerprint(note) in retopicIds
        }
        if (retopicIds.size > MAX_ROWS || retopicNotes.size != retopicIds.size ||
            retopicNotes.any { note ->
                val old = existingByIdentity[identity(note)]
                old == null || old.language != note.language || old.phrase != note.phrase
            }) return false

        val combined = current.map { original ->
            replacements[PrivatePhraseShortcuts.fingerprint(original)] ?: original
        } + additions
        if (combined.size > MAX_ROWS) return false
        val serialized = toJson(combined).toString()
        if (serialized.length > MAX_JSON) return false

        // A topic with the same name ignoring case is one existing local topic.
        // Preserve its exact local spelling; never rename local categories.
        val localNames = themeDeck.topics()
        val canonical = mutableMapOf<String, String>()
        localNames.forEach { canonical[it.lowercase(Locale.ROOT)] = it }
        val names = localNames.toMutableList()
        // Only themes belonging to genuinely new notes are imported. A
        // meaning correction must never create/rename themes behind the user's
        // back; unrelated imported themes remain external until selected.
        val needed = (additions + retopicNotes).mapNotNull { note ->
            candidate.assignments[PrivatePhraseShortcuts.fingerprint(note)]
        }.toSet()
        candidate.topics.filter { it in needed }.forEach { name ->
            val key = name.lowercase(Locale.ROOT)
            if (key !in canonical) {
                if (names.size >= PrivatePhraseTopics.MAX_TOPICS) return false
                names.add(name)
                canonical[key] = name
            }
        }

        // Exports are generated from checked topic records, not free text.
        // Existing phrase-topic associations always have priority.
        val linked = mutableMapOf<String, String>()
        if (currentThemes != null) {
            val document = runCatching {
                JSONObject(currentThemes.toString(Charsets.UTF_8))
            }.getOrNull() ?: return false
            val records = document.optJSONArray("assignments") ?: return false
            for (i in 0 until records.length()) {
                val record = records.optJSONObject(i) ?: return false
                linked[record.optString("identity")] = record.optString("topic")
            }
        }
        for (note in additions) {
            val hash = PrivatePhraseShortcuts.fingerprint(note)
            val chosen = candidate.assignments[hash] ?: continue
            linked[hash] = canonical[chosen.lowercase(Locale.ROOT)] ?: return false
        }
        // A separate, deliberate per-row opt-in permits reclassification
        // without replacing the underlying note, bookmark or review stage.
        // An empty incoming theme means remove only this one assignment.
        for (note in retopicNotes) {
            val hash = PrivatePhraseShortcuts.fingerprint(note)
            val chosen = candidate.assignments[hash]
            if (chosen == null) linked.remove(hash)
            else linked[hash] = canonical[chosen.lowercase(Locale.ROOT)] ?: return false
        }
        if (linked.size > MAX_ROWS) return false
        val rows = linked.map { (hash, topic) -> "$hash|$topic" }.toSet()
        return try {
            val editor = prefs.edit().putString(KEY, serialized)
                .putStringSet(PrivatePhraseTopics.TOPICS_KEY, names.toSet())
                .putStringSet(PrivatePhraseTopics.ASSIGNMENTS_KEY, rows)
            if (replacements.isNotEmpty()) {
                // Hashes are based on source language + phrase, so bookmarks
                // remain stable, but due dates for corrected answers must reset.
                val remainingReviews = reviewRows().filterKeys { it !in correctedIds }
                editor.putStringSet(REVIEW_ROWS, encodeReviews(remainingReviews))
            }
            editor.commit()
        } catch (_: RuntimeException) { false }
    }

    /**
     * Capture exactly the six private-phrase storage fields. No generic app
     * settings, microphone permissions, transcripts or cloud state are read.
     * Used solely for a user-confirmed, single-step undo in the same screen.
     */
    @Synchronized
    fun captureRollback(): PrivatePhraseRollback? {
        if (decode() == null) return null
        return try {
            val stored = prefs.all
            val names = listOf(
                KEY, PrivatePhraseTopics.TOPICS_KEY,
                PrivatePhraseTopics.ASSIGNMENTS_KEY,
                PrivatePhraseShortcuts.KEY, REVIEW_OPT_IN, REVIEW_ROWS
            )
            val snapshot = mutableMapOf<String, Any>()
            for (name in names) {
                if (!stored.containsKey(name)) continue
                val raw = stored[name] ?: return null
                snapshot[name] = when (name) {
                    KEY -> raw as? String ?: return null
                    REVIEW_OPT_IN -> raw as? Boolean ?: return null
                    else -> {
                        val values = raw as? Set<*> ?: return null
                        if (values.any { it !is String }) return null
                        values.filterIsInstance<String>().toSet()
                    }
                }
            }
            PrivatePhraseRollback(snapshot.toMap())
        } catch (_: RuntimeException) { null }
    }

    /**
     * Reverts one CSV transaction only when every owned field still equals the
     * exact post-import snapshot. Never overwrites a newer note, theme, pin or
     * learning event. All old values are restored in one transaction.
     */
    @Synchronized
    fun undoLastCsvImportIfUnchanged(
        before: PrivatePhraseRollback,
        after: PrivatePhraseRollback
    ): Boolean {
        if (before == after || captureRollback() != after) return false
        val fields = listOf(
            KEY, PrivatePhraseTopics.TOPICS_KEY,
            PrivatePhraseTopics.ASSIGNMENTS_KEY,
            PrivatePhraseShortcuts.KEY, REVIEW_OPT_IN, REVIEW_ROWS
        )
        if (before.entries.keys.any { it !in fields }) return false
        return try {
            val editor = prefs.edit()
            for (name in fields) {
                val original = before.entries[name]
                if (original == null) {
                    editor.remove(name)
                } else when (name) {
                    KEY -> {
                        if (original !is String) return false
                        editor.putString(name, original)
                    }
                    REVIEW_OPT_IN -> {
                        if (original !is Boolean) return false
                        editor.putBoolean(name, original)
                    }
                    else -> {
                        val strings = original as? Set<*> ?: return false
                        if (strings.any { it !is String }) return false
                        editor.putStringSet(name, strings.filterIsInstance<String>().toSet())
                    }
                }
            }
            editor.commit()
        } catch (_: RuntimeException) { false }
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
        if (!source.toByteArray(Charsets.UTF_8).contentEquals(bytes) ||
            !StrictJsonKeyGuard.accepts(source)) return null
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
     * Import an explicitly chosen UTF-8 CSV list of self-authored sentence pairs.
     * Exactly four columns, exact header, RFC-style comma and quote escaping.
     * One record per line: multi-line quoted cells are intentionally unsupported
     * because the personal phrase text policy rejects controls and line breaks.
     * Parsing never writes; the calling UI shows every row and asks separately
     * whether to merge or replace existing private phrases.
     */
    fun previewCsvImport(bytes: ByteArray): List<PersonalPhrase>? {
        if (bytes.isEmpty() || bytes.size > MAX_JSON) return null
        val decoded = bytes.toString(Charsets.UTF_8)
        if (!decoded.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return null
        val lines = decoded.removePrefix("\uFEFF").split('\n')
            .dropLastWhile { it.isEmpty() }
            .map { it.removeSuffix("\r") }
        if (lines.size !in 2..(MAX_ROWS + 1) ||
            lines.first() != "language,phrase,meaningLanguage,ownMeaning") return null

        fun fields(line: String): List<String>? {
            val result = mutableListOf<String>()
            var offset = 0
            while (true) {
                val cell = StringBuilder()
                if (offset < line.length && line[offset] == '"') {
                    offset++
                    var closed = false
                    while (offset < line.length) {
                        val ch = line[offset++]
                        if (ch == '"') {
                            if (offset < line.length && line[offset] == '"') {
                                cell.append('"')
                                offset++
                            } else {
                                closed = true
                                break
                            }
                        } else {
                            cell.append(ch)
                        }
                    }
                    if (!closed || offset < line.length && line[offset] != ',') return null
                } else {
                    while (offset < line.length && line[offset] != ',') {
                        val ch = line[offset++]
                        if (ch == '"') return null
                        cell.append(ch)
                    }
                }
                result.add(cell.toString())
                if (offset == line.length) break
                offset++ // Consume exactly one comma.
                if (offset == line.length) return null // No silently ignored fifth empty column.
            }
            return result
        }

        val seen = HashSet<String>()
        val result = ArrayList<PersonalPhrase>()
        for (line in lines.drop(1)) {
            val values = fields(line) ?: return null
            if (values.size != 4) return null
            val record = PersonalPhrase(values[0], values[1], values[2], values[3])
            val identity = record.language + "|" + record.phrase.lowercase(Locale.ROOT)
            if (!valid(record) || !seen.add(identity)) return null
            result.add(record)
        }
        return result.takeIf { it.isNotEmpty() }
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

    /**
     * An old import dialog may outlive another local edit. Only replace the
     * precise original notes that were present when the user saw the preview;
     * nullable expected entries also allow explicit recovery from unreadable
     * legacy/corrupt storage when that state has not become valid meanwhile.
     */
    @Synchronized
    fun replaceImportedIfUnchanged(
        records: List<PersonalPhrase>,
        expectedOriginal: List<PersonalPhrase>?
    ): Boolean {
        if (decode() != expectedOriginal) return false
        return write(records, resetReview = true)
    }


    @Synchronized
    fun clear(): Boolean = try {
        prefs.edit().remove(KEY).remove(PrivatePhraseShortcuts.KEY)
            .remove(REVIEW_OPT_IN).remove(REVIEW_ROWS)
            .remove(PrivatePhraseTopics.ASSIGNMENTS_KEY)
            .remove(PrivatePhraseTopics.TOPICS_KEY).commit()
    }
    catch (_: RuntimeException) { false }
}
