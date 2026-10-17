package com.livelingo.android

/**
 * Accept callbacks only from the most recently started on-device recognizer.
 * A new session must invalidate the previous session even when Android ended
 * speech on its own (without an explicit user stop).
 */
internal class VoiceSessionGeneration {
    private var epoch = 0L

    @Synchronized
    fun begin(): Long = advance()

    @Synchronized
    fun invalidate() { advance() }

    @Synchronized
    fun isCurrent(token: Long): Boolean = token > 0 && token == epoch

    private fun advance(): Long {
        check(epoch != Long.MAX_VALUE) { "Speech session generation exhausted" }
        epoch += 1
        return epoch
    }
}
