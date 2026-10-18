package com.livelingo.android

import android.content.Intent

/**
 * Opt-in Android Sharesheet ingress. Only literal text/plain ACTION_SEND
 * content is accepted; never follow URLs, attachments or HTML providers.
 * Importing prepares an editable draft, not an automatic translation/export.
 */
internal data class IncomingSharedText(val draft: String?, val notice: String?)

internal object SharedPlainTextIntent {
    fun read(intent: Intent?): IncomingSharedText? {
        if (intent?.action != Intent.ACTION_SEND ||
            intent.type?.substringBefore(';')?.trim()?.lowercase() != "text/plain"
        ) return null

        // Even a mislabeled text/plain send can carry attachments or HTML.
        // Refuse rather than reading or silently downgrading that payload.
        if (intent.hasExtra(Intent.EXTRA_STREAM) ||
            intent.hasExtra(Intent.EXTRA_HTML_TEXT)) {
            return IncomingSharedText(null,
                "Bijlagen en HTML worden niet overgenomen. Deel alleen platte tekst.")
        }

        val supplied = runCatching {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        }.getOrNull()
        val clean = supplied?.let(OfflineTypedCaptionPolicy::normalize)
        return if (clean != null) {
            IncomingSharedText(clean, null)
        } else {
            IncomingSharedText(null,
                "Gedeelde tekst niet overgenomen. Gebruik platte tekst van maximaal " +
                    "${OfflineTypedCaptionPolicy.MAX_INPUT_UNITS} tekens, zonder stuurtekens.")
        }
    }
}
