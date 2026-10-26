package com.livelingo.android

import android.content.Context

/**
 * Device-only presentation controls for the live and archived caption reader.
 *
 * Only a bounded font multiplier is persisted. The reader's private captions,
 * active row, filters, scroll position and microphone state stay RAM-only.
 */
internal object PrivateAccessibleReaderPreferences {
    const val MIN_SCALE = 1.0f
    const val MAX_SCALE = 1.75f
    private const val NAME = "tuddy-reader-accessibility-v1"
    private const val KEY = "caption-zoom-step"
    private const val MAX_STEP = 3

    fun step(context: Context): Int = runCatching {
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getInt(KEY, 0).coerceIn(0, MAX_STEP)
    }.getOrDefault(0)

    fun multiplier(step: Int): Float = 1f + step.coerceIn(0, MAX_STEP) * 0.25f

    fun saveStep(context: Context, step: Int): Boolean {
        if (step !in 0..MAX_STEP) return false
        return runCatching {
            context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY, step).commit()
        }.getOrDefault(false)
    }

    /**
     * Columns are permitted only when enlarged text still has enough room.
     * Android fontScale already applies to TextUnit SP; account for it once
     * in the width admission check, never multiply visual text twice.
     */
    fun canUseTwoColumns(
        screenWidthDp: Int,
        androidFontScale: Float,
        appCaptionScale: Float,
        readerMultiplier: Float
    ): Boolean {
        val total = androidFontScale.coerceIn(1f, 3f) *
            appCaptionScale.coerceIn(0.8f, 3f) *
            readerMultiplier.coerceIn(MIN_SCALE, MAX_SCALE)
        return when {
            screenWidthDp < 600 -> false
            total >= 1.65f -> screenWidthDp >= 900
            total >= 1.25f -> screenWidthDp >= 750
            else -> true
        }
    }
}
