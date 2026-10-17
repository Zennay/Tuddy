package com.livelingo.android.model

import java.util.Locale

/**
 * Tiny, curated offline glossary based on the archived v0.4 language module.
 * An unknown word or language returns null; do not invent definitions or
 * silently send transcript text to a cloud dictionary.
 */
internal data class GlossaryEntry(val definition: String, val illustration: String)

internal object LocalGlossary {
    private val entries: Map<String, Map<String, GlossaryEntry>> = mapOf(
        "en" to mapOf(
            "hello" to GlossaryEntry("A greeting used when meeting someone.", "👋"),
            "understand" to GlossaryEntry("To grasp the meaning of something.", "💡"),
            "water" to GlossaryEntry("A clear liquid essential for life.", "💧"),
            "cat" to GlossaryEntry("A small domesticated feline animal.", "🐈"),
            "dog" to GlossaryEntry("A domesticated canine animal.", "🐕"),
            "home" to GlossaryEntry("The place where someone lives.", "🏠"),
            "food" to GlossaryEntry("Things people or animals eat.", "🍽️"),
            "book" to GlossaryEntry("Pages bound together to be read.", "📖"),
            "car" to GlossaryEntry("A road vehicle with an engine.", "🚗"),
            "love" to GlossaryEntry("Deep affection for someone or something.", "❤️")
        ),
        "de" to mapOf(
            "hallo" to GlossaryEntry("A greeting meaning hello.", "👋"),
            "verstehen" to GlossaryEntry("To understand or comprehend.", "💡"),
            "verstehst" to GlossaryEntry("Form of ‘verstehen’: you understand.", "💡"),
            "wasser" to GlossaryEntry("Water; a liquid essential for life.", "💧"),
            "katze" to GlossaryEntry("A cat, a small domesticated feline.", "🐈"),
            "hund" to GlossaryEntry("A dog, a domesticated canine.", "🐕"),
            "haus" to GlossaryEntry("A house or building used as a home.", "🏠"),
            "essen" to GlossaryEntry("To eat; also a meal or food.", "🍽️")
        ),
        "nl" to mapOf(
            "hallo" to GlossaryEntry("A greeting meaning hello.", "👋"),
            "begrijpen" to GlossaryEntry("To understand the meaning of something.", "💡"),
            "water" to GlossaryEntry("A clear liquid essential for life.", "💧"),
            "kat" to GlossaryEntry("A small domesticated feline.", "🐈"),
            "hond" to GlossaryEntry("A domesticated canine.", "🐕"),
            "huis" to GlossaryEntry("A house, where someone lives.", "🏠")
        ),
        "fr" to mapOf(
            "bonjour" to GlossaryEntry("A greeting meaning hello or good day.", "👋"),
            "eau" to GlossaryEntry("Water; a liquid essential for life.", "💧"),
            "chat" to GlossaryEntry("A cat, a small domesticated feline.", "🐈"),
            "maison" to GlossaryEntry("A house or home.", "🏠")
        ),
        "es" to mapOf(
            "hola" to GlossaryEntry("A greeting meaning hello.", "👋"),
            "agua" to GlossaryEntry("Water; a liquid essential for life.", "💧"),
            "gato" to GlossaryEntry("A cat, a small domesticated feline.", "🐈"),
            "casa" to GlossaryEntry("A house or home.", "🏠")
        )
    )

    fun lookup(languageTag: String?, word: String): GlossaryEntry? {
        val code = languageTag?.trim()?.lowercase(Locale.ROOT)
            ?.replace('_', '-')?.substringBefore('-') ?: return null
        val normalized = word.trim().trim('\'', '’', '-').lowercase(Locale.ROOT)
        return entries[code]?.get(normalized)
    }
}
