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

    // A bounded SRT/VTT cue cannot span days. Fail closed for impossible
    // arithmetic ranges rather than overflowing while mapping token positions.
    private fun safeWindow(startMs: Long, endMs: Long): Boolean =
        startMs >= 0L && endMs > startMs && endMs - startMs <= 86_400_000L &&
            endMs < Long.MAX_VALUE / MAX_WORDS

    private fun startAt(startMs: Long, endMs: Long, index: Int, count: Int): Long {
        // Exact integer ceilings keep seeks consistent with the highlighted
        // slot; unlike floating point, no rounding changes word boundaries.
        val duration = endMs - startMs
        val numerator = duration * index.toLong()
        return startMs + (numerator / count +
            if (numerator % count == 0L) 0L else 1L)
    }

    fun estimate(text: String, startMs: Long, endMs: Long, positionMs: Long): EstimatedCueWord? {
        if (!safeWindow(startMs, endMs) ||
            positionMs < startMs || positionMs >= endMs) return null
        val tokens = ranges(text) ?: return null
        // No meaningful per-word seek slots for implausibly short cue windows.
        if (endMs - startMs < tokens.size) return null
        val index = (((positionMs - startMs) * tokens.size) /
            (endMs - startMs)).toInt().coerceIn(0, tokens.lastIndex)
        return EstimatedCueWord(
            tokens[index], index, tokens.size, startAt(startMs, endMs, index, tokens.size)
        )
    }

    /** Seek by estimated word index, still only relying on the user's cue clock. */
    fun estimatedWordStart(text: String, startMs: Long, endMs: Long, index: Int): Long? {
        if (!safeWindow(startMs, endMs)) return null
        val tokens = ranges(text) ?: return null
        if (endMs - startMs < tokens.size || index !in tokens.indices) return null
        return startAt(startMs, endMs, index, tokens.size)
    }

    /**
     * A very short, explicitly requested replay region within the same authored
     * subtitle cue. Not a detected spoken word; reject windows under 750ms so
     * MediaPlayer seek/loop controls cannot pretend to give phoneme precision.
     */
    fun estimatedWordReplayWindow(
        text: String, startMs: Long, endMs: Long, index: Int
    ): Pair<Long, Long>? {
        if (!safeWindow(startMs, endMs)) return null
        val tokens = ranges(text) ?: return null
        if (index !in tokens.indices || endMs - startMs < tokens.size) return null
        val begin = startAt(startMs, endMs, index, tokens.size)
        val finish = if (index == tokens.lastIndex) endMs
            else startAt(startMs, endMs, index + 1, tokens.size)
        return (begin to finish).takeIf { (a, b) -> b > a && b - a >= 750L }
    }

    /** All explicitly selectable estimated positions for one visible cue. */
    fun choices(text: String, startMs: Long, endMs: Long): List<EstimatedCueWord>? {
        if (!safeWindow(startMs, endMs)) return null
        val tokens = ranges(text) ?: return null
        if (endMs - startMs < tokens.size) return null
        return tokens.mapIndexed { index, span ->
            EstimatedCueWord(
                span = span, index = index, count = tokens.size,
                estimatedStartMs = startAt(startMs, endMs, index, tokens.size)
            )
        }
    }

    fun estimatedSpan(
        text: String, startMs: Long, endMs: Long, positionMs: Long
    ): IntRange? = estimate(text, startMs, endMs, positionMs)?.span
}
