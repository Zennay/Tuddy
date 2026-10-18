package com.livelingo.android

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Optional, short on-device haptic cue for a *completed* live speech caption.
 *
 * No caption content, timestamps, volume, locale, identity or audio stored.
 * Does not create notifications, background services or analytics.
 * Vibration availability and system preferences are owned by Android.
 */
internal class CaptionHapticCues(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        "tuddy-caption-haptics-v1", Context.MODE_PRIVATE
    )
    private val vibrator: Vibrator? = runCatching {
        appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }.getOrNull()
    private var lastSignalAtMillis = 0L

    fun available(): Boolean =
        runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)

    fun enabled(): Boolean =
        runCatching { preferences.getBoolean("completed-caption-signal", false) }
            .getOrDefault(false)

    fun changeEnabled(enabled: Boolean): Boolean {
        if (enabled && !available()) return false
        return runCatching {
            preferences.edit().putBoolean("completed-caption-signal", enabled).commit()
        }.getOrDefault(false)
    }

    fun notifyCompletedSpeechCaption() {
        if (!enabled() || !available()) return
        val now = SystemClock.elapsedRealtime()
        // Frequent recognition restarts must not make the phone buzz continuously.
        if (lastSignalAtMillis > 0 && now - lastSignalAtMillis < 3_000L) return
        if (pulse()) lastSignalAtMillis = now
    }

    /** Explicit preview gesture. Never played automatically by settings UI. */
    fun preview(): Boolean = enabled() && available() && pulse()

    private fun pulse(): Boolean = runCatching {
        vibrator?.vibrate(
            VibrationEffect.createOneShot(65L, VibrationEffect.DEFAULT_AMPLITUDE)
        )
        vibrator != null
    }.getOrDefault(false)
}
