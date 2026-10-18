package com.livelingo.android

/** Exact offline cue retiming. All edits remain temporary until separate SAF export. */
internal enum class ExactCueTimingMode { ONLY_SELECTED, FOLLOW_START_CHANGE, FOLLOW_END_CHANGE }

internal data class ExactCueTimingPreview(
    val cues: List<PrivateSubtitleCue>,
    val changedFollowing: Int,
    val previousStart: Long,
    val previousEnd: Long,
    val updatedStart: Long,
    val updatedEnd: Long,
    val overlapPairs: Int
)

internal object PrivateSubtitleExactTiming {
    private const val MAX_MS = 359_999_999L

    fun preview(
        current: List<PrivateSubtitleCue>, index: Int,
        typedStart: String, typedEnd: String, mode: ExactCueTimingMode
    ): ExactCueTimingPreview? {
        if (current.isEmpty() || current.size > 500 || index !in current.indices ||
            PrivateSubtitleDraftEditor.snapshot(current, SubtitleDraftFormat.SRT) == null
        ) return null
        val selected = current[index]
        val begin = PrivateSubtitleTimelineNavigation.timestamp(typedStart) ?: return null
        val end = PrivateSubtitleTimelineNavigation.timestamp(typedEnd) ?: return null
        if (begin !in 0L..MAX_MS || end !in 0L..MAX_MS || begin >= end ||
            (begin == selected.startMillis && end == selected.endMillis)) return null
        val delta = when (mode) {
            ExactCueTimingMode.ONLY_SELECTED -> 0L
            ExactCueTimingMode.FOLLOW_START_CHANGE -> begin - selected.startMillis
            ExactCueTimingMode.FOLLOW_END_CHANGE -> end - selected.endMillis
        }
        val updated = ArrayList<PrivateSubtitleCue>(current.size)
        current.forEachIndexed { i, cue ->
            val newStart = when {
                i < index -> cue.startMillis
                i == index -> begin
                else -> cue.startMillis + delta
            }
            val newEnd = when {
                i < index -> cue.endMillis
                i == index -> end
                else -> cue.endMillis + delta
            }
            if (newStart < 0L || newEnd > MAX_MS || newEnd <= newStart ||
                (updated.isNotEmpty() && updated.last().startMillis > newStart)) return null
            updated.add(cue.copy(
                startMillis = newStart, endMillis = newEnd,
                startLabel = PrivateSubtitleRetiming.label(newStart),
                endLabel = PrivateSubtitleRetiming.label(newEnd)
            ))
        }
        if (PrivateSubtitleDraftEditor.snapshot(updated, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(updated, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return ExactCueTimingPreview(
            cues = updated,
            changedFollowing = if (delta != 0L) current.size - index - 1 else 0,
            previousStart = selected.startMillis,
            previousEnd = selected.endMillis,
            updatedStart = begin,
            updatedEnd = end,
            overlapPairs = updated.zipWithNext().count { (a, b) ->
                a.endMillis > b.startMillis
            }
        )
    }
}
