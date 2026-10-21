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

    /**
     * Select the union of complete cues in explicit, disjoint time intervals:
     * "00:10-00:20; 01:00-01:15". Input is bounded and never reorders cues.
     * A cue spanning two intervals is selected once, in original source order.
     * Empty, overlapping, reversed or malformed intervals are rejected.
     */
    fun expressionForWindows(
        cues: List<PrivateSubtitleCue>,
        windowList: String
    ): String? {
        if (cues.isEmpty() || cues.size > 500 ||
            windowList.isBlank() || windowList.length > 512) return null
        val segments = windowList.split(';')
        if (segments.size !in 1..16) return null
        val windows = ArrayList<Pair<Long, Long>>(segments.size)
        for (segment in segments) {
            val bounds = segment.trim().split('-')
            if (bounds.size != 2) return null
            val start = PrivateSubtitleTimelineNavigation
                .timestamp(bounds[0].trim()) ?: return null
            val end = PrivateSubtitleTimelineNavigation
                .timestamp(bounds[1].trim()) ?: return null
            if (start < 0L || end <= start) return null
            windows += start to end
        }
        val ordered = windows.sortedBy { it.first }
        if (ordered.zipWithNext().any { (previous, next) ->
                previous.second > next.first
            }) return null

        val matchingRows = cues.withIndex().filter { (_, cue) ->
            cue.startMillis >= 0L && cue.endMillis > cue.startMillis &&
                ordered.any { (start, end) ->
                    cue.startMillis < end && cue.endMillis > start
                }
        }.map { it.index + 1 }
        return PrivateSubtitleRowSelection.formatSourceRows(matchingRows, cues.size)
    }
}
