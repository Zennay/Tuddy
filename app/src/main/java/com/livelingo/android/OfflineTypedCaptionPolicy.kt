package com.livelingo.android

/**
 * Manual offline text is a fallback when the device has no usable on-device
 * speech recognizer. The input is RAM-only and shares the existing bounded
 * caption/ML Kit translation pipeline; it never asks for microphone access.
 */
internal object OfflineTypedCaptionPolicy {
    const val MAX_INPUT_UNITS = 1500

    fun normalize(raw: String): String? {
        // The same Unicode boundary applies to manually typed captions as
        // to Android speech and device-local ML Kit results. Never truncate.
        return LocalTextBoundaryPolicy.typedCaption(raw)
    }
}
