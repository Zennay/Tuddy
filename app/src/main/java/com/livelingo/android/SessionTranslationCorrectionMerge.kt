package com.livelingo.android

/**
 * Opt-in, all-or-nothing reconciliation of HUMAN-authored corrections.
 * Identity is an exact sentence + ordered language pair; never fuzzy matching.
 * Nothing is translated, persisted, sent to the cloud or silently evicted.
 */
internal enum class CorrectionConflictChoice { KEEP_CURRENT, USE_IMPORTED }

internal data class CorrectionMergeKey(
    val original: String,
    val sourceCode: String,
    val targetCode: String
)

internal data class CorrectionMergeConflict(
    val current: SessionTranslationCorrection,
    val incoming: SessionTranslationCorrection
) {
    val key: CorrectionMergeKey get() = CorrectionMergeKey(
        incoming.original, incoming.sourceCode, incoming.targetCode
    )
}

internal data class CorrectionMergeProposal(
    val rows: List<SessionTranslationCorrection>,
    val added: Int,
    val unchanged: Int,
    val replaced: Int,
    val kept: Int,
    val conflicts: List<CorrectionMergeConflict>
)

internal object SessionTranslationCorrectionMerge {
    fun prepare(
        current: List<SessionTranslationCorrection>,
        imported: List<SessionTranslationCorrection>,
        choice: CorrectionConflictChoice
    ): CorrectionMergeProposal? = reconcile(current, imported) {
        choice == CorrectionConflictChoice.USE_IMPORTED
    }

    /** Only the specifically ticked exact conflicts use incoming translations. */
    fun prepareSelected(
        current: List<SessionTranslationCorrection>,
        imported: List<SessionTranslationCorrection>,
        useImportedFor: Set<CorrectionMergeKey>
    ): CorrectionMergeProposal? {
        val proposal = reconcile(current, imported) { key -> key in useImportedFor }
            ?: return null
        // Fail closed on stale selections after the source document changes.
        val actualKeys = proposal.conflicts.map { it.key }.toSet()
        return proposal.takeIf { actualKeys.containsAll(useImportedFor) }
    }

    private fun reconcile(
        current: List<SessionTranslationCorrection>,
        imported: List<SessionTranslationCorrection>,
        preferImported: (CorrectionMergeKey) -> Boolean
    ): CorrectionMergeProposal? {
        if (!SessionTranslationCorrections.validEntries(current) ||
            !SessionTranslationCorrections.validEntries(imported)) return null

        val merged = current.toMutableList()
        val positions = merged.indices.associate { index ->
            val row = merged[index]
            CorrectionMergeKey(row.original, row.sourceCode, row.targetCode) to index
        }.toMutableMap()
        val differences = mutableListOf<CorrectionMergeConflict>()
        var added = 0
        var unchanged = 0
        var replaced = 0
        var kept = 0

        for (incoming in imported) {
            val key = CorrectionMergeKey(
                incoming.original, incoming.sourceCode, incoming.targetCode
            )
            val index = positions[key]
            if (index == null) {
                // Full import refusal is safer than dropping an older suggestion.
                if (merged.size >= SessionTranslationCorrections.MAX_ENTRIES) return null
                positions[key] = merged.size
                merged.add(incoming)
                added++
            } else {
                val previous = merged[index]
                if (previous.translation == incoming.translation) {
                    unchanged++
                } else {
                    differences.add(CorrectionMergeConflict(previous, incoming))
                    if (preferImported(key)) {
                        merged[index] = incoming
                        replaced++
                    } else {
                        kept++
                    }
                }
            }
        }
        if (!SessionTranslationCorrections.validEntries(merged)) return null
        return CorrectionMergeProposal(
            rows = merged.toList(), added = added, unchanged = unchanged,
            replaced = replaced, kept = kept, conflicts = differences.toList()
        )
    }
}
