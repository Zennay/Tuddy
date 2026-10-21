package com.livelingo.android

/**
 * Manually align the beginnings of two explicitly selected subtitle rows.
 * This does not inspect media or infer which captions correspond. The user
 * picks both 1-based row numbers and explicitly applies the resulting offset.
 */
internal object PrivateSubtitleTrackAnchorOffset {
    fun fromRows(
        primary: List<PrivateSubtitleCue>,
        secondary: List<PrivateSubtitleCue>,
        primaryRowText: String,
        secondaryRowText: String
    ): Long? {
        if (primary.isEmpty() || secondary.isEmpty() ||
            primary.size > 500 || secondary.size > 500) return null
        val primaryRow = parseRow(primaryRowText, primary.size) ?: return null
        val secondaryRow = parseRow(secondaryRowText, secondary.size) ?: return null
        val primaryCue = primary[primaryRow - 1]
        val secondaryCue = secondary[secondaryRow - 1]
        if (primaryCue.endMillis <= primaryCue.startMillis ||
            secondaryCue.endMillis <= secondaryCue.startMillis) return null
        val difference = primaryCue.startMillis - secondaryCue.startMillis
        return difference.takeIf(PrivateSubtitleOffset::isSupported)
    }

    private fun parseRow(raw: String, size: Int): Int? {
        if (raw.isEmpty() || raw.length > 3 || raw.any { it !in '0'..'9' }) return null
        return raw.toIntOrNull()?.takeIf { it in 1..size }
    }
}
