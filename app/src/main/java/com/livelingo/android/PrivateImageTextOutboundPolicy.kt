package com.livelingo.android

/**
 * Exact, user-reviewed OCR text boundary for explicit Android export.
 * No image URI, provider metadata, language hypothesis or model output is
 * included. Reject rather than silently truncate, normalize confusing bidi
 * controls, or change the user's hand-edited image text.
 *
 * Clipboard, document creation and sharesheet remain three separate
 * user gestures; calling this policy has no side effects.
 */
internal object PrivateImageTextOutboundPolicy {
    const val FILE_NAME = "Tuddy-afbeeldingstekst.txt"

    fun reviewedText(raw: String): String? =
        LocalTextBoundaryPolicy.selectedTranslation(raw)

    fun utf8Bytes(raw: String): ByteArray? =
        reviewedText(raw)?.toByteArray(Charsets.UTF_8)
}
