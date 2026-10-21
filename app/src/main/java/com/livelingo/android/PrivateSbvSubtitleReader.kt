package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Explicitly picked YouTube SubViewer/SBV caption text.
 *
 * Input example:
 * 0:00:01.000,0:00:03.500
 * Hello world
 *
 * All timestamps come from the user-provided file. No media fetching,
 * subtitle recognition, markup execution, speech or translation.
 */
internal object PrivateSbvSubtitleReader {
    private const val MAX_CUES = 500
    private val timing = Regex(
        """^(\d{1,2}:[0-5]\d:[0-5]\d\.\d{3}),(\d{1,2}:[0-5]\d:[0-5]\d\.\d{3})$"""
    )

    fun read(raw: String): List<PrivateSubtitleCue>? {
        if (raw.isEmpty() || raw.length > PrivateSubtitleFileReader.MAX_BYTES)
            return null
        val encoded = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(raw))
        }.getOrNull() ?: return null
        if (encoded.remaining() > PrivateSubtitleFileReader.MAX_BYTES)
            return null
        val normalized = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n')
        if (normalized.any { ch ->
                ch == '\u0000' ||
                    (ch.isISOControl() && ch != '\n' && ch != '\t')
            }) return null
        val blocks = normalized.trim().split(Regex("""\n[ \t]*\n"""))
        if (blocks.size !in 1..MAX_CUES) return null
        val cues = ArrayList<PrivateSubtitleCue>(blocks.size)
        var previousStart = -1L
        for (block in blocks) {
            val rows = block.lines()
            if (rows.size !in 2..9) return null
            val match = timing.matchEntire(rows.first().trim()) ?: return null
            val start = PrivateSubtitleTimelineNavigation.timestamp(
                match.groupValues[1]
            ) ?: return null
            val end = PrivateSubtitleTimelineNavigation.timestamp(
                match.groupValues[2]
            ) ?: return null
            if (start < previousStart || end <= start) return null
            val textLines = rows.drop(1)
            if (textLines.any { it.length > 800 }) return null
            val text = textLines.joinToString("\n").trim()
            if (text.isEmpty() || text.length > 2400 ||
                text.any { it == '\u0000' ||
                    (it.isISOControl() && it != '\n' && it != '\t') })
                return null
            val startLabel = PrivateSubtitleTimelineNavigation
                .formatTimestamp(start) ?: return null
            val endLabel = PrivateSubtitleTimelineNavigation
                .formatTimestamp(end) ?: return null
            cues += PrivateSubtitleCue(start, end, startLabel, endLabel, text)
            previousStart = start
        }
        // The edit/export UI assumes any imported cue collection remains
        // representable in both offline subtitle interchange formats.
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
            return null
        return cues
    }
}
