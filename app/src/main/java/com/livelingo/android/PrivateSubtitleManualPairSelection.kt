package com.livelingo.android

/**
 * Strict, local, original-source-row mapping authored by the user.
 *
 * "1:2,3-5:7-9" pins one-to-one original source rows. Each side of
 * a range must have the same count and the entire expression must
 * increase in both source orders. This is not translation or alignment
 * inferred from audio. Never guess missing or reordered rows.
 */
internal object PrivateSubtitleManualPairSelection {
    const val MAX_EXPRESSION_LENGTH = 512
    const val MAX_PAIRS = 32

    fun parse(
        expression: String,
        primaryCount: Int,
        secondaryCount: Int
    ): List<SubtitleManualPair>? {
        if (expression.isBlank() ||
            expression.length > MAX_EXPRESSION_LENGTH ||
            primaryCount !in 1..500 ||
            secondaryCount !in 1..500
        ) return null

        fun range(raw: String, count: Int): IntRange? {
            val parts = raw.trim().split('-')
            if (parts.size !in 1..2 ||
                parts.any { it.isEmpty() || it.length > 3 ||
                    it.any { char -> char !in '0'..'9' } }
            ) return null
            val first = parts[0].toIntOrNull() ?: return null
            val last = parts.getOrNull(1)?.toIntOrNull() ?: first
            if (first !in 1..count || last !in first..count) return null
            return first..last
        }

        val pairs = ArrayList<SubtitleManualPair>()
        var previousPrimary = 0
        var previousSecondary = 0
        for (token in expression.split(',')) {
            val sides = token.trim().split(':')
            if (sides.size != 2) return null
            val first = range(sides[0], primaryCount) ?: return null
            val second = range(sides[1], secondaryCount) ?: return null
            if (first.last - first.first != second.last - second.first ||
                first.first <= previousPrimary ||
                second.first <= previousSecondary ||
                pairs.size + first.last - first.first + 1 > MAX_PAIRS
            ) return null
            for (index in 0..(first.last - first.first)) {
                pairs += SubtitleManualPair(first.first + index,
                    second.first + index)
            }
            previousPrimary = first.last
            previousSecondary = second.last
        }
        return pairs.takeIf { it.isNotEmpty() }
    }
}
