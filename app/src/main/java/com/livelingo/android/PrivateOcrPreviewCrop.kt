package com.livelingo.android

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * User-confirmed RAM-only crop on a previously reviewed, bounded image preview.
 *
 * The source is never modified, no file/URI is written, and the user sees the
 * *actual resulting bitmap* before explicitly starting bundled offline OCR.
 * Apply current right-angle preview rotation before selecting the image area,
 * so "upper/left" matches what the user is looking at.
 */
internal enum class PrivateOcrCropArea(
    val title: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    UPPER_HALF("Bovenste helft", 0f, 0f, 1f, 0.5f),
    LOWER_HALF("Onderste helft", 0f, 0.5f, 1f, 1f),
    LEFT_HALF("Linkerhelft", 0f, 0f, 0.5f, 1f),
    RIGHT_HALF("Rechterhelft", 0.5f, 0f, 1f, 1f),
    CENTER("Middelste helft", 0.25f, 0.25f, 0.75f, 0.75f)
}

internal object PrivateOcrPreviewCrop {
    const val MAX_SIDE = 1600
    const val MIN_CROP_SIDE = 32

    /**
     * Returns a new bitmap owned by the caller, or null without modifying the
     * original. The temporary rotated copy is always freed. No OCR launches
     * and no hidden whole-image fallback occurs on invalid crops.
     */
    fun create(source: Bitmap, rotationDegrees: Int, area: PrivateOcrCropArea): Bitmap? {
        if (source.isRecycled || source.width !in 1..MAX_SIDE ||
            source.height !in 1..MAX_SIDE ||
            rotationDegrees !in setOf(0, 90, 180, 270)) return null

        var rotated: Bitmap? = null
        return try {
            val visible = if (rotationDegrees == 0) source else {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                    .also { rotated = it }
            }
            val left = (visible.width * area.left).toInt().coerceIn(0, visible.width)
            val top = (visible.height * area.top).toInt().coerceIn(0, visible.height)
            val right = (visible.width * area.right).toInt().coerceIn(0, visible.width)
            val bottom = (visible.height * area.bottom).toInt().coerceIn(0, visible.height)
            val width = right - left
            val height = bottom - top
            if (width < MIN_CROP_SIDE || height < MIN_CROP_SIDE) null
            else Bitmap.createBitmap(visible, left, top, width, height)
                .takeIf { it !== source && it !== rotated }
        } catch (_: RuntimeException) {
            null
        } finally {
            if (rotated != null && rotated !== source) rotated?.recycle()
        }
    }
}
