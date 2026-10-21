package com.livelingo.android

/**
 * Strict single-record CSV reader for user-authored timed subtitle rows.
 * A physical line is always one cue; quoted CR/LF and malformed quoting are
 * rejected rather than silently changing the user's original row numbers.
 *
 * RFC-style comma delimiting and doubled quotes are supported. This parser
 * never evaluates spreadsheet formulas, guesses meanings or accesses files.
 */
internal object PrivateTimedCsvColumns {
    fun parseRow(row: String): List<String>? {
        if (row.isEmpty()) return null
        val fields = ArrayList<String>(4)
        val cell = StringBuilder()
        var inQuotes = false
        var afterQuote = false
        var atStart = true
        var index = 0
        while (index < row.length) {
            val ch = row[index]
            when {
                inQuotes && ch == '"' -> {
                    if (index + 1 < row.length && row[index + 1] == '"') {
                        cell.append('"')
                        index++
                    } else {
                        inQuotes = false
                        afterQuote = true
                    }
                }
                inQuotes -> cell.append(ch)
                ch == ',' -> {
                    fields.add(cell.toString())
                    if (fields.size > 4) return null
                    cell.setLength(0)
                    atStart = true
                    afterQuote = false
                }
                afterQuote -> return null // no content after a closing CSV quote
                atStart && ch == '"' -> {
                    inQuotes = true
                    atStart = false
                }
                ch == '"' -> return null // quote in an unquoted field
                else -> {
                    cell.append(ch)
                    atStart = false
                }
            }
            index++
        }
        if (inQuotes) return null
        fields.add(cell.toString())
        return fields.takeIf { it.size <= 4 }
    }
}
