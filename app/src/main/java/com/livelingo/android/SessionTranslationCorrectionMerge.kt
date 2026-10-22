package com.livelingo.android

/**
 * Opt-in, all-or-nothing reconciliation of two sets of HUMAN corrections.
 * Identity is an exact source sentence + ordered language pair, never a
 * normalized/fuzzy match. No translator or cloud service is involved.
 */
internal enum class CorrectionConflictChoice { KEEP_CURRENT, USE_IMPORTED }

internal data class CorrectionMergeConflict(
    val current: SessionTranslationCorrection,
    val incoming: SessionTranslationCorrection
)

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
    ): CorrectionMergeProposal? {
        if (!SessionTranslationCorrections.validEntries(current) ||
            !SessionTranslationCorrections.validEntries(imported)) return null

        val merged = current.toMutableList()
        val positions = merged.indices.associate { index ->
            val row = merged[index]
            Triple(row.original, row.sourceCode, row.targetCode) to index
        }.toMutableMap()
        val differences = mutableListOf<CorrectionMergeConflict>()
        var added = 0
        var unchanged = 0
        var replaced = 0
        var kept = 0

        imported.forEach { incoming ->
            val key = Triple(incoming.original, incoming.sourceCode, incoming.targetCode)
            val index = positions[key]
            if (index == null) {
                // Refuse overflow rather than silently evicting old suggestions.
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
                    if (choice == CorrectionConflictChoice.USE_IMPORTED) {
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
