package com.livelingo.android

import android.content.Context

/**
 * Separately opted-in language direction shortcuts for text-only two-person
 * conversations. Never stores words, captions, microphone permissions, provider
 * readiness, model inventory or download status.
 */
internal data class TypedLanguagePair(val from: String, val to: String)

internal class PrivateTypedLanguagePairs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "tuddy-private-typed-language-pairs-v1", Context.MODE_PRIVATE
    )
    private val key = "pinned-pairs"

    private fun allowed(pair: TypedLanguagePair): Boolean =
        pair.from in TargetCatalog.codes &&
            pair.to in TargetCatalog.codes &&
            pair.from != pair.to

    @Synchronized
    fun entries(): List<TypedLanguagePair> {
        val raw = runCatching { prefs.getString(key, "") }.getOrNull().orEmpty()
        if (raw.length > 400) return emptyList()
        return raw.split(';').mapNotNull { item ->
            val fields = item.split('~')
            if (fields.size != 2) null else
                TypedLanguagePair(fields[0], fields[1]).takeIf(::allowed)
        }.distinct().take(MAX_PAIRS)
    }

    @Synchronized
    fun pin(pair: TypedLanguagePair): Boolean {
        if (!allowed(pair)) return false
        val current = entries()
        if (pair in current) return true
        if (current.size >= MAX_PAIRS) return false
        return write(current + pair)
    }

    @Synchronized
    fun remove(pair: TypedLanguagePair): Boolean {
        if (!allowed(pair)) return false
        val current = entries()
        if (pair !in current) return false
        return write(current - pair)
    }

    @Synchronized
    fun clear(): Boolean = runCatching {
        prefs.edit().remove(key).commit()
    }.getOrDefault(false)

    private fun write(pairs: List<TypedLanguagePair>): Boolean {
        if (pairs.size > MAX_PAIRS || pairs.any { !allowed(it) } ||
            pairs.distinct().size != pairs.size) return false
        return runCatching {
            prefs.edit().putString(key,
                pairs.joinToString(";") { "${it.from}~${it.to}" }).commit()
        }.getOrDefault(false)
    }

    companion object { const val MAX_PAIRS = 6 }
}
