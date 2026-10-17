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

    fun delayForError(error: Int, noMatch: Int, speechTimeout: Int): Long? {
        require(noMatch != speechTimeout) { "Error codes must be distinct" }
        return if (error == noMatch || error == speechTimeout) QUIET_RETRY_MS else null
    }
}
