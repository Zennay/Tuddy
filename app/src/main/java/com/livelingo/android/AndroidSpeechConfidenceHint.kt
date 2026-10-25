package com.livelingo.android

import kotlin.math.roundToInt

/**
 * Non-authoritative Android speech-recognizer metadata for optional HUMAN
 * review, never a substitute for audio/model validation. Vendors can omit
 * scores (-1), reorder/misalign candidates or expose uncalibrated numbers.
 * Neither a high score nor a missing score proves that speech was correct.
 *
 * This policy is deliberately not used to auto-correct, discard, translate
 * differently or send any private utterance outside the local caption.
 */
internal data class AndroidSpeechConfidenceHint(
    val reportedPercent: Int,
    val needsReview: Boolean
)

internal object AndroidSpeechConfidencePolicy {
    private const val LOW_SCORE_THRESHOLD = 0.5f

    fun forSelectedHypothesis(
        hypotheses: List<String?>?,
        scores: FloatArray?,
        selectedText: String
    ): AndroidSpeechConfidenceHint? {
        if (selectedText.isBlank() || hypotheses == null || scores == null) return null
        // SpeechHypothesisPolicy chooses the first acceptable normalized
        // hypothesis; a score belongs to its ORIGINAL index, not always 0.
        val index = hypotheses.indexOfFirst { raw ->
            SpeechHypothesisPolicy.select(listOf(raw)) == selectedText
        }
        if (index < 0 || index >= scores.size) return null
        val score = scores[index]
        if (!score.isFinite() || score < 0f || score > 1f) return null
        return AndroidSpeechConfidenceHint(
            reportedPercent = (score * 100f).roundToInt().coerceIn(0, 100),
            needsReview = score < LOW_SCORE_THRESHOLD
        )
    }
}
