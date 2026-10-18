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

internal class PrivatePhraseDeck(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "tuddy_private_phrases_v1", Context.MODE_PRIVATE
    )

    private companion object {
        const val KEY = "user_authored"
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

    private fun decode(): List<PersonalPhrase>? {
        val raw = try { prefs.getString(KEY, null) } catch (_: RuntimeException) { return null }
        if (raw == null) return emptyList()
        if (raw.length > MAX_JSON) return null
        return try {
            val array = JSONArray(raw)
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
            entries
        } catch (_: Exception) {
            null
        }
    }

    private fun write(entries: List<PersonalPhrase>): Boolean {
        if (entries.size > MAX_ROWS || entries.any { !valid(it) }) return false
        val array = JSONArray()
        for (entry in entries) {
            array.put(JSONObject()
                .put("language", entry.language)
                .put("phrase", entry.phrase)
                .put("meaningLanguage", entry.meaningLanguage)
                .put("ownMeaning", entry.ownMeaning))
        }
        val value = array.toString()
        if (value.length > MAX_JSON) return false
        return try { prefs.edit().putString(KEY, value).commit() }
        catch (_: RuntimeException) { false }
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

    @Synchronized
    fun remove(language: String, phrase: String): Boolean {
        val current = decode() ?: return false
        val next = current.filterNot {
            it.language == language && it.phrase == phrase
        }
        return next.size < current.size && write(next)
    }

    @Synchronized
    fun clear(): Boolean = try { prefs.edit().remove(KEY).commit() }
    catch (_: RuntimeException) { false }
}
