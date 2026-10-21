package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * User-supplied LRC line start times only. Cue ends are derived from the
 * following start, and the final end is explicitly estimated (+3 seconds).
 * No audio alignment, speech recognition, or translation is performed.
 */
internal object PrivateLrcSubtitleDraft {
    private const val MAX_ROWS = 550
    private const val MAX_CUES = 500
    private const val MAX_TIME_MS = 359_999_999L
    private val timestamp = Regex("""^\[(\d{1,3}):([0-5]\d)\.(\d{2,3})\](.+)$""")
    private val metadata = Regex("""^\[(?:ar|ti|al|by|length):[^\r\n\[\]]{0,200}\]$""")

    fun preview(raw: String): List<PrivateSubtitleCue>? {
        val bytes = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(raw))
        }.getOrNull() ?: return null
        if (bytes.remaining() == 0 ||
            bytes.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        val normalized = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n')
        if (normalized.any {
                it == '\u0000' || (it.isISOControl() && it != '\n' && it != '\t')
            }) return null
        val rows = normalized.split('\n')
        if (rows.size > MAX_ROWS) return null
        val times = mutableListOf<Pair<Long, String>>()
        var header = true
        for (rawLine in rows) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (header && metadata.matches(line)) continue
            header = false
            val match = timestamp.matchEntire(line) ?: return null
            val minute = match.groupValues[1].toLong()
            val second = match.groupValues[2].toLong()
            val fraction = match.groupValues[3].padEnd(3, '0').toLong()
            val start = minute * 60_000L + second * 1_000L + fraction
            val words = match.groupValues[4].trim()
            if (start >= MAX_TIME_MS || words.isEmpty() || words.length > 800 ||
                "-->" in words || words.any { it.isISOControl() }) return null
            if (times.lastOrNull()?.first?.let { start <= it } == true) return null
            times.add(start to words)
            if (times.size > MAX_CUES) return null
        }
        if (times.isEmpty()) return null
        val cues = times.mapIndexed { index, pair ->
            val start = pair.first
            val end = if (index + 1 < times.size) times[index + 1].first
                else minOf(MAX_TIME_MS, start + 3_000L)
            if (end <= start) return null
            PrivateSubtitleCue(start, end, label(start), label(end), pair.second)
        }
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
            return null
        return cues
    }

    private fun label(ms: Long) =
        String.format(Locale.ROOT, "%02d:%02d:%02d.%03d",
            ms / 3_600_000L, (ms / 60_000L) % 60L,
            (ms / 1_000L) % 60L, ms % 1_000L)
}
