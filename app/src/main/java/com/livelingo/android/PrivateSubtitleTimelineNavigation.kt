package com.livelingo.android

import java.util.Locale

/**
 * Manual timecode navigation in an explicitly opened document.
 * It never advances audio, changes the transcript or relies on ASR timing.
 */
internal object PrivateSubtitleTimelineNavigation {
    private val hhmmss = Regex("""^(\d{1,2}):([0-5]\d):([0-5]\d)(?:[.,](\d{3}))?$""")
    private val mmss = Regex("""^([0-5]?\d):([0-5]\d)(?:[.,](\d{3}))?$""")
    private const val MAX_TIME = 359_999_999L

    fun timestamp(value: String): Long? {
        val cleaned = value.trim()
        if (cleaned.isEmpty() || cleaned.length > 13) return null
        val hoursMinutesSeconds = hhmmss.matchEntire(cleaned)
        val millis: Long = if (hoursMinutesSeconds != null) {
            val g = hoursMinutesSeconds.groupValues
            g[1].toLong() * 3_600_000L + g[2].toLong() * 60_000L +
                g[3].toLong() * 1_000L + g[4].ifBlank { "0" }.toLong()
        } else {
            val minutesSeconds = mmss.matchEntire(cleaned) ?: return null
            val g = minutesSeconds.groupValues
            g[1].toLong() * 60_000L + g[2].toLong() * 1_000L +
                g[3].ifBlank { "0" }.toLong()
        }
        return millis.takeIf { it <= MAX_TIME }
    }

    /** Human-readable absolute timestamp for a preview; null outside media bounds. */
    fun formatTimestamp(millis: Long): String? {
        if (millis !in 0L..MAX_TIME) return null
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d",
            millis / 3_600_000L, millis / 60_000L % 60L,
            millis / 1_000L % 60L, millis % 1_000L)
    }

    /** First cue starting at/after requested time; otherwise the final cue. */
    fun nearest(cues: List<PrivateSubtitleCue>, typedTime: String): Int? {
        val wanted = timestamp(typedTime) ?: return null
        if (cues.isEmpty() || cues.size > 500) return null
        return cues.indexOfFirst { it.startMillis >= wanted }.takeIf { it >= 0 }
            ?: cues.lastIndex
    }
}
