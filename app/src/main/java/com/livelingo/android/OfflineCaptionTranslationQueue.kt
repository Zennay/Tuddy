package com.livelingo.android

import java.util.ArrayDeque

/**
 * Explicit human correction/retry/typed caption requests may jump ahead of
 * waiting automatic live speech captions; *already submitted* Android model
 * tasks are never preempted. To prevent a busy typist from starving live
 * conversation, at most two explicit starts precede the next automatic start
 * whenever an automatic request is waiting.
 *
 * The queue retains only in-process callbacks and existing RAM caption IDs,
 * never text, models, audio, user identities, or persistent history.
 */
internal enum class CaptionTranslationPriority { AUTOMATIC, USER_REQUESTED }

internal class OfflineCaptionTranslationQueue {
    private data class Request(
        val id: Long, val revision: Int, val begin: () -> Unit,
        val onStartError: () -> Unit
    )
    private val automatic = ArrayDeque<Request>()
    private val userRequested = ArrayDeque<Request>()
    private val active = mutableMapOf<Long, Int>()
    private var consecutiveUserStarts = 0
    private var draining = false
    private var closed = false

    fun enqueue(
        id: Long,
        revision: Int,
        priority: CaptionTranslationPriority = CaptionTranslationPriority.AUTOMATIC,
        onStartError: () -> Unit,
        begin: () -> Unit
    ): Boolean {
        if (closed) return false
        cancel(id)
        if (automatic.size + userRequested.size >= CaptionRetentionPolicy.MAX_CAPTIONS) {
            return false
        }
        val request = Request(id, revision, begin, onStartError)
        when (priority) {
            CaptionTranslationPriority.AUTOMATIC -> automatic.addLast(request)
            CaptionTranslationPriority.USER_REQUESTED -> userRequested.addLast(request)
        }
        drain()
        return true
    }

    fun finished(id: Long, revision: Int) {
        if (active[id] == revision) {
            active.remove(id)
            drain()
        } else {
            val a = automatic.removeIf { it.id == id && it.revision == revision }
            val b = userRequested.removeIf { it.id == id && it.revision == revision }
            if (a || b) drain()
        }
    }

    fun cancel(id: Long) {
        val activeRemoved = active.remove(id) != null
        val autoRemoved = automatic.removeIf { it.id == id }
        val humanRemoved = userRequested.removeIf { it.id == id }
        if (activeRemoved || autoRemoved || humanRemoved) drain()
    }

    fun clear() {
        automatic.clear()
        userRequested.clear()
        active.clear()
        consecutiveUserStarts = 0
    }

    fun close() {
        closed = true
        clear()
    }

    private fun pick(): Request {
        val useHuman = userRequested.isNotEmpty() &&
            (automatic.isEmpty() || consecutiveUserStarts < MAX_CONSECUTIVE_USER_STARTS)
        return if (useHuman) {
            consecutiveUserStarts = (consecutiveUserStarts + 1)
                .coerceAtMost(MAX_CONSECUTIVE_USER_STARTS)
            userRequested.removeFirst()
        } else {
            consecutiveUserStarts = 0
            automatic.removeFirst()
        }
    }

    private fun drain() {
        if (draining || closed) return
        draining = true
        try {
            while (active.size < MAX_ACTIVE &&
                (automatic.isNotEmpty() || userRequested.isNotEmpty())) {
                val item = pick()
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
        const val MAX_CONSECUTIVE_USER_STARTS = 2
    }
}
