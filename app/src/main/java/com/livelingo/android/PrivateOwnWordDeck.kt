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

internal class PrivateOwnWordDeck(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "tuddy_own_word_notes_v1", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY = "own_word_notes"
        const val MAX_ENTRIES = 120
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

    private fun write(items: List<OwnWord>): Boolean {
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
        return try { prefs.edit().putString(KEY, json).commit() }
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

    @Synchronized
    fun clear(): Boolean = try { prefs.edit().remove(KEY).commit() }
    catch (_: RuntimeException) { false }
}
