package com.livelingo.android

/**
 * Explicit multi-range deletion in an ephemeral, user-opened subtitle draft.
 * Source row indices are 1-based, relative to the complete current draft.
 * No file IO, persistence, recognition, automatic cleanup or live edits.
 */
internal data class SubtitleBulkRemovalPreview(
    val remaining: List<PrivateSubtitleCue>,
    val removedRows: List<Int>
)

internal object PrivateSubtitleBulkRemoval {
    fun preview(
        current: List<PrivateSubtitleCue>,
        expression: String
    ): SubtitleBulkRemovalPreview? {
        if (current.size !in 2..500) return null
        val selected = PrivateSubtitleRowSelection.parse(expression, current.size)
            ?: return null
        // Deleting the complete document is not an editor operation.
        if (selected.isEmpty() || selected.size >= current.size) return null
        val marks = BooleanArray(current.size)
        for (index in selected) marks[index] = true
        val remaining = current.filterIndexed { index, _ -> !marks[index] }
        if (PrivateSubtitleDraftEditor.snapshot(remaining, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(remaining, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return SubtitleBulkRemovalPreview(remaining, selected.map { it + 1 })
    }
}
