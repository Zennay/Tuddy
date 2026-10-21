package com.livelingo.android

import java.util.Locale

/** Explicit user decision about where a second SRT/WebVTT timeline belongs. */
internal enum class SubtitleCombineMode {
    APPEND_AFTER_LAST,
    KEEP_SOURCE_TIMES,
    PAIR_OVERLAPPING_TIMES
}

internal data class SubtitleCombinePreview(
    val cues: List<PrivateSubtitleCue>,
    val importedCount: Int,
    val adjacentOverlaps: Int,
    val startsAtMillis: Long,
    val pairedCount: Int = 0,
    val unpairedSecondTrack: Int = 0,
    val unpairedFirstTrack: Int = 0,
    val pairedLinks: List<SubtitleCuePairingLink> = emptyList(),
    val unpairedPrimaryRows: List<Int> = emptyList(),
    val unpairedSecondaryRows: List<Int> = emptyList()
)

/**
 * Ephemeral assembly of two user-selected subtitle files. Nothing is
 * translated, aligned against media, stored, or added to a live transcript.
 * The existing export boundary controls all 500-cue/UTF-8/size limits.
 */
internal object PrivateSubtitleDocumentCombine {
    private const val GAP_MS = 1000L
    private const val MAX_TIME = 359_999_999L

    private fun timeLabel(milliseconds: Long): String =
        String.format(
            Locale.ROOT, "%02d:%02d:%02d,%03d",
            milliseconds / 3_600_000L,
            milliseconds / 60_000L % 60,
            milliseconds / 1000L % 60,
            milliseconds % 1000L
        )

    fun preview(
        current: List<PrivateSubtitleCue>,
        incoming: List<PrivateSubtitleCue>,
        mode: SubtitleCombineMode,
        secondTrackOffsetMs: Long = 0L,
        excludedSecondaryRows: Set<Int> = emptySet(),
        manualPairs: List<SubtitleManualPair> = emptyList()
    ): SubtitleCombinePreview? {
        if (current.isEmpty() || incoming.isEmpty() ||
            (mode != SubtitleCombineMode.PAIR_OVERLAPPING_TIMES &&
                (current.size + incoming.size > 500 ||
                    excludedSecondaryRows.isNotEmpty() ||
                    manualPairs.isNotEmpty())) ||
            PrivateSubtitleDraftEditor.snapshot(current, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(incoming, SubtitleDraftFormat.SRT) == null
        ) return null

        if (mode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES) {
            val paired = PrivateSubtitleTrackPairing.preview(
                current, incoming, secondTrackOffsetMs,
                excludedSecondaryRows, manualPairs) ?: return null
            return SubtitleCombinePreview(
                cues = paired.cues,
                importedCount = incoming.size,
                adjacentOverlaps = paired.cues.zipWithNext().count { (a, b) ->
                    a.endMillis > b.startMillis
                },
                startsAtMillis = incoming.first().startMillis,
                pairedCount = paired.pairedCount,
                unpairedSecondTrack = paired.unpairedSecondTrack,
                unpairedFirstTrack = paired.unpairedFirstTrack,
                pairedLinks = paired.pairedLinks,
                unpairedPrimaryRows = paired.unpairedPrimaryRows,
                unpairedSecondaryRows = paired.unpairedSecondaryRows
            )
        }
        val shifted = when (mode) {
            SubtitleCombineMode.KEEP_SOURCE_TIMES -> incoming
            SubtitleCombineMode.PAIR_OVERLAPPING_TIMES -> return null // handled above
            SubtitleCombineMode.APPEND_AFTER_LAST -> {
                val tail = current.maxOf { it.endMillis }
                val newStart = tail + GAP_MS
                if (newStart > MAX_TIME) return null
                val adjustment = newStart - incoming.first().startMillis
                incoming.map { cue ->
                    val begin = cue.startMillis + adjustment
                    val end = cue.endMillis + adjustment
                    if (begin < 0L || end > MAX_TIME || end <= begin) return null
                    cue.copy(
                        startMillis = begin,
                        endMillis = end,
                        startLabel = timeLabel(begin),
                        endLabel = timeLabel(end)
                    )
                }
            }
        }
        // Kotlin sorting is stable: at matching starts, old rows remain first.
        val combined = (current + shifted).sortedBy { it.startMillis }
        if (PrivateSubtitleDraftEditor.snapshot(combined, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(combined, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        val overlaps = combined.zipWithNext().count { (a, b) ->
            a.endMillis > b.startMillis
        }
        return SubtitleCombinePreview(
            cues = combined,
            importedCount = shifted.size,
            adjacentOverlaps = overlaps,
            startsAtMillis = shifted.first().startMillis
        )
    }
}
