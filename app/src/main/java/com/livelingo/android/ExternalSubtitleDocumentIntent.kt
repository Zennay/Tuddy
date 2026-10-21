package com.livelingo.android

import android.content.Intent
import android.net.Uri

/**
 * User-initiated Android "Open with" or single-file "Share to Tuddy" for a content:// subtitle document.
 * Capture only the ephemeral URI; never fetch URLs, follow links, persist
 * provider permissions, store document bytes or auto-start recognition.
 */
internal data class IncomingViewedSubtitleDocument(
    val requestId: Long,
    val uri: Uri?,
    val notice: String? = null
)

internal object ExternalSubtitleDocumentIntent {
    private val allowedTypes = setOf(
        "text/plain", "text/vtt", "text/srt", "application/x-subrip",
        "application/json", "application/ttml+xml", "application/xml",
        "text/xml", "text/x-ass", "text/x-ssa", "application/x-ass"
    )

    fun read(intent: Intent?, requestId: Long): IncomingViewedSubtitleDocument? {
        val action = intent?.action
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return null
        if (action == Intent.ACTION_SEND && !intent.hasExtra(Intent.EXTRA_STREAM))
            return null // Ordinary selected text remains with the text-only ingress.
        if (action == Intent.ACTION_SEND &&
            (intent.hasExtra(Intent.EXTRA_TEXT) ||
                intent.hasExtra(Intent.EXTRA_HTML_TEXT) ||
                (intent.clipData?.itemCount ?: 0) > 1))
            return IncomingViewedSubtitleDocument(requestId, null,
                "Gemengde tekst, HTML en meerdere bijlagen worden niet ingelezen. " +
                    "Deel één enkel tekstondertitelbestand.")

        val type = intent.type?.substringBefore(';')?.trim()?.lowercase()
        if (type !in allowedTypes)
            return IncomingViewedSubtitleDocument(requestId, null,
                "Geen ondersteund tekstondertiteltype. Kies een SRT/VTT/ASS/SSA/TTML/LRC/" +
                    "CSV/TSV/SBV/JSON3-document via Open ondertitelbestand…")
        val chosen = if (action == Intent.ACTION_VIEW) intent.data else
            runCatching {
                // This is a URI supplied by the user's own single-file SHARE.
                // Do not read clipData or recursively inspect attachments.
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }.getOrNull()
        if (chosen == null || chosen.scheme != "content" ||
            chosen.authority.isNullOrBlank())
            return IncomingViewedSubtitleDocument(requestId, null,
                "Alleen bewust geopende Android-contentdocumenten zijn toegestaan; " +
                    "geen externe of lokale bestands-URL's.")
        return IncomingViewedSubtitleDocument(requestId, chosen)
    }
}
