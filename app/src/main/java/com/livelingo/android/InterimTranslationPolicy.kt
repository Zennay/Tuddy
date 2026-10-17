package com.livelingo.android

/**
 * The original LiveLingo v0.4 displayed a live text translation while a
 * speaker was still talking. This version bounds work and uses *only*
 * already-installed on-device ML Kit text models during provisional speech.
 */
internal object InterimTranslationPolicy {
    const val STABLE_WAIT_MS = 650L
    const val MAX_INTERIM_CHARS = 1500

    fun eligible(text: String, sourceCode: String, targetCode: String, listening: Boolean): Boolean =
        listening && text.isNotBlank() && text.length <= MAX_INTERIM_CHARS &&
            sourceCode in TargetCatalog.codes && targetCode in TargetCatalog.codes

    fun mayPublish(
        requestedRevision: Int,
        currentRevision: Int,
        requestedGeneration: Int,
        currentGeneration: Int,
        recognizerStillCurrent: Boolean,
        listening: Boolean
    ): Boolean =
        listening && recognizerStillCurrent &&
            requestedRevision == currentRevision &&
            requestedGeneration == currentGeneration
}
