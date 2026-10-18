package com.livelingo.android

/**
 * Builds a single explicit clipboard payload; never copies interim speech,
 * unknown translation state, or text from a different conversation revision.
 * Android's system clipboard is outside Tuddy's RAM-only privacy boundary.
 */
internal object PrivateCaptionCopy {
    private const val MAX_FIELD_UNITS = 4096
    private const val MAX_BYTES = 16_384

    fun snapshot(row: TranscriptExportRow): String? {
        if (row.original.isBlank() || row.original.length > MAX_FIELD_UNITS ||
            row.sourceCode.isBlank() || row.targetCode.isBlank() ||
            row.sourceCode.length > 80 || row.targetCode.length > 80 ||
            row.translated.length > MAX_FIELD_UNITS
        ) return null

        fun clean(input: String): String =
            input.replace("\r\n", "\n").replace('\r', '\n')
                .filterNot { it == '\u0000' || (it.isISOControl() && it != '\n' && it != '\t') }
                .trim()

        val original = clean(row.original)
        if (original.isBlank()) return null
        val completed = row.status == "Lokaal" || row.status == "Lokaal vertaald"
        val target = if (completed && row.translated.isNotBlank()) clean(row.translated)
            else "[Vertaling niet voltooid]"
        val text = clean(row.sourceCode) + ": " + original + "\n" +
            clean(row.targetCode) + ": " + target
        return text.takeIf { it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES }
    }
}
