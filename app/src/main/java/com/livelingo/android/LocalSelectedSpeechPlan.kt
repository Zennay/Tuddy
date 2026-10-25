package com.livelingo.android

/**
 * RAM-only speech segmentation for Android's selected-text translator.
 *
 * LocalSelectedParagraphPlan already validates up to 8,000 UTF-16 units,
 * printable content and correct surrogate pairs. Here we further split its
 * bounded chunks for Android's per-utterance TTS admission limit, preserving
 * Unicode code point boundaries and original reading order. This class does
 * not own a TTS engine, clipboard, network or persistent state.
 */
internal object LocalSelectedSpeechPlan {
    private const val MAX_UTTERANCE_UNITS = 360
    private const val MAX_UTTERANCES = 100

    fun fromSelection(raw: String): List<String>? {
        val approved = LocalSelectedParagraphPlan.fromSelection(raw) ?: return null
        val lines = mutableListOf<String>()
        for (paragraph in approved) {
            val clean = paragraph.text
                .replace(Regex("\\s+"), " ")
                .trim()
            if (clean.isBlank()) continue
            var cursor = 0
            while (cursor < clean.length) {
                // Ignore boundary whitespace before a new voice chunk.
                while (cursor < clean.length && clean[cursor].isWhitespace()) cursor++
                if (cursor == clean.length) break
                var end = (cursor + MAX_UTTERANCE_UNITS).coerceAtMost(clean.length)
                if (end < clean.length) {
                    // Prefer sentence or word boundaries, provided the chunk
                    // isn't extremely short. Never split UTF-16 surrogate pairs.
                    val minimum = cursor + (MAX_UTTERANCE_UNITS / 2)
                    val spaceAt = (end downTo minimum)
                        .firstOrNull { clean[it - 1].isWhitespace() }
                    if (spaceAt != null) end = spaceAt
                    if (end < clean.length && end > cursor &&
                        Character.isHighSurrogate(clean[end - 1]) &&
                        Character.isLowSurrogate(clean[end])) end--
                }
                if (end <= cursor) return null
                val piece = clean.substring(cursor, end).trim()
                if (piece.isNotEmpty()) {
                    if (piece.length > MAX_UTTERANCE_UNITS ||
                        piece.any { it.isISOControl() || it == '\u0000' }
                    ) return null
                    lines.add(piece)
                    if (lines.size > MAX_UTTERANCES) return null
                }
                cursor = end
            }
        }
        return lines.takeIf { it.isNotEmpty() }
    }
}
