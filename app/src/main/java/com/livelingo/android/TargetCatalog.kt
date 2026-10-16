package com.livelingo.android

import java.util.Locale

/**
 * Text translation targets explicitly supported by ML Kit v17.
 * Availability of offline *speech recognition* is independent of this list.
 * Kurdish Kurmanji, Sorani and Pashto are intentionally absent until a
 * separate licensed, device-tested speech and translation engine exists.
 */
data class TextTarget(val code: String, val title: String)

object TargetCatalog {
    val codes = listOf(
        "af", "ar", "be", "bg", "bn", "ca", "cs", "cy", "da", "de",
        "el", "en", "eo", "es", "et", "fa", "fi", "fr", "ga", "gl",
        "gu", "he", "hi", "hr", "ht", "hu", "id", "is", "it", "ja",
        "ka", "kn", "ko", "lt", "lv", "mk", "mr", "ms", "mt", "nl",
        "no", "pl", "pt", "ro", "ru", "sk", "sl", "sq", "sv", "sw",
        "ta", "te", "th", "tl", "tr", "uk", "ur", "vi", "zh"
    )
    private val preferred = listOf("nl", "ar", "en", "fa", "uk", "tr", "ru", "fr", "de", "ur", "es")
    private val names = mapOf(
        "ar" to "العربية · Arabic", "fa" to "فارسی · Farsi",
        "uk" to "Українська · Ukrainian", "ur" to "اردو · Urdu",
        "nl" to "Nederlands · Dutch", "en" to "English",
        "de" to "Deutsch · German", "fr" to "Français · French",
        "es" to "Español · Spanish", "ru" to "Русский · Russian",
        "tr" to "Türkçe · Turkish", "zh" to "中文 · Chinese",
        "bn" to "বাংলা · Bengali", "hi" to "हिन्दी · Hindi"
    )
    private val aliases = mapOf(
        "ar" to "arabisch syrie syrië palestina palestine",
        "fa" to "perzisch persian iran afghanistan",
        "uk" to "oekraïens oekraïne",
        "nl" to "hollands nederlands",
        "ur" to "pakistan",
        "tr" to "turks",
        "ru" to "russisch"
    )
    val targets: List<TextTarget> = codes.map { code ->
        val tag = when (code) {
            "no" -> "nb"
            "tl" -> "fil"
            else -> code
        }
        val locale = Locale.forLanguageTag(tag)
        val label = names[code]
            ?: "${locale.getDisplayLanguage(locale).ifBlank { code }} · ${locale.getDisplayLanguage(Locale.ENGLISH)}"
        TextTarget(code, label)
    }.sortedWith(compareBy<TextTarget> { preferred.indexOf(it.code).let { idx -> if (idx < 0) 100 else idx } }
        .thenBy { it.title })

    fun search(query: String): List<TextTarget> {
        val term = query.trim().lowercase(Locale.ROOT)
        if (term.isEmpty()) return targets
        return targets.filter {
            it.code.contains(term) || it.title.lowercase(Locale.ROOT).contains(term) ||
                aliases[it.code]?.contains(term) == true
        }
    }
}
