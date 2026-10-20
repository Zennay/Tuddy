package com.livelingo.android

/**
 * Optional visual reading aid. A cue's time span is divided evenly over its
 * written words, NOT actual spoken-word timing, ASR or forced alignment.
 * No words, recordings or positions leave memory.
 */
internal object PrivateCueWordProgress {
    private const val MAX_WORDS = 300
    private const val MAX_CHARACTERS = 4096
    private val words = Regex("\\S+")

    /** Inclusive UTF-16 word range; caller uses last + 1 for AnnotatedString. */
    fun estimatedSpan(
        text: String,
        startMs: Long,
        endMs: Long,
        positionMs: Long
    ): IntRange? {
        if (text.isBlank() || text.length > MAX_CHARACTERS ||
            startMs < 0L || endMs <= startMs ||
            positionMs < startMs || positionMs >= endMs
        ) return null
        val tokens = words.findAll(text).take(MAX_WORDS + 1).toList()
        if (tokens.isEmpty() || tokens.size > MAX_WORDS) return null
        // Double fraction avoids overflow on long timestamps.
        val progress = ((positionMs - startMs).toDouble() /
            (endMs - startMs).toDouble()).coerceIn(0.0, 0.999999)
        val index = (progress * tokens.size).toInt().coerceIn(0, tokens.lastIndex)
        return tokens[index].range
    }
}
