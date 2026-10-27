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
    // Counts are scoped to the explicitly selected NL/EN direction. They
    // prevent a failed turn in the OTHER direction from being recorded here.
    val nlEnCaptureStarts: Int = 0,
    val enNlCaptureStarts: Int = 0,
    val nlEnRecognizerFailures: Int = 0,
    val enNlRecognizerFailures: Int = 0,
    val interimUpdates: Int = 0,
    val finalUtterances: Int = 0,
    val nlEnFinalUtterances: Int = 0,
    val enNlFinalUtterances: Int = 0,
    val nlEnLocalTranslations: Int = 0,
    val enNlLocalTranslations: Int = 0,
    val retryableSpeechErrors: Int = 0,
    val stalledSpeechTurns: Int = 0,
    val quietBudgetStops: Int = 0,
    val terminalSpeechErrors: Int = 0,
    val localTranslations: Int = 0,
    val missingTextModels: Int = 0,
    val translationFailures: Int = 0,
    val strictRadioInterruptions: Int = 0,
    val microphonePrivacyInterruptions: Int = 0,
    val powerSafetyInterruptions: Int = 0,
    val headsetMicInterruptions: Int = 0,
    val memorySafetyInterruptions: Int = 0,
    val translationLatenciesMs: List<Long> = emptyList()
) {
    /**
     * A fresh local capture/recognizer outcome is required between separately
     * recorded manual attempts. This is only a consistency signal: it cannot
     * attest the spoken words, radio isolation or a correct translation.
     */
    fun hasNewVoiceAttemptSince(previous: OfflineSessionDiagnostics): Boolean =
        listeningStarts > previous.listeningStarts ||
            finalUtterances > previous.finalUtterances ||
            retryableSpeechErrors > previous.retryableSpeechErrors ||
            stalledSpeechTurns > previous.stalledSpeechTurns ||
            quietBudgetStops > previous.quietBudgetStops ||
            terminalSpeechErrors > previous.terminalSpeechErrors

    fun withDirectionalRecognizerFailure(sourceCode: String, targetCode: String):
        OfflineSessionDiagnostics = when {
        sourceCode == "nl" && targetCode == "en" ->
            copy(nlEnRecognizerFailures = nlEnRecognizerFailures + 1)
        sourceCode == "en" && targetCode == "nl" ->
            copy(enNlRecognizerFailures = enNlRecognizerFailures + 1)
        else -> this
    }

    /**
     * Admission signal for a *new* manual observation in one direction.
     * A generic microphone tap/error from the other direction is insufficient.
     * The user must still observe the outcome and actual radio isolation.
     */
    fun hasNewVoiceAttemptInDirectionSince(
        previous: OfflineSessionDiagnostics,
        sourceCode: String,
        targetCode: String
    ): Boolean = when {
        sourceCode == "nl" && targetCode == "en" ->
            nlEnCaptureStarts > previous.nlEnCaptureStarts ||
                nlEnRecognizerFailures > previous.nlEnRecognizerFailures ||
                nlEnFinalUtterances > previous.nlEnFinalUtterances
        sourceCode == "en" && targetCode == "nl" ->
            enNlCaptureStarts > previous.enNlCaptureStarts ||
                enNlRecognizerFailures > previous.enNlRecognizerFailures ||
                enNlFinalUtterances > previous.enNlFinalUtterances
        else -> false
    }

    fun asReportLines(): List<String> = listOf(
        "Listening starts (user taps): $listeningStarts",
        "NL -> EN explicit microphone starts: $nlEnCaptureStarts",
        "EN -> NL explicit microphone starts: $enNlCaptureStarts",
        "NL -> EN recognizer failure events: $nlEnRecognizerFailures",
        "EN -> NL recognizer failure events: $enNlRecognizerFailures",
        "Changed nonempty interim hypotheses: $interimUpdates",
        "Nonempty final on-device hypotheses: $finalUtterances",
        "NL -> EN final on-device hypotheses (not 10-attempt acceptance): $nlEnFinalUtterances",
        "EN -> NL final on-device hypotheses (not 10-attempt acceptance): $enNlFinalUtterances",
        "NL -> EN completed local text translations: $nlEnLocalTranslations",
        "EN -> NL completed local text translations: $enNlLocalTranslations",
        "Retryable recognizer errors: $retryableSpeechErrors",
        "Android recognizer rounds without a terminal callback (RAM only): $stalledSpeechTurns",
        "Continuous microphone sessions ended after repeated silence (RAM only): $quietBudgetStops",
        "Terminal recognizer errors: $terminalSpeechErrors",
        "Strict airplane-mode radio interruptions (not proof of isolation): $strictRadioInterruptions",
        "Android microphone permission/mute/call interruptions (RAM only): $microphonePrivacyInterruptions",
        "Android severe thermal/critical battery stops (RAM only): $powerSafetyInterruptions",
        "Android headset microphone loss interruptions (RAM only): $headsetMicInterruptions",
        "Android critical-memory capture stops (RAM only): $memorySafetyInterruptions",
        "Completed local text translations of recognized speech: $localTranslations",
        "Recognized speech with missing text model: $missingTextModels",
        "Recognized speech translation failures: $translationFailures",
        "Last 10 local translation latencies (ms since each final ASR result): " +
            (translationLatenciesMs.joinToString(", ").ifBlank { "not observed" })
    )
}
