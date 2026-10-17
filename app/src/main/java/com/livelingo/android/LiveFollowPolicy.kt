package com.livelingo.android

/**
 * The active partial caption is a separate LazyColumn item after the final
 * transcript items. Never auto-scroll when the user has paused follow-live.
 */
internal object LiveFollowPolicy {
    /** Zero-based list index, or null when there is no visible item to follow. */
    fun targetIndex(finalCount: Int, hasPartial: Boolean, followEnabled: Boolean): Int? {
        require(finalCount >= 0) { "Invalid final-caption count" }
        if (!followEnabled) return null
        return when {
            hasPartial -> finalCount
            finalCount > 0 -> finalCount - 1
            else -> null
        }
    }
}
