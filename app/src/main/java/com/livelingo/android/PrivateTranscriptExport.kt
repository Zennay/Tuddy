package com.livelingo.android

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * User-initiated, bounded plain-text export of completed RAM-only captions.
 *
 * This module never writes to disk or invokes a share sheet. The Android
 * Storage Access Framework destination is selected by the user separately.
 * An unfinished translator task is NOT presented as a completed translation.
 */
internal data class TranscriptExportRow(
    val original: String,
    val translated: String,
    val status: String,
    val sourceCode: String,
    val targetCode: String,
    val recordedAtMillis: Long
)

internal object PrivateTranscriptExport {
    private const val MAX_LINES = 500
    private const val MAX_FIELD_UNITS = 4096
    private const val MAX_BYTES = 262_144

    private fun printable(text: String): String =
        text.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')
            .map { char -> if (char.isISOControl()) ' ' else char }
            .joinToString("")
            .trim()

    fun snapshot(rows: List<TranscriptExportRow>): ByteArray? {
        if (rows.isEmpty() || rows.size > MAX_LINES) return null
        val result = StringBuilder(
            "Tuddy · privé gespreksexport\n" +
                "Door jou opgeslagen; het bestand kan buiten Tuddy worden gesynchroniseerd.\n\n"
        )
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        rows.forEachIndexed { index, row ->
            if (row.recordedAtMillis <= 0L || row.recordedAtMillis > System.currentTimeMillis() + 86_400_000L) {
                return null // No invented or future-dated evidence in a private export.
            }
            if (listOf(row.original, row.translated, row.sourceCode, row.targetCode)
                    .any { it.length > MAX_FIELD_UNITS }) return null
            val from = printable(row.sourceCode)
            val to = printable(row.targetCode)
            val original = printable(row.original)
            if (from.isBlank() || to.isBlank() || original.isBlank()) return null
            val isComplete = row.status == "Lokaal" || row.status == "Lokaal vertaald"
            val translation = if (isComplete) printable(row.translated) else "[Vertaling niet voltooid]"
            result.append(index + 1).append(". ").append(from).append(" → ").append(to)
                .append(" · ").append(formatter.format(Date(row.recordedAtMillis)))
                .append("\nOrigineel: ").append(original)
                .append("\nVertaling: ").append(translation).append("\n\n")
            if (result.length > MAX_BYTES) return null
        }
        val bytes = result.toString().toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= MAX_BYTES }
    }
}


/**
 * A separate, explicit opt-in TXT product for manually annotated turns.
 * Ordinary TXT/JSON archives intentionally remain label-free. These labels
 * are not speaker identities, voice recognition or verified attribution.
 */
internal object PrivateSpeakerTranscriptExport {
    private const val MAX_ROWS = 500
    private const val MAX_BYTES = 262_144

    fun snapshot(rows: List<Pair<TranscriptExportRow, String?>>): ByteArray? {
        if (rows.isEmpty() || rows.size > MAX_ROWS) return null
        // Inherit canonical timestamp, source language and row length admission.
        if (PrivateTranscriptExport.snapshot(rows.map { it.first }) == null) return null

        val text = StringBuilder(
            "Tuddy · handmatig gelabeld privégesprek\n" +
                "A/B/onbekend zijn door jou gekozen labels, geen stemherkenning of identiteit.\n" +
                "Dit bestand kan door je gekozen provider extern worden opgeslagen of gesynchroniseerd.\n\n"
        )
        rows.forEachIndexed { index, (row, label) ->
            if (label != null && label != "A" && label != "B") return null
            // Reuse the same privacy-normalized original + translated lines
            // as the existing single-caption opt-in clipboard feature.
            val payload = PrivateCaptionCopy.snapshot(row) ?: return null
            text.append(index + 1).append(". Spreker ")
                .append(label ?: "onbekend").append(" (handmatig)\n")
                .append("  ").append(payload.replace("\n", "\n  ")).append("\n\n")
            // Fail early before generating large documents.
            if (text.length > MAX_BYTES) return null
        }
        return text.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
