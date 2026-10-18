package com.livelingo.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager

/**
 * User-selected Android FLAG_SECURE, not an encryption or storage feature.
 * Persists only one private Boolean. Never accesses captions, images, audio,
 * clipboard or network services.
 */
internal object PrivateScreenShield {
    private const val PREFS = "tuddy-private-screen"
    private const val ENABLED = "shield-enabled"

    fun enabled(context: Context): Boolean =
        runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(ENABLED, false) }.getOrDefault(false)

    fun activity(context: Context): Activity? {
        var current: Context = context
        repeat(8) {
            if (current is Activity) return current as Activity
            current = (current as? ContextWrapper)?.baseContext ?: return null
        }
        return null
    }

    fun restore(activity: Activity) {
        applyToWindow(activity, enabled(activity))
    }

    fun change(activity: Activity, requested: Boolean): Boolean {
        val previous = enabled(activity)
        if (!applyToWindow(activity, requested)) return false
        val stored = runCatching {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(ENABLED, requested).commit()
        }.getOrDefault(false)
        if (!stored) applyToWindow(activity, previous)
        return stored
    }

    private fun applyToWindow(activity: Activity, protect: Boolean): Boolean =
        runCatching {
            val flag = WindowManager.LayoutParams.FLAG_SECURE
            if (protect) activity.window.addFlags(flag)
            else activity.window.clearFlags(flag)
            true
        }.getOrDefault(false)
}
