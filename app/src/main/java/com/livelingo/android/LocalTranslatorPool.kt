package com.livelingo.android

import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * Session-scoped pool of local ML Kit translator clients.
 *
 * Finalized translations must pass the exact-pair model inventory first.
 * Provisional captions may attempt a no-download translation and fail closed
 * when a model is absent. This class never downloads models or persists text.
 */
internal class LocalTranslatorPool {
    internal class Entry(
        val translator: Translator,
        var inFlight: Int = 0,
        var closeWhenIdle: Boolean = false
    )

    internal class Lease internal constructor(internal val entry: Entry) {
        val translator: Translator get() = entry.translator
    }

    private companion object {
        const val MAX_CACHED_PAIRS = 4
    }

    // Access-order map: recently used language pairs stay warm; only idle
    // entries can be evicted, so an active translation is never closed early.
    private val entries = LinkedHashMap<Pair<String, String>, Entry>(8, 0.75f, true)
    private var closed = false

    fun acquire(source: String, target: String): Lease {
        check(!closed) { "Local translator pool is closed" }
        val key = source to target
        val entry = entries[key] ?: create(key).also { entries[key] = it }
        check(!entry.closeWhenIdle) { "Local translator is closing" }
        entry.inFlight++
        evictIdle(protect = key)
        return Lease(entry)
    }

    fun release(lease: Lease) {
        val entry = lease.entry
        if (entry.inFlight > 0) entry.inFlight--
        if (entry.inFlight == 0 && entry.closeWhenIdle) {
            runCatching { entry.translator.close() }
        }
        if (!closed) evictIdle()
    }

    private fun create(key: Pair<String, String>): Entry {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(key.first)
            .setTargetLanguage(key.second)
            .build()
        return Entry(Translation.getClient(options))
    }

    private fun evictIdle(protect: Pair<String, String>? = null) {
        while (entries.size > MAX_CACHED_PAIRS) {
            val oldest = entries.entries.firstOrNull { (key, value) ->
                key != protect && value.inFlight == 0 && !value.closeWhenIdle
            } ?: return
            entries.remove(oldest.key)
            runCatching { oldest.value.translator.close() }
        }
    }

    /** Clear clients after model inventory changes without abandoning live callbacks. */
    fun invalidate() {
        val staleEntries = entries.values.toList()
        entries.clear()
        staleEntries.forEach { entry ->
            if (entry.inFlight == 0) runCatching { entry.translator.close() }
            else entry.closeWhenIdle = true
        }
    }

    fun close() {
        if (closed) return
        closed = true
        invalidate()
    }
}
