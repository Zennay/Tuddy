package com.livelingo.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-scoped admission for heavy, uncancellable on-device OCR tasks.
 *
 * The 20-second visual deadline and Activity lifecycle invalidate private
 * content/results, but do not cancel a TextRecognizer.process() already
 * submitted to Android/ML Kit. Each admitted request holds one anonymous
 * slot until a real provider completion (including failures), not merely
 * until the screen disappears or the visible deadline expires.
 *
 * Fail closed when a provider never completes instead of allowing unlimited
 * concurrent bitmap recognition after repeated imports or recreations.
 * No image, recognized text, URI, language or user identity is stored.
 */
internal class LocalOcrWorkBudget private constructor() {
    // Compose only observes an anonymous busy bit, never an image or URI.
    // This lets the picker/camera buttons become available again precisely
    // when the actual provider callback releases the physical OCR slot.
    private var inFlight by mutableStateOf(false)

    internal inner class Lease {
        private var released = false

        /** Call on the main thread when work did not submit or actually ended. */
        fun release() {
            if (released) return
            released = true
            inFlight = false
        }
    }

    fun acquire(): Lease? {
        if (inFlight) return null
        inFlight = true
        return Lease()
    }

    fun isBusy(): Boolean = inFlight

    companion object {
        // OCR bitmaps and recognizers are heavy; one submitted request at a
        // time is a bounded, transparent device-level product constraint.
        val shared = LocalOcrWorkBudget()
    }
}
