package com.livelingo.android

/**
 * Prevents asynchronous language detection from starting a *new* model
 * download or translation after the user has cleared its source line.
 *
 * Already-started ML Kit downloads cannot be revoked by this gate; the gate
 * only prevents the next stage from being submitted after invalidation.
 */
internal object TranslationWorkPolicy {
    fun mayContinue(capturedGeneration: Int, currentGeneration: Int, lineStillPresent: Boolean): Boolean =
        capturedGeneration == currentGeneration && lineStillPresent
}
