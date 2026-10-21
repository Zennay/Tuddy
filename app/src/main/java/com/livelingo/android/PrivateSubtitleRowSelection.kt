package com.livelingo.android

/**
 * Explicit user-authored 1-based cue selection. A nonempty expression such
 * as "1-3,7,9-11" selects exactly those original source row numbers.
 *
 * Overlapping or repeated rows are rejected rather than silently merged.
 * The result is strictly source-ordered and never depends on search/filter UI.
 * No clipboard, IO, network access, or automatic transcription.
 */
internal object PrivateSubtitleRowSelection {
    private const val MAX_CUES = 500
    private const val MAX_EXPRESSION_LENGTH = 2_000

    fun parse(expression: String, sourceRows: Int): List<Int>? {
        if (sourceRows !in 1..MAX_CUES || expression.isBlank() ||
            expression.length > MAX_EXPRESSION_LENGTH) return null
        val marks = BooleanArray(sourceRows)
        val segments = expression.split(',')
        if (segments.isEmpty() || segments.size > MAX_CUES) return null
        for (segment in segments) {
            val numbers = segment.trim().split('-')
            if (numbers.size !in 1..2) return null
            val from = numbers[0].trim().takeIf { it.isNotEmpty() &&
                it.all { char -> char in '0'..'9' } }?.toIntOrNull()
                ?: return null
            val to = if (numbers.size == 2) {
                numbers[1].trim()
                    .takeIf { it.isNotEmpty() && it.all { char -> char in '0'..'9' } }
                    ?.toIntOrNull() ?: return null
            } else {
                from
            }
            if (from !in 1..sourceRows || to !in from..sourceRows) return null
            for (number in from..to) {
                if (marks[number - 1]) return null
                marks[number - 1] = true
            }
        }
        return marks.indices.filter { marks[it] }.takeIf { it.isNotEmpty() }
    }
}
