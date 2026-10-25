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
internal class LocalMlKitWorkBudget private constructor() {
    // Process-scoped: an Activity can be recreated while previously submitted
    // Android Tasks are still active. Recreating a screen must not reset the
    // true number of device tasks admitted by Tuddy.
    private val observers = mutableSetOf<(Int) -> Unit>()
    fun subscribe(listener: (Int) -> Unit): () -> Unit {
        observers.add(listener)
        listener(active.size)
        return { observers.remove(listener) }
    }

    private fun publish() {
        val count = active.size
        observers.toList().forEach { it(count) }
    }
    private val active = mutableSetOf<Long>()
    private var nextToken = 0L
 
    inner class Lease internal constructor(private val token: Long) {
        private var released = false

        fun release() {
            if (released) return
            released = true
            if (active.remove(token)) publish()
        }
    }

    fun acquire(): Lease? {
        if (active.size >= MAX_SUBMITTED_TASKS) return null
        val token = ++nextToken
        active.add(token)
        publish()
        return Lease(token)
    }

    fun activeCount(): Int = active.size

    // Intentionally no screen-level close(): only the real ML Kit Task
    // callback releases a slot. Android process death ends this memory-only
    // accounting and the old process's work together.

    companion object {
        const val MAX_SUBMITTED_TASKS = 3
        val shared = LocalMlKitWorkBudget()
    }
}
