package com.livelingo.android

import android.content.Intent
import android.net.Uri

/**
 * One user-initiated Android CSV document, never a URL or background import.
 * Store only the temporary content:// URI; the personal phrase panel performs
 * bounded strict UTF-8 parsing and asks for explicit row-by-row consent.
 */
internal data class IncomingPersonalCsvDocument(
    val requestId: Long,
    val uri: Uri?,
    val notice: String? = null
)

internal object ExternalPersonalCsvIntent {
    private val allowedTypes = setOf("text/csv", "application/csv")

    fun read(intent: Intent?, requestId: Long): IncomingPersonalCsvDocument? {
        val supplied = intent ?: return null
        val action = supplied.action
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return null
        val mime = supplied.type?.substringBefore(';')?.trim()?.lowercase()
        if (mime !in allowedTypes) return null // Other subtitle/text routes keep priority.
        if (action == Intent.ACTION_SEND && (
                !supplied.hasExtra(Intent.EXTRA_STREAM) ||
                supplied.hasExtra(Intent.EXTRA_TEXT) ||
                supplied.hasExtra(Intent.EXTRA_HTML_TEXT) ||
                (supplied.clipData?.itemCount ?: 0) > 1
            )) return IncomingPersonalCsvDocument(
                requestId, null, "Deel precies één CSV-bestand zonder extra tekst."
            )
        val chosen = if (action == Intent.ACTION_VIEW) supplied.data else
            runCatching { supplied.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) }
                .getOrNull()
        if (chosen == null || chosen.scheme != "content" ||
            chosen.authority.isNullOrBlank()) return IncomingPersonalCsvDocument(
                requestId, null,
                "Alleen bewust geselecteerde Android-contentbestanden zijn toegestaan. " +
                    "Geen bestandspaden, URL's of automatische netwerkimport."
            )
        return IncomingPersonalCsvDocument(requestId, chosen)
    }
}
