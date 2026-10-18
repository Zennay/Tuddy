package com.livelingo.android

import java.util.Locale

/**
 * Ephemeral user edits to an explicitly selected subtitle text file.
 * No original file is modified: output is a new opt-in SAF document.
 * Input is untrusted and no text is interpreted as a verified translation.
 */
internal object PrivateSubtitleDraftEditor {
    private const val MAX_CUES = 500
    private const val MAX_BYTES = 262_144
    private const val MAX_TIME = 359_999_999L // 99:59:59.999

    fun revise(
        current: List<PrivateSubtitleCue>,
        position: Int,
        replacement: String
    ): List<PrivateSubtitleCue>? {
        if (position !in current.indices || current.size > MAX_CUES ||
            replacement.isBlank() || replacement.length > 2400 ||
            replacement.lines().size > 8 || replacement.lines().any { it.length > 800 } ||
            replacement != replacement.trim() ||
            replacement.any { it == '\u0000' || it == '\r' ||
                (it.isISOControl() && it != '\n' && it != '\t') }
        ) return null
        return current.toMutableList().apply {
            this[position] = this[position].copy(text = replacement)
        }
    }

    /** Move all cues by a fixed amount, preserving their relative positions. */
    fun shift(
        current: List<PrivateSubtitleCue>, milliseconds: Long
    ): List<PrivateSubtitleCue>? {
        if (current.isEmpty() || current.size > MAX_CUES ||
            milliseconds !in setOf(-2000L, -500L, 500L, 2000L)) return null
        if (current.any { cue ->
                cue.startMillis + milliseconds < 0L ||
                cue.endMillis + milliseconds > MAX_TIME ||
                cue.endMillis <= cue.startMillis
            }) return null
        return current.map {
            it.copy(
                startMillis = it.startMillis + milliseconds,
                endMillis = it.endMillis + milliseconds,
                startLabel = stamp(it.startMillis + milliseconds, SubtitleDraftFormat.SRT),
                endLabel = stamp(it.endMillis + milliseconds, SubtitleDraftFormat.SRT)
            )
        }
    }

    private fun stamp(time: Long, format: SubtitleDraftFormat): String {
        val separator = if (format == SubtitleDraftFormat.WEBVTT) "." else ","
        return String.format(Locale.ROOT, "%02d:%02d:%02d%s%03d",
            time / 3_600_000L, time / 60_000L % 60, time / 1000L % 60,
            separator, time % 1000L)
    }

    /** Create only a bounded, valid, portable UTF-8 subtitle copy. */
    fun snapshot(
        cues: List<PrivateSubtitleCue>, format: SubtitleDraftFormat
    ): ByteArray? {
        if (cues.isEmpty() || cues.size > MAX_CUES ||
            cues.any { it.startMillis < 0L || it.endMillis > MAX_TIME ||
                it.endMillis <= it.startMillis ||
                it.text.isBlank() || it.text != it.text.trim() ||
                it.text.length > 2400 || it.text.lines().size > 8 ||
                it.text.lines().any { line -> line.length > 800 } ||
                it.text.any { ch -> ch == '\u0000' || ch == '\r' ||
                    (ch.isISOControl() && ch != '\n' && ch != '\t') } } ||
            cues.zipWithNext().any { (first, next) ->
                next.startMillis < first.startMillis
            }) return null

        val text = buildString {
            if (format == SubtitleDraftFormat.WEBVTT) append("WEBVTT\n\n")
            cues.forEachIndexed { index, cue ->
                append(index + 1).append('\n')
                append(stamp(cue.startMillis, format))
                    .append(" --> ").append(stamp(cue.endMillis, format))
                append('\n').append(cue.text).append("\n\n")
            }
        }
        return text.toByteArray(Charsets.UTF_8).takeIf { it.size <= MAX_BYTES }
    }
}
