package com.livelingo.android

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * User-initiated, bounded CSV transport for their own unverified word notes.
 * The existing exact TSV import parser remains the source of word validation,
 * deduplication, supported languages and maximum record counts.
 *
 * No spreadsheet, network or provider is opened here. Export deliberately
 * refuses spreadsheet-formula prefixes rather than emitting dangerous cells.
 */
internal object PrivateOwnWordCsv {
    private val formulaPrefix = setOf('=', '+', '-', '@')
    private const val HEADER = "language,word,meaning"

    fun encode(entries: List<OwnWord>, deck: PrivateOwnWordDeck): ByteArray? {
        if (entries.isEmpty() || entries.size > PrivateOwnWordDeck.MAX_ENTRIES) return null
        // Fail closed if a word changed while preparing the export. Reuse the
        // deck's canonical validation and UTF-8 byte limits.
        if (deck.exportTsvSelection(entries) == null) return null
        if (entries.any { row ->
            listOf(row.word, row.ownMeaning).any { value ->
                value.isNotEmpty() && value.first() in formulaPrefix
            }
        }) return null
        val text = buildString {
            append(HEADER).append("\r\n")
            for (entry in entries) {
                listOf(entry.language, entry.word, entry.ownMeaning)
                    .forEachIndexed { index, value ->
                        if (index != 0) append(',')
                        append('"').append(value.replace("\"", "\"\"")).append('"')
                    }
                append("\r\n")
            }
        }
        return text.toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= PrivateOwnWordDeck.MAX_BACKUP_BYTES }
    }

    fun previewImport(bytes: ByteArray, deck: PrivateOwnWordDeck): List<OwnWord>? {
        if (bytes.isEmpty() || bytes.size > PrivateOwnWordDeck.MAX_BACKUP_BYTES) return null
        val decoded = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        } catch (_: CharacterCodingException) { return null }
        if (decoded.isEmpty()) return null
        val rows = mutableListOf<List<String>>()
        val cells = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var started = false
        var closed = false
        var index = 0
        fun finishCell() {
            cells.add(field.toString())
            field.setLength(0)
            started = false
            closed = false
        }
        fun finishRow() {
            finishCell()
            rows.add(cells.toList())
            cells.clear()
        }
        while (index < decoded.length) {
            val ch = decoded[index]
            if (quoted) {
                when (ch) {
                    '"' -> {
                        if (index + 1 < decoded.length && decoded[index + 1] == '"') {
                            field.append('"')
                            index++
                        } else {
                            quoted = false
                            closed = true
                        }
                    }
                    '\r', '\n' -> return null // no multiline word notes
                    else -> field.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> {
                        if (started || closed) return null
                        started = true
                        quoted = true
                    }
                    ',' -> finishCell()
                    '\r', '\n' -> {
                        if (ch == '\r') {
                            if (index + 1 >= decoded.length || decoded[index + 1] != '\n')
                                return null
                            index++
                        }
                        finishRow()
                        if (rows.size > PrivateOwnWordDeck.MAX_ENTRIES + 1) return null
                    }
                    else -> {
                        if (closed) return null
                        field.append(ch)
                        started = true
                    }
                }
            }
            index++
        }
        if (quoted) return null
        if (started || closed || field.isNotEmpty() || cells.isNotEmpty())
            finishRow()
        if (rows.size !in 2..(PrivateOwnWordDeck.MAX_ENTRIES + 1) ||
            rows.first() != listOf("language", "word", "meaning") ||
            rows.drop(1).any { it.size != 3 }) return null
        // Existing validation rejects control chars (including tabs), broken
        // UTF-16, unsupported languages and duplicate case-insensitive keys.
        // Do not silently invent a translation or replace a local meaning.
        val tsv = buildString {
            append("language\tword\tmeaning\n")
            rows.drop(1).forEach { row ->
                append(row.joinToString("\t")).append('\n')
            }
        }
        return deck.previewTsvImport(tsv.toByteArray(Charsets.UTF_8))
    }
}
