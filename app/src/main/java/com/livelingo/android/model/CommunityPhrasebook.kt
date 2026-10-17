package com.livelingo.android.model

import java.util.Locale

/**
 * Deliberately small, audited-on-demand beginner vocabulary (not a translation model).
 * A phrase is only returned if we have an exact entry; no guessing, combining words,
 * approximate matching, network requests or misleading full-sentence translations.
 * Kurdish variants are NEVER collapsed into one ambiguous "ku" translation.
 */
object CommunityPhrasebook {
    private data class Entry(val english: List<String>, val dutch: List<String>,
                             val kurmanji: String, val sorani: String, val pashto: String)

    // Translation vocabulary sourced from Yari Kurmanji guide, Wiktionary Sorani,
    // Sorani phrase guide and learnpashtoo.com; independent native-speaker review
    // is still mandatory before use in high-stakes communication.
    private val entries = listOf(
        Entry(listOf("hello", "hi"), listOf("hallo", "hoi"), "Silav", "سڵاو", "سلام"),
        Entry(listOf("thank you", "thanks"), listOf("dank je", "bedankt", "dank u"), "Spas", "سوپاس", "مننه"),
        Entry(listOf("yes"), listOf("ja"), "Erê", "بەڵێ", "هو"),
        Entry(listOf("no"), listOf("nee"), "Na", "نەخێر", "نه"),
        Entry(listOf("water"), listOf("water"), "Av", "ئاو", "اوبه"),
    )

    val enabledTargets = setOf("kmr", "ckb", "ps")
    /** Canonical text IDs for future NLLB-style downloadable local model packs. */
    val nllbTags = mapOf("kmr" to "kmr_Latn", "ckb" to "ckb_Arab", "ps" to "pbt_Arab")

    private fun norm(value: String) = value.trim().lowercase(Locale.ROOT)
        .replace(Regex("[.!?؟،,;:]+$"), "").trim().replace(Regex("\\s+"), " ")

    fun translate(source: String?, target: String, phrase: String): Result<String> {
        if (target !in enabledTargets) return Result.failure(IllegalArgumentException("Not a phrasebook target"))
        if (source !in setOf("en", "nl")) return Result.failure(UnsupportedOperationException(
            "For $target, offline beginner words currently require recognized English or Dutch. Other speech and sentences need a new local model."))
        val normalized = norm(phrase)
        val entry = entries.firstOrNull { item ->
            (if (source == "en") item.english else item.dutch).any { norm(it) == normalized }
        } ?: return Result.failure(UnsupportedOperationException(
            "This phrase is not in the small $target offline beginner vocabulary. No translation invented."))
        return Result.success(when (target) {
            "kmr" -> entry.kurmanji
            "ckb" -> entry.sorani
            else -> entry.pashto
        })
    }
}
