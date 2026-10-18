package com.livelingo.android

/**
 * One bounded, RAM-only practice round over the user's opt-in curated words.
 * Self-assessments do not persist, change glossary definitions, or claim
 * measured language ability. Hard words reappear at most twice per round.
 */
internal data class WordPracticeCard(
    val identity: String,
    val language: String,
    val word: String,
    val definition: String,
    val illustration: String
)

internal data class WordPracticeRound(
    val pending: List<WordPracticeCard>,
    val initialCount: Int,
    val revealed: Boolean = false,
    val attempts: Int = 0,
    val recalled: Int = 0,
    val needsReview: Int = 0,
    val repeatAttempts: Int = 0,
    val retries: Map<String, Int> = emptyMap()
) {
    val current: WordPracticeCard? get() = pending.firstOrNull()
    val complete: Boolean get() = pending.isEmpty()
    val finishedCards: Int get() = recalled + needsReview

    fun reveal(): WordPracticeRound = if (current == null) this else copy(revealed = true)

    fun rate(remembered: Boolean): WordPracticeRound {
        val card = current ?: return this
        if (!revealed) return this
        val rest = pending.drop(1)
        if (remembered) {
            return copy(
                pending = rest, revealed = false, attempts = attempts + 1,
                recalled = recalled + 1
            )
        }
        val previous = retries[card.identity] ?: 0
        val canRepeat = previous < 2
        return copy(
            pending = if (canRepeat) rest + card else rest,
            revealed = false,
            attempts = attempts + 1,
            needsReview = needsReview + if (canRepeat) 0 else 1,
            repeatAttempts = repeatAttempts + 1,
            retries = retries + (card.identity to (previous + 1))
        )
    }

    companion object {
        const val MAX_CARDS = 20

        fun start(words: List<SavedWord>, firstIndex: Int = 0): WordPracticeRound {
            if (words.isEmpty()) return WordPracticeRound(emptyList(), 0)
            val offset = firstIndex.coerceAtLeast(0) % words.size
            val count = minOf(MAX_CARDS, words.size)
            val selected = (0 until count).map { words[(offset + it) % words.size] }
            val cards = selected.map { word ->
                WordPracticeCard(
                    identity = "${word.language}|${word.word}",
                    language = word.language,
                    word = word.word,
                    definition = word.entry.definition,
                    illustration = word.entry.illustration
                )
            }
            return WordPracticeRound(cards, cards.size)
        }
    }
}
