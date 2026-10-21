package com.livelingo.android

import android.content.Intent
import android.net.Uri

/**
 * User-initiated Android "Open with" for a content:// subtitle document.
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
        "application/json"
    )

    fun read(intent: Intent?, requestId: Long): IncomingViewedSubtitleDocument? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val type = intent.type?.substringBefore(';')?.trim()?.lowercase()
        if (type !in allowedTypes)
            return IncomingViewedSubtitleDocument(requestId, null,
                "Geen ondersteund tekstondertiteltype. Kies een SRT/VTT/LRC/" +
                    "CSV/TSV/SBV/JSON3-document via Open ondertitelbestand…")
        val chosen = intent.data
        if (chosen == null || chosen.scheme != "content" ||
            chosen.authority.isNullOrBlank())
            return IncomingViewedSubtitleDocument(requestId, null,
                "Alleen bewust geopende Android-contentdocumenten zijn toegestaan; " +
                    "geen externe of lokale bestands-URL's.")
        return IncomingViewedSubtitleDocument(requestId, chosen)
    }
}
