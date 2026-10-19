package com.livelingo.android

/**
 * Temporary, user-authored flashcard round. No text is translated, verified,
 * sent to a model or saved as a score. There is at most one retry per card.
 */
internal data class PersonalPhrasePracticeRound(
    val remaining: List<PersonalPhrase>,
    val total: Int,
    val revealed: Boolean = false,
    val attempts: Int = 0,
    val recalled: Int = 0,
    val needsPractice: Int = 0,
    val repeated: Set<String> = emptySet(),
    // Session-only original notes first marked difficult. Never persisted or
    // inferred by an automatic language model; the user can explicitly retry.
    val missed: List<PersonalPhrase> = emptyList()
) {
    val current: PersonalPhrase? get() = remaining.firstOrNull()
    val completed: Boolean get() = remaining.isEmpty()

    fun reveal(): PersonalPhrasePracticeRound =
        if (completed) this else copy(revealed = true)

    fun grade(remembered: Boolean): PersonalPhrasePracticeRound {
        val item = current ?: return this
        if (!revealed) return this
        val key = item.language + "|" + item.phrase
        val rest = remaining.drop(1)
        if (remembered) return copy(
            remaining = rest, revealed = false, attempts = attempts + 1,
            recalled = recalled + 1
        )
        val retry = key !in repeated
        val firstMiss = missed.none { it.language == item.language && it.phrase == item.phrase }
        return copy(
            remaining = if (retry) rest + item else rest,
            revealed = false,
            attempts = attempts + 1,
            needsPractice = needsPractice + if (retry) 0 else 1,
            repeated = repeated + key,
            missed = if (firstMiss) missed + item else missed
        )
    }

    companion object {
        const val BATCH_SIZE = 12

        fun start(items: List<PersonalPhrase>, position: Int = 0): PersonalPhrasePracticeRound {
            if (items.isEmpty()) return PersonalPhrasePracticeRound(emptyList(), 0)
            val offset = position.coerceAtLeast(0) % items.size
            val chosen = (0 until minOf(BATCH_SIZE, items.size))
                .map { items[(offset + it) % items.size] }
            return PersonalPhrasePracticeRound(chosen, chosen.size)
        }
    }
}
