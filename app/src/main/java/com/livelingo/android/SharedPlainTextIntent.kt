package com.livelingo.android

import android.content.Intent

/**
 * Opt-in Android Sharesheet and text-selection ingress. Accept literal
 * text/plain ACTION_SEND or ACTION_PROCESS_TEXT only; never follow URLs,
 * attachments or HTML providers.
 * Importing prepares an editable draft, not an automatic translation/export.
 */
internal data class IncomingSharedText(
    val draft: String?,
    val notice: String?,
    // Validated SRT/VTT from a deliberate Android text share. Preview only.
    val subtitleDraft: String? = null
)

internal object SharedPlainTextIntent {
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
        // A text/plain sharesheet item may be a complete SRT/WebVTT source,
        // not a short sentence. Only classify it as subtitles if the existing
        // strict, bounded parser accepts the WHOLE document. No URI reads,
        // automatic import, model calls or persisted text are introduced.
        // Reject malformed UTF-16 instead of silently replacing a surrogate
        // while converting the shared String to UTF-8 for parser admission.
        val subtitle = supplied?.takeIf { text ->
            text.length <= PrivateSubtitleFileReader.MAX_BYTES &&
                text.contains("-->") && wellFormedUtf16(text)
        }?.takeIf { text ->
            val bytes = text.toByteArray(Charsets.UTF_8)
            bytes.isNotEmpty() && bytes.size <= PrivateSubtitleFileReader.MAX_BYTES &&
                PrivateSubtitleFileReader.read(bytes) != null
        }
        if (subtitle != null) return IncomingSharedText(null, null, subtitle)
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
