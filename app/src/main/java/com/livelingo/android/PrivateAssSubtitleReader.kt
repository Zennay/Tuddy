package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Bounded, non-executing reader for user-selected SubStation Alpha (SSA) and
 * Advanced SubStation Alpha (ASS) subtitle text. Styling, animation, script,
 * audio and vector-drawing events are not interpreted. Only timed Dialogue
 * text is retained for the existing private SRT/WebVTT editor.
 */
internal object PrivateAssSubtitleReader {
    private const val MAX_CUES = 500
    private const val MAX_LINES = 6_000
    private const val MAX_TIME_MS = 359_999_999L
    private const val MAX_CUE_TEXT = 2_400
    private val time = Regex("""^(\d{1,2}):([0-5]\d):([0-5]\d)\.(\d{2})$""")
    private val overrideBlock = Regex("""\{[^{}]*}""")
    private val vectorDrawing = Regex("""\{[^{}]*\\p[1-9]\d*[^{}]*}""", RegexOption.IGNORE_CASE)

    fun read(raw: String): List<PrivateSubtitleCue>? {
        if (raw.isBlank() || raw.length > PrivateSubtitleFileReader.MAX_BYTES) return null
        val encoded = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(raw))
        }.getOrNull() ?: return null
        if (encoded.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        if (raw.any { ch ->
                ch == '\u0000' || (ch.isISOControl() &&
                    ch != '\n' && ch != '\r' && ch != '\t')
            }) return null

        val normalized = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val lines = normalized.lines()
        if (lines.size > MAX_LINES ||
            lines.none { it.trim().equals("[Events]", ignoreCase = true) }) return null

        var inEvents = false
        var format: List<String>? = null
        var startIndex = -1
        var endIndex = -1
        var textIndex = -1
        var previousStart = -1L
        val cues = ArrayList<PrivateSubtitleCue>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("[") && line.endsWith("]")) {
                inEvents = line.equals("[Events]", ignoreCase = true)
                continue
            }
            if (!inEvents || line.isEmpty() || line.startsWith(";")) continue
            if (line.length > 4_096) return null

            val colon = line.indexOf(':')
            if (colon <= 0) continue
            when (line.substring(0, colon).trim().lowercase(Locale.ROOT)) {
                "format" -> {
                    if (line.length > 512) return null
                    val fields = line.substring(colon + 1)
                        .split(',')
                        .map { it.trim().lowercase(Locale.ROOT) }
                    if (fields.size !in 3..20 ||
                        fields.any { it.isEmpty() } ||
                        fields.count { it == "start" } != 1 ||
                        fields.count { it == "end" } != 1 ||
                        fields.count { it == "text" } != 1 ||
                        fields.lastOrNull() != "text") return null
                    if (format != null && format != fields) return null
                    format = fields
                    startIndex = fields.indexOf("start")
                    endIndex = fields.indexOf("end")
                    textIndex = fields.indexOf("text")
                }
                "dialogue" -> {
                    val fields = format ?: return null
                    val payload = line.substring(colon + 1).trimStart()
                    val values = splitFields(payload, fields.size) ?: return null
                    val startLabel = values[startIndex].trim()
                    val endLabel = values[endIndex].trim()
                    val start = toMillis(startLabel) ?: return null
                    val end = toMillis(endLabel) ?: return null
                    if (start < previousStart || end <= start) return null
                    previousStart = start

                    val originalText = values[textIndex]
                    // Vector drawing data is not a caption. Drop that cue
                    // rather than exposing ASS drawing instructions as prose.
                    if (vectorDrawing.containsMatchIn(originalText)) continue
                    val text = cleanText(originalText) ?: return null
                    if (text.isEmpty()) continue
                    val rows = text.lines()
                    if (text.length > MAX_CUE_TEXT || rows.size > 8 ||
                        rows.any { it.isBlank() || it.length > 800 }) return null
                    cues.add(
                        PrivateSubtitleCue(
                            startMillis = start,
                            endMillis = end,
                            startLabel = stamp(start),
                            endLabel = stamp(end),
                            text = text
                        )
                    )
                    if (cues.size > MAX_CUES) return null
                }
                // Comments, pictures, sounds, movies and commands are not text cues.
                else -> Unit
            }
        }

        if (cues.isEmpty()) return null
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null) return null
        return cues
    }

    /** Split only the declared non-text columns; preserve commas inside Text. */
    private fun splitFields(payload: String, count: Int): List<String>? {
        val result = ArrayList<String>(count)
        var from = 0
        repeat(count - 1) {
            val separator = payload.indexOf(',', from)
            if (separator < 0) return null
            result.add(payload.substring(from, separator))
            from = separator + 1
        }
        result.add(payload.substring(from))
        return result
    }

    private fun toMillis(label: String): Long? {
        val match = time.matchEntire(label) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: return null
        val minutes = match.groupValues[2].toLongOrNull() ?: return null
        val seconds = match.groupValues[3].toLongOrNull() ?: return null
        val centiseconds = match.groupValues[4].toLongOrNull() ?: return null
        val milliseconds = hours * 3_600_000L + minutes * 60_000L +
            seconds * 1_000L + centiseconds * 10L
        return milliseconds.takeIf { it in 0L..MAX_TIME_MS }
    }

    private fun stamp(milliseconds: Long): String =
        String.format(
            Locale.ROOT, "%02d:%02d:%02d,%03d",
            milliseconds / 3_600_000L,
            milliseconds / 60_000L % 60,
            milliseconds / 1_000L % 60,
            milliseconds % 1_000L
        )

    private fun cleanText(original: String): String? {
        if (original.length > MAX_CUE_TEXT * 2) return null
        // ASS escapes literal braces; protect them before removing override tags.
        val protected = original.replace("\\{", "\uE000")
            .replace("\\}", "\uE001")
        val stripped = overrideBlock.replace(protected, "")
        if ('{' in stripped || '}' in stripped) return null
        val readable = stripped
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace("\\h", "\u00A0")
            .replace("\uE000", "{")
            .replace("\uE001", "}")
            .trim()
        if (readable.any { ch ->
                ch == '\u0000' || ch == '\r' ||
                    (ch.isISOControl() && ch != '\n' && ch != '\t')
            }) return null
        return readable
    }
}
