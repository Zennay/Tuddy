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

    /** Exact, user-selected one-cue adjustment; never retime neighboring cues. */
    fun adjustCue(
        current: List<PrivateSubtitleCue>, index: Int,
        startChangeMs: Long, endChangeMs: Long
    ): List<PrivateSubtitleCue>? {
        if (index !in current.indices || current.size > MAX_CUES ||
            startChangeMs !in setOf(0L, -500L, 500L) ||
            endChangeMs !in setOf(0L, -500L, 500L) ||
            (startChangeMs == 0L && endChangeMs == 0L)) return null
        val old = current[index]
        val start = old.startMillis + startChangeMs
        val end = old.endMillis + endChangeMs
        if (start < 0L || end > MAX_TIME || start >= end ||
            (index > 0 && start < current[index - 1].startMillis) ||
            (index < current.lastIndex && start > current[index + 1].startMillis)
        ) return null
        return current.toMutableList().apply {
            this[index] = old.copy(startMillis = start, endMillis = end,
                startLabel = stamp(start, SubtitleDraftFormat.SRT),
                endLabel = stamp(end, SubtitleDraftFormat.SRT))
        }
    }

    /** Insert a fully user-authored cue at an exact user-typed time. */
    fun insertCue(
        current: List<PrivateSubtitleCue>,
        start: Long, end: Long, text: String
    ): List<PrivateSubtitleCue>? {
        if (current.isEmpty() || current.size >= MAX_CUES ||
            start < 0L || end > MAX_TIME || end <= start ||
            revise(current, 0, text) == null) return null
        val newCue = PrivateSubtitleCue(
            startMillis = start, endMillis = end,
            startLabel = stamp(start, SubtitleDraftFormat.SRT),
            endLabel = stamp(end, SubtitleDraftFormat.SRT),
            text = text)
        val index = current.indexOfFirst { it.startMillis > start }
            .let { if (it < 0) current.size else it }
        val result = current.toMutableList().apply { add(index, newCue) }
        if (snapshot(result, SubtitleDraftFormat.SRT) == null ||
            snapshot(result, SubtitleDraftFormat.WEBVTT) == null) return null
        return result
    }

    /** Deleting a cue edits only RAM and cannot remove the last row. */
    fun dropCue(current: List<PrivateSubtitleCue>, index: Int): List<PrivateSubtitleCue>? =
        if (current.size <= 1 || current.size > MAX_CUES || index !in current.indices) null
        else current.filterIndexed { position, _ -> position != index }

    /**
     * Split one cue into two user-authored text parts. The default boundary is
     * the midpoint; an explicit in-cue millisecond can be supplied instead.
     * Both pieces retain at least 600 ms and the new boundary may not cross
     * the next cue. This is manual editing, never speech alignment.
     */
    fun splitCue(
        current: List<PrivateSubtitleCue>, index: Int,
        first: String, second: String, splitAtMillis: Long? = null
    ): List<PrivateSubtitleCue>? {
        if (index !in current.indices || current.size >= MAX_CUES ||
            revise(current, index, first) == null ||
            revise(current, index, second) == null) return null
        val old = current[index]
        if (old.startMillis < 0L || old.endMillis > MAX_TIME ||
            old.endMillis <= old.startMillis ||
            old.endMillis - old.startMillis < 1200L) return null
        val midpoint = old.startMillis + (old.endMillis - old.startMillis) / 2
        val boundary = splitAtMillis ?: midpoint
        if (boundary - old.startMillis < 600L ||
            old.endMillis - boundary < 600L ||
            (index < current.lastIndex &&
                boundary > current[index + 1].startMillis)) return null
        val leading = old.copy(endMillis = boundary,
            endLabel = stamp(boundary, SubtitleDraftFormat.SRT), text = first)
        val trailing = old.copy(startMillis = boundary,
            startLabel = stamp(boundary, SubtitleDraftFormat.SRT), text = second)
        val result = current.toMutableList().apply {
            this[index] = leading
            add(index + 1, trailing)
        }
        if (snapshot(result, SubtitleDraftFormat.SRT) == null ||
            snapshot(result, SubtitleDraftFormat.WEBVTT) == null) return null
        return result
    }

    /** Merge a cue and the immediately following cue after explicit consent. */
    fun mergeWithNext(current: List<PrivateSubtitleCue>, index: Int):
        List<PrivateSubtitleCue>? {
        if (index !in 0 until current.lastIndex) return null
        val first = current[index]
        val next = current[index + 1]
        val combined = first.text + "\n" + next.text
        if (next.endMillis <= first.startMillis ||
            revise(current, index, combined) == null) return null
        return current.toMutableList().apply {
            this[index] = first.copy(endMillis = next.endMillis,
                endLabel = next.endLabel, text = combined)
            removeAt(index + 1)
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
