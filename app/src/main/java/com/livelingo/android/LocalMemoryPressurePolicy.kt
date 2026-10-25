package com.livelingo.android

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context

/**
 * Android RAM-only memory-pressure admission policy. Never stores user data,
 * never submits a network request, and never attempts to change system limits.
 *
 * Android 14+ no longer emits the legacy RUNNING_LOW/CRITICAL levels. Always
 * consult ActivityManager.MemoryInfo.lowMemory for a fresh, conservative
 * foreground decision; onLowMemory() remains an explicit urgent signal.
 * UI_HIDDEN (20) is NOT a foreground critical warning.
 */
internal object LocalMemoryPressurePolicy {
    fun shouldTrimCachedModels(level: Int): Boolean =
        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE

    @Suppress("DEPRECATION")
    fun isCriticalForegroundTrim(level: Int): Boolean =
        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL &&
            level < ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN

    fun isSystemLowMemory(context: Context): Boolean {
        val manager = context.applicationContext.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as? ActivityManager ?: return false
        return runCatching {
            val state = ActivityManager.MemoryInfo()
            manager.getMemoryInfo(state)
            state.lowMemory
        }.getOrDefault(false)
    }

    fun needsUrgentStop(context: Context, trimLevel: Int): Boolean =
        isCriticalForegroundTrim(trimLevel) || isSystemLowMemory(context)
}
