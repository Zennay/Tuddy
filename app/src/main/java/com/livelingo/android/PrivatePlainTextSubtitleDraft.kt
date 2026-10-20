package com.livelingo.android

import java.util.Locale

/**
 * User-authored, one-line-per-cue *draft* with explicitly selected synthetic
 * durations. These are NOT speech timestamps, word alignment, or translations.
 * Only used after deliberate manual input; no audio, clipboard, or I/O.
 */
internal object PrivatePlainTextSubtitleDraft {
    private const val MAX_BYTES = 262_144
    private const val MAX_CUES = 500
    private const val MAX_TIME = 359_999_999L
    val durationOptions = listOf(2_000L, 3_500L, 5_000L, 7_000L)
    val startOptions = listOf(0L, 5_000L, 10_000L, 30_000L, 60_000L)

    private fun label(milliseconds: Long): String =
        String.format(Locale.ROOT, "%02d:%02d:%02d,%03d",
            milliseconds / 3_600_000L,
            milliseconds / 60_000L % 60,
            milliseconds / 1_000L % 60,
            milliseconds % 1_000L)

    fun preview(
        raw: String,
        startMillis: Long,
        durationMillis: Long
    ): List<PrivateSubtitleCue>? {
        if (startMillis !in startOptions || durationMillis !in durationOptions) return null
        val bytes = raw.toByteArray(Charsets.UTF_8)
        if (bytes.isEmpty() || bytes.size > MAX_BYTES ||
            bytes.toString(Charsets.UTF_8) != raw) return null
        val lines = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
            .lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty() || lines.size > MAX_CUES ||
            lines.first().startsWith("WEBVTT", ignoreCase = true) ||
            lines.any { line ->
                line.length > 800 || "-->" in line ||
                line.any { it == '\u0000' || (it.isISOControl() && it != '\t') }
            }) return null
        val cues = lines.mapIndexed { index, line ->
            val begin = startMillis + index.toLong() * durationMillis
            val end = begin + durationMillis
            if (end > MAX_TIME || end <= begin) return null
            PrivateSubtitleCue(begin, end, label(begin), label(end), line)
        }
        // Reuse the same portable SRT/VTT export boundaries as the editor.
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null) return null
        return cues
    }
}
