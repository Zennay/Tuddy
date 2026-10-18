package com.livelingo.android

/**
 * Make a separate, temporary editor draft from a user's manually calibrated
 * audio preview. This never mutates the opened SRT/VTT document or writes a file.
 *
 * Audio calibration is a user-authored clock offset, NOT a measured alignment.
 * Reject impossible negative/excessive cue boundaries instead of clipping,
 * clamping, dropping or silently changing the source.
 */
internal object PrivateSubtitleAudioAlignment {
    private const val MAX_OFFSET_MS = 30_000L
    private const val MAX_CUE_TIME_MS = 359_999_999L

    fun preview(cues: List<PrivateSubtitleCue>, offsetMs: Long): List<PrivateSubtitleCue>? {
        if (offsetMs == 0L || offsetMs !in -MAX_OFFSET_MS..MAX_OFFSET_MS ||
            cues.isEmpty() || cues.size > 500 ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null
        ) return null

        val shifted = ArrayList<PrivateSubtitleCue>(cues.size)
        for (cue in cues) {
            val start = cue.startMillis + offsetMs
            val end = cue.endMillis + offsetMs
            if (start !in 0L..MAX_CUE_TIME_MS ||
                end !in 0L..MAX_CUE_TIME_MS || end <= start) return null
            shifted.add(cue.copy(
                startMillis = start,
                endMillis = end,
                startLabel = PrivateSubtitleRetiming.label(start),
                endLabel = PrivateSubtitleRetiming.label(end)
            ))
        }
        // The editor may export SRT or WebVTT later. Validate both first.
        if (PrivateSubtitleDraftEditor.snapshot(shifted, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(shifted, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return shifted
    }
}
