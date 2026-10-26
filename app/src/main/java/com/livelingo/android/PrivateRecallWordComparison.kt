package com.livelingo.android

import java.text.Normalizer
import java.util.Locale

/**
 * Explicit, RAM-only *surface-form* comparison for bilingual practice.
 *
 * This is not language assessment: no ML model, edit distance grade, saved
 * learner answer, network, translation, phonetic/grammar claim, or feedback
 * based on any corpus. Do not call it until the user reveals the reference
 * and asks to compare their own text.
 */
internal object PrivateRecallWordComparison {
    private val words = Regex("[\\p{L}\\p{M}\\p{N}]+(?:['’-][\\p{L}\\p{M}\\p{N}]+)*")
    private const val MAX_ATTEMPT_UNITS = 400
    private const val MAX_REFERENCE_UNITS = 800
    private const val MAX_WORDS = 80

    internal data class SurfaceComparison(
        val sharedInOrder: List<String>,
        val onlyInReference: List<String>,
        val onlyInAttempt: List<String>,
        val referenceWords: Int,
        val attemptWords: Int
    )

    private data class Token(val shown: String, val normalized: String)

    private fun extract(text: String, maxLength: Int): List<Token>? {
        if (text.isBlank() || text.length > maxLength ||
            text.any { it == '\u0000' || (it.isISOControl() && it != '\n') }) return null
        val tokens = words.findAll(text).take(MAX_WORDS + 1).map { match ->
            Token(match.value, Normalizer.normalize(match.value, Normalizer.Form.NFC)
                .lowercase(Locale.ROOT))
        }.toList()
        return tokens.takeIf { it.isNotEmpty() && it.size <= MAX_WORDS }
    }

    /**
     * Bounded longest-common-subsequence alignment: repeated words and word
     * order matter, unlike loose set overlap. Deliberately ignores casing,
     * but NOT diacritics or word order. Punctuation is not a grammar signal.
     */
    fun compare(attempt: String, reference: String): SurfaceComparison? {
        val a = extract(attempt, MAX_ATTEMPT_UNITS) ?: return null
        val b = extract(reference, MAX_REFERENCE_UNITS) ?: return null
        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
            dp[i][j] = if (a[i].normalized == b[j].normalized)
                dp[i + 1][j + 1] + 1
            else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
        val matched = mutableListOf<String>()
        val missed = mutableListOf<String>()
        val extra = mutableListOf<String>()
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            when {
                a[i].normalized == b[j].normalized -> {
                    matched.add(b[j].shown)
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> {
                    extra.add(a[i].shown)
                    i++
                }
                else -> {
                    missed.add(b[j].shown)
                    j++
                }
            }
        }
        while (i < a.size) extra.add(a[i++].shown)
        while (j < b.size) missed.add(b[j++].shown)
        return SurfaceComparison(matched, missed, extra, b.size, a.size)
    }
}
