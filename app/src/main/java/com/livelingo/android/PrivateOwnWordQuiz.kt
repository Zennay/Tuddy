package com.livelingo.android

import java.util.Locale
import kotlin.random.Random

/** A private multiple-choice round over meanings that the user wrote themselves. */
internal data class OwnWordQuizCard(
    val entry: OwnWord,
    val choices: List<String>,
    val correctAnswer: String,
    val reverse: Boolean
)

/**
 * No model, recognition, network, progress database or automatic evaluation
 * of the *meaning* is involved. Matching means only matching the user's
 * previously stored pair, not that its translation is linguistically correct.
 */
internal object PrivateOwnWordQuiz {
    private const val MAX_QUESTIONS = 20
    private const val MAX_CHOICES = 4

    private fun normalized(text: String): String = text.lowercase(Locale.ROOT)

    fun create(
        pool: List<OwnWord>,
        focus: List<OwnWord> = pool,
        reverse: Boolean = false,
        seed: Int = 0
    ): List<OwnWordQuizCard> {
        if (pool.size > PrivateOwnWordDeck.MAX_ENTRIES || focus.size > PrivateOwnWordDeck.MAX_ENTRIES)
            return emptyList()
        val random = Random(seed)
        val available = pool.groupBy { it.language }.mapValues { (_, words) ->
            // When two words share the same user-written meaning, asking
            // which one is "correct" would be ambiguous in reverse mode.
            // Exclude those pairs in BOTH directions instead of marking an
            // equally valid answer wrong.
            val meanings = words.groupingBy { normalized(it.ownMeaning) }.eachCount()
            words.filter { meanings[normalized(it.ownMeaning)] == 1 }
                .takeIf { it.size >= 2 }.orEmpty()
        }
        val allowed = available.values.flatten().toSet()
        return focus.distinct().filter { it in allowed }.shuffled(random)
            .take(MAX_QUESTIONS).mapNotNull { question ->
                val candidates = available[question.language].orEmpty()
                if (candidates.size < 2) return@mapNotNull null
                fun answer(item: OwnWord) = if (reverse) item.word else item.ownMeaning
                val correct = answer(question)
                val alternatives = candidates.asSequence()
                    .filter { it != question }
                    .map(::answer)
                    .distinctBy(::normalized)
                    .shuffled(random)
                    .take(MAX_CHOICES - 1)
                    .toList()
                if (alternatives.isEmpty()) return@mapNotNull null
                val choices = (listOf(correct) + alternatives).shuffled(random)
                OwnWordQuizCard(question, choices, correct, reverse)
            }
    }
}
