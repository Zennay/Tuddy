package com.livelingo.android

/**
 * Ephemeral learner-owned self-assessment, never an automatic translation
 * grade. Indices refer only to the current immutable RAM practice snapshot.
 * A new round starts with an empty review ledger; no typed/spoken attempts
 * or reference text enter this object.
 */
internal enum class RecallSelfAssessment {
    SELF_GOOD, TRY_AGAIN, SKIPPED
}

internal data class PrivateRecallRoundReview(
    private val decisions: Map<Int, RecallSelfAssessment> = emptyMap()
) {
    fun mark(
        captionIndex: Int,
        currentRoundIndices: List<Int>,
        assessment: RecallSelfAssessment
    ): PrivateRecallRoundReview {
        if (captionIndex !in currentRoundIndices) return this
        return copy(decisions = decisions + (captionIndex to assessment))
    }

    fun undo(captionIndex: Int): PrivateRecallRoundReview =
        copy(decisions = decisions - captionIndex)

    fun missed(currentRoundIndices: List<Int>): List<Int> =
        currentRoundIndices.filter { decisions[it] != RecallSelfAssessment.SELF_GOOD }

    fun goodCount(currentRoundIndices: List<Int>): Int =
        currentRoundIndices.count { decisions[it] == RecallSelfAssessment.SELF_GOOD }

    fun skippedCount(currentRoundIndices: List<Int>): Int =
        currentRoundIndices.count { decisions[it] == RecallSelfAssessment.SKIPPED }

    fun needsPracticeCount(currentRoundIndices: List<Int>): Int =
        currentRoundIndices.count { decisions[it] == RecallSelfAssessment.TRY_AGAIN }
}
