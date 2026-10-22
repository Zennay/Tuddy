package com.livelingo.android

import android.content.Intent

/**
 * Opt-in Android Sharesheet and text-selection ingress. Accept literal
 * text/plain ACTION_SEND or ACTION_PROCESS_TEXT only; never follow URLs,
 * attachments or HTML providers. Known Tuddy word/phrasebook/correction formats
 * route to their existing explicit import previews; other text remains a draft.
 * Nothing is automatically translated, imported or exported.
 */
internal data class IncomingSharedText(
    val draft: String?,
    val notice: String?,
    // Validated SRT/VTT/ASS/SSA/LRC/timed CSV/TSV from user-shared text; preview only.
    val subtitleDraft: String? = null,
    val subtitleIsLrc: Boolean = false,
    val subtitleTimedSheetMode: PlainSubtitleSplitMode? = null,
    // A valid, explicitly shared Tuddy correction JSON is handed to its import preview.
    val correctionTransferText: String? = null,
    // The exact five-column personal phrasebook CSV is handed to its own preview.
    val personalPhraseCsvText: String? = null,
    // A Tuddy private phrase backup is routed to the existing merge/replace preview.
    val personalPhraseBackupJson: String? = null,
    // A Tuddy curated glossary-key backup is routed to its replacement preview.
    val curatedWordBackupJson: String? = null,
    // A Tuddy private-word backup is routed to its existing import preview.
    val ownWordBackupJson: String? = null
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
        // Classify complete, explicit user-shared subtitle documents only.
        // Both parsers are already used by the private Android editor. Refuse
        // malformed UTF-16 or excessive UTF-8 before opening a RAM-only draft.
        val candidate = supplied?.takeIf { text ->
            text.length <= PrivateSubtitleFileReader.MAX_BYTES && wellFormedUtf16(text)
        }
        if (candidate != null) {
            val bytes = candidate.toByteArray(Charsets.UTF_8)
            if (bytes.isNotEmpty() && bytes.size <= PrivateSubtitleFileReader.MAX_BYTES) {
                // Tuddy's own portable phrasebook CSV must never fall through
                // as a translation prompt. Recognize only its exact exported
                // header; the phrasebook screen performs full bounded parsing
                // and presents a human-confirmed import preview.
                if (PrivatePhraseTopicCsv.hasPortableHeader(candidate)) {
                    return IncomingSharedText(null, null, personalPhraseCsvText = candidate)
                }
                // Curated glossary-key backups must never fall through as translation text.
                if (candidate.trimStart().startsWith("{") &&
                    candidate.contains("\"tuddy-curated-word-keys-v1\"")) {
                    return IncomingSharedText(null, null, curatedWordBackupJson = candidate)
                }
                // Tuddy's own private-word backup must never fall through as a translation prompt.
                // The word panel reuses the strict parser and explicit merge/replace preview.
                if (candidate.trimStart().startsWith("{") &&
                    candidate.contains("\"tuddy-own-words-v1\"")) {
                    return IncomingSharedText(null, null, ownWordBackupJson = candidate)
                }
                // Tuddy's own private phrase backup must never fall through
                // as a translation prompt. The phrasebook screen performs the
                // strict version/schema/50 KB validation before its import preview.
                if (candidate.trimStart().startsWith("{") &&
                    candidate.contains("\"tuddy-private-phrases-v1\"")) {
                    return IncomingSharedText(null, null, personalPhraseBackupJson = candidate)
                }
                // Tuddy's own correction JSON must never fall through as a
                // translation prompt. Validate its strict, bounded format and
                // send it to the explicit correction-import preview.
                if (candidate.trimStart().startsWith("{") &&
                    candidate.contains("\"tuddy-human-corrections\"")) {
                    val decoded = if (bytes.size <= SessionTranslationCorrectionTransfer.MAX_BYTES)
                        SessionTranslationCorrectionTransfer.decode(bytes) else null
                    return if (decoded != null) {
                        IncomingSharedText(null, null, correctionTransferText = candidate)
                    } else {
                        IncomingSharedText(null,
                            "Gedeelde Tuddy-correcties geweigerd: ongeldige JSON of groter dan 48 KB. " +
                                "De inhoud is niet als vertaaltekst geopend.")
                    }
                }
                if (candidate.contains("[Events]", ignoreCase = true) &&
                    PrivateAssSubtitleReader.read(candidate) != null)
                    return IncomingSharedText(null, null, candidate)
                if (candidate.trimStart().let { it.startsWith("<tt") || it.startsWith("<?xml") }) {
                    if (PrivateTtmlSubtitleReader.read(candidate) != null)
                        return IncomingSharedText(null, null, candidate)
                    return IncomingSharedText(null,
                        "Geen geldig TTML-ondertitelbestand; kies een ondersteund XML-tekstbestand.")
                }
                if (candidate.contains("-->") &&
                    PrivateSubtitleFileReader.read(bytes) != null)
                    return IncomingSharedText(null, null, candidate)
                // LRC tags are NOT speech alignment. Validate every timestamp
                // and lyric row; metadata-only sheets cannot be imported.
                if (candidate.contains('[') &&
                    PrivateLrcSubtitleDraft.preview(candidate) != null)
                    return IncomingSharedText(null, null, candidate, subtitleIsLrc = true)
                // A user may share the exact three-/four-column timed sheet
                // that Tuddy can export. Preserve the original text and
                // explicitly selected input mode; do not treat it as a
                // sentence to translate or invent subtitle timestamps.
                // YouTube SBV timecodes are untrusted user text too. Keep
                // the original full document in a preview-only RAM draft.
                if (PrivateSbvSubtitleReader.read(candidate) != null)
                    return IncomingSharedText(null, null, candidate)
                // A complete, deliberately shared JSON3 caption sheet is a
                // preview-only subtitle draft. Limit Intent/Compose work to
                // 64 KiB; larger sources belong in the async document picker.
                if (candidate.length <= 65_536 &&
                    bytes.size <= 65_536 &&
                    PrivateYoutubeJson3SubtitleReader.read(candidate) != null)
                    return IncomingSharedText(null, null, candidate)
                // An invalid or oversized structured JSON3 share is not a
                // normal sentence. Refuse instead of offering it for translation.
                if (candidate.trimStart().startsWith("{") &&
                    candidate.contains("\"events\""))
                    return IncomingSharedText(null,
                        "Geen geldig gedeeld JSON3-ondertitelblok (maximaal 64 KiB). " +
                            "Gebruik Open ondertitelbestand… voor een groot JSON3-bestand.")
                val timed = PrivateTimedSubtitleSheet.readAuthored(candidate)
                if (timed != null) {
                    val paired = timed.first().text.contains('\n')
                    val mode = if ('\t' in candidate) {
                        if (paired) PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS
                        else PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS
                    } else {
                        if (paired) PlainSubtitleSplitMode.TIMED_PAIRED_CSV
                        else PlainSubtitleSplitMode.TIMED_TEXT_CSV
                    }
                    return IncomingSharedText(null, null, candidate,
                        subtitleTimedSheetMode = mode)
                }
            }
        }
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
