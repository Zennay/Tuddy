package com.livelingo.android

import java.util.Locale

/**
 * User-authored line, sentence or explicitly paired text cue draft with
 * chosen synthetic durations. These are NOT speech timestamps, word alignment, or translations.
 * Only used after deliberate manual input; no audio, clipboard, or I/O.
 */
internal enum class PlainSubtitleSplitMode { LINES, SENTENCES, PAIRED_COLUMNS }

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
        durationMillis: Long,
        splitMode: PlainSubtitleSplitMode = PlainSubtitleSplitMode.LINES
    ): List<PrivateSubtitleCue>? {
        if (startMillis !in startOptions || durationMillis !in durationOptions) return null
        val bytes = raw.toByteArray(Charsets.UTF_8)
        if (bytes.isEmpty() || bytes.size > MAX_BYTES ||
            bytes.toString(Charsets.UTF_8) != raw) return null
        val normalized = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        // Never classify natural sentences as recognized speech: this merely
        // suggests cut points at punctuation followed by whitespace.
        val rows = when (splitMode) {
            PlainSubtitleSplitMode.LINES -> normalized.lines()
                .map { it.trim() }.filter { it.isNotEmpty() }
            PlainSubtitleSplitMode.SENTENCES -> normalized.trim()
                .replace(Regex("""[ \t\n]+"""), " ")
                .split(Regex("""(?<=[.!?。！？؟])\s+"""))
                .map { it.trim() }.filter { it.isNotEmpty() }
            PlainSubtitleSplitMode.PAIRED_COLUMNS -> normalized.lines()
                .map { it.trim() }.filter { it.isNotEmpty() }.map { row ->
                    val columns = row.split('\t')
                    // Require exactly two explicit, nonempty author-supplied
                    // fields. Do not invent a missing translation or split
                    // language text heuristically.
                    if (columns.size != 2) return null
                    val first = columns[0].trim()
                    val second = columns[1].trim()
                    if (first.isEmpty() || second.isEmpty() ||
                        first.length > 800 || second.length > 800) return null
                    first + "\n" + second
                }
        }
        if (rows.isEmpty() || rows.size > MAX_CUES ||
            rows.first().startsWith("WEBVTT", ignoreCase = true) ||
            rows.any { row ->
                row.length > 2400 || row.lines().size > 2 ||
                row.lines().any { it.length > 800 } || "-->" in row ||
                row.any { it == '\u0000' || (it.isISOControl() && it != '\n') }
            }) return null
        val cues = rows.mapIndexed { index, row ->
            val begin = startMillis + index.toLong() * durationMillis
            val end = begin + durationMillis
            if (end > MAX_TIME || end <= begin) return null
            PrivateSubtitleCue(begin, end, label(begin), label(end), row)
        }
        // Reuse the same portable SRT/VTT export boundaries as the editor.
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null) return null
        return cues
    }
}
