package com.livelingo.android

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Parse untrusted, explicitly selected SRT / common WebVTT text documents.
 * This never executes cue markup, audio, external references or model calls.
 * VTT NOTE/STYLE/REGION metadata is ignored; only timecoded cue text is shown.
 */
internal data class PrivateSubtitleCue(
    val startMillis: Long,
    val endMillis: Long,
    val startLabel: String,
    val endLabel: String,
    val text: String
)

internal object PrivateSubtitleFileReader {
    const val MAX_BYTES = 262_144
    private const val MAX_CUES = 500
    private const val MAX_TIME = 359_999_999L
    // HH:MM:SS or WebVTT's shorter MM:SS; a comma is also accepted for SRT.
    private val time = Regex("""^(?:(\d{2,3}):)?([0-5]\d):([0-5]\d)[,.](\d{3})$""")
    private val cueTiming = Regex("""^(\S+)[ \t]+-->[ \t]+(\S+)(?:[ \t]+.*)?$""")
    private val vttHeader = Regex("""^WEBVTT(?:[ \t].*)?$""")

    private fun toMillis(label: String): Long? {
        val match = time.matchEntire(label) ?: return null
        val hours = match.groupValues[1].ifEmpty { "0" }.toLong()
        val minutes = match.groupValues[2].toLong()
        val seconds = match.groupValues[3].toLong()
        val milliseconds = match.groupValues[4].toLong()
        val sum = hours * 3_600_000L + minutes * 60_000L +
            seconds * 1_000L + milliseconds
        return sum.takeIf { it <= MAX_TIME }
    }

    fun read(bytes: ByteArray): List<PrivateSubtitleCue>? {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
        val decoded = runCatching {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        }.getOrNull() ?: return null
        val normalized = decoded.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n')
        if (normalized.any { ch ->
            ch == '\u0000' || (ch.isISOControl() && ch != '\n' && ch != '\t')
        }) return null
        val blocks = normalized.trim().split(Regex("""\n[ \t]*\n"""))
        if (blocks.isEmpty()) return null
        val firstBlock = blocks[0].trim()
        val headerLines = firstBlock.lines()
        val firstLine = headerLines.firstOrNull().orEmpty()
        val headerIsVtt = vttHeader.matches(firstLine) && !firstLine.contains("-->")
        // Some real WebVTT subtitle providers put Kind/Language header fields
        // before the first blank line. These are descriptive only: reject
        // timestamp-map or unknown directives rather than misaligning cues.
        val supportedHeader = headerLines.size <= 8 &&
            headerLines.drop(1).all { line ->
                line.length <= 200 &&
                    (line.startsWith("Kind: ") || line.startsWith("Language: ")) &&
                    !line.contains("-->")
            }
        val isWebVtt = headerIsVtt && supportedHeader
        if (!isWebVtt && firstLine.startsWith("WEBVTT")) return null
        val payloadBlocks = if (isWebVtt) blocks.drop(1) else blocks
        if (payloadBlocks.isEmpty() || payloadBlocks.size > MAX_CUES + 100) return null
        val cues = mutableListOf<PrivateSubtitleCue>()
        for (block in payloadBlocks) {
            val rows = block.trim().lines()
            if (rows.isEmpty()) continue
            val leading = rows.first().trim()
            if (isWebVtt && (leading == "NOTE" || leading.startsWith("NOTE ") ||
                    leading == "STYLE" || leading == "REGION")) {
                // Metadata is deliberately not interpreted, exposed or exported.
                continue
            }
            // SRT may contain a numeric index; WebVTT may contain any cue ID.
            // Neither is evidence of a speaker, a language or a translation.
            val offset = if (rows[0].contains("-->")) 0 else 1
            if (rows.size < offset + 2) return null
            val timing = cueTiming.matchEntire(rows[offset].trim()) ?: return null
            val first = toMillis(timing.groupValues[1]) ?: return null
            val last = toMillis(timing.groupValues[2]) ?: return null
            if (last <= first ||
                (cues.lastOrNull()?.startMillis ?: -1L) > first) return null
            val textRows = rows.drop(offset + 1)
            if (textRows.isEmpty() || textRows.size > 8 ||
                textRows.any { it.length > 800 }) return null
            val cueText = textRows.joinToString("\n").trim()
            if (cueText.isEmpty() || cueText.length > 2400) return null
            cues.add(PrivateSubtitleCue(
                first, last, timing.groupValues[1], timing.groupValues[2], cueText))
            if (cues.size > MAX_CUES) return null
        }
        return cues.takeIf { it.isNotEmpty() }
    }
}
