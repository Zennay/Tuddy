package com.livelingo.android

import java.text.BreakIterator
import java.util.Locale

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

    /**
     * Unicode character boundaries, rather than UTF-16 code units, represent
     * the glyph clusters users see. Emoji (including surrogate pairs), joined
     * marks and Sorani/Pashto diacritics must not count as multiple columns.
     * We intentionally only insert breaks at existing ASCII spaces.
     */
    private fun visibleWidth(line: String): Int {
        val cursor = BreakIterator.getCharacterInstance(Locale.ROOT)
        cursor.setText(line)
        var count = 0
        while (cursor.next() != BreakIterator.DONE) count++
        return count
    }

    /** UTF-16 index at the end of at most `width` grapheme clusters. */
    private fun cutoff(line: String, width: Int): Int {
        val cursor = BreakIterator.getCharacterInstance(Locale.ROOT)
        cursor.setText(line)
        var end = 0
        repeat(width) {
            val next = cursor.next()
            if (next == BreakIterator.DONE) return line.length
            end = next
        }
        return end
    }

    private fun wrapLine(line: String, width: Int): String {
        var remaining = line
        val pieces = mutableListOf<String>()
        while (visibleWidth(remaining) > width && pieces.size < 7) {
            // Use the grapheme cutoff only to FIND an existing separator.
            // Do not split a word, combining sequence, emoji or RTL cluster.
            val point = remaining.lastIndexOf(' ', cutoff(remaining, width))
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
            if (safe.lines().any { visibleWidth(it) > width }) unresolved++
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
