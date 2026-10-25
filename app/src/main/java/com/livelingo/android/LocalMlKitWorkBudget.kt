package com.livelingo.android

/**
 * Counts actual submitted, unfinished local ML Kit translate() tasks, not UI
 * rows or logical queue slots. A user can cancel/retry a row or the 30-second
 * presentation deadline can expire while its underlying Google Task continues
 * running; neither operation releases this physical admission slot.
 *
 * The owner releases a slot ONLY when ML Kit delivers a terminal callback, or
 * when the request throws before it could be submitted. Consequently an
 * unresponsive Android provider can exhaust the three slots; later attempts
 * fail clearly rather than spawning unlimited additional device work.
 *
 * This holds only anonymous slot IDs. No caption text, language or user
 * identity is stored. Call from Android's main thread.
 */
internal class LocalMlKitWorkBudget(
    private val onChanged: (Int) -> Unit = {}
) {
    private val active = mutableSetOf<Long>()
    private var nextToken = 0L
    private var closed = false

    inner class Lease internal constructor(private val token: Long) {
        private var released = false

        fun release() {
            if (released) return
            released = true
            if (active.remove(token)) onChanged(active.size)
        }
    }

    fun acquire(): Lease? {
        if (closed || active.size >= MAX_SUBMITTED_TASKS) return null
        val token = ++nextToken
        active.add(token)
        onChanged(active.size)
        return Lease(token)
    }

    fun activeCount(): Int = active.size

    /** End screen ownership; late ML Kit callbacks cannot re-open admission. */
    fun close() {
        if (closed) return
        closed = true
        active.clear()
        onChanged(0)
    }

    companion object {
        const val MAX_SUBMITTED_TASKS = 3
    }
}
