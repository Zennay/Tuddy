package com.livelingo.android

import java.util.ArrayDeque

/**
 * Session-local fair FIFO admission for expensive Android ML Kit translation.
 * At most three completed captions can actively request translation at once.
 * Requests are not stored, persisted, sent online, or retried automatically;
 * each holds only the existing temporary row's pending launch closure.
 *
 * Every terminal outcome, row expiry, explicit correction, wipe or removal
 * releases its slot; outdated completions cannot free a newer revision.
 */
internal class OfflineCaptionTranslationQueue {
    private data class Request(
        val id: Long, val revision: Int, val begin: () -> Unit,
        val onStartError: () -> Unit
    )
    private val waiting = ArrayDeque<Request>()
    private val active = mutableMapOf<Long, Int>()
    private var draining = false
    private var closed = false

    fun enqueue(
        id: Long, revision: Int, onStartError: () -> Unit, begin: () -> Unit
    ): Boolean {
        if (closed) return false
        cancel(id)
        if (waiting.size >= CaptionRetentionPolicy.MAX_CAPTIONS) return false
        waiting.addLast(Request(id, revision, begin, onStartError))
        drain()
        return true
    }

    fun finished(id: Long, revision: Int) {
        if (active[id] == revision) {
            active.remove(id)
            drain()
        } else if (waiting.removeIf { it.id == id && it.revision == revision }) {
            drain()
        }
    }

    fun cancel(id: Long) {
        val removed = active.remove(id) != null
        val dequeued = waiting.removeIf { it.id == id }
        if (removed || dequeued) drain()
    }

    fun clear() {
        waiting.clear()
        active.clear()
    }

    fun close() {
        closed = true
        clear()
    }

    private fun drain() {
        if (draining || closed) return
        draining = true
        try {
            while (active.size < MAX_ACTIVE && waiting.isNotEmpty()) {
                val item = waiting.removeFirst()
                active[item.id] = item.revision
                try {
                    item.begin()
                } catch (_: Exception) {
                    if (active[item.id] == item.revision) active.remove(item.id)
                    item.onStartError()
                }
            }
        } finally {
            draining = false
        }
    }

    companion object {
        const val MAX_ACTIVE = 3
    }
}
