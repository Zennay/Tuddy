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
    // Consecutive [mm:ss.xx] tags refer to the *same* lyric in repeated choruses.
    private val timestampTag = Regex("""\[(\d{1,3}):([0-5]\d)\.(\d{2,3})\]""")
    private const val MAX_TIMESTAMPS_PER_LINE = 16
    // User-provided whole-track calibration, not discovered from audio.
    private const val MAX_OFFSET_MS = 30_000L
    private val offsetHeader = Regex("""^\[offset:([+-]?[0-9]{1,5})\]$""")
    private val metadata = Regex("""^\[(?:ar|ti|al|by|length):[^\r\n\[\]]{0,200}\]$""")

    /**
     * Only a header before the first lyric can move cues.
     * The Android preview calls this only for a successfully parsed document.
     */
    fun declaredOffsetMillis(raw: String): Long? {
        val lines = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n').lineSequence()
        for (source in lines) {
            val line = source.trim()
            if (line.isEmpty() || metadata.matches(line)) continue
            return offsetHeader.matchEntire(line)?.groupValues?.get(1)?.toLongOrNull()
        }
        return null
    }

    /**
     * Explicit editor-only change: replace or insert the single LRC header.
     * A valid existing document must stay valid after rewriting; neither the
     * Android-picked original file nor any audio is touched.
     */
    fun withDeclaredOffset(raw: String, newOffsetMs: Long): String? {
        if (newOffsetMs !in -MAX_OFFSET_MS..MAX_OFFSET_MS || preview(raw) == null)
            return null
        val lines = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n').split('\n').toMutableList()
        var existingOffset = -1
        for ((index, source) in lines.withIndex()) {
            val line = source.trim()
            if (line.isEmpty() || metadata.matches(line)) continue
            if (offsetHeader.matches(line)) existingOffset = index
            break
        }
        // Removing a header keeps all actual text unchanged. Inserting a new
        // one happens at the start, before authored lyric rows and metadata.
        if (existingOffset >= 0) lines.removeAt(existingOffset)
        if (newOffsetMs != 0L) {
            val header = "[offset:" +
                (if (newOffsetMs > 0L) "+" else "") + newOffsetMs + "]"
            lines.add(if (existingOffset >= 0) existingOffset else 0, header)
        }
        val candidate = lines.joinToString("\n")
        return candidate.takeIf { preview(it) != null }
    }

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
        var offsetMs = 0L
        var offsetWasDeclared = false
        for (rawLine in rows) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (header && metadata.matches(line)) continue
            if (header && line.startsWith("[offset:")) {
                if (offsetWasDeclared) return null
                val millis = offsetHeader.matchEntire(line)?.groupValues?.get(1)
                    ?.toLongOrNull() ?: return null
                if (millis !in -MAX_OFFSET_MS..MAX_OFFSET_MS) return null
                offsetWasDeclared = true
                offsetMs = millis
                continue
            }
            header = false

            // LRC allows repeated occurrences of one lyric in a single row:
            // [00:01.25][01:05.80]same chorus text. Each explicit tag becomes
            // an independent cue; no text repetition or timing is invented.
            var position = 0
            val starts = ArrayList<Long>(2)
            while (position < line.length && line[position] == '[') {
                val match = timestampTag.find(line, position)
                    ?.takeIf { it.range.first == position } ?: break
                val minute = match.groupValues[1].toLong()
                val second = match.groupValues[2].toLong()
                val fraction = match.groupValues[3].padEnd(3, '0').toLong()
                val authoredStart = minute * 60_000L + second * 1_000L + fraction
                val start = authoredStart + offsetMs
                if (start !in 0L until MAX_TIME_MS) return null
                starts.add(start)
                if (starts.size > MAX_TIMESTAMPS_PER_LINE ||
                    times.size + starts.size > MAX_CUES) return null
                position = match.range.last + 1
            }
            if (starts.isEmpty()) return null
            val words = line.substring(position).trim()
            if (words.isEmpty() || words.length > 800 ||
                "-->" in words || words.any { it.isISOControl() }) return null
            for (start in starts) times.add(start to words)
        }
        if (times.isEmpty()) return null

        // A repeated lyric may list its second occurrence long before the
        // next physical LRC row. Restore chronological cue order explicitly.
        // Identical timestamps are ambiguous and must never silently overwrite.
        val ordered = times.sortedBy { it.first }
        if (ordered.zipWithNext().any { (a, b) -> a.first >= b.first })
            return null
        val cues = ordered.mapIndexed { index, pair ->
            val start = pair.first
            val end = if (index + 1 < ordered.size) ordered[index + 1].first
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
