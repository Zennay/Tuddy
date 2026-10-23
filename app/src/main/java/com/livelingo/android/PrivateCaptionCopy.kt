package com.livelingo.android

/**
 * Builds a single explicit clipboard payload; never copies interim speech,
 * unknown translation state, or text from a different conversation revision.
 * Android's system clipboard is outside Tuddy's RAM-only privacy boundary.
 */
internal object PrivateCaptionCopy {
    private const val MAX_FIELD_UNITS = 4096
    private const val MAX_BYTES = 16_384

    fun snapshot(row: TranscriptExportRow): String? =
        snapshot(
            original = row.original,
            translated = row.translated,
            status = row.status,
            sourceCode = row.sourceCode,
            targetCode = row.targetCode
        )

    /** Safe bounded payload for a single explicitly copied completed caption. */
    fun snapshot(
        original: String,
        translated: String,
        status: String,
        sourceCode: String,
        targetCode: String
    ): String? {
        if (original.isBlank() || original.length > MAX_FIELD_UNITS ||
            sourceCode.isBlank() || targetCode.isBlank() ||
            sourceCode.length > 80 || targetCode.length > 80 ||
            translated.length > MAX_FIELD_UNITS
        ) return null

        fun clean(input: String): String =
            input.replace("\r\n", "\n").replace('\r', '\n')
                .filterNot { it == '\u0000' || (it.isISOControl() && it != '\n' && it != '\t') }
                .trim()

        val safeOriginal = clean(original)
        if (safeOriginal.isBlank()) return null
        val completed = status == "Lokaal" || status == "Lokaal vertaald"
        val target = if (completed && translated.isNotBlank()) clean(translated)
            else "[Vertaling niet voltooid]"
        val text = clean(sourceCode) + ": " + safeOriginal + "\n" +
            clean(targetCode) + ": " + target
        return text.takeIf { it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES }
    }
}
