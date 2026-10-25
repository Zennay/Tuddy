package com.livelingo.android

/**
 * A recognizer can report multiple terminal callbacks for one recognition turn.
 * The session epoch rejects old recognizers; this gate also makes a current
 * recognizer's final result and terminal error mutually exclusive.
 *
 * It holds no transcript or provider data, starts no service, and does not
 * suppress intentionally repeated phrases in *different* listening turns.
 */
internal class SpeechTurnCompletionGate {
    private var completed = false

    fun isAwaitingResult(): Boolean = !completed

    /** Exactly one onResults callback may commit a final caption or restart. */
    fun acceptFinalResult(): Boolean {
        if (completed) return false
        completed = true
        return true
    }

    /** An error after a final result cannot cancel a successfully heard turn. */
    fun acceptTerminalError(): Boolean {
        if (completed) return false
        completed = true
        return true
    }
}
