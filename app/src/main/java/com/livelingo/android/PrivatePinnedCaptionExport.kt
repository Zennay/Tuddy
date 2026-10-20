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

internal data class PinnedCaptionRow(val id: Long, val caption: TranscriptExportRow)

internal data class PendingPinnedCaptionExport(
    val generation: Int,
    val selection: List<PinnedCaptionRow>,
    val format: PinnedCaptionFormat,
    val subtitleOptions: SubtitleDraftOptions,
    val bytes: ByteArray
)

/**
 * Reuses the existing bounded private exporters; does not persist pin identities,
 * synthesize translations or claim accurate audio/video timing.
 */
internal object PrivatePinnedCaptionExport {
    fun snapshot(
        selection: List<PinnedCaptionRow>,
        format: PinnedCaptionFormat,
        subtitleOptions: SubtitleDraftOptions
    ): ByteArray? {
        if (selection.isEmpty() || selection.size > SessionCaptionPins.MAX_PINS ||
            selection.map { it.id }.distinct().size != selection.size
        ) return null
        val rows = selection.map { it.caption }
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
