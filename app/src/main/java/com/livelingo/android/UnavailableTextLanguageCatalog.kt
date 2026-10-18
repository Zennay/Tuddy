package com.livelingo.android

import java.util.Locale

/**
 * Non-selectable languages found in the user's archived LiveLingo v0.4
 * language requests. No claim of an installed or licensable ASR/MT model.
 * Kept entirely separate from TargetCatalog.codes, speech and ML Kit APIs.
 */
internal data class UnavailableTextLanguage(
    val code: String,
    val title: String,
    val reason: String,
    val hasFivePhrasePreview: Boolean = false,
    val aliases: String = ""
)

internal object UnavailableTextLanguageCatalog {
    val entries = listOf(
        UnavailableTextLanguage(
            "kmr", "Kurmancî · Kurmanji (Noord-Koerdisch)",
            "Geen volledig vrij gelicentieerd offline spraak- of tekstvertaalmodel. " +
                "Alleen vijf ingebouwde basiswoorden; niet voor vrije zinnen.",
            hasFivePhrasePreview = true,
            aliases = "ku kurdish koerdisch kurmanji kurmanci noordkoerdisch"
        ),
        UnavailableTextLanguage(
            "ckb", "سۆرانی · Sorani (Centraal-Koerdisch)",
            "Geen volledig vrij gelicentieerd offline spraak- of tekstvertaalmodel. " +
                "Alleen vijf ingebouwde basiswoorden; niet voor vrije zinnen.",
            hasFivePhrasePreview = true,
            aliases = "ku kurdish koerdisch sorani centraalkoerdisch"
        ),
        UnavailableTextLanguage(
            "ps", "پښتو · Pasjtoe / Pashto",
            "Geen volledig vrij gelicentieerd offline spraak- of tekstvertaalmodel. " +
                "Alleen vijf ingebouwde basiswoorden; niet voor vrije zinnen.",
            hasFivePhrasePreview = true,
            aliases = "pashto pashtu pasjtoe afghanistan پښتو"
        ),
        UnavailableTextLanguage(
            "so", "Soomaali · Somalisch",
            "Geen geverifieerd lokaal ML Kit-tekstmodel of toegelaten offline spraakmodel.",
            aliases = "somali somalisch somalia"
        ),
        UnavailableTextLanguage(
            "ti", "ትግርኛ · Tigrinya",
            "Geen geverifieerd lokaal ML Kit-tekstmodel of toegelaten offline spraakmodel.",
            aliases = "tigrinya tigrigna"
        ),
        UnavailableTextLanguage(
            "am", "አማርኛ · Amhaars",
            "Geen geverifieerd lokaal ML Kit-tekstmodel of toegelaten offline spraakmodel.",
            aliases = "amharic amhaars ethiopia"
        ),
        UnavailableTextLanguage(
            "om", "Afaan Oromoo · Oromo",
            "Geen geverifieerd lokaal ML Kit-tekstmodel of toegelaten offline spraakmodel.",
            aliases = "oromo oromoo ethiopia"
        ),
        UnavailableTextLanguage(
            "rhg", "Rohingya",
            "Geen geverifieerd lokaal ML Kit-tekstmodel of toegelaten offline spraakmodel.",
            aliases = "rohingya"
        ),
        UnavailableTextLanguage(
            "prs", "دری · Dari (Afghaans Perzisch)",
            "Dari is niet als onafhankelijk gevalideerde lokale tekst- of " +
                "spraaktaal beschikbaar. De ML Kit-code 'fa' voor Perzisch " +
                "is geen geverifieerde vervanger voor Dari.",
            aliases = "dari afghaans perzisch persian"
        )
    )

    fun search(query: String): List<UnavailableTextLanguage> {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return emptyList()
        return entries.filter {
            it.code.contains(needle) ||
                it.title.lowercase(Locale.ROOT).contains(needle) ||
                it.aliases.contains(needle)
        }
    }
}
