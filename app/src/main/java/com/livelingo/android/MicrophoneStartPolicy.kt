package com.livelingo.android

import androidx.lifecycle.Lifecycle

/**
 * A permission callback can complete after an Activity was backgrounded.
 * Only allow the local recognizer while its owner is at least STARTED.
 */
internal object MicrophoneStartPolicy {
    fun mayStart(state: Lifecycle.State): Boolean = state.isAtLeast(Lifecycle.State.STARTED)
}
