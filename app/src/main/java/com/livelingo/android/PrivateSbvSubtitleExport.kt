package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Explicit user-consented export of current in-memory subtitle edits as
 * YouTube/SubViewer SBV. Never touches the source file or guesses cue times.
 */
internal object PrivateSbvSubtitleExport {
    fun snapshot(cues: List<PrivateSubtitleCue>): ByteArray? {
        if (cues.isEmpty() || cues.size > 500 ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
            return null
        fun stamp(ms: Long): String? {
            if (ms !in 0L..359_999_999L) return null
            return String.format(Locale.ROOT, "%d:%02d:%02d.%03d",
                ms / 3_600_000L, ms / 60_000L % 60L,
                ms / 1_000L % 60L, ms % 1_000L)
        }
        val source = buildString {
            for (cue in cues) {
                val start = stamp(cue.startMillis) ?: return null
                val end = stamp(cue.endMillis) ?: return null
                if (end == start || cue.text.lines().size > 8 ||
                    cue.text.any { it == '\r' || it == '\u0000' } ||
                    cue.text.lines().any { it.isBlank() }) return null
                append(start).append(',').append(end).append('\n')
                append(cue.text).append("\n\n")
            }
        }
        val encoded = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(source))
        }.getOrNull() ?: return null
        if (encoded.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        val bytes = ByteArray(encoded.remaining()).also { encoded.get(it) }
        // Fail closed when YouTube/SBV import would lose any cue/time/text.
        val decoded = PrivateSbvSubtitleReader.read(source) ?: return null
        if (decoded.size != cues.size ||
            decoded.indices.any { i ->
                decoded[i].startMillis != cues[i].startMillis ||
                    decoded[i].endMillis != cues[i].endMillis ||
                    decoded[i].text != cues[i].text
            }) return null
        return bytes
    }
}
