package com.livelingo.android

/**
 * Select complete authored cues that intersect an explicitly typed interval.
 * This does not crop text/timestamps, read audio, or automatically export.
 * Converts results back into original 1-based source-row ranges for
 * the existing explicit range selection and final consent checks.
 */
internal object PrivateSubtitleTimeWindowRows {
    fun expression(
        cues: List<PrivateSubtitleCue>,
        startText: String,
        endText: String
    ): String? {
        if (cues.isEmpty() || cues.size > 500) return null
        val start = PrivateSubtitleTimelineNavigation.timestamp(startText) ?: return null
        val end = PrivateSubtitleTimelineNavigation.timestamp(endText) ?: return null
        if (end <= start) return null
        val matchingRows = cues.withIndex()
            .filter { (_, cue) ->
                cue.startMillis >= 0L && cue.endMillis > cue.startMillis &&
                    cue.startMillis < end && cue.endMillis > start
            }
            .map { it.index + 1 }
        return PrivateSubtitleRowSelection.formatSourceRows(matchingRows, cues.size)
    }
}
