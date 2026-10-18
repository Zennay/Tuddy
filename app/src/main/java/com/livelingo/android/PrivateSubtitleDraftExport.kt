package com.livelingo.android

import java.util.Locale

/**
 * Explicit, bounded SRT editing draft for the current RAM-only conversation.
 *
 * Recognition callbacks record the end of an utterance, not the audio onset,
 * word timestamps or a synchronized media clock. Cue times below are only
 * rough relative positions derived from those completion timestamps. The app
 * must never describe them as synchronized/verified subtitles.
 *
 * No file, model, network request or clipboard is touched here. The caller
 * presents informed consent and Android's destination picker separately.
 */
internal object PrivateSubtitleDraftExport {
    private const val MAX_ROWS = 500
    private const val MAX_FIELD_UNITS = 4096
    private const val MAX_BYTES = 262_144
    private const val MAX_DURATION_MS = 86_399_000L
    private const val MIN_GAP_MS = 1200L
    private const val CUE_MS = 3500L

    private fun oneLine(value: String): String =
        value.replace('\r', ' ').replace('\n', ' ')
            .map { if (it.isISOControl()) ' ' else it }
            .joinToString("")
            .trim()

    private fun stamp(millis: Long): String {
        val hours = millis / 3_600_000L
        val minutes = millis / 60_000L % 60
        val seconds = millis / 1_000L % 60
        val remainder = millis % 1_000L
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d",
            hours, minutes, seconds, remainder)
    }

    fun snapshot(rows: List<TranscriptExportRow>): ByteArray? {
        if (rows.isEmpty() || rows.size > MAX_ROWS) return null
        val now = System.currentTimeMillis()
        // A pending/failed translation is not a confirmed subtitle. Its
        // original sentence is also excluded until the turn completes.
        val completed = rows.filter {
            it.status == "Lokaal" || it.status == "Lokaal vertaald"
        }
        if (completed.isEmpty()) return null

        val cues = mutableListOf<Pair<Long, Pair<String, String>>>()
        val origin = completed.first().recordedAtMillis
        var previousStart = -MIN_GAP_MS
        for (row in completed) {
            if (row.recordedAtMillis <= 0L ||
                row.recordedAtMillis > now + 86_400_000L ||
                listOf(row.original, row.translated, row.sourceCode, row.targetCode)
                    .any { it.length > MAX_FIELD_UNITS } ||
                row.sourceCode.isBlank() || row.targetCode.isBlank()
            ) return null
            val original = oneLine(row.original)
            if (original.isEmpty()) return null
            val translated = oneLine(row.translated)
            // Retain chronological transcript order even if device clock moves
            // backwards, or several recognition callbacks share a timestamp.
            val start = maxOf(
                (row.recordedAtMillis - origin).coerceAtLeast(0L),
                previousStart + MIN_GAP_MS
            )
            if (start > MAX_DURATION_MS - CUE_MS) return null
            cues.add(start to (original to translated))
            previousStart = start
        }

        val output = StringBuilder()
        cues.forEachIndexed { index, (start, caption) ->
            val next = cues.getOrNull(index + 1)?.first
            val end = if (next == null) start + CUE_MS
                else minOf(start + CUE_MS, next - 150L)
            output.append(index + 1).append('\n')
                .append(stamp(start)).append(" --> ").append(stamp(end))
                .append('\n').append(caption.first)
            if (caption.second.isNotBlank() && caption.second != caption.first) {
                output.append('\n').append(caption.second)
            }
            output.append("\n\n")
            if (output.length > MAX_BYTES) return null
        }
        val bytes = output.toString().toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= MAX_BYTES }
    }
}
