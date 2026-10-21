package com.livelingo.android

/**
 * A manual audio-reading aid. Even division of user-provided SRT/VTT cue
 * duration is NOT measured word timing or speech alignment. Optional marks
 * are manually tapped by the listener, never detected from the audio.
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
    private const val MAX_MANUAL_MARKS = 16
    // Preserve the complete Unicode White_Space set plus the explicitly
    // supported zero-width space (U+200B). Java regex \\s is ASCII-oriented
    // by default and would miss NBSP/narrow-NBSP and other Unicode separators.
    private fun isBoundary(codePoint: Int): Boolean =
        codePoint == 0x200B || codePoint == 0x0085 ||
            Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)

    private fun ranges(text: String): List<IntRange>? {
        if (text.isBlank() || text.length > MAX_CHARACTERS) return null
        val result = mutableListOf<IntRange>()
        var tokenStart = -1
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            if (isBoundary(codePoint)) {
                if (tokenStart >= 0) {
                    result += tokenStart..(offset - 1)
                    if (result.size > MAX_WORDS) return null
                    tokenStart = -1
                }
            } else if (tokenStart < 0) {
                tokenStart = offset
            }
            offset += Character.charCount(codePoint)
        }
        if (tokenStart >= 0) result += tokenStart..text.lastIndex
        return result.takeIf { it.isNotEmpty() && it.size <= MAX_WORDS }
    }

    private fun safeWindow(startMs: Long, endMs: Long): Boolean =
        startMs >= 0L && endMs > startMs && endMs - startMs <= 86_400_000L &&
            endMs < Long.MAX_VALUE / MAX_WORDS

    private fun startAt(startMs: Long, endMs: Long, index: Int, count: Int): Long {
        val duration = endMs - startMs
        val numerator = duration * index.toLong()
        return startMs + numerator / count +
            if (numerator % count == 0L) 0L else 1L
    }

    /**
     * Word zero is fixed to the user's cue start, and the right sentinel is
     * fixed to the cue end. Manual marks move only interior word boundaries.
     * Each segment is linearly interpolated in integer milliseconds.
     */
    private fun wordStarts(
        text: String, startMs: Long, endMs: Long,
        marks: Map<Int, Long>
    ): Pair<List<IntRange>, List<Long>>? {
        if (!safeWindow(startMs, endMs)) return null
        val tokens = ranges(text) ?: return null
        val count = tokens.size
        if (endMs - startMs < count || marks.size > MAX_MANUAL_MARKS ||
            marks.keys.any { it !in 1 until count }) return null
        val anchors = (listOf(0 to startMs) + marks.map { it.key to it.value } +
            listOf(count to endMs)).sortedBy { it.first }
        val starts = MutableList(count + 1) { startMs }
        for (i in 0 until anchors.lastIndex) {
            val (leftIndex, leftTime) = anchors[i]
            val (rightIndex, rightTime) = anchors[i + 1]
            val steps = rightIndex - leftIndex
            // At least one distinct millisecond per word. Reject crossings,
            // negative positions, and anchors that cannot fit in this cue.
            if (steps <= 0 || rightTime <= leftTime ||
                rightTime - leftTime < steps || leftTime < startMs ||
                rightTime > endMs) return null
            for (word in leftIndex..rightIndex) {
                starts[word] = startAt(leftTime, rightTime, word - leftIndex, steps)
            }
        }
        return tokens to starts
    }

    /** Returns a new bounded, strictly ordered set or rejects the entire edit. */
    fun withManualMark(
        text: String, startMs: Long, endMs: Long,
        current: Map<Int, Long>, wordIndex: Int, positionMs: Long
    ): Map<Int, Long>? {
        if (wordStarts(text, startMs, endMs, current) == null ||
            wordIndex <= 0 || positionMs <= startMs || positionMs >= endMs ||
            (wordIndex !in current && current.size >= MAX_MANUAL_MARKS)) return null
        val proposed = current + (wordIndex to positionMs)
        return proposed.takeIf { wordStarts(text, startMs, endMs, it) != null }
    }

    fun estimate(
        text: String, startMs: Long, endMs: Long, positionMs: Long,
        marks: Map<Int, Long> = emptyMap()
    ): EstimatedCueWord? {
        if (positionMs < startMs || positionMs >= endMs) return null
        val (tokens, starts) = wordStarts(text, startMs, endMs, marks) ?: return null
        val index = starts.indexOfLast { it <= positionMs }.coerceIn(0, tokens.lastIndex)
        return EstimatedCueWord(tokens[index], index, tokens.size, starts[index])
    }

    fun estimatedWordStart(
        text: String, startMs: Long, endMs: Long, index: Int,
        marks: Map<Int, Long> = emptyMap()
    ): Long? {
        val (tokens, starts) = wordStarts(text, startMs, endMs, marks) ?: return null
        if (index !in tokens.indices) return null
        return starts[index]
    }

    /**
     * An explicitly requested short word replay. It is still only a user
     * estimate from cue clocks and optional manual marks, not measured speech.
     */
    fun estimatedWordReplayWindow(
        text: String, startMs: Long, endMs: Long, index: Int,
        marks: Map<Int, Long> = emptyMap()
    ): Pair<Long, Long>? {
        val (tokens, starts) = wordStarts(text, startMs, endMs, marks) ?: return null
        if (index !in tokens.indices) return null
        return (starts[index] to starts[index + 1]).takeIf { (a, b) ->
            b - a >= 750L
        }
    }

    fun estimatedPhraseReplayWindow(
        text: String, startMs: Long, endMs: Long,
        firstIndex: Int, lastIndex: Int,
        marks: Map<Int, Long> = emptyMap()
    ): Pair<Long, Long>? {
        val (tokens, starts) = wordStarts(text, startMs, endMs, marks) ?: return null
        if (firstIndex !in tokens.indices || lastIndex !in tokens.indices ||
            lastIndex < firstIndex) return null
        return (starts[firstIndex] to starts[lastIndex + 1]).takeIf { (a, b) ->
            b > a && b - a in 750L..120_000L
        }
    }

    fun choices(
        text: String, startMs: Long, endMs: Long,
        marks: Map<Int, Long> = emptyMap()
    ): List<EstimatedCueWord>? {
        val (tokens, starts) = wordStarts(text, startMs, endMs, marks) ?: return null
        return tokens.mapIndexed { index, span ->
            EstimatedCueWord(span, index, tokens.size, starts[index])
        }
    }

    fun estimatedSpan(
        text: String, startMs: Long, endMs: Long, positionMs: Long,
        marks: Map<Int, Long> = emptyMap()
    ): IntRange? = estimate(text, startMs, endMs, positionMs, marks)?.span
}
