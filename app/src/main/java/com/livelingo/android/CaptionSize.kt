package com.livelingo.android

/**
 * Presentation preference only. Persisting a size cannot persist or reveal
 * private speech, transcript, language content, or saved session progress.
 */
internal enum class CaptionSize(val label: String, val scale: Float) {
    NORMAL("Normaal", 1.0f),
    LARGE("Groot", 1.25f),
    EXTRA_LARGE("Extra groot", 1.5f);

    companion object {
        fun restore(raw: String?): CaptionSize =
            entries.firstOrNull { it.name == raw } ?: NORMAL
    }
}

internal object CaptionDisplayPreferences {
    const val NAME = "tuddy_caption_presentation_v1"
    const val SIZE_KEY = "caption_font_size"
}
