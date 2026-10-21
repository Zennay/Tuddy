package com.livelingo.android.model

/** A target translation language supported by Google ML Kit on-device models.
 * Speech recognition support is *independent* and depends on the Android speech service.
 */
data class SpokenLanguage(val code: String, val name: String, val speechTag: String)
data class RequestedLanguage(val code: String, val name: String)

object Languages {
    /** Complete ML Kit on-device translation catalog (59 languages). */
    val choices: List<SpokenLanguage> = listOf(
        SpokenLanguage("af", "Afrikaans", "af-ZA"),
        SpokenLanguage("ar", "العربية · Arabic", "ar-SY"),
        SpokenLanguage("be", "Беларуская · Belarusian", "be-BY"),
        SpokenLanguage("bg", "Български · Bulgarian", "bg-BG"),
        SpokenLanguage("bn", "বাংলা · Bengali", "bn-BD"),
        SpokenLanguage("ca", "Català · Catalan", "ca-ES"),
        SpokenLanguage("cs", "Čeština · Czech", "cs-CZ"),
        SpokenLanguage("cy", "Cymraeg · Welsh", "cy-GB"),
        SpokenLanguage("da", "Dansk · Danish", "da-DK"),
        SpokenLanguage("de", "Deutsch · German", "de-DE"),
        SpokenLanguage("el", "Ελληνικά · Greek", "el-GR"),
        SpokenLanguage("en", "English", "en-US"),
        SpokenLanguage("eo", "Esperanto", "eo"),
        SpokenLanguage("es", "Español · Spanish", "es-ES"),
        SpokenLanguage("et", "Eesti · Estonian", "et-EE"),
        SpokenLanguage("fa", "فارسی · Persian / Farsi", "fa-IR"),
        SpokenLanguage("fi", "Suomi · Finnish", "fi-FI"),
        SpokenLanguage("fr", "Français · French", "fr-FR"),
        SpokenLanguage("ga", "Gaeilge · Irish", "ga-IE"),
        SpokenLanguage("gl", "Galego · Galician", "gl-ES"),
        SpokenLanguage("gu", "ગુજરાતી · Gujarati", "gu-IN"),
        SpokenLanguage("he", "עברית · Hebrew", "he-IL"),
        SpokenLanguage("hi", "हिन्दी · Hindi", "hi-IN"),
        SpokenLanguage("hr", "Hrvatski · Croatian", "hr-HR"),
        SpokenLanguage("ht", "Kreyòl ayisyen · Haitian Creole", "ht-HT"),
        SpokenLanguage("hu", "Magyar · Hungarian", "hu-HU"),
        SpokenLanguage("id", "Bahasa Indonesia", "id-ID"),
        SpokenLanguage("is", "Íslenska · Icelandic", "is-IS"),
        SpokenLanguage("it", "Italiano · Italian", "it-IT"),
        SpokenLanguage("ja", "日本語 · Japanese", "ja-JP"),
        SpokenLanguage("ka", "ქართული · Georgian", "ka-GE"),
        SpokenLanguage("kn", "ಕನ್ನಡ · Kannada", "kn-IN"),
        SpokenLanguage("ko", "한국어 · Korean", "ko-KR"),
        SpokenLanguage("lt", "Lietuvių · Lithuanian", "lt-LT"),
        SpokenLanguage("lv", "Latviešu · Latvian", "lv-LV"),
        SpokenLanguage("mk", "Македонски · Macedonian", "mk-MK"),
        SpokenLanguage("mr", "मराठी · Marathi", "mr-IN"),
        SpokenLanguage("ms", "Bahasa Melayu · Malay", "ms-MY"),
        SpokenLanguage("mt", "Malti · Maltese", "mt-MT"),
        SpokenLanguage("nl", "Nederlands · Dutch", "nl-NL"),
        SpokenLanguage("no", "Norsk · Norwegian", "nb-NO"),
        SpokenLanguage("pl", "Polski · Polish", "pl-PL"),
        SpokenLanguage("pt", "Português · Portuguese", "pt-PT"),
        SpokenLanguage("ro", "Română · Romanian", "ro-RO"),
        SpokenLanguage("ru", "Русский · Russian", "ru-RU"),
        SpokenLanguage("sk", "Slovenčina · Slovak", "sk-SK"),
        SpokenLanguage("sl", "Slovenščina · Slovenian", "sl-SI"),
        SpokenLanguage("sq", "Shqip · Albanian", "sq-AL"),
        SpokenLanguage("sv", "Svenska · Swedish", "sv-SE"),
        SpokenLanguage("sw", "Kiswahili · Swahili", "sw-KE"),
        SpokenLanguage("ta", "தமிழ் · Tamil", "ta-IN"),
        SpokenLanguage("te", "తెలుగు · Telugu", "te-IN"),
        SpokenLanguage("th", "ไทย · Thai", "th-TH"),
        SpokenLanguage("tl", "Filipino / Tagalog", "fil-PH"),
        SpokenLanguage("tr", "Türkçe · Turkish", "tr-TR"),
        SpokenLanguage("uk", "Українська · Ukrainian", "uk-UA"),
        SpokenLanguage("ur", "اردو · Urdu", "ur-PK"),
        SpokenLanguage("vi", "Tiếng Việt · Vietnamese", "vi-VN"),
        SpokenLanguage("zh", "中文 · Chinese", "zh-CN"),
    )
    /** High-frequency language families for onboarding and local speech switching.
     * Language packs need to exist in the Android speech recognizer.
     */
    val preferred = listOf("nl", "ar", "en", "fa", "uk", "tr", "ru", "fr", "de", "ur", "es")
    val voiceHints = listOf("nl-NL", "ar-SY", "ar-PS", "en-US", "fa-IR", "uk-UA", "tr-TR", "ru-RU", "fr-FR", "de-DE", "ur-PK", "es-ES")
    /** Small, bundled phrasebook previews only: NOT full sentence translation or offline ASR.
     * These cannot be passed to ML Kit's TranslateLanguage or used as recognized speech tags.
     */
    val communityPhrasebooks: List<SpokenLanguage> = listOf(
        SpokenLanguage("kmr", "Kurmancî · Kurdish (Kurmanji) · basiswoorden", "ku"),
        SpokenLanguage("ckb", "سۆرانی · Kurdish (Sorani) · basiswoorden", "ckb"),
        SpokenLanguage("ps", "پښتو · Pashto · basiswoorden", "ps"),
    )
    val allTargets: List<SpokenLanguage> get() = choices + communityPhrasebooks
    fun findTarget(code: String?) = allTargets.firstOrNull { it.code == code }
    fun isCommunityPhrasebook(code: String?) = communityPhrasebooks.any { it.code == code }
    fun normalizeTarget(tag: String?): String? {
        val code = tag?.trim()?.lowercase(java.util.Locale.ROOT)?.replace('_', '-')?.substringBefore('-')
        return findTarget(code)?.code
    }
    private val communityAliases = mapOf(
        "kmr" to "kurmanji kurmanci noordkoerdisch noord-koerdisch kurdish kurdî koerdisch kurmanjî",
        "ckb" to "sorani centraal-koerdisch centraal koerdisch kurdish koerdistan کوردی",
        "ps" to "pashto pasjtoe pashtu afghanistan afghaans پښتو",
    )
    fun searchCommunity(query: String): List<SpokenLanguage> {
        val text = query.trim().lowercase(java.util.Locale.ROOT)
        return communityPhrasebooks.filter { text.isBlank() || it.code.contains(text) ||
            it.name.lowercase(java.util.Locale.ROOT).contains(text) ||
            communityAliases[it.code]?.contains(text) == true }
    }
    /** Still not offered at all; these need a validated local machine-translation engine. */
    val requested: List<RequestedLanguage> = listOf(
        RequestedLanguage("so", "Soomaali · Somali"),
        RequestedLanguage("ti", "ትግርኛ · Tigrinya"),
        RequestedLanguage("am", "አማርኛ · Amharic"),
        RequestedLanguage("om", "Afaan Oromoo · Oromo"),
        RequestedLanguage("rhg", "Rohingya"),
        RequestedLanguage("prs", "دری · Dari (regional Persian)"),
    )
    fun find(code: String?) = choices.firstOrNull { it.code == code }
    fun normalize(tag: String?): String? {
        val code = tag?.trim()?.lowercase(java.util.Locale.ROOT)?.replace('_', '-')?.substringBefore('-')
        return find(code)?.code
    }
    private val aliases = mapOf(
        "ar" to "arabisch arabe syrie syrië syria palestine palestina palestinian levantine شامي سوري فلسطيني",
        "fa" to "persian farsi perzisch iran afghanistan",
        "nl" to "dutch nederlands holland",
        "uk" to "ukrainian oekraïens oekrains",
        "tr" to "turkish turks",
        "ru" to "russian russisch",
        "ur" to "pakistan urdu",
    )
    fun search(query: String): List<SpokenLanguage> {
        val text = query.trim().lowercase(java.util.Locale.ROOT)
        return choices.filter { text.isBlank() || it.name.lowercase(java.util.Locale.ROOT).contains(text) ||
            it.code.contains(text) || aliases[it.code]?.contains(text) == true }
            .sortedWith(compareBy<SpokenLanguage> { if (it.code in preferred) 0 else 1 }
                .thenBy { preferred.indexOf(it.code).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                .thenBy { it.name.lowercase(java.util.Locale.ROOT) })
    }
}

/** Bounded common-source cache plan; the app never downloads 58 pairs at startup. */
object LanguagePairPlan {
    fun starterSources(target: String, phoneLanguage: String?): List<String> {
        val phone = Languages.normalize(phoneLanguage)
        return listOfNotNull(phone, "ar", "en", "nl")
            .distinct().filter { it != target }.take(3)
    }
}

/** A small bundled glossary for true definitions. Other words get a translation, not a fabricated definition. */
data class GlossaryEntry(val definition: String, val illustration: String)
object LocalGlossary {
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
            "love" to GlossaryEntry("Deep affection for someone or something.", "❤️"),
        ),
        "de" to mapOf(
            "hallo" to GlossaryEntry("A greeting meaning hello.", "👋"),
            "verstehen" to GlossaryEntry("To understand or comprehend.", "💡"),
            "verstehst" to GlossaryEntry("Form of ‘verstehen’: you understand.", "💡"),
            "wasser" to GlossaryEntry("Water; a liquid essential for life.", "💧"),
            "katze" to GlossaryEntry("A cat, a small domesticated feline.", "🐈"),
            "hund" to GlossaryEntry("A dog, a domesticated canine.", "🐕"),
            "haus" to GlossaryEntry("A house or building used as a home.", "🏠"),
            "essen" to GlossaryEntry("To eat; also a meal or food.", "🍽️"),
        ),
        "nl" to mapOf(
            "hallo" to GlossaryEntry("A greeting meaning hello.", "👋"),
            "begrijpen" to GlossaryEntry("To understand the meaning of something.", "💡"),
            "water" to GlossaryEntry("A clear liquid essential for life.", "💧"),
            "kat" to GlossaryEntry("A small domesticated feline.", "🐈"),
            "hond" to GlossaryEntry("A domesticated canine.", "🐕"),
            "huis" to GlossaryEntry("A house, where someone lives.", "🏠"),
        ),
        "fr" to mapOf(
            "bonjour" to GlossaryEntry("A greeting meaning hello or good day.", "👋"),
            "eau" to GlossaryEntry("Water; a liquid essential for life.", "💧"),
            "chat" to GlossaryEntry("A cat, a small domesticated feline.", "🐈"),
            "maison" to GlossaryEntry("A house or home.", "🏠"),
        ),
        "es" to mapOf(
            "hola" to GlossaryEntry("A greeting meaning hello.", "👋"),
            "agua" to GlossaryEntry("Water; a liquid essential for life.", "💧"),
            "gato" to GlossaryEntry("A cat, a small domesticated feline.", "🐈"),
            "casa" to GlossaryEntry("A house or home.", "🏠"),
        ),
    )
    fun lookup(language: String?, word: String): GlossaryEntry? =
        entries[Languages.normalize(language)]?.get(word.trim().lowercase())
}

/** Find the Unicode word beneath a press, excluding surrounding punctuation. */
fun wordAt(text: String, characterOffset: Int): String? {
    if (characterOffset !in text.indices) return null
    fun valid(c: Char) = c.isLetter() || c == '\'' || c == '’' || c == '-'
    if (!valid(text[characterOffset])) return null
    var left = characterOffset
    var right = characterOffset + 1
    while (left > 0 && valid(text[left - 1])) left--
    while (right < text.length && valid(text[right])) right++
    return text.substring(left, right).trim('\'', '’', '-').takeIf { it.isNotEmpty() }
}
