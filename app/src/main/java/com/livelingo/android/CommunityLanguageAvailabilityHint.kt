package com.livelingo.android

/**
 * Honest guidance for languages that are currently available only in the
 * separate five-phrase preview, not as free-form offline speech/translation.
 */
internal object CommunityLanguageAvailabilityHint {
    private val phraseOnlyNames = setOf(
        "kurmanji", "kurmani", "kurmanci", "kurdi", "kurdish", "koerdisch", "kurdisch",
        "sorani", "pashto", "pasjtoe", "pushto", "kmr", "ckb", "ps",
        "کوردی", "کوردیی", "کردی", "کورمانجی", "كورمانجي", "سۆرانی", "پښتو", "پشتو"
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

    /** A specific dialect can be preselected in the existing five-phrase panel. */
    fun targetForQuery(query: String): String? {
        val normalized = LanguageSearch.normalize(query)
        val words = normalized.split(Regex("[^\\p{L}\\p{N}-]+"))
            .filter { it.isNotBlank() }
        return when {
            words.any { it in setOf("kurmanji", "kurmani", "kurmanci", "kmr", "کورمانجی", "كورمانجي") } -> "kmr"
            words.any { it in setOf("sorani", "ckb", "سۆرانی", "کوردی", "کوردیی", "کردی") } ||
                normalized.contains("central kurdish") -> "ckb"
            words.any { it in setOf("pashto", "pasjtoe", "pushto", "ps", "پښتو", "پشتو") } -> "ps"
            else -> null
        }
    }
}
