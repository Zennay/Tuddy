package com.livelingo.android

import android.content.Context
import java.util.Locale

/**
 * Explicit user-selected local TTS preference, stored privately per language.
 * The preferred voice name is only a device-local choice; no speech content is saved.
 */
internal object LocalSpeechVoicePreferences {
    private const val PREFERENCES = "tuddy_local_tts_voice_preferences"
    private const val VOICE_PREFIX = "voice."

    fun selectedVoiceName(context: Context, languageTag: String): String? {
        val language = languageKey(languageTag) ?: return null
        return context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(VOICE_PREFIX + language, null)
    }

    fun selectVoice(context: Context, languageTag: String, voiceName: String): Boolean {
        val language = languageKey(languageTag) ?: return false
        if (voiceName.isBlank() || voiceName.length > 256) return false
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(VOICE_PREFIX + language, voiceName)
            .apply()
        return true
    }

    fun clearVoice(context: Context, languageTag: String): Boolean {
        val language = languageKey(languageTag) ?: return false
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(VOICE_PREFIX + language)
            .apply()
        return true
    }

    fun clearAll(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun languageKey(languageTag: String): String? {
        if (!Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$").matches(languageTag)) return null
        return Locale.forLanguageTag(languageTag).language
            .takeIf { it.isNotBlank() && it != "und" }
    }
}
