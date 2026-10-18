package com.livelingo.android

/**
 * Explicitly requested formatting of an open, RAM-only subtitle draft.
 * Replaces ordinary single word-separator spaces with line breaks without
 * changing words, their order, or any cue timestamps. Never rewrites files.
 */
internal data class SubtitleLineWrapPreview(
    val cues: List<PrivateSubtitleCue>,
    val changedCues: Int,
    val addedBreaks: Int,
    val remainingLongCues: Int
)

internal object PrivateSubtitleLineWrap {
    private val allowedWidths = setOf(32, 42, 50)

    private fun wrapLine(line: String, width: Int): String {
        var remaining = line
        val pieces = mutableListOf<String>()
        while (remaining.length > width && pieces.size < 7) {
            // Prefer an ordinary single inter-word space. Do not change
            // punctuation, repeated spaces, tabs or words without spaces.
            val point = remaining.lastIndexOf(' ', width)
            if (point < 1 || point >= remaining.lastIndex ||
                remaining[point - 1] == ' ' || remaining[point + 1] == ' '
            ) break
            pieces += remaining.substring(0, point)
            remaining = remaining.substring(point + 1)
        }
        if (pieces.isEmpty()) return line
        pieces += remaining
        return pieces.joinToString("\n")
    }

    fun preview(
        original: List<PrivateSubtitleCue>,
        width: Int = 42
    ): SubtitleLineWrapPreview? {
        if (width !in allowedWidths ||
            PrivateSubtitleDraftEditor.snapshot(original, SubtitleDraftFormat.SRT) == null
        ) return null
        var changed = 0
        var breaks = 0
        var unresolved = 0
        val next = original.map { cue ->
            val proposed = cue.text.lines().joinToString("\n") { wrapLine(it, width) }
            // Never exceed the existing editor's maximum of eight visual lines.
            val safe = if (proposed.lines().size <= 8) proposed else cue.text
            if (safe != cue.text) {
                changed++
                breaks += safe.count { it == '\n' } - cue.text.count { it == '\n' }
            }
            if (safe.lines().any { it.length > width }) unresolved++
            cue.copy(text = safe)
        }
        if (PrivateSubtitleDraftEditor.snapshot(next, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(next, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        return SubtitleLineWrapPreview(
            cues = next, changedCues = changed, addedBreaks = breaks,
            remainingLongCues = unresolved
        )
    }
}
