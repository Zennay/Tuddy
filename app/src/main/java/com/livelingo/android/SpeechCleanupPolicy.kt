package com.livelingo.android

/**
 * Close the on-device Android SpeechRecognizer on every terminal path.
 *
 * A failing cancel call must never prevent destroy from running. Both
 * operations are best-effort: the caller invalidates its callback generation
 * and removes the recognizer reference BEFORE invoking this boundary.
 *
 * Pure Kotlin callbacks keep this lifecycle invariant independently testable
 * without an Android microphone or external speech provider.
 */
internal object SpeechCleanupPolicy {
    fun release(cancel: () -> Unit, destroy: () -> Unit) {
        try {
            cancel()
        } catch (_: Exception) {
            // Android service can already be disconnected after permission loss.
        }
        try {
            destroy()
        } catch (_: Exception) {
            // Do not resume old callbacks or resurrect the failed session.
        }
    }
}
