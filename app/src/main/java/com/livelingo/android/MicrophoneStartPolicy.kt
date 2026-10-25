package com.livelingo.android

import androidx.lifecycle.Lifecycle

/**
 * Existing coarse foreground eligibility gate: a permission callback must not
 * start a local recognizer after the owner has STOPPED.
 *
 * Android multi-window can keep an unfocused Activity STARTED. Production
 * microphone entrypoints additionally require RESUMED before actually
 * opening or restarting capture. Keep this compatibility policy separate
 * from that stricter user-interaction admission check.
 */
internal object MicrophoneStartPolicy {
    fun mayStart(state: Lifecycle.State): Boolean =
        state.isAtLeast(Lifecycle.State.STARTED)
}
