package com.livelingo.android

import android.content.Context
import java.security.MessageDigest

/**
 * Private shortcuts to consciously saved phrase notes. Only SHA-256 keys are
 * persisted here, never another copy of an authored sentence or its meaning.
 *
 * These are only local bookmarks, NOT vetted translations, usage analytics or
 * remote recommendations. The normal personal-phrase erase clears this store.
 */
internal class PrivatePhraseShortcuts(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "tuddy_private_phrases_v1", Context.MODE_PRIVATE
    )

    companion object {
        const val KEY = "favorite_phrase_hashes_v1"
        const val MAX_PINS = 12

        fun fingerprint(phrase: PersonalPhrase): String {
            val input = (phrase.language + "\u0000" + phrase.phrase)
                .toByteArray(Charsets.UTF_8)
            return MessageDigest.getInstance("SHA-256").digest(input)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }

    private fun read(): Set<String>? = try {
        prefs.getStringSet(KEY, emptySet())?.toSet()?.takeIf {
            it.size <= 160 && it.all { hash ->
                hash.length == 64 && hash.all { c -> c in "0123456789abcdef" }
            }
        }
    } catch (_: RuntimeException) { null }

    /** Ignore shortcuts to removed/renamed notes. Never expose old phrase text. */
    @Synchronized
    fun pinned(notes: List<PersonalPhrase>): Set<String> {
        val available = notes.map(::fingerprint).toSet()
        return read().orEmpty().intersect(available)
    }

    @Synchronized
    fun pinnedNotes(notes: List<PersonalPhrase>): Set<PersonalPhrase> {
        val hashes = pinned(notes)
        return notes.filterTo(mutableSetOf()) { fingerprint(it) in hashes }
    }

    @Synchronized
    fun isPinned(note: PersonalPhrase, notes: List<PersonalPhrase>): Boolean =
        fingerprint(note) in pinned(notes)

    /**
     * One explicit tap, no auto-favorites. Null is a refusal or storage error;
     * true/false reflects the resulting pinned state.
     */
    @Synchronized
    fun toggle(note: PersonalPhrase, notes: List<PersonalPhrase>): Boolean? {
        if (note !in notes) return null
        val existing = read() ?: return null
        val available = notes.map(::fingerprint).toSet()
        val live = existing.intersect(available)
        val hash = fingerprint(note)
        val next = if (hash in live) live - hash else live + hash
        if (next.size > MAX_PINS) return null
        return try {
            if (prefs.edit().putStringSet(KEY, next).commit()) hash in next else null
        } catch (_: RuntimeException) { null }
    }
}
