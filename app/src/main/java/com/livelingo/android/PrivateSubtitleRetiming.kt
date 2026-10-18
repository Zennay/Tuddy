package com.livelingo.android

import java.util.Locale

internal data class SubtitleRetimingPreview(
    val updated: List<PrivateSubtitleCue>,
    val oldFirstMs: Long,
    val newFirstMs: Long,
    val oldLastMs: Long,
    val newLastMs: Long,
    val overlappingPairs: Int
)

/** User-selected offline retiming; never modifies source or claims audio alignment. */
internal object PrivateSubtitleRetiming {
    val percentages = setOf(80, 90, 95, 100, 105, 110, 125)
    val offsets = setOf(-5000L, -2000L, -500L, 0L, 500L, 2000L, 5000L)
    private const val MAX_MS = 359_999_999L

    fun preview(
        cues: List<PrivateSubtitleCue>, percentage: Int, offsetMs: Long,
        anchorFirst: Boolean
    ): SubtitleRetimingPreview? {
        if (cues.isEmpty() || cues.size > 500 || percentage !in percentages ||
            offsetMs !in offsets || (percentage == 100 && offsetMs == 0L) ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null
        ) return null
        val anchor = if (anchorFirst) cues.first().startMillis else 0L
        fun retime(time: Long): Long? {
            val transformed = anchor + ((time - anchor) * percentage + 50L) / 100L + offsetMs
            return transformed.takeIf { it in 0L..MAX_MS }
        }
        val updated = ArrayList<PrivateSubtitleCue>(cues.size)
        for (cue in cues) {
            val start = retime(cue.startMillis) ?: return null
            val end = retime(cue.endMillis) ?: return null
            if (end <= start || (updated.isNotEmpty() &&
                start < updated.last().startMillis)) return null
            updated.add(cue.copy(
                startMillis = start, endMillis = end,
                startLabel = label(start), endLabel = label(end)
            ))
        }
        if (PrivateSubtitleDraftEditor.snapshot(updated, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(updated, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return SubtitleRetimingPreview(
            updated = updated, oldFirstMs = cues.first().startMillis,
            newFirstMs = updated.first().startMillis,
            oldLastMs = cues.maxOf { it.endMillis },
            newLastMs = updated.maxOf { it.endMillis },
            overlappingPairs = updated.zipWithNext().count { (a, b) ->
                a.endMillis > b.startMillis
            }
        )
    }

    fun label(ms: Long): String = String.format(
        Locale.ROOT, "%02d:%02d:%02d,%03d",
        ms / 3_600_000L, ms / 60_000L % 60, ms / 1000L % 60, ms % 1000L
    )
}
