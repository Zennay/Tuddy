package com.livelingo.android

import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only inspection of a user-selected JSON archive. Never imports records
 * into a live speech session, persistence, translation engines or preferences.
 */
internal object PrivateTranscriptArchive {
    private const val MAX_BYTES = 262_144
    private const val MAX_ROWS = 500
    private const val MAX_FIELD_UNITS = 4096
    private const val MAX_FUTURE_DRIFT_MILLIS = 86_400_000L

    private val ROOT_FIELDS = setOf("format", "version", "privacy", "captions")
    private val CAPTION_FIELDS = setOf(
        "recorded_at_ms", "source_language", "target_language",
        "source_text", "translation_status", "translated_text"
    )

    private fun exactFields(json: JSONObject, expected: Set<String>): Boolean {
        if (json.length() != expected.size) return false
        return expected.all { json.has(it) }
    }

    private fun cleanText(value: String, required: Boolean = false): Boolean =
        value.length <= MAX_FIELD_UNITS &&
            (!required || value.isNotBlank()) &&
            value.none { it == '\u0000' || (it.isISOControl() && it != '\r' && it != '\n' && it != '\t') }

    fun read(bytes: ByteArray): List<TranscriptExportRow>? {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
        val utf8 = bytes.toString(Charsets.UTF_8)
        // Kotlin's default UTF-8 decoder replaces invalid sequences. Refuse
        // those bytes rather than displaying altered user-supplied evidence.
        if (!utf8.toByteArray(Charsets.UTF_8).contentEquals(bytes) ||
            !StrictJsonKeyGuard.accepts(utf8)) return null
        return try {
            val root = JSONObject(utf8)
            if (!exactFields(root, ROOT_FIELDS) ||
                root.opt("format") != "tuddy-private-transcript" ||
                root.opt("version") != 1 ||
                root.opt("privacy") !is String ||
                root.opt("captions") !is JSONArray
            ) return null
            val records = root.getJSONArray("captions")
            if (records.length() !in 1..MAX_ROWS) return null

            val latest = System.currentTimeMillis() + MAX_FUTURE_DRIFT_MILLIS
            val result = ArrayList<TranscriptExportRow>(records.length())
            for (i in 0 until records.length()) {
                val item = records.optJSONObject(i) ?: return null
                if (!exactFields(item, CAPTION_FIELDS)) return null
                val time = item.opt("recorded_at_ms") as? Long ?: return null
                val sourceCode = item.opt("source_language") as? String ?: return null
                val targetCode = item.opt("target_language") as? String ?: return null
                val original = item.opt("source_text") as? String ?: return null
                val status = item.opt("translation_status") as? String ?: return null
                val translatedValue = item.opt("translated_text")
                if (time <= 0L || time > latest ||
                    !cleanText(sourceCode, required = true) ||
                    !cleanText(targetCode, required = true) ||
                    !cleanText(original, required = true)
                ) return null
                val complete = when (status) {
                    "complete" -> true
                    "not_finished" -> false
                    else -> return null
                }
                val translated = when {
                    complete && translatedValue is String &&
                        cleanText(translatedValue, required = true) -> translatedValue
                    !complete && translatedValue == JSONObject.NULL -> ""
                    else -> return null
                }
                result.add(
                    TranscriptExportRow(
                        original = original,
                        translated = translated,
                        status = if (complete) "Lokaal vertaald" else "Niet voltooid",
                        sourceCode = sourceCode,
                        targetCode = targetCode,
                        recordedAtMillis = time
                    )
                )
            }
            result
        } catch (_: Exception) {
            null
        }
    }
}
