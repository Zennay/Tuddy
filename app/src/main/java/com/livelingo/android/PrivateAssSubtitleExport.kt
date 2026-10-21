package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * An explicit, local, unstyled ASS copy for the user's existing subtitle
 * draft. ASS stores centiseconds: never round a millisecond timestamp.
 * The output is re-imported with the same bounded reader used for imports,
 * and rejected if ANY original cue text or timing would change.
 */
internal object PrivateAssSubtitleExport {
    fun snapshot(cues: List<PrivateSubtitleCue>): ByteArray? {
        if (cues.isEmpty() || cues.size > 500 ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null ||
            cues.any { it.startMillis % 10L != 0L || it.endMillis % 10L != 0L })
            return null

        val content = buildString {
            append("[Script Info]\n")
            append("Title: Tuddy private subtitle copy\n")
            append("ScriptType: v4.00+\n")
            append("WrapStyle: 0\n\n")
            append("[V4+ Styles]\n")
            append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, ")
            append("OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ")
            append("ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, ")
            append("Alignment, MarginL, MarginR, MarginV, Encoding\n")
            append("Style: Default,Arial,36,&H00FFFFFF,&H000000FF,&H00000000,")
            append("&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,20,20,20,1\n\n")
            append("[Events]\n")
            append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, ")
            append("MarginV, Effect, Text\n")
            for (cue in cues) {
                val start = stamp(cue.startMillis) ?: return null
                val end = stamp(cue.endMillis) ?: return null
                if (cue.endMillis <= cue.startMillis) return null
                append("Dialogue: 0,").append(start).append(',')
                    .append(end).append(",Default,,0,0,0,,")
                for (character in cue.text) {
                    when (character) {
                        '\n' -> append("\\N")
                        '{' -> append("\\{")
                        '}' -> append("\\}")
                        else -> append(character)
                    }
                }
                append('\n')
            }
        }
        val buffer = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(content))
        }.getOrNull() ?: return null
        if (buffer.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        val result = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val restored = PrivateAssSubtitleReader.read(content) ?: return null
        if (restored.size != cues.size || restored.indices.any { index ->
                restored[index].startMillis != cues[index].startMillis ||
                    restored[index].endMillis != cues[index].endMillis ||
                    restored[index].text != cues[index].text
            }) return null
        return result
    }

    private fun stamp(ms: Long): String? {
        if (ms !in 0L..359_999_999L || ms % 10L != 0L) return null
        return String.format(Locale.ROOT, "%d:%02d:%02d.%02d",
            ms / 3_600_000L, ms / 60_000L % 60L,
            ms / 1_000L % 60L, ms % 1_000L / 10L)
    }
}
