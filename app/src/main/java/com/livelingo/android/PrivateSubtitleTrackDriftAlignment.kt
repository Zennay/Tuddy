package com.livelingo.android

/**
 * Temporary, user-controlled correction for a secondary subtitle track whose
 * clock runs slightly faster/slower than the primary track. The user supplies
 * two known corresponding cue pairs; we only map their authored start times.
 * No audio/media analysis, automatic matching, translation or permanent write.
 */
internal data class PrivateSubtitleDriftPreview(
    val adjustedSecondary: List<PrivateSubtitleCue>,
    val firstPrimary: PrivateSubtitleCue,
    val firstSecondary: PrivateSubtitleCue,
    val lastPrimary: PrivateSubtitleCue,
    val lastSecondary: PrivateSubtitleCue,
    val sourceSpanMs: Long,
    val targetSpanMs: Long
)

internal object PrivateSubtitleTrackDriftAlignment {
    private const val MAX_CUES = 500
    private const val MAX_TIME_MS = 359_999_999L

    fun preview(
        primary: List<PrivateSubtitleCue>,
        secondary: List<PrivateSubtitleCue>,
        firstPrimaryRow: String,
        firstSecondaryRow: String,
        lastPrimaryRow: String,
        lastSecondaryRow: String
    ): PrivateSubtitleDriftPreview? {
        if (primary.size !in 2..MAX_CUES || secondary.size !in 2..MAX_CUES ||
            PrivateSubtitleDraftEditor.snapshot(primary, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(secondary, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(primary, SubtitleDraftFormat.WEBVTT) == null ||
            PrivateSubtitleDraftEditor.snapshot(secondary, SubtitleDraftFormat.WEBVTT) == null
        ) return null

        fun index(raw: String, count: Int): Int? {
            if (raw.isEmpty() || raw.length > 3 || raw.any { it !in '0'..'9' })
                return null
            val number = raw.toIntOrNull() ?: return null
            return (number - 1).takeIf { number in 1..count }
        }

        val p0 = index(firstPrimaryRow, primary.size) ?: return null
        val s0 = index(firstSecondaryRow, secondary.size) ?: return null
        val p1 = index(lastPrimaryRow, primary.size) ?: return null
        val s1 = index(lastSecondaryRow, secondary.size) ?: return null
        if (p0 >= p1 || s0 >= s1) return null

        val oldFirst = secondary[s0].startMillis
        val oldLast = secondary[s1].startMillis
        val newFirst = primary[p0].startMillis
        val newLast = primary[p1].startMillis
        val oldSpan = oldLast - oldFirst
        val newSpan = newLast - newFirst
        if (oldSpan <= 0L || newSpan <= 0L ||
            newSpan * 2L < oldSpan || newSpan > oldSpan * 2L ||
            !PrivateSubtitleOffset.isSupported(newFirst - oldFirst) ||
            !PrivateSubtitleOffset.isSupported(newLast - oldLast)
        ) return null

        // Half-away-from-zero rounded integer interpolation, including cues
        // outside both anchors; all intermediate products fit in signed Long.
        fun mapped(time: Long): Long? {
            val distanceProduct = (time - oldFirst) * newSpan
            val shifted = if (distanceProduct >= 0L)
                (distanceProduct + oldSpan / 2L) / oldSpan
            else -((-distanceProduct + oldSpan / 2L) / oldSpan)
            return (newFirst + shifted).takeIf { it in 0L..MAX_TIME_MS }
        }

        val adjusted = ArrayList<PrivateSubtitleCue>(secondary.size)
        for (cue in secondary) {
            val begin = mapped(cue.startMillis) ?: return null
            val end = mapped(cue.endMillis) ?: return null
            if (end <= begin ||
                (adjusted.isNotEmpty() && begin < adjusted.last().startMillis)
            ) return null
            adjusted += cue.copy(
                startMillis = begin,
                endMillis = end,
                startLabel = PrivateSubtitleRetiming.label(begin),
                endLabel = PrivateSubtitleRetiming.label(end)
            )
        }
        if (PrivateSubtitleDraftEditor.snapshot(adjusted, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(adjusted, SubtitleDraftFormat.WEBVTT) == null
        ) return null

        return PrivateSubtitleDriftPreview(
            adjustedSecondary = adjusted,
            firstPrimary = primary[p0], firstSecondary = secondary[s0],
            lastPrimary = primary[p1], lastSecondary = secondary[s1],
            sourceSpanMs = oldSpan, targetSpanMs = newSpan
        )
    }
}
