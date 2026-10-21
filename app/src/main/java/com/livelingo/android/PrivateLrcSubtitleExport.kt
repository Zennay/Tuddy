package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Deliberate export to start-timestamp-only LRC for a user-edited subtitle
 * draft. Unlike SRT/VTT, LRC has no cue end times; never claim end-time
 * preservation, word alignment or automatic recognition.
 */
internal object PrivateLrcSubtitleExport {
    private const val MAX_LRC_MINUTES = 999L

    fun snapshot(cues: List<PrivateSubtitleCue>): ByteArray? {
        if (cues.isEmpty() || cues.size > 500) return null
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
            return null
        val lines = ArrayList<String>(cues.size)
        var previousStart = -1L
        for (cue in cues) {
            val start = cue.startMillis
            val text = cue.text
            if (start <= previousStart || start < 0 ||
                start / 60_000L > MAX_LRC_MINUTES ||
                text.isBlank() || text != text.trim() || text.length > 800 ||
                "-->" in text || text.any { it.isISOControl() }) return null
            previousStart = start
            val minute = start / 60_000L
            val second = (start / 1000L) % 60L
            val millis = start % 1000L
            lines += String.format(Locale.ROOT, "[%02d:%02d.%03d]%s",
                minute, second, millis, text)
        }
        val output = lines.joinToString("\n", postfix = "\n")
        val bytes = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(output))
        }.getOrNull() ?: return null
        if (bytes.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        val array = ByteArray(bytes.remaining()).also { bytes.get(it) }
        // Guarantee our own strict single-start LRC reader accepts the copy.
        if (PrivateLrcSubtitleDraft.preview(output) == null) return null
        return array
    }
}
