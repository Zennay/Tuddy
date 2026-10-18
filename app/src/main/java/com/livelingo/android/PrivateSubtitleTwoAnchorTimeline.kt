package com.livelingo.android

internal data class SubtitleTwoAnchorPreview(
    val cues: List<PrivateSubtitleCue>,
    val originalFirst: Long,
    val originalLast: Long,
    val proposedFirst: Long,
    val proposedLast: Long,
    val overlapPairs: Int
)

/**
 * Manually align a complete local subtitle draft from two explicit time anchors.
 * No media is opened or analyzed. The proportional map retimes all cue ends too.
 */
internal object PrivateSubtitleTwoAnchorTimeline {
    private const val MAX_MS = 359_999_999L

    fun preview(
        cues: List<PrivateSubtitleCue>,
        newFirstLabel: String,
        newLastLabel: String
    ): SubtitleTwoAnchorPreview? {
        if (cues.size !in 2..500 ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null
        ) return null
        val sourceFirst = cues.first().startMillis
        val sourceLast = cues.last().startMillis
        val newFirst = PrivateSubtitleTimelineNavigation.timestamp(newFirstLabel) ?: return null
        val newLast = PrivateSubtitleTimelineNavigation.timestamp(newLastLabel) ?: return null
        val oldSpan = sourceLast - sourceFirst
        val targetSpan = newLast - newFirst
        if (oldSpan <= 0L || targetSpan <= 0L ||
            newFirst !in 0L..MAX_MS || newLast !in 0L..MAX_MS ||
            (newFirst == sourceFirst && newLast == sourceLast)) return null
        fun shifted(time: Long): Long? {
            // Bounded to 99h59m59s: product fits comfortably in signed Long.
            val distance = time - sourceFirst
            val mapped = newFirst + (distance * targetSpan + oldSpan / 2L) / oldSpan
            return mapped.takeIf { it in 0L..MAX_MS }
        }
        val result = ArrayList<PrivateSubtitleCue>(cues.size)
        for (cue in cues) {
            val begin = shifted(cue.startMillis) ?: return null
            val end = shifted(cue.endMillis) ?: return null
            if (end <= begin || (result.isNotEmpty() &&
                begin < result.last().startMillis)) return null
            result.add(cue.copy(
                startMillis = begin, endMillis = end,
                startLabel = PrivateSubtitleRetiming.label(begin),
                endLabel = PrivateSubtitleRetiming.label(end)
            ))
        }
        if (PrivateSubtitleDraftEditor.snapshot(result, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(result, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return SubtitleTwoAnchorPreview(
            cues = result,
            originalFirst = sourceFirst, originalLast = sourceLast,
            proposedFirst = result.first().startMillis,
            proposedLast = result.last().startMillis,
            overlapPairs = result.zipWithNext().count { (a, b) ->
                a.endMillis > b.startMillis
            }
        )
    }
}
