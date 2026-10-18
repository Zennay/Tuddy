package com.livelingo.android

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Bounded, read-only handling of a document explicitly selected through SAF.
 * Subtitle cues are untrusted external text, NOT audio-verified timestamps
 * or validated translations. This parser performs no I/O or model activity.
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
    private val timing = Regex(
        """^(\d{2,3}):([0-5]\d):([0-5]\d)[,.](\d{3}) --> (\d{2,3}):([0-5]\d):([0-5]\d)[,.](\d{3})$"""
    )

    private fun millis(groups: MatchGroupCollection, offset: Int): Long =
        (groups[offset]!!.value.toLong() * 3_600_000) +
            (groups[offset + 1]!!.value.toLong() * 60_000) +
            (groups[offset + 2]!!.value.toLong() * 1_000) +
            groups[offset + 3]!!.value.toLong()

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
        val cueBlocks = if (blocks.firstOrNull()?.startsWith("WEBVTT") == true) {
            if (blocks[0].trim() != "WEBVTT") return null
            blocks.drop(1)
        } else blocks
        if (cueBlocks.isEmpty() || cueBlocks.size > MAX_CUES) return null
        val cues = mutableListOf<PrivateSubtitleCue>()
        for (block in cueBlocks) {
            val rows = block.trim().lines()
            // The optional numeric SRT index is not the cue content.
            val offset = if (rows.firstOrNull()?.matches(Regex("""[0-9]{1,5}""")) == true)
                1 else 0
            if (rows.size < offset + 2) return null
            val line = rows[offset].trim()
            val match = timing.matchEntire(line) ?: return null
            val first = millis(match.groups, 1)
            val last = millis(match.groups, 5)
            if (last <= first || (cues.lastOrNull()?.startMillis ?: -1L) > first)
                return null
            val payload = rows.drop(offset + 1)
            if (payload.isEmpty() || payload.size > 8 ||
                payload.any { it.length > 800 }) return null
            val text = payload.joinToString("\n").trim()
            if (text.isBlank() || text.length > 2400) return null
            cues.add(PrivateSubtitleCue(first, last,
                line.substringBefore(" --> "),
                line.substringAfter(" --> "),
                text))
        }
        return cues.takeIf { it.isNotEmpty() }
    }
}
