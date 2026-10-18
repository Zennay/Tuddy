package com.livelingo.android

import java.util.Locale

/**
 * A completely local video-clip subtitle draft: slice an explicitly opened
 * subtitle document, clamp boundaries and optionally retime to clip 00:00.
 * Never reads media, never guesses audio alignment and never performs I/O.
 */
internal data class PrivateSubtitleClipPreview(
    val cues: List<PrivateSubtitleCue>,
    val retainedCount: Int,
    val clippedCount: Int,
    val firstOffsetMs: Long,
    val windowDurationMs: Long
)

internal object PrivateSubtitleClip {
    private const val MAX_TIME = 359_999_999L

    private fun label(time: Long): String =
        String.format(Locale.ROOT, "%02d:%02d:%02d,%03d",
            time / 3_600_000L, (time / 60_000L) % 60L,
            (time / 1000L) % 60L, time % 1000L)

    /** [start, end): trim partial cues and shift all included times to 00:00. */
    fun extract(
        original: List<PrivateSubtitleCue>,
        start: Long,
        end: Long
    ): PrivateSubtitleClipPreview? {
        if (original.isEmpty() || original.size > 500 ||
            start < 0L || end > MAX_TIME || end <= start) return null

        var boundaryCues = 0
        val selected = original.mapNotNull { cue ->
            // Reject structurally incorrect external or locally malformed time.
            if (cue.startMillis < 0L || cue.endMillis > MAX_TIME ||
                cue.endMillis <= cue.startMillis) return null
            val from = maxOf(cue.startMillis, start)
            val until = minOf(cue.endMillis, end)
            if (from >= until) return@mapNotNull null
            if (cue.startMillis < start || cue.endMillis > end) boundaryCues++
            val adjustedStart = from - start
            val adjustedEnd = until - start
            cue.copy(
                startMillis = adjustedStart,
                endMillis = adjustedEnd,
                startLabel = label(adjustedStart),
                endLabel = label(adjustedEnd)
            )
        }
        if (selected.isEmpty() ||
            PrivateSubtitleDraftEditor.snapshot(selected, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(selected, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return PrivateSubtitleClipPreview(
            cues = selected,
            retainedCount = selected.size,
            clippedCount = boundaryCues,
            firstOffsetMs = start,
            windowDurationMs = end - start
        )
    }
}
