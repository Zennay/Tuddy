package com.livelingo.android

import java.util.Locale

/**
 * Android's on-device recognizer may optionally detect/switch languages on
 * Android 14+, but only for packs supported by that particular device.
 * Treat hints as requests, not installed-pack/recognition evidence.
 */
internal object OfflineSpeechLanguageSwitchPolicy {
    fun shouldRequest(apiLevel: Int, optedIn: Boolean, onDeviceRecognizerAvailable: Boolean): Boolean =
        apiLevel >= 34 && optedIn && onDeviceRecognizerAvailable

    /**
     * Accept only a tag from the *user-visible spoken source picker*.
     * Unknown, unsupported or unexpected callback values fall back to the
     * explicit choice without inventing an Android speech pack.
     */
    fun resolveDetectedTextSource(
        rawTag: String?,
        selectedTextCode: String,
        availableSpokenTags: Collection<String>
    ): String {
        val tag = rawTag?.trim()?.replace('_', '-')?.lowercase(Locale.ROOT)
            ?: return selectedTextCode
        if (!Regex("^[a-z]{2,3}(?:-[a-z0-9]{2,8}){0,3}$").matches(tag)) {
            return selectedTextCode
        }
        val known = availableSpokenTags.any {
            it.lowercase(Locale.ROOT) == tag
        }
        if (!known) return selectedTextCode
        val code = tag.substringBefore('-')
        return if (code in TargetCatalog.codes) code else selectedTextCode
    }
}
