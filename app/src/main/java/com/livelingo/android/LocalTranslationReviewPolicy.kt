package com.livelingo.android

/**
 * Admission for explicit human corrections and ALL Android outbound text
 * routes (clipboard, sharesheet, bilingual handoff and PROCESS_TEXT result).
 *
 * Unlike Android's editor widget, unsafe bidi override characters are NOT
 * necessarily ISO control characters. Reuse the same Unicode boundary as
 * incoming selected text and local ML Kit results; never let a manually edited
 * result silently bypass those checks on its way to another app.
 *
 * Normal RTL scripts, joiners and isolate marks remain allowed. This is a
 * privacy/display integrity boundary, NOT independent linguistic validation.
 */
internal object LocalTranslationReviewPolicy {
    const val MAX_REVIEW_UNITS = 8_000
    const val MAX_EDIT_BUFFER_UNITS = MAX_REVIEW_UNITS + 1

    /** No truncation. Preserves paragraph breaks and ordinary RTL writing. */
    fun normalize(draft: String): String? =
        LocalTextBoundaryPolicy.selectedTranslation(draft)
            ?.takeIf { it.length <= MAX_REVIEW_UNITS }

    /**
     * Explain an invalid user edit without logging or echoing private text.
     * If accepted, return null. Save and export use normalize() as the
     * authoritative admission, never this message as a security decision.
     */
    fun validationNotice(draft: String): String? {
        if (normalize(draft) != null) return null
        if (draft.isBlank()) return "Voer eerst je eigen vertaling in."
        if (draft.length > MAX_REVIEW_UNITS) {
            return "Maximaal 8.000 tekens; Tuddy kapt je correctie niet stilzwijgend af."
        }
        var index = 0
        while (index < draft.length) {
            val ch = draft[index]
            if (ch in '\u202A'..'\u202E') {
                return "Verborgen Unicode-richtingsoverschrijvingen zijn niet toegestaan. " +
                    "Gebruik gewone RTL-tekst zonder geforceerde tekenvolgorde."
            }
            if (Character.isHighSurrogate(ch)) {
                if (index + 1 >= draft.length ||
                    !Character.isLowSurrogate(draft[index + 1])) {
                    return "Een Unicode-teken is onvolledig; controleer de ingevoerde tekst."
                }
                index += 2
                continue
            }
            if (Character.isLowSurrogate(ch)) {
                return "Een Unicode-teken is onvolledig; controleer de ingevoerde tekst."
            }
            if (ch == '\u0000' ||
                (ch.isISOControl() && ch != '\n' && ch != '\r' && ch != '\t')) {
                return "Onzichtbare besturingstekens kunnen niet worden gekopieerd of gedeeld."
            }
            index++
        }
        return "Deze tekst voldoet niet aan de veilige Unicode-grens voor Android-uitwisseling."
    }
}
