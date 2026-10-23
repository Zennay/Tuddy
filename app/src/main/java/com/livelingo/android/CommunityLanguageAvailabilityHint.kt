package com.livelingo.android

/**
 * Honest guidance for community languages with a five-phrase preview and
 * user-authored personal cards, not free-form offline speech/translation.
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
        return "Kurmanji, Sorani en Pasjtoe hebben 5 vaste basisuitdrukkingen en jouw eigen offline zinnen. " +
            "Volledige offline spraakherkenning en vrije vertaling zijn niet beschikbaar. " +
            "Kies hieronder bewust een preview of je eigen, niet-gecontroleerde gesprekskaarten."
    }

    /**
     * Preserve distinct dialects for ambiguous generic Kurdish requests.
     * Never silently guess Kurmanji or Sorani in the automatic translation picker.
     */
    fun targetsForQuery(query: String): List<String> {
        targetForQuery(query)?.let { return listOf(it) }
        val normalized = LanguageSearch.normalize(query)
        val words = normalized.split(Regex("[^\\p{L}\\p{N}-]+"))
            .filter { it.isNotBlank() }
        return if (words.any {
                it in setOf("kurdish", "koerdisch", "kurdisch", "kurdi", "کردی", "کوردی", "کوردیی")
            }) listOf("kmr", "ckb") else emptyList()
    }

    /** A specific dialect can be preselected in the existing five-phrase panel. */
    fun targetForQuery(query: String): String? {
        val normalized = LanguageSearch.normalize(query)
        val words = normalized.split(Regex("[^\\p{L}\\p{N}-]+"))
            .filter { it.isNotBlank() }
        return when {
            words.any { it in setOf("kurmanji", "kurmani", "kurmanci", "kmr", "کورمانجی", "كورمانجي") } -> "kmr"
            words.any { it in setOf("sorani", "ckb", "سۆرانی") } ||
                normalized.contains("central kurdish") -> "ckb"
            words.any { it in setOf("pashto", "pasjtoe", "pushto", "ps", "پښتو", "پشتو") } -> "ps"
            else -> null
        }
    }
}
