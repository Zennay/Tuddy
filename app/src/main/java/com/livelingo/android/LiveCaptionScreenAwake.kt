package com.livelingo.android

import android.content.Context

/**
 * An opt-in viewing preference, never an audio/session permission.
 * Only the Boolean is persisted. The Activity owns the actual window flag and
 * must remove it as soon as listening stops or the UI is disposed.
 */
internal object LiveCaptionScreenAwake {
    private const val PREFS = "tuddy_live_screen_awake_v1"
    private const val KEY = "keep_display_on_during_listening"

    fun enabled(context: Context): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)
    }.getOrDefault(false)

    fun changeEnabled(context: Context, requested: Boolean): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, requested).commit()
    }.getOrDefault(false)
}
