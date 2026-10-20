package com.livelingo.android

/** Only the explicitly marked rows in this in-memory conversation may be exported. */
internal enum class PinnedCaptionFormat(
    val title: String,
    val mimeType: String,
    val fileName: String
) {
    TXT("Gesprek · TXT", "text/plain", "Tuddy-selectie.txt"),
    TSV("Leerkaarten · TSV", "text/tab-separated-values", "Tuddy-eigen-leerkaarten.tsv"),
    CSV("Leerkaarten · CSV", "text/csv", "Tuddy-eigen-leerkaarten.csv"),
    JSON("Archief · JSON", "application/json", "Tuddy-selectie.json"),
    HTML("Afdrukbaar · HTML", "text/html", "Tuddy-selectie.html"),
    SRT("Ondertitelconcept · SRT", "application/x-subrip", "Tuddy-selectie-concept.srt"),
    WEBVTT("Ondertitelconcept · WebVTT", "text/vtt", "Tuddy-selectie-concept.vtt")
}

/** A visible, explicit content scope: never silently drop unfinished rows. */
internal enum class PinnedCaptionScope(val title: String) {
    ALL("Alle gemarkeerde regels"),
    COMPLETED("Alleen afgeronde regels"),
    TRANSLATED("Alleen voltooide vertalingen")
}

internal enum class PinnedStudyDirection(val title: String) {
    SOURCE_TO_TARGET("Bronzin → lokale modelvertaling"),
    TARGET_TO_SOURCE("Lokale modelvertaling → bronzin"),
    BOTH("Beide richtingen · twee kaarten per zin")
}

internal data class PinnedCaptionRow(val id: Long, val caption: TranscriptExportRow)

/** Explicit authoring is tied to the exact model sentence it corrected. */
internal data class PinnedStudyCorrection(
    val source: String,
    val modelAnswer: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val answer: String
) {
    fun appliesTo(row: TranscriptExportRow): Boolean =
        source == row.original && modelAnswer == row.translated &&
            sourceLanguage == row.sourceCode && targetLanguage == row.targetCode &&
            row.status == "Lokaal vertaald" &&
            PrivatePinnedCaptionExport.validStudyEdit(answer)
}

internal data class PendingPinnedCaptionExport(
    val generation: Int,
    val selection: List<PinnedCaptionRow>,
    val format: PinnedCaptionFormat,
    val scope: PinnedCaptionScope,
    val languagePair: Pair<String, String>?,
    val studyDirection: PinnedStudyDirection,
    val studyEdits: Map<Long, PinnedStudyCorrection>,
    val onlyPersonallyCorrected: Boolean,
    val subtitleOptions: SubtitleDraftOptions,
    val bytes: ByteArray
)

/**
 * Reuses the existing bounded private exporters; does not persist pin identities,
 * synthesize translations or claim accurate audio/video timing.
 */
internal object PrivatePinnedCaptionExport {
    // Only an explicit user edit may replace the local model answer in a study
    // card. The original transcript/archives and SRT/VTT are never modified.
    fun validStudyEdit(value: String): Boolean {
        if (value.isBlank() || value.length > 800) return false
        var i = 0
        var count = 0
        while (i < value.length) {
            val char = value[i]
            if (char == '\n' || char == '\r' || char == '\t' ||
                char.isISOControl() || char in '\u202a'..'\u202e' ||
                char in '\u2066'..'\u2069' ||
                char == '\u2028' || char == '\u2029') return false
            if (Character.isHighSurrogate(char)) {
                if (i + 1 >= value.length ||
                    !Character.isLowSurrogate(value[i + 1])) return false
                i += 2
            } else {
                if (Character.isLowSurrogate(char)) return false
                i++
            }
            count++
            if (count > 400) return false
        }
        return true
    }

    fun select(
        rows: List<PinnedCaptionRow>, scope: PinnedCaptionScope
    ): List<PinnedCaptionRow> = when (scope) {
        PinnedCaptionScope.ALL -> rows
        PinnedCaptionScope.COMPLETED -> rows.filter {
            it.caption.status == "Lokaal" || it.caption.status == "Lokaal vertaald"
        }
        PinnedCaptionScope.TRANSLATED -> rows.filter {
            it.caption.status == "Lokaal vertaald" && it.caption.translated.isNotBlank()
        }
    }

    /**
     * Actual output rows: SRT/VTT always omit unfinished translations,
     * and translation-only mode also omits source-only completed rows.
     */
    fun includedRows(
        rows: List<PinnedCaptionRow>,
        format: PinnedCaptionFormat,
        options: SubtitleDraftOptions,
        scope: PinnedCaptionScope,
        languagePair: Pair<String, String>? = null,
        studyEdits: Map<Long, PinnedStudyCorrection> = emptyMap(),
        onlyPersonallyCorrected: Boolean = false
    ): List<PinnedCaptionRow> {
        val scoped = select(rows, scope)
        val selected = if (languagePair == null) scoped else scoped.filter {
            it.caption.sourceCode == languagePair.first &&
                it.caption.targetCode == languagePair.second
        }
        if (format == PinnedCaptionFormat.TSV ||
            format == PinnedCaptionFormat.CSV) {
            return selected.filter { row ->
                row.caption.status == "Lokaal vertaald" &&
                    row.caption.translated.isNotBlank() &&
                    (!onlyPersonallyCorrected ||
                        studyEdits[row.id]?.appliesTo(row.caption) == true)
            }.map { row ->
                val edit = studyEdits[row.id]
                if (edit == null || !edit.appliesTo(row.caption)) row
                else row.copy(caption = row.caption.copy(translated = edit.answer.trim()))
            }.distinctBy {
                // Identical actual front/back pairs within the same language
                // direction produce one card, regardless of timestamp.
                listOf(it.caption.sourceCode, it.caption.targetCode,
                    it.caption.original, it.caption.translated)
            }
        }
        if (format != PinnedCaptionFormat.SRT &&
            format != PinnedCaptionFormat.WEBVTT) return selected
        return selected.filter { row ->
            val complete = row.caption.status == "Lokaal" ||
                row.caption.status == "Lokaal vertaald"
            complete && (options.mode != SubtitleDraftMode.TRANSLATION_ONLY ||
                (row.caption.status == "Lokaal vertaald" &&
                    row.caption.translated.isNotBlank()))
        }
    }

    // Short textual summary shown before choosing the file destination.
    fun preview(text: String): String = text.asSequence().filter {
        it >= ' ' && it != '\u007f' &&
            it !in '\u202a'..'\u202e' &&
            it !in '\u2066'..'\u2069'
    }.take(140).joinToString("").trim()

    fun snapshot(
        selection: List<PinnedCaptionRow>,
        format: PinnedCaptionFormat,
        subtitleOptions: SubtitleDraftOptions,
        scope: PinnedCaptionScope = PinnedCaptionScope.ALL,
        languagePair: Pair<String, String>? = null,
        studyDirection: PinnedStudyDirection = PinnedStudyDirection.SOURCE_TO_TARGET,
        studyEdits: Map<Long, PinnedStudyCorrection> = emptyMap(),
        onlyPersonallyCorrected: Boolean = false
    ): ByteArray? {
        if (selection.isEmpty() || selection.size > SessionCaptionPins.MAX_PINS ||
            selection.map { it.id }.distinct().size != selection.size
        ) return null
        val rows = includedRows(selection, format, subtitleOptions, scope,
            languagePair, studyEdits, onlyPersonallyCorrected).map { it.caption }
        if (rows.isEmpty()) return null
        return when (format) {
            PinnedCaptionFormat.TXT -> PrivateTranscriptExport.snapshot(rows)
            PinnedCaptionFormat.TSV -> PrivatePinnedCaptionStudyTsv.snapshot(rows, studyDirection)
            PinnedCaptionFormat.CSV -> PrivatePinnedCaptionStudyCsv.snapshot(rows, studyDirection)
            PinnedCaptionFormat.JSON -> PrivateTranscriptJsonExport.snapshot(rows)
            PinnedCaptionFormat.HTML -> PrivatePrintableConversation.snapshot(rows, onlyFinished = false)
            PinnedCaptionFormat.SRT ->
                PrivateSubtitleDraftExport.snapshot(rows,
                    subtitleOptions.copy(format = SubtitleDraftFormat.SRT))
            PinnedCaptionFormat.WEBVTT ->
                PrivateSubtitleDraftExport.snapshot(rows,
                    subtitleOptions.copy(format = SubtitleDraftFormat.WEBVTT))
        }
    }
}

/**
 * Explicitly exported two-column study cards, suitable for manual import in
 * programs that accept UTF-8 tab-separated front/back fields. Only genuinely
 * completed on-device model translations enter the file; cards are not a verified dictionary.
 * No local persistence, clipboard or network access.
 */
internal object PrivatePinnedCaptionStudyTsv {
    private const val MAX_BYTES = 262_144

    private fun wellFormedUtf16(text: String): Boolean {
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (Character.isHighSurrogate(char)) {
                if (index + 1 >= text.length ||
                    !Character.isLowSurrogate(text[index + 1])) return false
                index += 2
            } else {
                if (Character.isLowSurrogate(char)) return false
                index++
            }
        }
        return true
    }

    private fun field(text: String): String {
        // Each card must remain exactly one physical TSV line.
        val flattened = text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ')
            .map { char ->
                if (char.isISOControl() || char in '\u202a'..'\u202e' ||
                    char in '\u2066'..'\u2069' ||
                    char == '\u2028' || char == '\u2029') ' ' else char
            }.joinToString("").trim()
        // TSV is literal data, not HTML: leave &, <, > and emoji unchanged.
        // Prefix spreadsheet formula triggers with an apostrophe, even when
        // preceded by common invisible formatting characters. Importers may
        // choose whether to display the leading apostrophe.
        val formulaCandidate = flattened.dropWhile {
            it.isWhitespace() || Character.getType(it) == Character.FORMAT.toInt()
        }
        return if (formulaCandidate.startsWith("=") ||
            formulaCandidate.startsWith("+") ||
            formulaCandidate.startsWith("-") ||
            formulaCandidate.startsWith("@")
        ) "'$flattened" else flattened
    }

    fun snapshot(rows: List<TranscriptExportRow>, direction: PinnedStudyDirection): ByteArray? {
        if (rows.isEmpty() || rows.size > SessionCaptionPins.MAX_PINS) return null
        if (rows.any { it.status != "Lokaal vertaald" || it.translated.isBlank() ||
                it.original.isBlank() || it.original.length > 4096 ||
                it.translated.length > 4096 ||
                !wellFormedUtf16(it.original) || !wellFormedUtf16(it.translated) ||
                it.original.contains('\u0000') || it.translated.contains('\u0000')
        }) return null
        // Inherit existing constraints on code fields, timestamps, unsafe controls
        // and entire source row validity before offering an editable export.
        if (PrivateTranscriptJsonExport.snapshot(rows) == null) return null
        val out = StringBuilder()
        for (row in rows) {
            val source = field(row.original)
            val meaning = field(row.translated)
            if (source.isBlank() || meaning.isBlank()) return null
            if (direction != PinnedStudyDirection.TARGET_TO_SOURCE) {
                out.append(source).append('\t').append(meaning).append('\n')
            }
            if (direction != PinnedStudyDirection.SOURCE_TO_TARGET) {
                out.append(meaning).append('\t').append(source).append('\n')
            }
            if (out.length > MAX_BYTES) return null
        }
        return out.toString().toByteArray(Charsets.UTF_8).takeIf { it.size <= MAX_BYTES }
    }
}

/**
 * CSV companion to the explicitly chosen private TSV study cards.
 * Derive from the exact same bounded two-column UTF-8 TSV snapshot so there
 * is no mismatch in selected cards, unverified translations or formula safety.
 * RFC 4180 style comma separator, double-quoted cells and CRLF line endings.
 * No network calls, additional permissions or hidden persistence.
 */
internal object PrivatePinnedCaptionStudyCsv {
    private const val MAX_BYTES = 262_144

    fun snapshot(rows: List<TranscriptExportRow>, direction: PinnedStudyDirection): ByteArray? {
        val source = PrivatePinnedCaptionStudyTsv.snapshot(rows, direction)
            ?.toString(Charsets.UTF_8) ?: return null
        val csv = StringBuilder()
        for (line in source.lineSequence()) {
            if (line.isEmpty()) continue
            val fields = line.split('\t')
            if (fields.size != 2) return null
            csv.append('"').append(fields[0].replace("\"", "\"\"")).append('"')
                .append(',').append('"').append(fields[1].replace("\"", "\"\""))
                .append('"').append("\r\n")
            if (csv.length > MAX_BYTES) return null
        }
        if (csv.isEmpty()) return null
        return csv.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
