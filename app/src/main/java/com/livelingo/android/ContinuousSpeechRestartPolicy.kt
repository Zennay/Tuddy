package com.livelingo.android

/**
 * Restart only normal end-of-utterance and recognition no-match/timeout
 * situations. Permission, unsupported language, recognizer/client or
 * unexpected errors are never retried in a busy loop.
 *
 * The Activity must separately check the lifecycle, active voice session,
 * permission and user stop state before a delayed restart.
 */
internal object ContinuousSpeechRestartPolicy {
    const val RESULT_DELAY_MS = 350L
    const val QUIET_RETRY_MS = 850L
    // Four consecutive silent recognizer turns without any completed sentence
    // stop a continuous microphone session, requiring another explicit tap.
    // Unlike the no-callback watchdog this also caps normal NO_MATCH,
    // SPEECH_TIMEOUT and empty onResults callbacks.
    const val MAX_QUIET_TURNS = 4
    fun quietRetryExhausted(consecutiveQuietTurns: Int): Boolean =
        consecutiveQuietTurns >= MAX_QUIET_TURNS

    fun delayForError(error: Int, noMatch: Int, speechTimeout: Int): Long? {
        require(noMatch != speechTimeout) { "Error codes must be distinct" }
        return if (error == noMatch || error == speechTimeout) QUIET_RETRY_MS else null
    }
}
