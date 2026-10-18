package com.livelingo.android

import org.json.JSONArray
import org.json.JSONObject

/**
 * Opt-in machine-readable export of completed RAM-only conversation captions.
 * Nothing is persisted here: callers provide a user-selected SAF destination.
 * Pending translations are represented as null, never as translated text.
 */
internal object PrivateTranscriptJsonExport {
    private const val MAX_LINES = 500
    private const val MAX_FIELD_UNITS = 4096
    private const val MAX_BYTES = 262_144
    private const val MAX_FUTURE_DRIFT_MILLIS = 86_400_000L

    private fun safeField(value: String): Boolean =
        value.length <= MAX_FIELD_UNITS &&
            value.none { it == '\u0000' || (it.isISOControl() && it != '\n' && it != '\t' && it != '\r') }

    fun snapshot(rows: List<TranscriptExportRow>): ByteArray? {
        if (rows.isEmpty() || rows.size > MAX_LINES) return null

        val capturedAt = System.currentTimeMillis()
        val captions = JSONArray()
        for (row in rows) {
            if (row.recordedAtMillis <= 0 ||
                row.recordedAtMillis > capturedAt + MAX_FUTURE_DRIFT_MILLIS ||
                !safeField(row.original) || row.original.isBlank() ||
                !safeField(row.translated) ||
                !safeField(row.sourceCode) || row.sourceCode.isBlank() ||
                !safeField(row.targetCode) || row.targetCode.isBlank()
            ) return null

            val completed = row.status == "Lokaal" || row.status == "Lokaal vertaald"
            val translation = row.translated.takeIf { completed && it.isNotBlank() }
            val entry = JSONObject()
                .put("recorded_at_ms", row.recordedAtMillis)
                .put("source_language", row.sourceCode)
                .put("target_language", row.targetCode)
                .put("source_text", row.original)
                .put("translation_status", if (translation == null) "not_finished" else "complete")
                .put("translated_text", translation ?: JSONObject.NULL)
            captions.put(entry)
            // Count UTF-8 bytes instead of assuming Kotlin UTF-16 length equals file size.
            if (captions.toString().toByteArray(Charsets.UTF_8).size > MAX_BYTES) return null
        }

        val document = JSONObject()
            .put("format", "tuddy-private-transcript")
            .put("version", 1)
            .put("privacy", "Explicit user export; a chosen file provider may synchronize this file.")
            .put("captions", captions)
        return document.toString(2).toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
