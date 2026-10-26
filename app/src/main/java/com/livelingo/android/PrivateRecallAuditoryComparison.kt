package com.livelingo.android

/**
 * A private, user-triggered two-line listening plan for an already revealed
 * bilingual recall card. This never grades an answer, changes a caption,
 * invokes ML Kit, starts a microphone, or saves a learner attempt.
 *
 * The Android TextToSpeech adapter still performs its independent installed,
 * non-network voice and output-device checks on every explicit Play.
 */
internal object PrivateRecallAuditoryComparison {
    private const val MAX_UTTERANCE_UNITS = 400

    fun twoUtterances(attempt: String, revealedReference: String): List<String>? {
        val own = admitted(attempt) ?: return null
        val reference = admitted(revealedReference) ?: return null
        return listOf(own, reference)
    }

    private fun admitted(text: String): String? {
        val normalized = LocalTextBoundaryPolicy.speechHypothesis(text) ?: return null
        if (normalized.length > MAX_UTTERANCE_UNITS ||
            normalized.any { it.isISOControl() }) return null
        return normalized
    }
}
