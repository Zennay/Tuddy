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

    /**
     * Explicit, reusable *text*, not a subtitle or audio file.
     * Example: 2=00:00:12.250;5=00:00:13.900
     * The user supplies absolute cue-timeline millisecond positions. A full
     * replacement is applied only when all entries are in range and all
     * interpolated word positions retain positive duration.
     */
    fun parseManualMarkSheet(
        source: String, text: String, startMs: Long, endMs: Long
    ): Map<Int, Long>? {
        if (source.isBlank() || source.length > 512) return null
        val pieces = source.split(';', '\n').map(String::trim)
        if (pieces.size !in 1..MAX_MANUAL_MARKS || pieces.any(String::isEmpty))
            return null
        val pattern = Regex(
            """^([1-9][0-9]{0,2})=([0-9]{2,6}):([0-5][0-9]):([0-5][0-9])\\.([0-9]{3})$"""
        )
        val marks = linkedMapOf<Int, Long>()
        for (piece in pieces) {
            val match = pattern.matchEntire(piece) ?: return null
            val number = match.groupValues[1].toIntOrNull() ?: return null
            val hours = match.groupValues[2].toLongOrNull() ?: return null
            val minutes = match.groupValues[3].toLongOrNull() ?: return null
            val seconds = match.groupValues[4].toLongOrNull() ?: return null
            val millis = match.groupValues[5].toLongOrNull() ?: return null
            val absoluteMs = hours * 3_600_000L + minutes * 60_000L +
                seconds * 1_000L + millis
            if (number < 2 || number > MAX_WORDS ||
                marks.put(number - 1, absoluteMs) != null) return null
        }
        val proposed = marks.toSortedMap()
        return proposed.takeIf { wordStarts(text, startMs, endMs, it) != null }
    }

    /** Deterministic, human-copyable draft from current manual marks only. */
    fun formatManualMarkSheet(marks: Map<Int, Long>): String? {
        if (marks.isEmpty() || marks.size > MAX_MANUAL_MARKS ||
            marks.any { (index, timestamp) ->
                index !in 1 until MAX_WORDS || timestamp < 0L
            }) return null
        return marks.toSortedMap().entries.joinToString(";") { (index, stamp) ->
            val seconds = stamp / 1_000L
            (index + 1).toString() + "=" +
                String.format(java.util.Locale.ROOT,
                    "%02d:%02d:%02d.%03d",
                    seconds / 3_600L, seconds / 60L % 60L,
                    seconds % 60L, stamp % 1_000L)
        }
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
