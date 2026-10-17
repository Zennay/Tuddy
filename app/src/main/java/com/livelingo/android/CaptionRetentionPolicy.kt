package com.livelingo.android

/**
 * Privacy and memory boundary for a long-lived, RAM-only conversation.
 * Retaining a bounded number of captions mirrors the original v0.4
 * in-memory timeline contract. Never write raw speech or text to disk.
 */
internal object CaptionRetentionPolicy {
    const val MAX_CAPTIONS = 500

    /** Number of oldest entries to evict after appending a final transcript. */
    fun oldestToDrop(count: Int): Int {
        require(count >= 0) { "Invalid caption count" }
        return (count - MAX_CAPTIONS).coerceAtLeast(0)
    }
}
