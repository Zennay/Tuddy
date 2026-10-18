package com.livelingo.android

import java.util.Locale

/** Deliberate choice for exporting selected matches from a local subtitle file. */
internal enum class SubtitleExcerptTiming { KEEP_SOURCE_TIMES, COMPACT_SELECTED }

/**
 * Only cues explicitly selected by the current search filter are exported.
 * No media retiming claims or background writes. The existing SRT/VTT
 * encoder is the final validation and size boundary.
 */
internal object PrivateSubtitleFilteredExcerpt {
    private const val MAX_TIME = 359_999_999L
    private const val GAP_MS = 250L

    private fun label(value: Long): String =
        String.format(Locale.ROOT, "%02d:%02d:%02d,%03d",
            value / 3_600_000L, value / 60_000L % 60,
            value / 1000L % 60, value % 1000L)

    fun prepare(
        selected: List<PrivateSubtitleCue>,
        mode: SubtitleExcerptTiming
    ): List<PrivateSubtitleCue>? {
        if (selected.isEmpty() || selected.size > 500 ||
            PrivateSubtitleDraftEditor.snapshot(selected, SubtitleDraftFormat.SRT) == null)
            return null
        val result = when (mode) {
            SubtitleExcerptTiming.KEEP_SOURCE_TIMES -> selected.toList()
            SubtitleExcerptTiming.COMPACT_SELECTED -> {
                val result = mutableListOf<PrivateSubtitleCue>()
                var nextStart = 0L
                for ((index, cue) in selected.withIndex()) {
                    val duration = cue.endMillis - cue.startMillis
                    if (duration <= 0 || nextStart > MAX_TIME - duration) return null
                    val end = nextStart + duration
                    result.add(cue.copy(
                        startMillis = nextStart,
                        endMillis = end,
                        startLabel = label(nextStart),
                        endLabel = label(end)
                    ))
                    if (nextStart > MAX_TIME - duration - GAP_MS && index != selected.lastIndex)
                        return null
                    nextStart = end + GAP_MS
                }
                result
            }
        }
        if (PrivateSubtitleDraftEditor.snapshot(result, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(result, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return result
    }
}
