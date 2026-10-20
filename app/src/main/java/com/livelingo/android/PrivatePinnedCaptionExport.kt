package com.livelingo.android

/** Only the explicitly marked rows in this in-memory conversation may be exported. */
internal enum class PinnedCaptionFormat(
    val title: String,
    val mimeType: String,
    val fileName: String
) {
    TXT("Gesprek · TXT", "text/plain", "Tuddy-selectie.txt"),
    TSV("Leerkaarten · TSV", "text/tab-separated-values", "Tuddy-eigen-leerkaarten.tsv"),
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

internal data class PendingPinnedCaptionExport(
    val generation: Int,
    val selection: List<PinnedCaptionRow>,
    val format: PinnedCaptionFormat,
    val scope: PinnedCaptionScope,
    val languagePair: Pair<String, String>?,
    val studyDirection: PinnedStudyDirection,
    val subtitleOptions: SubtitleDraftOptions,
    val bytes: ByteArray
)

/**
 * Reuses the existing bounded private exporters; does not persist pin identities,
 * synthesize translations or claim accurate audio/video timing.
 */
internal object PrivatePinnedCaptionExport {
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
        languagePair: Pair<String, String>? = null
    ): List<PinnedCaptionRow> {
        val scoped = select(rows, scope)
        val selected = if (languagePair == null) scoped else scoped.filter {
            it.caption.sourceCode == languagePair.first &&
                it.caption.targetCode == languagePair.second
        }
        if (format == PinnedCaptionFormat.TSV) return selected.filter {
            it.caption.status == "Lokaal vertaald" && it.caption.translated.isNotBlank()
        }.distinctBy {
            // Identical authored pairs need only one card per requested direction,
            // but different language pairs must never be silently conflated.
            listOf(it.caption.sourceCode, it.caption.targetCode,
                it.caption.original, it.caption.translated)
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
        studyDirection: PinnedStudyDirection = PinnedStudyDirection.SOURCE_TO_TARGET
    ): ByteArray? {
        if (selection.isEmpty() || selection.size > SessionCaptionPins.MAX_PINS ||
            selection.map { it.id }.distinct().size != selection.size
        ) return null
        val rows = includedRows(selection, format, subtitleOptions, scope, languagePair)
            .map { it.caption }
        if (rows.isEmpty()) return null
        return when (format) {
            PinnedCaptionFormat.TXT -> PrivateTranscriptExport.snapshot(rows)
            PinnedCaptionFormat.TSV -> PrivatePinnedCaptionStudyTsv.snapshot(rows, studyDirection)
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
            it == '\uFEFF' || it == '\u200B' || it == '\u2060'
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
