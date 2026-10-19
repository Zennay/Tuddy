package com.livelingo.android

/**
 * Temporary fill-the-gap prompt from one human-authored phrase.
 * No scoring, translation, saved attempt, network request or inferred meaning.
 * Keeps original punctuation/RTL script outside the hidden token unchanged.
 */
internal data class PrivatePhraseGap(
    val hiddenPhrase: String,
    val missingToken: String
) {
    companion object {
        fun from(note: PersonalPhrase, variation: Int = 0): PrivatePhraseGap? {
            if (variation < 0) return null
            val source = note.phrase
            if (source.length !in 3..120) return null
            val words = Regex("\\S+").findAll(source)
                .filter { match -> match.value.any { it.isLetterOrDigit() } }
                .toList()
            // A single word has no usable sentence context: keep normal flashcards.
            if (words.size < 2 || words.size > 45) return null
            // Prefer the interior where possible, rotating gaps on repetitions.
            val choices = if (words.size > 2) words.drop(1).dropLast(1) else words
            val chosen = choices[variation % choices.size]
            val obscured = source.substring(0, chosen.range.first) +
                "＿＿＿＿" + source.substring(chosen.range.last + 1)
            return PrivatePhraseGap(obscured, chosen.value)
        }
    }
}
