package com.livelingo.android

import java.security.MessageDigest

/**
 * RAM-only progress receipt for an explicit Android selected-text translation.
 *
 * The prefix is private and is never rendered, copied, returned to the
 * originating app, or persisted. It represents only fully completed ML Kit
 * chunks. A failed/cancelled native task cannot add to it. Reuse requires the
 * original exact selection, ordered chunk plan and chosen direction.
 */
internal object PrivateSelectedTranslationCheckpoint {
    private const val MAX_RESULT_UNITS = 8_000

    internal data class State(
        val source: String,
        val target: String,
        val selectionFingerprint: String,
        val totalChunks: Int,
        val completedChunks: Int,
        val translatedPrefix: String
    )

    fun completed(
        selection: String,
        source: String,
        target: String,
        chunks: List<LocalSelectedParagraphPlan.Chunk>,
        completedChunks: Int,
        translatedPrefix: String
    ): State? {
        if (selection.isBlank() || selection.length > LocalSelectedParagraphPlan.MAX_SOURCE_UNITS ||
            chunks.size !in 2..LocalSelectedParagraphPlan.MAX_CHUNKS ||
            completedChunks !in 1 until chunks.size ||
            translatedPrefix.isBlank() ||
            translatedPrefix.length > MAX_RESULT_UNITS ||
            source !in TargetCatalog.codes || target !in TargetCatalog.codes ||
            source == target
        ) return null
        return State(
            source, target, fingerprint(selection), chunks.size,
            completedChunks, translatedPrefix
        )
    }

    fun isUsable(
        state: State,
        selection: String,
        source: String,
        target: String,
        chunks: List<LocalSelectedParagraphPlan.Chunk>
    ): Boolean =
        state.source == source && state.target == target &&
            state.selectionFingerprint == fingerprint(selection) &&
            state.totalChunks == chunks.size &&
            state.completedChunks in 1 until chunks.size &&
            state.translatedPrefix.isNotBlank() &&
            state.translatedPrefix.length <= MAX_RESULT_UNITS

    private fun fingerprint(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
