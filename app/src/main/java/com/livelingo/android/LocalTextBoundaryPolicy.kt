package com.livelingo.android

/**
 * Shared text-only boundary for strings supplied by Android recognition,
 * local ML Kit translation and the in-app typed-caption fallback.
 *
 * Do not silently truncate a word, code point or user meaning. Reject
 * malformed UTF-16, unsafe control characters and explicit bidirectional
 * overrides before the text enters a live caption, speaker or export flow.
 * Ordinary RTL scripts, joining marks and isolated directional marks remain
 * intact: this boundary is not a language detector or a native review.
 */
internal object LocalTextBoundaryPolicy {
    const val MAX_SPEECH_UNITS = 1_500
    const val MAX_TYPED_UNITS = 1_500
    const val MAX_CAPTION_TRANSLATION_UNITS = 6_000
    const val MAX_INTERIM_TRANSLATION_UNITS = 1_500
    const val MAX_SELECTED_TRANSLATION_UNITS = 8_000

    fun speechHypothesis(value: String): String? =
        normalize(value, MAX_SPEECH_UNITS, oneLine = true)

    fun typedCaption(value: String): String? =
        normalize(value, MAX_TYPED_UNITS, oneLine = false)

    fun captionTranslation(value: String): String? =
        normalize(value, MAX_CAPTION_TRANSLATION_UNITS, oneLine = false)

    fun interimTranslation(value: String): String? =
        normalize(value, MAX_INTERIM_TRANSLATION_UNITS, oneLine = true)

    fun selectedTranslation(value: String): String? =
        normalize(value, MAX_SELECTED_TRANSLATION_UNITS, oneLine = false)

    private fun normalize(raw: String, maxUnits: Int, oneLine: Boolean): String? {
        if (raw.isEmpty() || raw.length > maxUnits) return null
        var index = 0
        while (index < raw.length) {
            val ch = raw[index]
            when {
                Character.isLowSurrogate(ch) -> return null
                Character.isHighSurrogate(ch) -> {
                    if (index + 1 >= raw.length ||
                        !Character.isLowSurrogate(raw[index + 1])) return null
                    index += 2
                    continue
                }
                ch == '\u0000' -> return null
                ch.isISOControl() && ch != '\n' && ch != '\r' && ch != '\t' ->
                    return null
                // The old bidi override controls can visually reorder
                // what a caption/export appears to contain. Do not strip
                // them and accidentally alter a user's intended meaning.
                ch in '\u202A'..'\u202E' -> return null
            }
            index++
        }
        val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
            .let { if (oneLine) it.replace('\n', ' ').replace('\t', ' ') else it }
            .trim()
        return normalized.takeIf { it.isNotBlank() && it.length <= maxUnits }
    }
}
