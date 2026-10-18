package com.livelingo.android

/** Local, non-destructive timing information for a user-opened subtitle file. */
internal enum class SubtitleTimelineFindingKind {
    SAME_TIMESPAN, OVERLAP, SHORT_GAP, LONG_GAP, BRIEF_CUE
}

internal data class SubtitleTimelineFinding(
    val cueIndex: Int,
    val kind: SubtitleTimelineFindingKind,
    val explanation: String,
    val timeMillis: Long
)

internal data class SubtitleTimelineOverview(
    val findings: List<SubtitleTimelineFinding>,
    val totalFindings: Int,
    val overlapCount: Int,
    val shortGapCount: Int,
    val longGapCount: Int,
    val briefCueCount: Int,
    val sameSpanCount: Int
)

/** Heuristics describe timing; they are not a media-sync or translation check. */
internal object PrivateSubtitleTimelineInspector {
    private const val SHORT_GAP_MS = 150L
    private const val LONG_GAP_MS = 10_000L
    private const val BRIEF_CUE_MS = 500L
    private const val MAX_VISIBLE = 160

    fun analyze(cues: List<PrivateSubtitleCue>): SubtitleTimelineOverview {
        val found = mutableListOf<SubtitleTimelineFinding>()
        if (cues.size !in 1..500) return SubtitleTimelineOverview(
            emptyList(), 0, 0, 0, 0, 0, 0
        )
        var overlaps = 0
        var shortGaps = 0
        var longGaps = 0
        var brief = 0
        var identical = 0
        fun record(index: Int, type: SubtitleTimelineFindingKind, label: String) {
            if (found.size < MAX_VISIBLE) {
                found.add(SubtitleTimelineFinding(index, type, label, cues[index].startMillis))
            }
        }
        cues.forEachIndexed { index, cue ->
            if (cue.endMillis - cue.startMillis in 1L until BRIEF_CUE_MS) {
                brief++
                record(index, SubtitleTimelineFindingKind.BRIEF_CUE,
                    "Regel zichtbaar korter dan 0,5 seconde.")
            }
            if (index == 0) return@forEachIndexed
            val previous = cues[index - 1]
            val gap = cue.startMillis - previous.endMillis
            when {
                cue.startMillis == previous.startMillis &&
                    cue.endMillis == previous.endMillis -> {
                    identical++
                    record(index, SubtitleTimelineFindingKind.SAME_TIMESPAN,
                        "Zelfde tijdvak als regel " + index + ".")
                }
                gap < 0L -> {
                    overlaps++
                    record(index, SubtitleTimelineFindingKind.OVERLAP,
                        "Overlap met regel " + index + " (" + (-gap) + " ms).")
                }
                gap in 0L until SHORT_GAP_MS -> {
                    shortGaps++
                    record(index, SubtitleTimelineFindingKind.SHORT_GAP,
                        "Minder dan 0,15 seconde tussen regels " + index +
                            " en " + (index + 1) + ".")
                }
                gap > LONG_GAP_MS -> {
                    longGaps++
                    record(index, SubtitleTimelineFindingKind.LONG_GAP,
                        "Meer dan 10 seconden tussen regels " + index +
                            " en " + (index + 1) + ".")
                }
            }
        }
        return SubtitleTimelineOverview(
            findings = found,
            totalFindings = overlaps + shortGaps + longGaps + brief + identical,
            overlapCount = overlaps, shortGapCount = shortGaps,
            longGapCount = longGaps, briefCueCount = brief,
            sameSpanCount = identical
        )
    }
}
