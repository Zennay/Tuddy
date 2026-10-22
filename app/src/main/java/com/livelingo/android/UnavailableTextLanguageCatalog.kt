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

    private val kurmanjiCompactResearchCandidate = OfflineLanguageModelCandidate(
        category = "Spraakherkenning (ASR)",
        title = "amedcj/kurmanji-asr-model",
        url = "https://huggingface.co/amedcj/kurmanji-asr-model",
        declaredLicense = "MIT volgens modelkaart",
        footprint = "94,4M parameters; bestandsgrootte niet gepubliceerd",
        limitation = "Modelkaart bevat nog placeholderbeschrijving en geen bruikbare dataset- of evaluatiegegevens. " +
            "Datasetherkomst, dialectdekking, latency, geheugen en Android-runtime zijn niet onafhankelijk gecontroleerd. " +
            "Niet geïntegreerd of op Android getest."
    )

    private val soraniTinyResearchCandidate = OfflineLanguageModelCandidate(
        category = "Spraakherkenning (ASR)",
        title = "RevgeAI/vekol-stt-ckb-tiny",
        url = "https://huggingface.co/RevgeAI/vekol-stt-ckb-tiny",
        declaredLicense = "CC-BY-NC-4.0 (niet-commercieel)",
        footprint = "18 MB int4 / 37 MB int8 volgens modelkaart",
        limitation = "Modelkaart meldt zelfgerapporteerd WER 35,0% en CER 9,85% op Common Voice 25. " +
            "Niet onafhankelijk herhaald of op Android gevalideerd; de niet-commerciële licentie sluit " +
            "commercieel gebruik uit zonder aparte toestemming. Niet geïntegreerd of geactiveerd."
    )

    private val soraniBaseResearchCandidate = OfflineLanguageModelCandidate(
        category = "Spraakherkenning (ASR)",
        title = "RevgeAI/vekol-stt-ckb-base",
        url = "https://huggingface.co/RevgeAI/vekol-stt-ckb-base",
        declaredLicense = "CC-BY-NC-4.0 (niet-commercieel)",
        footprint = "36 MB int4 / 72 MB int8; 74M parameters volgens modelkaart",
        limitation = "Modelkaart rapporteert WER 29,0% en CER 7,95% op de speaker-disjoint Common Voice 25-test. " +
            "Zelfgerapporteerd, niet onafhankelijk herhaald op Android; niet-commerciële licentie. " +
            "Niet geïntegreerd of geactiveerd."
    )

    private val soraniSmallResearchCandidate = OfflineLanguageModelCandidate(
        category = "Spraakherkenning (ASR)",
        title = "RevgeAI/vekol-stt-ckb-small",
        url = "https://huggingface.co/RevgeAI/vekol-stt-ckb-small",
        declaredLicense = "CC-BY-NC-4.0 (niet-commercieel)",
        footprint = "121 MB int4 / 241 MB int8; 244M parameters volgens modelkaart",
        limitation = "Modelkaart rapporteert WER 15,5% en circa 3% CER op de speaker-disjoint Common Voice 25-test. " +
            "Zelfgerapporteerd, niet onafhankelijk herhaald op Android; niet-commerciële licentie. " +
            "Niet geïntegreerd of geactiveerd."
    )

    private val kurmanjiWav2VecResearchCandidate = OfflineLanguageModelCandidate(
        category = "Spraakherkenning (ASR)",
        title = "salamhasan/wav2vec2-large-xlsr-kurmanji",
        url = "https://huggingface.co/salamhasan/wav2vec2-large-xlsr-kurmanji",
        declaredLicense = "Apache-2.0 volgens modelkaart; toegang gated",
        footprint = "0,3B parameters; exacte bestandsgrootte niet geverifieerd",
        limitation = "Modeltoegang vereist eerst akkoord met het delen van contactgegevens. " +
            "Modelkaart rapporteert zelf WER 32,98% en CER 7,97% op Common Voice 8.0; alleen gelezen Latijnse spraak is beschreven. " +
            "Niet onafhankelijk herhaald, niet op Android getest en niet geïntegreerd."
    )

    private val soraniOnnxResearchCandidate = OfflineLanguageModelCandidate(
        category = "Spraakherkenning (ASR)",
        title = "asounimelb/whisper-small-ckb-onnx",
        url = "https://huggingface.co/asounimelb/whisper-small-ckb-onnx",
        declaredLicense = "Apache-2.0 volgens modelkaart",
        footprint = "244M parameters; encoder alleen circa 353 MB FP32; volledig pakket groter",
        limitation = "Publieke ONNX-export kan verder onderzoek naar ONNX Runtime op Android vereenvoudigen, " +
            "maar de encoder alleen is al groot. De modelkaart gebruikt een Engelse decoder-token als Sorani-workaround; " +
            "datarechten, volledige modelomvang, kwaliteit en Android-prestaties zijn niet onafhankelijk geverifieerd. " +
            "Niet geïntegreerd of geactiveerd."
    )

    private val badiniTranslationResearchCandidate = OfflineLanguageModelCandidate(
        category = "Tekstvertaling",
        title = "arkanit/badini-nllb-translator",
        url = "https://huggingface.co/arkanit/badini-nllb-translator",
        declaredLicense = "MIT volgens afgeleide modelkaart; upstream NLLB CC-BY-NC-4.0 — onopgelost",
        footprint = "circa 0,6B parameters; exacte pakketgrootte niet geverifieerd",
        limitation = "Modelkaart beschrijft Badini, maar adverteert de taalcode ckb_Arab; dit bewijst geen standaard " +
            "Kurmanji (kmr_Latn) of Sorani-dekking. Het noemt NLLB-200 distilled 600M als basis, waarvan de upstream " +
            "gewichten CC-BY-NC-4.0 zijn. Een MIT-label op de afgeleide repository lost die rechtenvraag niet op. " +
            "Niet bundelen of activeren zonder herkomst-/licentiebeoordeling; niet geïntegreerd."
    )

    private val soraniApacheResearchCandidate = OfflineLanguageModelCandidate(
        category = "Spraakherkenning (ASR)",
        title = "SECT19N/whisper-small-ckb-merged",
        url = "https://huggingface.co/SECT19N/whisper-small-ckb-merged",
        declaredLicense = "Apache-2.0 volgens modelkaart; toegang gated",
        footprint = "0,2B parameters; exacte bestandsgrootte niet geverifieerd",
        limitation = "Modeltoegang vereist eerst akkoord met het delen van contactgegevens. " +
            "Modelkaart rapporteert zelf WER 7,96% op Common Voice 24.0; dit is niet onafhankelijk herhaald of op Android gevalideerd. " +
            "Gebruikt de Perzische decoder-token als Sorani-workaround; niet geïntegreerd of geactiveerd."
    )

    private val pashtoGhagResearchCandidate = OfflineLanguageModelCandidate(
        category = "Spraakherkenning (ASR)",
        title = "Nasimbahar/pashto-ghag-whisper-medium-asr",
        url = "https://huggingface.co/Nasimbahar/pashto-ghag-whisper-medium-asr",
        declaredLicense = "Apache-2.0 volgens modelkaart",
        footprint = "3,06 GB; 0,8B parameters (FP32 volgens modelkaart)",
        limitation = "Modelkaart rapporteert gebalanceerde gemiddelde WER 14,63% / CER 4,66% over drie held-out splits " +
            "op 54,4 uur gevalideerde Pashto Common Voice 17.0. Zelfgerapporteerd, niet onafhankelijk herhaald; " +
            "de footprint is te groot voor brede Android-dekking zonder quantisatie. Niet geïntegreerd of op Android getest."
    )

    private val opusMtIndoEuropeanResearchCandidate = OfflineLanguageModelCandidate(
        category = "Tekstvertaling",
        title = "Helsinki-NLP/opus-mt-ine-ine · 59-talig",
        url = "https://huggingface.co/Helsinki-NLP/opus-mt-ine-ine",
        declaredLicense = "Apache-2.0 volgens modelkaart; corpusrechten nog te controleren",
        footprint = "536 MB volledige repository; 266 MB PyTorch-gewichten plus tokenizer volgens bestandslijst",
        limitation = "De modelkaart noemt kur_Arab, kur_Latn, pus, nld en eng als bron-/doeltokens en vereist " +
            "een doelprefix zoals >>nld<<. Dit is een veelbelovende kleinere MT-onderzoekskandidaat, maar " +
            "kur_Arab/kur_Latn zijn geen dialectspecifiek bewijs voor Sorani/Kurmanji en pus-codecompatibiliteit " +
            "moet worden gecontroleerd. Geen passende pair-specifieke kwaliteitsmetingen, dataset-/corpusrechten, " +
            "Android-runtime, quantisatie, latency of geheugen zijn onafhankelijk geverifieerd. Niet geïntegreerd."
    )

    private val opusMtOnnxInt8ResearchCandidate = OfflineLanguageModelCandidate(
        category = "Tekstvertaling · Android-runtimeonderzoek",
        title = "SashaSobchuk/opus-mt-onnx-int8 · INT8 ONNX",
        url = "https://huggingface.co/SashaSobchuk/opus-mt-onnx-int8",
        declaredLicense = "CC-BY-4.0 volgens conversierepo; upstream modelkaart Apache-2.0 — provenance onopgelost",
        footprint = "Bevat een opus-mt-ine-ine ONNX-int8-map; exacte omvang per benodigde richting niet vastgesteld",
        limitation = "Deze conversiecollectie is bedoeld voor lokale/on-device vertaling en bevat een ine-ine map, " +
            "maar de exacte pairs_map voor kur_Arab/kur_Latn/pus naar en van nld/eng is niet bevestigd. " +
            "De conversierepo zegt dat richtingen slechts op voorbeeldwoorden zijn gecontroleerd en noemt CC-BY-4.0, " +
            "terwijl de officiële Helsinki-NLP modelkaart Apache-2.0 vermeldt. Eerst paar-dekking, modelherkomst, " +
            "checksums, Android-runtime en native-speakerkwaliteit beoordelen; niet bundelen of activeren."
    )

    private val madladResearchCandidate = OfflineLanguageModelCandidate(
        category = "Tekstvertaling",
        title = "Google MADLAD-400-3B-MT",
        url = "https://huggingface.co/google/madlad400-3b-mt",
        declaredLicense = "Apache-2.0 volgens modelkaart",
        footprint = "3B-parameter model; genoemde gequantiseerde GGUF is circa 1,65 GB",
        limitation = "Modelkaart noemt ckb, ps, ku, nl en en, maar niet dialectspecifiek kmr. " +
            "Kwaliteit varieert per taal en is niet productiebeoordeeld. Niet geïntegreerd " +
            "of op Android getest; zelfs de gequantiseerde variant is groot."
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
                ),
                kurmanjiCompactResearchCandidate,
                kurmanjiWav2VecResearchCandidate
            ),
            translationCandidates = listOf(
                nllbResearchCandidate, madladResearchCandidate,
                opusMtIndoEuropeanResearchCandidate, opusMtOnnxInt8ResearchCandidate,
                badiniTranslationResearchCandidate
            )
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
                ),
                soraniTinyResearchCandidate,
                soraniBaseResearchCandidate,
                soraniSmallResearchCandidate,
                soraniApacheResearchCandidate,
                soraniOnnxResearchCandidate
            ),
            translationCandidates = listOf(
                nllbResearchCandidate, madladResearchCandidate,
                opusMtIndoEuropeanResearchCandidate, opusMtOnnxInt8ResearchCandidate
            )
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
                    footprint = "0,2B parameters; bestandsgrootte niet vastgesteld",
                    limitation = "Modelkaart rapporteert zelf WER 43,83% op de eigen Pashto-ASR-dataset; dit is niet onafhankelijk herhaald. " +
                        "Datasetlicentie, splitsing, gewichtenherkomst, dialectdekking en Android-prestaties zijn niet geverifieerd. " +
                        "Niet geïntegreerd of op Android getest."
                ),
                pashtoGhagResearchCandidate
            ),
            translationCandidates = listOf(
                nllbResearchCandidate,
                madladResearchCandidate,
                opusMtIndoEuropeanResearchCandidate,
                opusMtOnnxInt8ResearchCandidate,
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

    /**
     * Return only the candidate cards relevant to a model-specific query.
     * A language-name query intentionally keeps all cards for that language.
     */
    fun candidatesFor(
        item: UnavailableTextLanguage,
        query: String
    ): List<OfflineLanguageModelCandidate> {
        val candidates = item.speechCandidates + item.translationCandidates
        if (LanguageSearch.normalize(query).isEmpty() ||
            LanguageSearch.matches(query, listOf(item.code, item.title, item.aliases, item.reason))
        ) {
            return candidates
        }
        return candidates.filter { candidate ->
            LanguageSearch.matches(query, listOf(
                candidate.category,
                candidate.title,
                candidate.url,
                candidate.declaredLicense,
                candidate.footprint,
                candidate.limitation
            ))
        }
    }

    fun search(query: String): List<UnavailableTextLanguage> {
        if (LanguageSearch.normalize(query).isEmpty()) return emptyList()
        return entries.filter { item ->
            val candidateFields = (item.speechCandidates + item.translationCandidates).flatMap { candidate ->
                listOf(
                    candidate.category,
                    candidate.title,
                    candidate.url,
                    candidate.declaredLicense,
                    candidate.footprint,
                    candidate.limitation
                )
            }
            LanguageSearch.matches(
                query,
                listOf(item.code, item.title, item.aliases, item.reason) + candidateFields
            )
        }
    }
}
