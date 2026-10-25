package com.livelingo.android

/**
 * Bounded text-only segmentation for Android's explicit selected-text action.
 * Splits long selections into serial local ML Kit requests without ever
 * splitting a UTF-16 surrogate pair. User text stays in Activity RAM.
 *
 * The resulting translation keeps paragraph breaks, but cannot guarantee
 * typographic/semantic alignment between languages. Nothing is emitted
 * externally until every piece succeeds and the user explicitly exports it.
 */
internal object LocalSelectedParagraphPlan {
    const val MAX_SOURCE_UNITS = 8_000
    const val MAX_CHUNK_UNITS = 800
    const val MAX_CHUNKS = 24
    private const val PREFERRED_MIN_UNITS = 460

    data class Chunk(val text: String, val separatorBefore: String)

    fun fromSelection(raw: String): List<Chunk>? {
        if (raw.isBlank() || raw.length > MAX_SOURCE_UNITS) return null
        if (raw.any { it == '\u0000' || (it.isISOControl() && it != '\n' &&
                it != '\r' && it != '\t') }) return null
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (Character.isLowSurrogate(ch)) return null
            if (Character.isHighSurrogate(ch)) {
                if (i + 1 >= raw.length || !Character.isLowSurrogate(raw[i + 1])) return null
                i += 2
            } else i++
        }
        val clean = raw.replace("\r\n", "\n").replace('\r', '\n').trim()
        val chunks = mutableListOf<Chunk>()
        var cursor = 0
        var separator = ""
        while (cursor < clean.length) {
            val spacesStart = cursor
            while (cursor < clean.length && clean[cursor].isWhitespace()) cursor++
            if (cursor > spacesStart && chunks.isNotEmpty()) {
                val skipped = clean.substring(spacesStart, cursor)
                separator = if ('\n' in skipped) "\n".repeat(
                    skipped.count { it == '\n' }.coerceAtMost(2)
                ) else " "
            }
            if (cursor >= clean.length) break
            val maxEnd = (cursor + MAX_CHUNK_UNITS).coerceAtMost(clean.length)
            var end = maxEnd
            if (maxEnd < clean.length) {
                for (at in maxEnd - 1 downTo cursor + PREFERRED_MIN_UNITS) {
                    if (clean[at].isWhitespace()) {
                        end = at
                        break
                    }
                }
                // Avoid cutting in the middle of a surrogate pair for a
                // single overlong word with no whitespace.
                if (end > cursor && end < clean.length &&
                    Character.isHighSurrogate(clean[end - 1]) &&
                    Character.isLowSurrogate(clean[end])) end--
            }
            if (end <= cursor) return null
            val piece = clean.substring(cursor, end).trim()
            if (piece.isNotEmpty()) {
                chunks += Chunk(piece, if (chunks.isEmpty()) "" else separator)
                if (chunks.size > MAX_CHUNKS) return null
            }
            separator = " "
            cursor = end
        }
        return chunks.takeIf { it.isNotEmpty() }
    }
}
