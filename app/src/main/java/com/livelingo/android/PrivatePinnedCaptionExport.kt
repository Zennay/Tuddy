package com.livelingo.android

/** Only the explicitly marked rows in this in-memory conversation may be exported. */
internal enum class PinnedCaptionFormat(
    val title: String,
    val mimeType: String,
    val fileName: String
) {
    TXT("Gesprek · TXT", "text/plain", "Tuddy-selectie.txt"),
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

internal data class PinnedCaptionRow(val id: Long, val caption: TranscriptExportRow)

internal data class PendingPinnedCaptionExport(
    val generation: Int,
    val selection: List<PinnedCaptionRow>,
    val format: PinnedCaptionFormat,
    val scope: PinnedCaptionScope,
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

    fun preview(text: String): String = text.asSequence().filter {
        it >= ' ' && it != '\u007f' &&
            it !in '\u202a'..'\u202e' &&
            it !in '\u2066'..'\u2069'
    }.take(140).joinToString("").trim()

    fun snapshot(
        selection: List<PinnedCaptionRow>,
        format: PinnedCaptionFormat,
        subtitleOptions: SubtitleDraftOptions,
        scope: PinnedCaptionScope = PinnedCaptionScope.ALL
    ): ByteArray? {
        if (selection.isEmpty() || selection.size > SessionCaptionPins.MAX_PINS ||
            selection.map { it.id }.distinct().size != selection.size
        ) return null
        val rows = select(selection, scope).map { it.caption }
        if (rows.isEmpty()) return null
        return when (format) {
            PinnedCaptionFormat.TXT -> PrivateTranscriptExport.snapshot(rows)
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
