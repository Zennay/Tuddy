package com.livelingo.android

import android.content.Intent

/**
 * Opt-in Android Sharesheet and text-selection ingress. Accept literal
 * text/plain ACTION_SEND or ACTION_PROCESS_TEXT only; never follow URLs,
 * attachments or HTML providers.
 * Importing prepares an editable draft, not an automatic translation/export.
 */
internal data class IncomingSharedText(val draft: String?, val notice: String?)

internal object SharedPlainTextIntent {
    fun read(intent: Intent?): IncomingSharedText? {
        val action = intent?.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_PROCESS_TEXT) return null
        if (intent.type?.substringBefore(';')?.trim()?.lowercase() != "text/plain")
            return null

        // Even a mislabeled text/plain send can carry attachments or HTML.
        // Refuse rather than reading or silently downgrading that payload.
        if (intent.hasExtra(Intent.EXTRA_STREAM) ||
            intent.hasExtra(Intent.EXTRA_HTML_TEXT)) {
            return IncomingSharedText(null,
                "Bijlagen en HTML worden niet overgenomen. Deel alleen platte tekst.")
        }

        val supplied = runCatching {
            val key = if (action == Intent.ACTION_PROCESS_TEXT)
                Intent.EXTRA_PROCESS_TEXT else Intent.EXTRA_TEXT
            intent.getCharSequenceExtra(key)?.toString()
        }.getOrNull()
        val clean = supplied?.let(OfflineTypedCaptionPolicy::normalize)
        return if (clean != null) {
            IncomingSharedText(clean, null)
        } else {
            IncomingSharedText(null,
                "Tekst niet overgenomen. Gebruik platte tekst van maximaal " +
                    "${OfflineTypedCaptionPolicy.MAX_INPUT_UNITS} tekens, zonder stuurtekens.")
        }
    }
}
