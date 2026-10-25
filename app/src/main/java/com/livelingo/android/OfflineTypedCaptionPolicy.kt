package com.livelingo.android

/**
 * Manual offline text is a fallback when the device has no usable on-device
 * speech recognizer. The input is RAM-only and shares the existing bounded
 * caption/ML Kit translation pipeline; it never asks for microphone access.
 */
internal object OfflineTypedCaptionPolicy {
    const val MAX_INPUT_UNITS = 1500

    fun normalize(raw: String): String? {
        // Preserve the established, explicit per-field admission guarantee;
        // the new shared boundary adds UTF-16 and bidi checks after this.
        if (raw.length > MAX_INPUT_UNITS ||
            raw.any { Character.isISOControl(it) && it != '\n' && it != '\t' }) return null
        return LocalTextBoundaryPolicy.typedCaption(raw)
    }
}
