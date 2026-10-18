package com.livelingo.android

import android.content.Context
import com.livelingo.android.model.GlossaryEntry
import com.livelingo.android.model.LocalGlossary
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

internal class PrivateVocabularyDeck(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "tuddy_private_vocabulary_v1", Context.MODE_PRIVATE
    )

    private companion object {
        const val ENTRIES = "saved_curated_word_keys"
        const val MAX_ENTRIES = 120
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
        return preferences.edit().putStringSet(ENTRIES, current - key).commit()
    }

    @Synchronized
    fun clear(): Boolean = preferences.edit().remove(ENTRIES).commit()
}
