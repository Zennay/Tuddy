package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

/** A new, explicitly user-saved TTML copy; never writes to source or infers audio. */
internal object PrivateTtmlSubtitleExport {
    fun snapshot(cues: List<PrivateSubtitleCue>): ByteArray? {
        if (cues.isEmpty() || cues.size > 500 ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
            return null
        val content = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<tt xmlns=\"http://www.w3.org/ns/ttml\">\n  <body><div>\n")
            for (cue in cues) {
                val start = stamp(cue.startMillis) ?: return null
                val end = stamp(cue.endMillis) ?: return null
                if (cue.endMillis <= cue.startMillis ||
                    cue.text.lines().size > 8 ||
                    cue.text.lines().any { it.isBlank() || it.length > 800 })
                    return null
                append("    <p begin=\"").append(start).append("\" end=\"")
                    .append(end).append("\">")
                cue.text.split('\n').forEachIndexed { index, line ->
                    if (index > 0) append("<br/>")
                    append(escape(line))
                }
                append("</p>\n")
            }
            append("  </div></body>\n</tt>\n")
        }
        val buffer = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(content))
        }.getOrNull() ?: return null
        if (buffer.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        // Refuse lossy exports: identical authored text and millisecond timings
        // must survive the same restricted reader used for file/share imports.
        val imported = PrivateTtmlSubtitleReader.read(content) ?: return null
        if (imported.size != cues.size || imported.indices.any { i ->
                imported[i].startMillis != cues[i].startMillis ||
                    imported[i].endMillis != cues[i].endMillis ||
                    imported[i].text != cues[i].text
            }) return null
        return bytes
    }

    private fun stamp(ms: Long): String? {
        if (ms !in 0L..359_999_999L) return null
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d",
            ms / 3_600_000L, ms / 60_000L % 60,
            ms / 1_000L % 60, ms % 1_000L)
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
