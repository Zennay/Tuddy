package com.livelingo.android

/**
 * Honest guidance for languages that are currently available only in the
 * separate five-phrase preview, not as free-form offline speech/translation.
 */
internal object CommunityLanguageAvailabilityHint {
    private val phraseOnlyNames = setOf(
        "kurmanji", "kurmani", "kurmanci", "kurdish", "koerdisch", "kurdisch",
        "sorani", "pashto", "pasjtoe", "pushto", "kmr", "ckb", "ps"
    )
    private val phraseOnlyPhrases = setOf("northern kurdish", "central kurdish")

    fun forQuery(query: String): String? {
        val normalized = LanguageSearch.normalize(query)
        if (normalized.isBlank()) return null
        val words = normalized.split(Regex("[^\\p{L}\\p{N}-]+"))
            .filter { it.isNotBlank() }
        val isPhraseOnly = words.any { it in phraseOnlyNames } ||
            phraseOnlyPhrases.any { normalized.contains(it) }
        if (!isPhraseOnly) return null
        return "Kurmanji, Sorani en Pasjtoe zijn nu alleen beschikbaar als vijf-woordenpreview. " +
            "Volledige offline spraakherkenning en vrije vertaling zijn nog niet beschikbaar."
    }
}
