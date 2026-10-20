package com.livelingo.android

/**
 * A manual audio-reading aid. Even division of user-provided SRT/VTT cue
 * duration is NOT measured word timing or speech alignment.
 */
internal data class EstimatedCueWord(
    val span: IntRange,
    val index: Int,
    val count: Int,
    val estimatedStartMs: Long
)

internal object PrivateCueWordProgress {
    private const val MAX_WORDS = 300
    private const val MAX_CHARACTERS = 4096
    private val words = Regex("\\S+")

    private fun ranges(text: String): List<IntRange>? {
        if (text.isBlank() || text.length > MAX_CHARACTERS) return null
        val result = words.findAll(text).take(MAX_WORDS + 1).map { it.range }.toList()
        return result.takeIf { it.isNotEmpty() && it.size <= MAX_WORDS }
    }

    private fun startAt(startMs: Long, endMs: Long, index: Int, count: Int): Long =
        // Ceil to avoid navigating into the previous word's fractional window.
        startMs + kotlin.math.ceil(
            (endMs - startMs).toDouble() * index / count
        ).toLong()

    fun estimate(text: String, startMs: Long, endMs: Long, positionMs: Long): EstimatedCueWord? {
        if (startMs < 0L || endMs <= startMs ||
            positionMs < startMs || positionMs >= endMs) return null
        val tokens = ranges(text) ?: return null
        // No meaningful per-word seek slots for implausibly short cue windows.
        if (endMs - startMs < tokens.size) return null
        val share = ((positionMs - startMs).toDouble() /
            (endMs - startMs).toDouble()).coerceIn(0.0, 0.999999)
        val index = (share * tokens.size).toInt().coerceIn(0, tokens.lastIndex)
        return EstimatedCueWord(
            tokens[index], index, tokens.size, startAt(startMs, endMs, index, tokens.size)
        )
    }

    /** Seek by estimated word index, still only relying on the user's cue clock. */
    fun estimatedWordStart(text: String, startMs: Long, endMs: Long, index: Int): Long? {
        if (startMs < 0L || endMs <= startMs) return null
        val tokens = ranges(text) ?: return null
        if (endMs - startMs < tokens.size || index !in tokens.indices) return null
        return startAt(startMs, endMs, index, tokens.size)
    }

    fun estimatedSpan(
        text: String, startMs: Long, endMs: Long, positionMs: Long
    ): IntRange? = estimate(text, startMs, endMs, positionMs)?.span
}
