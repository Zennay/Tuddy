# Taalondersteuning in Tuddy

Open in de app **Taal- en modelinstellingen → Bekijk alle taalondersteuning**. Het overzicht is doorzoekbaar op taalcode, Engelse of eigen naam, Nederlandse naam, aliassen, modelkaartnaam, modelcategorie, gedeclareerde licentie, modelomvang en vermelde taalrichtingen/codes; accenten zijn niet vereist. Zoeken naar een model toont de talen waarvoor die onderzoekskandidaat relevant is, maar maakt het model niet beschikbaar of gedownload. Richtingcodes die een modelkaart noemt, zijn onderzoeksmetadata: ze bewijzen niet dat elk bron-doelpaar bestaat, dat de dialectkwaliteit geschikt is of dat de combinatie op Android werkt.

## Drie aparte categorieën

1. **Tekstvertaling (59 ML Kit-doelen):** de code staat in de vertaalcatalogus. Voor een specifieke bron-doelcombinatie moet Tuddy de lokale modelinventaris apart controleren; de lijst bewijst niet dat de modellen al zijn gedownload.
2. **Spraakherkenning (Android-locale-kandidaten):** deze keuzes vragen de spraakdienst van het toestel om ondersteuning en geïnstalleerde modellen te controleren. Een kandidaatnaam is geen bewijs van lokale beschikbaarheid. Android 12 kan de beschikbaarheid per taal niet altijd inspecteren; onbekend blijft onbekend.
3. **Taalverzoeken zonder vrij offline ASR+MT-model:** Kurmanji, Sorani en Pasjtoe blijven beperkt tot vijf vaste basiswoorden. Somalisch, Tigrinya, Amhaars, Oromo, Rohingya en Dari worden niet als bewezen lokale vrije vertaling aangeboden. Perzisch is geen bewezen vervanger voor Dari.

De app voert geen automatische cloudspraakfallback uit. Het overzicht downloadt geen modellen, verandert geen taalkeuze en slaat geen gespreksinhoud op. Gebruik Offline startcheck voor de geselecteerde combinatie en controleer daadwerkelijke prestaties afzonderlijk op een fysiek toestel.

## Waarom dit onderscheid belangrijk is

- Een ML Kit-tekstmodel is geen spraakherkenningsmodel.
- Een Android-spraakpakket bewijst niet dat tekstvertaling offline werkt.
- Een locale-kandidaat bewijst niet dat een toestel het pakket bevat.
- Een vaste woordenlijst is geen vrije vertaling en bewijst geen ASR. Bij Kurmanji, Sorani en Pasjtoe opent de knop in het overzicht rechtstreeks de bestaande vijfwoordenpreview.
- Volledige Kurmanji/Sorani/Pasjtoe-ondersteuning vereist een geschikt gelicentieerd on-device model, native-speakerbeoordeling en fysieke vliegtuigmodusacceptatie.

De gids is informatief en doorzoekbaar; zij vervangt geen toestelcontrole, modellicentiecontrole of native-speakerbeoordeling.

Bij een zoekopdracht op modelnaam, licentie of technische beperking toont Tuddy alleen de bijpassende modelkaarten. Het onderzoeksdeel heeft daarnaast een taakfilter: **Alle modellen**, **Spraak → tekst (ASR)** of **Tekstvertaling (MT)**. Dit maakt het makkelijker om een mogelijke spraakherkenningsstap te onderscheiden van een vertaalmodel; het maakt een kandidaat niet beschikbaar of gevalideerd. **Kopieer gefilterde onderzoeksresultaten** kopieert uitsluitend de zichtbare kandidaatmetadata binnen de huidige zoekopdracht en het gekozen taakfilter, inclusief modelkaartlinks, gedeclareerde licenties, omvang, taalrichtingsnotities en bekende beperkingen (maximaal 16.000 tekens). De zoektekst, eigen zinnen, transcripties en audio worden niet gekopieerd. Dit downloadt of activeert geen model.

## Modelonderzoek: concrete kandidaten, nog geen ondersteuning

De taalondersteuningdialoog toont nu externe modelkaarten voor drie ASR-kandidaten. Ze zijn **niet** ingebouwd, gedownload of op Android gevalideerd:

| Taal | ASR-onderzoekskandidaat | Modelbestand / licentie volgens kaart | Open punt |
|---|---|---|---|
| Kurmanji | [amedcj/whisper-kurmanji](https://huggingface.co/amedcj/whisper-kurmanji) | circa 969 MB; Apache-2.0 vermeld; modelkaart noemt Common Voice maar geen release-ID/evaluatiescore | [Common Voice Kurmanji 26.0](https://mozilladatacollective.com/datasets/cmqinqhul00wynq07koxlxbo5) is CC0-1.0, maar de gebruikte release is onbekend; trainingsdatarechten, dialectnauwkeurigheid, Android-runtime en geheugen blijven onbevestigd |
| Sorani | [roshna-omer/whisper-small-Kurdish-Sorani](https://huggingface.co/roshna-omer/whisper-small-Kurdish-Sorani) | circa 969 MB; Apache-2.0 vermeld; modelkaart noemt dataset `None` en zelfgerapporteerde WER 24,12% / orthografische WER 26,73% | Datasetrechten, evaluatieset, Sorani-variantdekking, Android-runtime en geheugen niet geverifieerd |
| Pasjtoe | [Sabtain-Dev/STT-Whisper-Pashto](https://huggingface.co/Sabtain-Dev/STT-Whisper-Pashto) | MIT op model- en datasetkaart; dataset 4.819 rijen / circa 1,45 GB; zelfgerapporteerde WER 43,83% | Rechtenherkomst, representativiteit, dialectdekking en Android-runtime nog niet onafhankelijk geverifieerd |

### Waarom de vertaalroute nog geblokkeerd is

- [Meta NLLB-200 distilled 600M](https://huggingface.co/facebook/nllb-200-distilled-600M) dekt de relevante taalcodefamilies, maar de gepubliceerde gewichten zijn circa 2,46 GB en de licentie is **CC-BY-NC-4.0**. Dat is geen vrijgaveklare kandidaat voor commercieel gebruik of een kleine Android-pack.
- [Google MADLAD-400-3B-MT](https://huggingface.co/google/madlad400-3b-mt) vermeldt Apache-2.0 en een lijst met onder meer ckb, ps, ku, nl en en. Het model card noemt geen dialectspecifieke kmr-code; de genoemde quantized GGUF is circa 1,65 GB, en de kaart waarschuwt dat kwaliteit per taal varieert en niet productiebeoordeeld is. Dit is een betere licentiekandidaat voor Sorani/Pasjtoe-onderzoek, maar nog steeds geen geïntegreerde Android-pack.
- De [ZamAI Pashto-English translator](https://huggingface.co/tasal9/ZamAI-Pashto-Translator-FacebookNLB-ps-en) vermeldt Apache-2.0 voor de afgeleide repository, maar noemt NLLB-200 3.3B als basismodel; de [upstream modelkaart](https://huggingface.co/facebook/nllb-200-3.3B) vermeldt CC-BY-NC-4.0. Licentiecompatibiliteit moet eerst worden opgelost.
- [Meta M2M100 418M](https://huggingface.co/facebook/m2m100_418M) vermeldt MIT en bevat volgens de modelkaart Pasjtoe, Nederlands en Engels, maar de volledige repository is circa 3,88 GB. Het model bevat geen Kurmanji- of Sorani-taalcode; zonder gequantiseerd Android-pakket, runtime- en toestelbewijs is dit alleen een Pasjtoe-onderzoekskandidaat, geen Tuddy-feature.
- Een licentielabel van een modelkaart is geen onafhankelijke audit van de dataset, gewichten, tokenizer en alle upstreamcomponenten. Er is geen modeldownload of externe runtime in Tuddy toegevoegd.

**Volgende productgate:** selecteer per dialect een model met controleerbare herkomst en geschikte licentie, maak een realistische gequantiseerde Android-pack, integreer echte lokale ASR én MT, en pas daarna native-speakerreview en geautoriseerde fysieke vliegtuigmodusacceptatie toe. Tot dat moment blijft de app de drie talen als preview-only markeren.

## Scriptrichting bij eigen Sorani- en Pasjtoe-zinnen

De privé-zinnenlijst en de import-/verwijderpreview tonen eigen Sorani (`ckb`) en Pasjtoe (`ps`) tekst van rechts naar links. Het invoerveld, de oefenpoging en de betekenis die de gebruiker in Nederlands of Engels zelf schrijft blijven respectievelijk gericht op de geselecteerde taal en links-naar-rechts. Deze opmaak verandert geen tekst, translitereert niets en controleert geen spelling of vertaling.

## Automatische tekstrichting voor gemengde schriften

De algemene ondertitel-plakbewerker kiest nu de schrijfrichting op basis van het eerste Unicode-teken met een sterke links- of rechtsrichting, niet op basis van de telefooninstelling. Dat geldt voor de brontekst en de zelfgeschreven betekenis; het grote live-ondertitelkaartje gebruikt dezelfde regel met de herkende taalcode alleen als terugval voor tekst zonder richtingsinformatie. Tekst wordt niet herschreven of getranslitereerd.


## Compact ASR research leads — 21 October 2026

These cards improve research visibility only. None is downloaded, bundled, integrated or enabled as a Tuddy speech backend.

- [amedcj/kurmanji-asr-model](https://huggingface.co/amedcj/kurmanji-asr-model): model card declares MIT and 94.4M parameters, but its description is still placeholder text and it provides no useful evaluation summary. Dataset provenance, dialect coverage, file footprint and Android runtime suitability remain unverified.
- [salamhasan/wav2vec2-large-xlsr-kurmanji](https://huggingface.co/salamhasan/wav2vec2-large-xlsr-kurmanji): Apache-2.0 on the model card; it reports WER 32.98% / CER 7.97% on Common Voice 8.0 and 0.3B parameters. The repo requires agreeing to share contact information before access; model footprint and spontaneous-speech/Android performance are not verified.
- [RevgeAI/vekol-stt-ckb-tiny](https://huggingface.co/RevgeAI/vekol-stt-ckb-tiny): model card reports 18 MB int4 / 37 MB int8 and self-reported WER 35.0% / CER 9.85% on Common Voice 25. It is **CC-BY-NC-4.0**, so it is not a commercial-release candidate without separate permission. The figures have not been independently reproduced on Android.
- [RevgeAI/vekol-stt-ckb-base](https://huggingface.co/RevgeAI/vekol-stt-ckb-base): model card reports 36 MB int4 / 72 MB int8, WER 29.0% and CER 7.95% on its speaker-disjoint Common Voice 25 test. CC-BY-NC-4.0, non-commercial only; figures not independently reproduced on Android.
- [RevgeAI/vekol-stt-ckb-small](https://huggingface.co/RevgeAI/vekol-stt-ckb-small): model card reports 121 MB int4 / 241 MB int8, WER 15.5% and about 3% CER on the same reported test. CC-BY-NC-4.0, non-commercial only; larger footprint and figures not independently reproduced on Android.
- [SECT19N/whisper-small-ckb-merged (ZarosASR)](https://huggingface.co/SECT19N/whisper-small-ckb-merged): the model card declares Apache-2.0 and reports WER 7.96% on the Common Voice 24.0 CKB test; Mozilla declares Common Voice contributions CC0. Access to model weight files is gated behind agreement to share contact information, although the card is publicly readable. Exact candidate file size is not verified; Whisper-small upstream weights are about 967 MB. The score is self-reported and not independently reproduced. The model repurposes Whisper's Persian decoder token for Sorani, so dialect quality, model-weight provenance/license compatibility, native-speaker review, Android runtime/latency/memory and real conversational speech remain unverified. No immutable model revision or file SHA is recorded because the weight files are gated. Research candidate only; no weights downloaded, bundled or activated.
- [Sabtain-Dev/STT-Whisper-Pashto](https://huggingface.co/Sabtain-Dev/STT-Whisper-Pashto): model card declares MIT, 0.2B parameters and self-reported WER 43.83% on its Pashto-ASR dataset. Model-file size, dataset terms/split and Android performance remain unverified.
- [Nasimbahar/pashto-ghag-whisper-medium-asr](https://huggingface.co/Nasimbahar/pashto-ghag-whisper-medium-asr): Apache-2.0 on the model card, 3.06 GB / 0.8B parameters (FP32), with self-reported balanced mean WER 14.63% / CER 4.66% across three held-out splits from 54.4 hours of validated Pashto Common Voice 17.0. This is a stronger reported quality baseline but too large for broad Android deployment without quantization; no Android runtime or device performance is validated.

A permissive model-card license is not a dataset/provenance audit. These leads do not meet the full offline ASR+MT gate and must not change the Kurmanji, Sorani or Pashto preview-only status.


## Model-herkomst update — 22 oktober 2026

Twee nieuwe onderzoekskaarten zijn zichtbaar in het doorzoekbare taaloverzicht. Ze veranderen de status van Kurmanji, Sorani of Pasjtoe niet: geen model wordt gedownload, ingebouwd of als ondersteund gekozen.

- [asounimelb/whisper-small-ckb-onnx](https://huggingface.co/asounimelb/whisper-small-ckb-onnx) vermeldt Apache-2.0 en is als ONNX-export gepubliceerd. De modelkaart noemt 244M parameters en een encoderbestand van circa 353 MB FP32; het volledige pakket is groter. De Engelse decoder-token is een workaround omdat Whisper geen native Sorani-token heeft. Dat maakt het een mogelijke ONNX Runtime-onderzoeksroute, geen gevalideerde Android-feature: datarechten, totale downloadgrootte, decoderkwaliteit, geheugen en toestelprestaties zijn nog niet onafhankelijk vastgesteld.
- [arkanit/badini-nllb-translator](https://huggingface.co/arkanit/badini-nllb-translator) vermeldt MIT op de afgeleide modelkaart, maar noemt NLLB-200 distilled 600M als basis. De upstream [NLLB-200 distilled 600M](https://huggingface.co/facebook/nllb-200-distilled-600M) heeft CC-BY-NC-4.0. De afgeleide kaart beschrijft Badini en gebruikt `ckb_Arab`; dit bewijst geen standaard Kurmanji `kmr_Latn` of algemene Sorani-dekking. De licentie- en modelherkomstketen is dus **onopgelost**: Tuddy mag dit niet bundelen of activeren op basis van alleen het MIT-label.

Dit is een expliciete licentie-/dialectwaarschuwing, geen juridische conclusie over afgeleide gewichten. Vóór integratie zijn schriftelijke herkomst-/rechten-verduidelijking en een geschikte Android-runtime/footprint nodig. De bestaande volledige offline ASR+MT-gate en de vijf-zinnen-previewlabels blijven ongewijzigd.


## Extra MT-lead — 22 oktober 2026: OPUS-MT ine-ine

[Helsinki-NLP/opus-mt-ine-ine](https://huggingface.co/Helsinki-NLP/opus-mt-ine-ine) is een aanvullende onderzoekskandidaat waarvan de Hugging Face-modelkaart Apache-2.0 vermeldt. De [algemene OPUS-MT-train documentatie](https://github.com/Helsinki-NLP/OPUS-MT-train) zegt echter dat publieke modellen CC-BY-4.0 worden verspreid. Dit licentieconflict is niet opgelost: behandel de kandidaat niet als distributiegereed totdat de model-specifieke voorwaarden en corpusrechten zijn bevestigd. De Hugging Face repository is circa 536 MB; de bestandslijst noemt een PyTorch-model van circa 266 MB plus tokenizers. De modelkaart noemt onder meer `kur_Arab`, `kur_Latn`, `pus`, `nld` en `eng` aan zowel bron- als doelzijde en vereist een doelprefix in de vorm `>>id<<`. Dit blijft een onderzoekslead, geen goedgekeurde MT-pack.

**Niet als ondersteund beschouwen:** `kur_Arab`/`kur_Latn` zijn generieke taal-/schriftcodes en geen bewijs van native-speaker-goedgekeurde Sorani- of Kurmanji-kwaliteit; `pus` moet naar Tuddy's Pasjtoe-code worden gemapt. De modelkaart biedt geen bruikbare paar-specifieke kwaliteitsmeting voor deze dialecten, en corpus-/trainingsdatarechten zijn niet onafhankelijk geauditeerd. De Android-runtime, tokenizer, doelprefix, kwantisatie, geheugen, snelheid, batterij en daadwerkelijke offline werking zijn niet gevalideerd. Geen modelbestanden worden gedownload of ingebouwd door deze kaart.

**Concrete vervolgstap voor issue #2:** controleer eerst de upstream model-/corpusrechten en taalcode-/prefixmapping; bepaal daarna of de ~266 MB gewichten met de bijbehorende tokenizer naar een bruikbare, permissief gelicentieerde Android-pack kunnen worden geconverteerd. Pas daarna native-speakerreview en echte offline-toestelacceptatie. Tot die bewijzen bestaan blijven Kurmanji, Sorani en Pasjtoe preview-only.


## Runtime lead — preconverted INT8 ONNX OPUS-MT

[SashaSobchuk/opus-mt-onnx-int8](https://huggingface.co/SashaSobchuk/opus-mt-onnx-int8) publishes quantized ONNX model folders for local/on-device translation and lists an `opus-mt-ine-ine` folder. Its card says each language direction was checked only on sample words and points to a `pairs_map.json` for directions that produced output. This may reduce Android runtime/conversion work compared with converting the original 536 MB repository from scratch.

**Still not ready to ship:** the conversion repository declares CC-BY-4.0 and says the source models are CC-BY-4.0, while the official [Helsinki-NLP/opus-mt-ine-ine](https://huggingface.co/Helsinki-NLP/opus-mt-ine-ine) card declares Apache-2.0. That metadata/provenance mismatch is unresolved. The exact `kur_Arab`, `kur_Latn`, `pus`, `nld` and `eng` direction entries in `pairs_map.json`, model hashes and actual sentence quality have not been confirmed. Do not bundle, download or activate this converted pack until source/license provenance and exact direction coverage are clear, then native-speaker and real Android offline behavior are reviewed.
