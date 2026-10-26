package com.livelingo.android

import android.content.Context

/**
 * A user-controlled, app-wide privacy preference for local Android TTS.
 *
 * Only the boolean choice is persisted. No text, device names, routes, audio
 * samples, timings or caller context are written to storage. Disabled by
 * default to preserve the existing deliberate loudspeaker playback path.
 */
internal object PrivatePlaybackRoutingPreference {
    private const val PREFS = "tuddy-private-playback-routing-v1"
    private const val HEADPHONES_ONLY = "headphones-only"
    private const val RESPECT_QUIET_MODE = "respect-quiet-mode"

    fun headphonesOnly(context: Context): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(HEADPHONES_ONLY, false)
    // Corrupt/unreadable privacy settings must never silently permit speakers.
    }.getOrDefault(true)

    /**
     * Additional opt-in: no local TTS in Android silent or vibrate ringer
     * mode. Default false preserves intentional media playback on existing
     * devices. If preferences cannot be read, favor the private choice.
     */
    fun respectQuietMode(context: Context): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(RESPECT_QUIET_MODE, false)
    }.getOrDefault(true)

    /** Stop local TTS before saving; affects the main and selected-text screen. */
    fun setRespectQuietMode(context: Context, enabled: Boolean): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(RESPECT_QUIET_MODE, enabled)
            .commit()
    }.getOrDefault(false)

    /** The caller must stop any active TTS session *before* changing policy. */
    fun setHeadphonesOnly(context: Context, enabled: Boolean): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(HEADPHONES_ONLY, enabled)
            .commit()
    }.getOrDefault(false)
}
