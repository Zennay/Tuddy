package com.livelingo.android

import java.util.Locale

/**
 * Combine two manually selected caption tracks into a temporary parallel reading copy.
 *
 * Pairing is based ONLY on overlapping timestamps (not an audio clock, translation
 * engine or language identification). Each cue is paired at most once. Secondary
 * cues without a sufficiently overlapping primary cue remain visible as standalone
 * rows, rather than silently disappearing. Original source files are untouched.
 */
internal data class SubtitleTrackPairingPreview(
    val cues: List<PrivateSubtitleCue>,
    val pairedCount: Int,
    val unpairedSecondTrack: Int
)

internal object PrivateSubtitleTrackPairing {
    // Offsets are explicitly authored by the user (up to ten minutes each way).
    // Timing, pair counts, and BOTH export snapshots still gate the preview.
    private fun stamp(millis: Long): String = String.format(Locale.ROOT,
        "%02d:%02d:%02d,%03d", millis / 3_600_000L,
        millis / 60_000L % 60, millis / 1000L % 60, millis % 1000L)

    fun preview(
        primary: List<PrivateSubtitleCue>,
        secondary: List<PrivateSubtitleCue>,
        secondTrackOffsetMs: Long = 0L
    ): SubtitleTrackPairingPreview? {
        if (!PrivateSubtitleOffset.isSupported(secondTrackOffsetMs) ||
            primary.isEmpty() || secondary.isEmpty() ||
            primary.size > 500 || secondary.size > 500 ||
            PrivateSubtitleDraftEditor.snapshot(primary, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(secondary, SubtitleDraftFormat.SRT) == null
        ) return null

        // The offset is user-selected and applies only in this temporary draft.
        // Reject shifts beyond the same limits as the underlying SRT/VTT editor.
        val adjusted = mutableListOf<PrivateSubtitleCue>()
        for (cue in secondary) {
            val start = cue.startMillis + secondTrackOffsetMs
            val end = cue.endMillis + secondTrackOffsetMs
            if (start < 0L || end > 359_999_999L) return null
            adjusted += if (secondTrackOffsetMs == 0L) cue else cue.copy(
                startMillis = start, endMillis = end,
                startLabel = stamp(start), endLabel = stamp(end)
            )
        }
        val used = BooleanArray(primary.size)
        val matched = arrayOfNulls<String>(primary.size)
        val leftovers = mutableListOf<PrivateSubtitleCue>()
        var paired = 0
        // Consider secondary cues chronologically. For each, choose the unused
        // source cue with greatest actual interval overlap. A brief crossing of
        // boundaries is not enough to infer that two sentences belong together.
        for (candidate in adjusted) {
            var best = -1
            var bestOverlap = 0L
            primary.forEachIndexed { index, source ->
                if (!used[index]) {
                    val overlap = minOf(source.endMillis, candidate.endMillis) -
                        maxOf(source.startMillis, candidate.startMillis)
                    val shorter = minOf(
                        source.endMillis - source.startMillis,
                        candidate.endMillis - candidate.startMillis
                    )
                    if (overlap > 0L && overlap >= (shorter + 1L) / 2L &&
                        overlap > bestOverlap
                    ) {
                        best = index
                        bestOverlap = overlap
                    }
                }
            }
            if (best < 0) {
                leftovers += candidate
            } else {
                used[best] = true
                matched[best] = candidate.text
                paired++
            }
        }

        val rows = primary.mapIndexed { index, cue ->
            val other = matched[index]
            if (other == null || other == cue.text) cue
            else cue.copy(text = cue.text + "\n" + other)
        }.plus(leftovers).sortedBy { it.startMillis }

        // Enforce BOTH export formats before allowing the RAM-only change.
        if (PrivateSubtitleDraftEditor.snapshot(rows, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(rows, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return SubtitleTrackPairingPreview(
            cues = rows,
            pairedCount = paired,
            unpairedSecondTrack = leftovers.size
        )
    }
}
