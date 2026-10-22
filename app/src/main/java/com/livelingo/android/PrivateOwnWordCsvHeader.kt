package com.livelingo.android

/**
 * Recognizes only Tuddy's exact three-column own-word CSV header.
 *
 * CSV tools may quote individual header cells while preserving their values.
 * Keep this lightweight classifier aligned with the strict full parser; it
 * never validates/imports rows or bypasses the destination preview.
 */
internal object PrivateOwnWordCsvHeader {
    private const val MAX_HEADER_CHARS = 64
    private val columns = listOf("language", "word", "meaning")

    fun matches(text: String): Boolean {
        val csv = text.removePrefix("\uFEFF")
        val lineEnd = csv.indexOfAny(charArrayOf('\r', '\n'))
        val header = if (lineEnd < 0) {
            csv
        } else {
            if (csv[lineEnd] == '\r' && csv.getOrNull(lineEnd + 1) != '\n')
                return false
            csv.substring(0, lineEnd)
        }
        if (header.isEmpty() || header.length > MAX_HEADER_CHARS) return false
        val fields = header.split(',')
        if (fields.size != columns.size) return false
        return fields.indices.all { index ->
            fields[index] == columns[index] ||
                fields[index] == "\"" + columns[index] + "\""
        }
    }
}
