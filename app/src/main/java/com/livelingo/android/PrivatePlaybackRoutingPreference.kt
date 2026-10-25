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

    fun headphonesOnly(context: Context): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(HEADPHONES_ONLY, false)
    }.getOrDefault(false)

    /** The caller must stop any active TTS session *before* changing policy. */
    fun setHeadphonesOnly(context: Context, enabled: Boolean): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(HEADPHONES_ONLY, enabled)
            .commit()
    }.getOrDefault(false)
}
