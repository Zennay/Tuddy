package com.livelingo.android

import android.content.Context

/**
 * A tiny, allowlisted list of manually pinned picker identifiers.
 * Audio, transcripts, translation results, recognition availability and
 * downloaded-model state are NEVER stored.
 */
internal data class FavoriteLanguagePair(val speechTag: String, val targetCode: String)

internal class PrivateLanguagePairFavorites(context: Context) {
    private val prefs = context.getSharedPreferences("tuddy-private-language-pairs", Context.MODE_PRIVATE)

    fun entries(supportedSpeechTags: Collection<String>): List<FavoriteLanguagePair> {
        val targetCodes = TargetCatalog.codes.toSet()
        val knownSources = supportedSpeechTags.toSet()
        val raw = runCatching { prefs.getString("favorites-v1", "") }.getOrNull().orEmpty()
        if (raw.length > 600) return emptyList()
        return raw.split(';').mapNotNull { record ->
            val pieces = record.split('~')
            if (pieces.size != 2 || pieces[0] !in knownSources ||
                pieces[1] !in targetCodes) null
            else FavoriteLanguagePair(pieces[0], pieces[1])
        }.distinct().take(MAX_PAIRS)
    }

    fun save(
        pair: FavoriteLanguagePair,
        supportedSpeechTags: Collection<String>
    ): Boolean {
        if (pair.speechTag !in supportedSpeechTags || pair.targetCode !in TargetCatalog.codes)
            return false
        val current = entries(supportedSpeechTags)
        if (pair in current) return true
        if (current.size >= MAX_PAIRS) return false
        return write(current + pair)
    }

    fun remove(
        pair: FavoriteLanguagePair,
        supportedSpeechTags: Collection<String>
    ): Boolean = write(entries(supportedSpeechTags).filterNot { it == pair })

    fun clear(): Boolean = runCatching {
        prefs.edit().remove("favorites-v1").commit()
    }.getOrDefault(false)

    private fun write(list: List<FavoriteLanguagePair>): Boolean = runCatching {
        prefs.edit().putString("favorites-v1",
            list.joinToString(";") { "${it.speechTag}~${it.targetCode}" }).commit()
    }.getOrDefault(false)

    companion object { const val MAX_PAIRS = 3 }
}
