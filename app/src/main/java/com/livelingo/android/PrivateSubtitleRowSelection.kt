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

    /** Compact the visible original 1-based row numbers for deliberate editing. */
    fun formatSourceRows(rows: List<Int>, sourceRows: Int): String? {
        if (sourceRows !in 1..MAX_CUES || rows.isEmpty() || rows.size > MAX_CUES)
            return null
        val sorted = rows.sorted()
        if (sorted.first() < 1 || sorted.last() > sourceRows ||
            sorted.zipWithNext().any { (a, b) -> a == b }) return null
        val parts = ArrayList<String>()
        var start = sorted.first()
        var previous = start
        for (row in sorted.drop(1)) {
            if (row == previous + 1) {
                previous = row
            } else {
                parts += if (start == previous) "$start" else "$start-$previous"
                start = row
                previous = row
            }
        }
        parts += if (start == previous) "$start" else "$start-$previous"
        val expression = parts.joinToString(",")
        if (expression.length > MAX_EXPRESSION_LENGTH) return null
        // Never display an expression that fails the same parser used at consent.
        return expression.takeIf { parsed ->
            parse(parsed, sourceRows) == sorted.map { it - 1 }
        }
    }

}
