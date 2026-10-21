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
    STUDY_HTML("Oefenblad · HTML", "text/html", "Tuddy-leerkaarten-oefenblad.html"),
    FOLD_HTML("Vouwkaartjes · HTML", "text/html", "Tuddy-leerkaarten-vouwkaartjes.html"),
    CLOZE_HTML("Aanvuloefening · HTML", "text/html", "Tuddy-leerkaarten-aanvuloefening.html"),
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
    val showLanguageTags: Boolean,
    val clozeDifficulty: PinnedClozeDifficulty,
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
            format == PinnedCaptionFormat.CSV ||
            format == PinnedCaptionFormat.STUDY_HTML ||
            format == PinnedCaptionFormat.FOLD_HTML ||
            format == PinnedCaptionFormat.CLOZE_HTML) {
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
        onlyPersonallyCorrected: Boolean = false,
        showLanguageTags: Boolean = false,
        clozeDifficulty: PinnedClozeDifficulty = PinnedClozeDifficulty.NORMAL
    ): ByteArray? {
        if (selection.isEmpty() || selection.size > SessionCaptionPins.MAX_PINS ||
            selection.map { it.id }.distinct().size != selection.size
        ) return null
        val rows = includedRows(selection, format, subtitleOptions, scope,
            languagePair, studyEdits, onlyPersonallyCorrected).map { it.caption }
        if (rows.isEmpty()) return null
        return when (format) {
            PinnedCaptionFormat.TXT -> PrivateTranscriptExport.snapshot(rows)
            PinnedCaptionFormat.TSV ->
                PrivatePinnedCaptionStudyTsv.snapshot(rows, studyDirection, showLanguageTags)
            PinnedCaptionFormat.CSV ->
                PrivatePinnedCaptionStudyCsv.snapshot(rows, studyDirection, showLanguageTags)
            PinnedCaptionFormat.STUDY_HTML ->
                PrivatePinnedCaptionStudyHtml.snapshot(rows, studyDirection, showLanguageTags)
            PinnedCaptionFormat.FOLD_HTML ->
                PrivatePinnedCaptionFoldHtml.snapshot(rows, studyDirection, showLanguageTags)
            PinnedCaptionFormat.CLOZE_HTML ->
                PrivatePinnedCaptionClozeHtml.snapshot(
                    rows, studyDirection, showLanguageTags, clozeDifficulty)
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

    internal fun field(text: String, spreadsheetSafe: Boolean = true): String {
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
        return if (spreadsheetSafe && (formulaCandidate.startsWith("=") ||
            formulaCandidate.startsWith("+") ||
            formulaCandidate.startsWith("-") ||
            formulaCandidate.startsWith("@"))
        ) "'$flattened" else flattened
    }

    fun snapshot(rows: List<TranscriptExportRow>,
                 direction: PinnedStudyDirection,
                 showLanguageTags: Boolean = false): ByteArray? {
        if (rows.isEmpty() || rows.size > SessionCaptionPins.MAX_PINS) return null
        if (showLanguageTags && rows.any { row ->
                listOf(row.sourceCode, row.targetCode).any { code ->
                    code.isBlank() || code.length > 16 ||
                        code.any { ch -> !ch.isLetterOrDigit() &&
                            ch != '-' && ch != '_' }
                }
            }) return null
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
            val front = if (showLanguageTags)
                "[${row.sourceCode}] $source" else source
            val back = if (showLanguageTags)
                "[${row.targetCode}] $meaning" else meaning
            if (direction != PinnedStudyDirection.TARGET_TO_SOURCE) {
                out.append(front).append('\t').append(back).append('\n')
            }
            if (direction != PinnedStudyDirection.SOURCE_TO_TARGET) {
                out.append(back).append('\t').append(front).append('\n')
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

    fun snapshot(rows: List<TranscriptExportRow>,
                 direction: PinnedStudyDirection,
                 showLanguageTags: Boolean = false): ByteArray? {
        val source = PrivatePinnedCaptionStudyTsv.snapshot(rows, direction, showLanguageTags)
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


/**
 * A print-only, self-contained HTML workbook for the same explicitly selected
 * two-sided study cards as CSV/TSV. No script, remote content, storage or audio.
 * Questions appear first; answers are on a separate print page so they cannot
 * be accidentally exposed when distributing the question sheet.
 */
/**
 * Single-sided paper cards. Print and fold the right-hand answer panel behind
 * the left-hand prompt; cut on the card borders. This avoids unreliable
 * automatic duplex settings and never needs JavaScript or online storage.
 */
internal object PrivatePinnedCaptionFoldHtml {
    private const val MAX_BYTES = 262_144
    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            append(when (ch) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> ch.toString()
            })
        }
    }

    fun snapshot(
        rows: List<TranscriptExportRow>,
        direction: PinnedStudyDirection,
        showLanguageTags: Boolean = false
    ): ByteArray? {
        // Validate using the identical private TSV path and final capped
        // selection, but don't show a spreadsheet-specific leading apostrophe
        // on paper. A card is not a live web view or editable form.
        if (PrivatePinnedCaptionStudyTsv.snapshot(rows, direction, showLanguageTags) == null)
            return null
        val cards = ArrayList<Pair<String, String>>()
        for (row in rows) {
            val source = PrivatePinnedCaptionStudyTsv.field(row.original, spreadsheetSafe = false)
            val answer = PrivatePinnedCaptionStudyTsv.field(row.translated, spreadsheetSafe = false)
            if (source.isBlank() || answer.isBlank()) return null
            val front = if (showLanguageTags) "[" + row.sourceCode + "] " + source else source
            val back = if (showLanguageTags) "[" + row.targetCode + "] " + answer else answer
            if (direction != PinnedStudyDirection.TARGET_TO_SOURCE) cards.add(front to back)
            if (direction != PinnedStudyDirection.SOURCE_TO_TARGET) cards.add(back to front)
        }
        if (cards.isEmpty() || cards.size > SessionCaptionPins.MAX_PINS * 2) return null

        val out = StringBuilder(
            "<!doctype html><html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<title>Tuddy · Vouwkaartjes</title>" +
                "<style>body{font:16px system-ui,sans-serif;max-width:850px;margin:auto;" +
                "padding:18px;color:#202020}p{line-height:1.5}" +
                ".card{display:grid;grid-template-columns:1fr 1fr;border:1px dashed #555;" +
                "min-height:95px;margin:10px 0;break-inside:avoid;page-break-inside:avoid}" +
                ".side{padding:12px;overflow-wrap:anywhere;min-width:0}" +
                ".front{border-right:1px dotted #555}" +
                ".label{font:12px system-ui,sans-serif;letter-spacing:.04em;" +
                "color:#444;margin-bottom:8px}" +
                "@media print{@page{margin:12mm}body{padding:0}" +
                ".card{min-height:30mm;break-inside:avoid}}" +
                "</style></head><body><h1>Mijn tweetalige vouwkaartjes</h1>" +
                "<p>Print op één zijde. Knip langs de gestippelde buitenrand;" +
                " vouw de rechter betekenis achter de linker vraag. " +
                "Eigen modelvertalingen en correcties zijn niet onafhankelijk" +
                " geverifieerd. Deel geen gevoelige tekst zonder toestemming.</p>"
        )
        for ((index, card) in cards.withIndex()) {
            out.append("<section class=\"card\"><div class=\"side front\">")
                .append("<div class=\"label\">KAART ").append(index + 1)
                .append(" · VOORKANT</div><bdi dir=\"auto\">")
                .append(escape(card.first)).append("</bdi></div>")
                .append("<div class=\"side back\"><div class=\"label\">VOUW ACHTER</div>")
                .append("<bdi dir=\"auto\">").append(escape(card.second))
                .append("</bdi></div></section>")
            if (out.length > MAX_BYTES) return null
        }
        out.append("<p>Deze privé-HTML is door jou via Android bewaard." +
            " De gekozen bestandsprovider kan je tekst synchroniseren." +
            " Geen externe scripts, media of automatische datatransmissie.</p></body></html>")
        return out.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}

internal object PrivatePinnedCaptionStudyHtml {
    private const val MAX_BYTES = 262_144

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(when (char) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> char.toString()
            })
        }
    }

    fun snapshot(
        rows: List<TranscriptExportRow>,
        direction: PinnedStudyDirection,
        showLanguageTags: Boolean = false
    ): ByteArray? {
        // First validate with the exact existing two-column exporter, including
        // bounded source data. HTML isn't a spreadsheet: don't display the
        // additional formula-protection apostrophe that TSV/CSV require.
        if (PrivatePinnedCaptionStudyTsv.snapshot(rows, direction, showLanguageTags) == null)
            return null
        val cards = ArrayList<Pair<String, String>>()
        for (row in rows) {
            val source = PrivatePinnedCaptionStudyTsv.field(row.original, spreadsheetSafe = false)
            val meaning = PrivatePinnedCaptionStudyTsv.field(row.translated, spreadsheetSafe = false)
            if (source.isBlank() || meaning.isBlank()) return null
            val front = if (showLanguageTags) "[${row.sourceCode}] $source" else source
            val back = if (showLanguageTags) "[${row.targetCode}] $meaning" else meaning
            if (direction != PinnedStudyDirection.TARGET_TO_SOURCE) cards.add(front to back)
            if (direction != PinnedStudyDirection.SOURCE_TO_TARGET) cards.add(back to front)
        }
        if (cards.isEmpty() || cards.size > SessionCaptionPins.MAX_PINS * 2) return null

        val out = StringBuilder(
            "<!doctype html><html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<title>Tuddy · Mijn leerkaarten</title>" +
                "<style>body{font:16px system-ui,sans-serif;max-width:850px;margin:auto;" +
                "padding:24px;color:#161616}li{margin:18px 0;white-space:pre-wrap;" +
                "overflow-wrap:anywhere;unicode-bidi:plaintext}" +
                "summary{cursor:pointer;color:#254c84;padding:6px 0}" +
                "details{margin-top:8px} .write-line{display:none}" +
                ".answer-key{break-before:page;page-break-before:always}" +
                "@media print{body{padding:0}li{break-inside:avoid}" +
                "details{display:none}.write-line{display:block}}</style></head><body>" +
                "<h1>Mijn tweetalige oefenblad</h1>" +
                "<p>Zelf gekozen en lokaal verwerkte teksten. Modelvertalingen en eigen " +
                "correcties zijn niet onafhankelijk geverifieerd. " +
                "Vul eerst de antwoorden in; de antwoorden staan op een aparte pagina.</p>" +
                "<h2>Vragen</h2><ol>"
        )
        for ((front, back) in cards) {
            out.append("<li><bdi dir=\"auto\">").append(escape(front)).append("</bdi>")
                .append("<details><summary>Toon antwoord</summary><bdi dir=\"auto\">")
                .append(escape(back)).append("</bdi></details>")
                .append("<span class=\"write-line\" aria-hidden=\"true\">")
                .append("________________________</span></li>")
            if (out.length > MAX_BYTES) return null
        }
        out.append("</ol><section class=\"answer-key\"><h2>Antwoorden</h2><ol>")
        for ((_, back) in cards) {
            out.append("<li><bdi dir=\"auto\">").append(escape(back)).append("</bdi></li>")
            if (out.length > MAX_BYTES) return null
        }
        out.append("</ol><p>Privé-uitvoer via een door jou gekozen Android-bestandsprovider." +
            " De provider kan bestanden synchroniseren of delen.</p></section></body></html>")
        return out.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
