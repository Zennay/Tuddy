package com.livelingo.android

import java.util.Locale

internal enum class SubtitleDraftMode {
    BILINGUAL, ORIGINAL_ONLY, TRANSLATION_ONLY
}

internal enum class SubtitleDraftFormat { SRT, WEBVTT }

/**
 * Deliberately coarse manual adjustments for a rough SRT editing draft.
 * Values are constrained to the choices exposed in the UI.
 */
internal data class SubtitleDraftOptions(
    val mode: SubtitleDraftMode = SubtitleDraftMode.BILINGUAL,
    val startOffsetMs: Long = 0L,
    val cueDurationMs: Long = 3500L,
    val format: SubtitleDraftFormat = SubtitleDraftFormat.SRT
)

/**
 * Explicit, bounded SRT or WebVTT editing draft. Completion callbacks are NOT the real
 * audio onset, word timestamps or a synchronized media clock. The manual
 * offset/duration settings remain estimates and never verify alignment.
 *
 * This module performs no persistence, browser, network or share operations.
 */
internal object PrivateSubtitleDraftExport {
    private const val MAX_ROWS = 500
    private const val MAX_FIELD_UNITS = 4096
    private const val MAX_BYTES = 262_144
    private const val MAX_DURATION_MS = 86_399_000L
    private const val MIN_GAP_MS = 1200L

    private val ALLOWED_OFFSETS = setOf(0L, 5000L, 10000L, 30000L, 60000L)
    private val ALLOWED_DURATIONS = setOf(2000L, 3500L, 5000L, 7000L)

    private fun oneLine(value: String): String =
        value.replace('\r', ' ').replace('\n', ' ')
            .map { if (it.isISOControl()) ' ' else it }
            .joinToString("")
            .trim()

    private fun stamp(millis: Long, format: SubtitleDraftFormat): String {
        val hours = millis / 3_600_000L
        val minutes = millis / 60_000L % 60
        val seconds = millis / 1_000L % 60
        val remainder = millis % 1_000L
        val punctuation = if (format == SubtitleDraftFormat.WEBVTT) "." else ","
        return String.format(Locale.ROOT, "%02d:%02d:%02d%s%03d",
            hours, minutes, seconds, punctuation, remainder)
    }

    /** Preserve the original bilingual behavior for existing callers. */
    fun snapshot(rows: List<TranscriptExportRow>): ByteArray? =
        snapshot(rows, SubtitleDraftOptions())

    fun snapshot(rows: List<TranscriptExportRow>, options: SubtitleDraftOptions): ByteArray? {
        if (rows.isEmpty() || rows.size > MAX_ROWS ||
            options.startOffsetMs !in ALLOWED_OFFSETS ||
            options.cueDurationMs !in ALLOWED_DURATIONS
        ) return null

        val now = System.currentTimeMillis()
        val finished = rows.filter {
            it.status == "Lokaal" || it.status == "Lokaal vertaald"
        }
        val completed = if (options.mode == SubtitleDraftMode.TRANSLATION_ONLY) {
            finished.filter {
                it.status == "Lokaal vertaald" && oneLine(it.translated).isNotBlank()
            }
        } else finished
        // Do not silently fill translation-only drafts with source sentences.
        if (completed.isEmpty()) return null

        val cues = mutableListOf<Pair<Long, Pair<String, String>>>()
        val origin = completed.first().recordedAtMillis
        var previousStart = options.startOffsetMs - MIN_GAP_MS
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
            val start = maxOf(
                (row.recordedAtMillis - origin).coerceAtLeast(0L) + options.startOffsetMs,
                previousStart + MIN_GAP_MS
            )
            if (start > MAX_DURATION_MS - options.cueDurationMs) return null
            val first = if (options.mode == SubtitleDraftMode.TRANSLATION_ONLY)
                translated else original
            val second = if (options.mode == SubtitleDraftMode.BILINGUAL)
                translated else ""
            if (first.isBlank()) return null
            cues.add(start to (first to second))
            previousStart = start
        }

        val output = StringBuilder()
        if (options.format == SubtitleDraftFormat.WEBVTT) output.append("WEBVTT\n\n")
        cues.forEachIndexed { index, (start, caption) ->
            val next = cues.getOrNull(index + 1)?.first
            val end = if (next == null) start + options.cueDurationMs
                else minOf(start + options.cueDurationMs, next - 150L)
            output.append(index + 1).append('\n')
                .append(stamp(start, options.format)).append(" --> ")
                .append(stamp(end, options.format))
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
