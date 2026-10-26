package com.livelingo.android

/**
 * Local, RAM-only admission state for one user-requested Android TTS sequence.
 *
 * An Android speech engine may deliver delayed or repeated onStart/onRangeStart,
 * onDone and onError callbacks. Match the full engine utterance ID, not the
 * trailing number alone. A completed earlier line must never stop or update a
 * later line, and only one terminal callback may advance each line.
 *
 * Calls originate on Android's main thread; no text, audio or device metadata
 * is retained here. A new sequence owns a new gate.
 */
internal class OfflineSpeechSequenceTurnGate(
    private val generation: Long,
    private val total: Int
) {
    private var activeIndex = -1
    private var completed = false
    private var terminal = false
    private var lastRangeEnd = 0
    private var started = false

    fun queueNext(index: Int): Boolean {
        if (total !in 1..100 || completed || terminal ||
            index != activeIndex + 1 || index !in 0 until total) return false
        activeIndex = index
        started = false
        lastRangeEnd = 0
        return true
    }

    fun activeId(index: Int): String = "tuddy-offline-sequence-$generation-$index"

    private fun matches(id: String?): Boolean =
        !completed && !terminal && activeIndex in 0 until total &&
            id == activeId(activeIndex)

    /** Calls UI only on a real callback from the currently queued line. */
    fun started(id: String?): Int? {
        if (!matches(id) || started) return null
        started = true
        return activeIndex
    }

    /** Advance accessible word progress monotonically for the active line. */
    fun range(id: String?, start: Int, end: Int, length: Int): Pair<Int, Int>? {
        if (!matches(id) || start < 0 || end <= start || end > length ||
            start < lastRangeEnd) return null
        lastRangeEnd = end
        return start to end
    }

    /** Only the first exact current-ID completion can finish this turn. */
    fun completed(id: String?): Int? {
        if (!matches(id)) return null
        terminal = true
        val finished = activeIndex
        if (finished + 1 == total) completed = true
        return finished
    }

    /** Explicit transition after an admitted completion, never before. */
    fun readyForNext(): Boolean {
        if (!terminal || completed) return false
        terminal = false
        return true
    }

    /** Stale errors from earlier lines cannot abort the currently queued line. */
    fun failed(id: String?): Boolean {
        if (!matches(id)) return false
        terminal = true
        completed = true
        return true
    }
}
