package com.livelingo.android

/**
 * Keep only a tiny, session-scoped set of Android's *unverified* alternate
 * offline recognizer hypotheses for deliberate manual caption correction.
 * No API call, model inference, logging, raw audio, disk or network usage.
 */
internal object OfflineSpeechCorrectionChoices {
    private const val MAX_ANDROID_HYPOTHESES = 8
    private const val MAX_CHOICES = 3

    fun fromDeviceResults(
        candidates: List<String?>?,
        selectedCaption: String
    ): List<String> {
        if (selectedCaption.isBlank()) return emptyList()
        val seen = mutableSetOf(selectedCaption)
        val choices = mutableListOf<String>()
        for (raw in candidates.orEmpty().take(MAX_ANDROID_HYPOTHESES)) {
            // Reuse the exact accepted-caption size and whitespace restrictions.
            val normalized = SpeechHypothesisPolicy.select(listOf(raw))
            if (normalized.isBlank() || !seen.add(normalized)) continue
            choices.add(normalized)
            if (choices.size == MAX_CHOICES) break
        }
        return choices
    }
}
