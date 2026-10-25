package com.livelingo.android

import androidx.lifecycle.Lifecycle

/**
 * A permission callback can complete after an Activity was backgrounded.
 * Android multi-window may leave an Activity STARTED while its user has
 * switched focus to another app. Require RESUMED: neither permission-return
 * callbacks nor automated speech restarts may activate the microphone while
 * Tuddy is merely visible but not foreground-interactive.
 */
internal object MicrophoneStartPolicy {
    fun mayStart(state: Lifecycle.State): Boolean = state.isAtLeast(Lifecycle.State.RESUMED)
}
