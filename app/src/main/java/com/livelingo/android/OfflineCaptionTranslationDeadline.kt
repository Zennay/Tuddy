package com.livelingo.android

import android.os.Handler
import android.os.Looper

/**
 * A user-visible pending-caption deadline, not a model/AI timeout guarantee.
 *
 * Language detection, read-only installed-model lookup and local ML Kit
 * translation can all complete asynchronously. Android providers may also
 * fail to return a callback. Leave no caption permanently in "Bezig":
 * the owner will mark an unchanged row retryable and increment its revision,
 * so late callbacks can never silently replace newer user edits.
 *
 * Does not speak, send, store or cancel provider tasks. Timers only exist
 * while their source rows live in the temporary conversation.
 */
internal class OfflineCaptionTranslationDeadline {
    private val handler = Handler(Looper.getMainLooper())
    private data class Pending(val revision: Int, val timeout: Runnable)
    private val pending = mutableMapOf<Long, Pending>()

    fun watch(id: Long, revision: Int, onTimeout: () -> Unit) {
        cancel(id)
        val task = Runnable {
            val entry = pending[id] ?: return@Runnable
            if (entry.revision != revision) return@Runnable
            pending.remove(id)
            onTimeout()
        }
        pending[id] = Pending(revision, task)
        handler.postDelayed(task, MAX_PENDING_MS)
    }

    /** An old callback cannot cancel a newer attempt's timeout. */
    fun finished(id: Long, revision: Int) {
        if (pending[id]?.revision == revision) cancel(id)
    }

    fun cancel(id: Long) {
        pending.remove(id)?.let { handler.removeCallbacks(it.timeout) }
    }

    fun clear() {
        pending.values.forEach { handler.removeCallbacks(it.timeout) }
        pending.clear()
    }

    companion object {
        const val MAX_PENDING_MS = 30_000L
    }
}
