package com.livelingo.android

/**
 * Exact, user-authored relative subtitle offset in whole milliseconds.
 * A comma or period denotes milliseconds (at most three decimal places).
 * This is a manual timing hint, not media/audio alignment.
 */
internal object PrivateSubtitleOffset {
    private const val MAX_ABSOLUTE_MS = 600_000L
    private val SECONDS = Regex("""^([+-]?)([0-9]{1,3})(?:[.,]([0-9]{1,3}))?$""")

    fun parseSeconds(raw: String, allowZero: Boolean = false): Long? {
        val match = SECONDS.matchEntire(raw.trim()) ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: return null
        val fraction = match.groupValues[3].padEnd(3, '0')
            .ifEmpty { "0" }.toLongOrNull() ?: return null
        val absolute = seconds * 1_000L + fraction
        if (absolute > MAX_ABSOLUTE_MS || (!allowZero && absolute == 0L)) return null
        return if (match.groupValues[1] == "-") -absolute else absolute
    }

    fun formatSeconds(offsetMs: Long): String {
        val absolute = kotlin.math.abs(offsetMs)
        val whole = absolute / 1_000L
        val fraction = absolute % 1_000L
        val sign = if (offsetMs < 0L) "-" else if (offsetMs > 0L) "+" else ""
        return sign + whole.toString() +
            if (fraction == 0L) "" else "." + fraction.toString().padStart(3, '0')
    }

    fun isSupported(offsetMs: Long): Boolean = offsetMs in -MAX_ABSOLUTE_MS..MAX_ABSOLUTE_MS
}
