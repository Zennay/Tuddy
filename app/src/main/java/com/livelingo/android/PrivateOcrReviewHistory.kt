package com.livelingo.android

import android.os.SystemClock

/**
 * Ephemeral OCR text-edit history. No images, OCR text or revisions leave RAM.
 * One history tracks manual edits AND selection/reading-order changes, so an
 * accidental block reorder cannot silently destroy earlier corrections.
 */
internal data class PrivateOcrReviewState(
    val draft: String,
    val selectedIds: Set<Int>,
    val order: List<Int>
)

internal class PrivateOcrReviewHistory {
    private val past = ArrayDeque<PrivateOcrReviewState>()
    private val future = ArrayDeque<PrivateOcrReviewState>()
    private var current: PrivateOcrReviewState? = null
    private var lastTypingAt = 0L
    private var lastWasTyping = false

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun reset(start: PrivateOcrReviewState? = null) {
        past.clear()
        future.clear()
        current = start
        lastTypingAt = 0L
        lastWasTyping = false
    }

    fun change(next: PrivateOcrReviewState, typing: Boolean = false): Boolean {
        if (next.draft.length > MAX_TEXT) return false
        val previous = current
        if (previous == next) return false
        if (previous == null) {
            reset(next)
            return true
        }
        val now = SystemClock.uptimeMillis()
        // A rapid stream of IME keystrokes should undo as one human edit,
        // rather than spending all 16 snapshots on individual characters.
        val coalesce = typing && lastWasTyping &&
            now - lastTypingAt in 0..TYPING_GROUP_MS &&
            future.isEmpty()
        if (!coalesce) {
            if (past.size == MAX_UNDO) past.removeFirst()
            past.addLast(previous)
        }
        current = next
        future.clear()
        lastWasTyping = typing
        lastTypingAt = now
        return true
    }

    fun undo(): PrivateOcrReviewState? {
        val previous = past.removeLastOrNull() ?: return null
        current?.let { future.addLast(it) }
        current = previous
        lastWasTyping = false
        return previous
    }

    fun redo(): PrivateOcrReviewState? {
        val next = future.removeLastOrNull() ?: return null
        current?.let {
            if (past.size == MAX_UNDO) past.removeFirst()
            past.addLast(it)
        }
        current = next
        lastWasTyping = false
        return next
    }

    companion object {
        private const val MAX_UNDO = 16
        private const val MAX_TEXT = 8_000
        private const val TYPING_GROUP_MS = 750L
    }
}

/** Literal (never regex) offline OCR correction, only after user confirmation. */
internal data class PrivateOcrReplacePlan(
    val original: String,
    val updated: String,
    val occurrences: Int
)

internal object PrivateOcrReplacePolicy {
    fun plan(text: String, needle: String, replacement: String): PrivateOcrReplacePlan? {
        if (text.isBlank() || text.length > 8_000 ||
            needle.isEmpty() || needle.length > 120 || replacement.length > 120 ||
            needle.any { it.isISOControl() } ||
            replacement.any { it.isISOControl() }) return null
        var pos = 0
        var found = 0
        val result = StringBuilder(text.length)
        while (pos < text.length) {
            val next = text.indexOf(needle, pos, ignoreCase = false)
            if (next < 0) {
                result.append(text, pos, text.length)
                break
            }
            if (++found > 100) return null
            result.append(text, pos, next)
            result.append(replacement)
            if (result.length > 8_000) return null
            pos = next + needle.length
        }
        val revised = result.toString()
        // Reuse the same UTF-16/control/bidi admission as the existing
        // explicit translate/copy/save/share path. A replacement must never
        // manufacture a broken surrogate or unsafe output string.
        return if (found in 1..100 && revised.length <= 8_000 &&
            revised != text && LocalTextBoundaryPolicy.selectedTranslation(revised) != null)
            PrivateOcrReplacePlan(text, revised, found) else null
    }
}
