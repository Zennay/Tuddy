package com.livelingo.android

/**
 * The archived LiveLingo v0.4 remembered a target language. The modern
 * Tuddy screen forgot both dropdowns on process restart.
 *
 * Keep the on-disk surface to two reviewed language identifiers ONLY.
 * Never persist microphone bytes, subtitles, word lookups, prompts, API keys,
 * language-detection observations or ML Kit download claims.
 *
 * The returned identifiers must come from the actual current picker catalog,
 * not from an old preference or externally provided value.
 */
internal object PrivateLanguageSelectionPolicy {
    const val SETTINGS_NAME = "tuddy-language-selection"
    const val SOURCE_KEY = "spoken-source-tag"
    const val TARGET_KEY = "translation-target-code"

    fun restoreSource(raw: String?, supported: Collection<String>, fallback: String): String =
        if (raw != null && raw in supported) raw else fallback

    fun restoreTarget(raw: String?, supported: Collection<String>, fallback: String): String =
        if (raw != null && raw in supported) raw else fallback
}
