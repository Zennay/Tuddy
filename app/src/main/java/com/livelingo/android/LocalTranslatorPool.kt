package com.livelingo.android

import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * Session-scoped pool of local ML Kit translator clients.
 *
 * The caller must first confirm that the exact model pair is installed.
 * This class never downloads models or persists source/translated text.
 */
internal class LocalTranslatorPool {
    private data class Entry(
        val translator: Translator,
        var inFlight: Int = 0,
        var closeWhenIdle: Boolean = false
    )

    private companion object {
        const val MAX_CACHED_PAIRS = 4
    }

    // Access-order map: recently used language pairs stay warm; only idle
    // entries can be evicted, so an active translation is never closed early.
    private val entries = LinkedHashMap<Pair<String, String>, Entry>(8, 0.75f, true)
    private var closed = false

    fun translate(
        source: String,
        target: String,
        text: String,
        onResult: (Result<String>) -> Unit
    ) {
        if (closed) {
            onResult(Result.failure(IllegalStateException("Local translator pool is closed")))
            return
        }
        val key = source to target
        val entry = try {
            entries[key] ?: create(key).also { entries[key] = it }
        } catch (error: Exception) {
            onResult(Result.failure(error))
            return
        }
        if (entry.closeWhenIdle) {
            onResult(Result.failure(IllegalStateException("Local translator is closing")))
            return
        }

        entry.inFlight++
        try {
            entry.translator.translate(text)
                .addOnSuccessListener { output ->
                    try {
                        if (!closed) onResult(Result.success(output))
                    } finally {
                        release(entry)
                    }
                }
                .addOnFailureListener { error ->
                    try {
                        if (!closed) onResult(Result.failure(error))
                    } finally {
                        release(entry)
                    }
                }
        } catch (error: Exception) {
            release(entry)
            if (!closed) onResult(Result.failure(error))
        }
        evictIdle(protect = key)
    }

    private fun create(key: Pair<String, String>): Entry {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(key.first)
            .setTargetLanguage(key.second)
            .build()
        return Entry(Translation.getClient(options))
    }

    private fun release(entry: Entry) {
        if (entry.inFlight > 0) entry.inFlight--
        if (entry.inFlight == 0 && entry.closeWhenIdle) {
            runCatching { entry.translator.close() }
        }
        if (!closed) evictIdle()
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
