package com.livelingo.android

/**
 * Ephemeral counters for a user's explicitly started on-device recognition.
 *
 * Values contain no microphone samples, utterance text, translation text,
 * speaker identifiers, language predictions, timestamps or stable identifiers.
 * A hosted build or a count in this class is NOT physical offline acceptance.
 */
internal data class OfflineSessionDiagnostics(
    val listeningStarts: Int = 0,
    val interimUpdates: Int = 0,
    val finalUtterances: Int = 0,
    val retryableSpeechErrors: Int = 0,
    val terminalSpeechErrors: Int = 0,
    val localTranslations: Int = 0,
    val missingTextModels: Int = 0,
    val translationFailures: Int = 0,
    val lastTranslationLatencyMs: Long? = null
) {
    fun asReportLines(): List<String> = listOf(
        "Listening starts (user taps): $listeningStarts",
        "Changed nonempty interim hypotheses: $interimUpdates",
        "Nonempty final on-device hypotheses: $finalUtterances",
        "Retryable recognizer errors: $retryableSpeechErrors",
        "Terminal recognizer errors: $terminalSpeechErrors",
        "Completed local text translations of recognized speech: $localTranslations",
        "Recognized speech with missing text model: $missingTextModels",
        "Recognized speech translation failures: $translationFailures",
        "Last completed translation latency (ms since final ASR result): " +
            (lastTranslationLatencyMs?.toString() ?: "not observed")
    )
}
