package com.livelingo.android

/**
 * A public model card is a research lead, not evidence that Tuddy bundles,
 * installs, or has validated the model. Keep this separate from selectable
 * ML Kit targets and Android's actual per-device speech-pack inventory.
 */
internal data class OfflineLanguageModelCandidate(
    val category: String,
    val title: String,
    val url: String,
    val declaredLicense: String,
    val footprint: String,
    val limitation: String
)

internal data class UnavailableTextLanguage(
    val code: String,
    val title: String,
    val reason: String,
    val hasFivePhrasePreview: Boolean = false,
    val aliases: String = "",
    val speechCandidates: List<OfflineLanguageModelCandidate> = emptyList(),
    val translationCandidates: List<OfflineLanguageModelCandidate> = emptyList()
)

internal object UnavailableTextLanguageCatalog {
    private val nllbResearchCandidate = OfflineLanguageModelCandidate(
        category = "Tekstvertaling",
        title = "Meta NLLB-200 distilled 600M",
        url = "https://huggingface.co/facebook/nllb-200-distilled-600M",
        declaredLicense = "CC-BY-NC-4.0 (niet-commercieel)",
        footprint = "modelgewichten circa 2,46 GB",
        limitation = "Niet geïntegreerd of op Android gevalideerd. De licentie sluit " +
            "commercieel gebruik uit; dit is geen goedgekeurd Tuddy-downloadpakket."
    )

    val entries = listOf(
        UnavailableTextLanguage(
            "kmr", "Kurmancî · Kurmanji (Noord-Koerdisch)",
            "Nog geen complete, geïntegreerde en op toestel gevalideerde offline " +
                "spraak- én tekstvertaalroute. Er bestaat een ASR-onderzoekskandidaat, " +
                "maar die is groot en niet onafhankelijk gecontroleerd. Alleen vijf " +
                "vaste basisuitdrukkingen zijn beschikbaar; geen vrije zinsvertaling.",
            hasFivePhrasePreview = true,
            aliases = "ku kurdish koerdisch kurmanji kurmanci noordkoerdisch",
            speechCandidates = listOf(
                OfflineLanguageModelCandidate(
                    category = "Spraakherkenning (ASR)",
                    title = "amedcj/whisper-kurmanji",
                    url = "https://huggingface.co/amedcj/whisper-kurmanji",
                    declaredLicense = "Apache-2.0 volgens modelkaart",
                    footprint = "modelbestand circa 967 MB",
                    limitation = "Niet geïntegreerd of op Android getest. Trainingsdatarechten, " +
                        "dialectnauwkeurigheid, latency en geheugenbeslag zijn niet geverifieerd."
                )
            ),
            translationCandidates = listOf(nllbResearchCandidate)
        ),
        UnavailableTextLanguage(
            "ckb", "سۆرانی · Sorani (Centraal-Koerdisch)",
            "Nog geen complete, geïntegreerde en op toestel gevalideerde offline " +
                "spraak- én tekstvertaalroute. Er bestaat een ASR-onderzoekskandidaat, " +
                "maar die is groot en niet onafhankelijk gecontroleerd. Alleen vijf " +
                "vaste basisuitdrukkingen zijn beschikbaar; geen vrije zinsvertaling.",
            hasFivePhrasePreview = true,
            aliases = "ku kurdish koerdisch sorani centraalkoerdisch",
            speechCandidates = listOf(
                OfflineLanguageModelCandidate(
                    category = "Spraakherkenning (ASR)",
                    title = "roshna-omer/whisper-small-Kurdish-Sorani",
                    url = "https://huggingface.co/roshna-omer/whisper-small-Kurdish-Sorani",
                    declaredLicense = "Apache-2.0 volgens modelkaart",
                    footprint = "modelbestand circa 967 MB",
                    limitation = "Niet geïntegreerd of op Android getest. Trainingsdatarechten, " +
                        "Sorani-variantdekking, latency en geheugenbeslag zijn niet geverifieerd."
                )
            ),
            translationCandidates = listOf(nllbResearchCandidate)
        ),
        UnavailableTextLanguage(
            "ps", "پښتو · Pasjtoe / Pashto",
            "Nog geen complete, geïntegreerde en op toestel gevalideerde offline " +
                "spraak- én tekstvertaalroute. Er bestaat een ASR-onderzoekskandidaat, " +
                "maar grootte, datasetrechten en prestaties zijn nog niet gecontroleerd. " +
                "Alleen vijf vaste basisuitdrukkingen zijn beschikbaar; geen vrije zinsvertaling.",
            hasFivePhrasePreview = true,
            aliases = "pashto pashtu pasjtoe afghanistan پښتو",
            speechCandidates = listOf(
                OfflineLanguageModelCandidate(
                    category = "Spraakherkenning (ASR)",
                    title = "Sabtain-Dev/STT-Whisper-Pashto",
                    url = "https://huggingface.co/Sabtain-Dev/STT-Whisper-Pashto",
                    declaredLicense = "MIT volgens modelkaart",
                    footprint = "grootte van het modelbestand nog niet vastgesteld",
                    limitation = "Niet geïntegreerd of op Android getest. Dataset- en " +
                        "gewichtenherkomst, dialectdekking, latency en geheugenbeslag " +
                        "zijn niet onafhankelijk geverifieerd."
                )
            ),
            translationCandidates = listOf(
                nllbResearchCandidate,
                OfflineLanguageModelCandidate(
                    category = "Tekstvertaling",
                    title = "ZamAI Pashto ↔ English translator",
                    url = "https://huggingface.co/tasal9/ZamAI-Pashto-Translator-FacebookNLB-ps-en",
                    declaredLicense = "Modelkaart noemt Apache-2.0; upstream is CC-BY-NC-4.0",
                    footprint = "gebaseerd op NLLB-200 3.3B",
                    limitation = "Licentiecompatibiliteit is niet opgehelderd: de upstream " +
                        "facebook/nllb-200-3.3B-gewichten zijn niet-commercieel gelicentieerd. " +
                        "Niet vrijgeven of bundelen zonder rechtenbeoordeling."
                ),
                OfflineLanguageModelCandidate(
                    category = "Tekstvertaling",
                    title = "Meta M2M100 418M",
                    url = "https://huggingface.co/facebook/m2m100_418M",
                    declaredLicense = "MIT volgens modelkaart",
                    footprint = "repository circa 3,88 GB",
                    limitation = "De modelkaart bevat Pasjtoe (ps), Nederlands (nl) en Engels (en), " +
                        "maar geen Koerdische taalcode. Niet geïntegreerd; er is geen gequantiseerd " +
                        "Android-pakket of toestelprestatie aangetoond."
                )
            )
        ),
        UnavailableTextLanguage(
            "so", "Soomaali · Somalisch",
            "Geen complete, geïntegreerde en op toestel gevalideerde offline " +
                "spraak- en tekstvertaalroute.",
            aliases = "somali somalisch somalia"
        ),
        UnavailableTextLanguage(
            "ti", "ትግርኛ · Tigrinya",
            "Geen complete, geïntegreerde en op toestel gevalideerde offline " +
                "spraak- en tekstvertaalroute.",
            aliases = "tigrinya tigrigna"
        ),
        UnavailableTextLanguage(
            "am", "አማርኛ · Amhaars",
            "Geen complete, geïntegreerde en op toestel gevalideerde offline " +
                "spraak- en tekstvertaalroute.",
            aliases = "amharic amhaars ethiopia"
        ),
        UnavailableTextLanguage(
            "om", "Afaan Oromoo · Oromo",
            "Geen complete, geïntegreerde en op toestel gevalideerde offline " +
                "spraak- en tekstvertaalroute.",
            aliases = "oromo oromoo ethiopia"
        ),
        UnavailableTextLanguage(
            "rhg", "Rohingya",
            "Geen complete, geïntegreerde en op toestel gevalideerde offline " +
                "spraak- en tekstvertaalroute.",
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
        if (LanguageSearch.normalize(query).isEmpty()) return emptyList()
        return entries.filter { item ->
            LanguageSearch.matches(query, listOf(item.code, item.title, item.aliases))
        }
    }
}
