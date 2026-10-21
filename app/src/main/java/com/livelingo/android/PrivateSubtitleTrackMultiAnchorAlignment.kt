package com.livelingo.android

/**
 * Explicit, temporary piecewise time adjustment of an imported subtitle track.
 * Every anchor is a source-row pair chosen by the user. Nothing is inferred
 * from audio, language, translation, or text similarity.
 */
internal data class PrivateSubtitleMultiAnchor(
    val primaryRow: Int,
    val secondaryRow: Int
)

internal data class PrivateSubtitleMultiAnchorPreview(
    val adjustedSecondary: List<PrivateSubtitleCue>,
    val anchors: List<PrivateSubtitleMultiAnchor>,
    val segmentSourceSpansMs: List<Long>,
    val segmentTargetSpansMs: List<Long>
)

internal object PrivateSubtitleTrackMultiAnchorAlignment {
    const val MAX_ANCHORS = 8
    private const val MAX_TIME_MS = 359_999_999L

    fun preview(
        primary: List<PrivateSubtitleCue>,
        secondary: List<PrivateSubtitleCue>,
        anchors: List<PrivateSubtitleMultiAnchor>
    ): PrivateSubtitleMultiAnchorPreview? {
        if (primary.size !in 2..500 || secondary.size !in 2..500 ||
            anchors.size !in 2..MAX_ANCHORS ||
            PrivateSubtitleDraftEditor.snapshot(primary, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(primary, SubtitleDraftFormat.WEBVTT) == null ||
            PrivateSubtitleDraftEditor.snapshot(secondary, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(secondary, SubtitleDraftFormat.WEBVTT) == null
        ) return null

        val oldTimes = LongArray(anchors.size)
        val newTimes = LongArray(anchors.size)
        var lastPrimary = 0
        var lastSecondary = 0
        for ((index, pair) in anchors.withIndex()) {
            if (pair.primaryRow !in 1..primary.size ||
                pair.secondaryRow !in 1..secondary.size ||
                pair.primaryRow <= lastPrimary ||
                pair.secondaryRow <= lastSecondary
            ) return null
            val source = secondary[pair.secondaryRow - 1].startMillis
            val target = primary[pair.primaryRow - 1].startMillis
            if (!PrivateSubtitleOffset.isSupported(target - source)) return null
            oldTimes[index] = source
            newTimes[index] = target
            lastPrimary = pair.primaryRow
            lastSecondary = pair.secondaryRow
        }

        val sourceSpans = ArrayList<Long>(anchors.size - 1)
        val targetSpans = ArrayList<Long>(anchors.size - 1)
        for (index in 0 until anchors.lastIndex) {
            val oldSpan = oldTimes[index + 1] - oldTimes[index]
            val newSpan = newTimes[index + 1] - newTimes[index]
            if (oldSpan <= 0L || newSpan <= 0L ||
                newSpan * 2L < oldSpan || newSpan > oldSpan * 2L
            ) return null
            sourceSpans += oldSpan
            targetSpans += newSpan
        }

        // Between user-written anchors we interpolate; outside them we
        // extrapolate using the nearest edge segment. All arithmetic is
        // bounded integer milliseconds with half-away-from-zero rounding.
        fun adjustedTime(time: Long): Long? {
            val segment = when {
                time <= oldTimes[0] -> 0
                time >= oldTimes[oldTimes.lastIndex] -> oldTimes.size - 2
                else -> {
                    var index = 0
                    while (index + 1 < oldTimes.lastIndex &&
                        time >= oldTimes[index + 1]) index++
                    index
                }
            }
            val diff = time - oldTimes[segment]
            val product = diff * targetSpans[segment]
            val denominator = sourceSpans[segment]
            val mapped = if (product >= 0L)
                (product + denominator / 2L) / denominator
            else -((-product + denominator / 2L) / denominator)
            return (newTimes[segment] + mapped)
                .takeIf { it in 0L..MAX_TIME_MS }
        }

        val adjusted = ArrayList<PrivateSubtitleCue>(secondary.size)
        for (cue in secondary) {
            val begin = adjustedTime(cue.startMillis) ?: return null
            val end = adjustedTime(cue.endMillis) ?: return null
            if (end <= begin ||
                (adjusted.isNotEmpty() && begin < adjusted.last().startMillis)
            ) return null
            adjusted += cue.copy(
                startMillis = begin,
                endMillis = end,
                startLabel = PrivateSubtitleRetiming.label(begin),
                endLabel = PrivateSubtitleRetiming.label(end)
            )
        }
        // Never allow a convenient preview to bypass either complete export.
        if (PrivateSubtitleDraftEditor.snapshot(adjusted, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(adjusted, SubtitleDraftFormat.WEBVTT) == null
        ) return null

        return PrivateSubtitleMultiAnchorPreview(
            adjustedSecondary = adjusted,
            anchors = anchors.toList(),
            segmentSourceSpansMs = sourceSpans,
            segmentTargetSpansMs = targetSpans
        )
    }
}
