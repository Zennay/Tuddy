package com.livelingo.android

/**
 * Explicit human changes to an offline ML Kit translation, kept in RAM only.
 *
 * Android-selected text can be much longer than one subtitle; reject blank,
 * oversized, invalid Unicode and unexpected control characters rather than
 * sending a truncated or corrupt partial correction to another app.
 *
 * This is a text safety bound, NOT any claim that a human-reviewed sentence
 * has been linguistically verified by Tuddy or an independent speaker.
 */
internal object LocalTranslationReviewPolicy {
    const val MAX_REVIEW_UNITS = 8_000
    const val MAX_EDIT_BUFFER_UNITS = MAX_REVIEW_UNITS + 1

    fun normalize(draft: String): String? {
        val text = draft.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (text.isEmpty() || text.length > MAX_REVIEW_UNITS) return null
        for (i in text.indices) {
            val c = text[i]
            if (c == '\u0000' || (c.isISOControl() && c != '\n' && c != '\t')) return null
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= text.length || !Character.isLowSurrogate(text[i + 1])) return null
            }
            if (Character.isLowSurrogate(c)) {
                if (i == 0 || !Character.isHighSurrogate(text[i - 1])) return null
            }
        }
        return text
    }
}
