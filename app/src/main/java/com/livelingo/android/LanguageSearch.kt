package com.livelingo.android

import java.text.Normalizer
import java.util.Locale

/**
 * Shared search behavior for source and target language pickers.
 * Names are discovery hints only; they never change model or speech support.
 */
internal object LanguageSearch {
    private val dutchLocale = Locale.forLanguageTag("nl")

    fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('_', '-')
            .trim()
            .replace(Regex("\\s+"), " ")

    fun matches(query: String, candidates: Iterable<String>): Boolean {
        val term = normalize(query)
        if (term.isEmpty()) return true
        return candidates.any { normalize(it).contains(term) }
    }

    fun localizedNames(languageTag: String): List<String> {
        val locale = Locale.forLanguageTag(languageTag)
        return listOf(
            locale.getDisplayLanguage(dutchLocale),
            locale.getDisplayLanguage(Locale.ENGLISH),
            locale.getDisplayLanguage(locale)
        )
    }
}
