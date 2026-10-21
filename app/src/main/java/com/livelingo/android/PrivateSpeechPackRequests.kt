package com.livelingo.android

import android.content.Context

/**
 * A locally remembered ATTEMPT, never proof that Android downloaded a model.
 * Stores only a selected locale and a timestamp, not user speech, transcripts,
 * recognizer responses or downloaded model data.
 */
internal class PrivateSpeechPackRequests(
    context: Context,
    private val allowed: Set<String>
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "tuddy_speech_model_requests_v1", Context.MODE_PRIVATE
    )

    private companion object {
        const val KEY = "requested_locales_v1"
        const val MAX_ROWS = 24
    }

    @Synchronized
    fun entries(): Map<String, Long> {
        val rows = runCatching {
            prefs.getStringSet(KEY, emptySet())?.toSet()
        }.getOrNull() ?: return emptyMap()
        if (rows.size > MAX_ROWS) return emptyMap()
        val parsed = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()
        for (row in rows) {
            val separator = row.lastIndexOf('|')
            if (separator <= 0) return emptyMap()
            val tag = row.substring(0, separator)
            val timestamp = row.substring(separator + 1).toLongOrNull()
                ?: return emptyMap()
            if (tag !in allowed || tag in parsed ||
                timestamp <= 0 || timestamp > now + 60_000L) return emptyMap()
            parsed[tag] = timestamp
        }
        return parsed
    }

    @Synchronized
    fun recordAttempt(tag: String): Boolean {
        if (tag !in allowed) return false
        val updated = entries().toMutableMap()
        if (updated.size >= MAX_ROWS && tag !in updated) return false
        updated[tag] = System.currentTimeMillis()
        return write(updated)
    }

    @Synchronized
    fun forget(tag: String): Boolean {
        if (tag !in allowed) return false
        val current = entries()
        if (tag !in current) return true
        return write(current - tag)
    }

    @Synchronized
    fun clear(): Boolean = runCatching {
        prefs.edit().remove(KEY).commit()
    }.getOrDefault(false)

    private fun write(items: Map<String, Long>): Boolean = runCatching {
        if (items.size > MAX_ROWS || items.keys.any { it !in allowed })
            return@runCatching false
        prefs.edit().putStringSet(
            KEY, items.map { (tag, timestamp) -> "$tag|$timestamp" }.toSet()
        ).commit()
    }.getOrDefault(false)
}
