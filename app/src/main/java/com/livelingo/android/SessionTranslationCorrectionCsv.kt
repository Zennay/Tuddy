package com.livelingo.android

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Strict, editable UTF-8 CSV for user-authored session corrections.
 * Import produces an uncommitted list; the dialog controls every mutation.
 * CSV is not a trust or translation-quality assertion.
 */
internal object SessionTranslationCorrectionCsv {
    private const val HEADER = "sourceCode,targetCode,original,translation"
    const val MAX_BYTES = SessionTranslationCorrectionTransfer.MAX_BYTES

    private fun spreadsheetSafe(value: String): Boolean {
        // Quoted cells can still execute formulas when opened by spreadsheet apps.
        // Fail closed rather than modifying a user's phrase or silently exporting it.
        val first = value.trimStart().firstOrNull() ?: return false
        return first !in charArrayOf('=', '+', '-', '@')
    }

    fun encode(entries: List<SessionTranslationCorrection>): ByteArray? {
        if (entries.isEmpty() || !SessionTranslationCorrections.validEntries(entries)) return null
        if (entries.any { !spreadsheetSafe(it.original) || !spreadsheetSafe(it.translation) })
            return null
        fun cell(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
        val payload = buildString {
            append(HEADER).append("\r\n")
            entries.forEach { item ->
                append(listOf(item.sourceCode, item.targetCode, item.original, item.translation)
                    .joinToString(",") { cell(it) })
                append("\r\n")
            }
        }.toByteArray(Charsets.UTF_8)
        return payload.takeIf { it.size <= MAX_BYTES }
    }

    fun decode(bytes: ByteArray): List<SessionTranslationCorrection>? {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
        val raw = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            return null
        }
        val source = raw.removePrefix("\uFEFF")
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        // 0 = unquoted cell, 1 = quoted, 2 = just closed a quoted cell
        var mode = 0
        var position = 0
        fun endField() {
            fields.add(field.toString())
            field.setLength(0)
            mode = 0
        }
        fun endRecord(): Boolean {
            endField()
            if (fields.size != 4 || records.size > SessionTranslationCorrections.MAX_ENTRIES)
                return false
            records.add(fields)
            fields = mutableListOf()
            return true
        }
        while (position < source.length) {
            val char = source[position]
            if (mode == 1) {
                if (char == '"') {
                    if (position + 1 < source.length && source[position + 1] == '"') {
                        field.append('"')
                        position++
                    } else mode = 2
                } else field.append(char)
            } else {
                when (char) {
                    ',' -> endField()
                    '\n' -> if (!endRecord()) return null
                    '\r' -> {
                        if (position + 1 >= source.length || source[position + 1] != '\n')
                            return null
                        if (!endRecord()) return null
                        position++
                    }
                    '"' -> if (mode == 0 && field.isEmpty()) mode = 1 else return null
                    else -> if (mode == 2) return null else field.append(char)
                }
            }
            if (field.length > SessionTranslationCorrections.MAX_TEXT_UNITS + 16)
                return null
            position++
        }
        if (mode == 1) return null
        if (fields.isNotEmpty() || field.isNotEmpty() || mode == 2) {
            if (!endRecord()) return null
        }
        if (records.isEmpty() || records.first() != listOf(
                "sourceCode", "targetCode", "original", "translation"
            )) return null
        val rows = records.drop(1).map { record ->
            if (!spreadsheetSafe(record[2]) || !spreadsheetSafe(record[3])) return null
            SessionTranslationCorrection(record[2], record[3], record[0], record[1])
        }
        return rows.takeIf {
            it.isNotEmpty() && SessionTranslationCorrections.validEntries(it)
        }
    }
}
