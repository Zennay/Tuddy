package com.livelingo.android

/**
 * Exact, user-authored shift of original subtitle rows, not of the whole file.
 * Disjoint ranges are interpreted against the complete current draft.
 * The text and all non-selected cues remain unchanged; nothing is persisted.
 */
internal data class SubtitleScopedShiftPreview(
    val updated: List<PrivateSubtitleCue>,
    val movedRows: List<Int>,
    val newOverlapPairs: Int
)

internal object PrivateSubtitleScopedShift {
    private const val MAX_TIME_MS = 359_999_999L
    val allowedOffsets = setOf(-5000L, -2000L, -500L, 500L, 2000L, 5000L)

    fun preview(
        cues: List<PrivateSubtitleCue>,
        originalRows: String,
        offsetMs: Long
    ): SubtitleScopedShiftPreview? {
        if (cues.isEmpty() || cues.size > 500 || offsetMs !in allowedOffsets ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        val indices = PrivateSubtitleRowSelection.parse(originalRows, cues.size)
            ?: return null
        val selected = indices.toSet()
        val shifted = ArrayList<PrivateSubtitleCue>(cues.size)
        for ((index, cue) in cues.withIndex()) {
            if (index !in selected) {
                shifted.add(cue)
            } else {
                val newStart = cue.startMillis + offsetMs
                val newEnd = cue.endMillis + offsetMs
                if (newStart < 0L || newEnd > MAX_TIME_MS ||
                    newEnd <= newStart) return null
                shifted.add(cue.copy(
                    startMillis = newStart,
                    endMillis = newEnd,
                    startLabel = PrivateSubtitleRetiming.label(newStart),
                    endLabel = PrivateSubtitleRetiming.label(newEnd)
                ))
            }
        }
        // Reject ordering inversions at boundaries of the selected ranges.
        // Unchanged cues may already overlap: we do not change their timing.
        if (shifted.zipWithNext().any { (first, second) ->
                second.startMillis < first.startMillis }) return null
        if (PrivateSubtitleDraftEditor.snapshot(shifted, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(shifted, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return SubtitleScopedShiftPreview(
            updated = shifted,
            movedRows = indices.map { it + 1 },
            newOverlapPairs = shifted.zipWithNext().count { (a, b) ->
                a.endMillis > b.startMillis
            }
        )
    }
}
