# Taalondersteuning in Tuddy

Open in de app **Taal- en modelinstellingen → Bekijk alle taalondersteuning**. Het overzicht is doorzoekbaar op taalcode, Engelse of eigen naam, Nederlandse naam, aliassen, modelkaartnaam, modelcategorie, gedeclareerde licentie, modelomvang en vermelde taalrichtingen/codes; accenten zijn niet vereist. Onderzoekskandidaten zijn bovendien te filteren op taak (ASR/MT) en licentie-/toegangscategorie. De labels onderscheiden onopgeloste licentie-/provenanceconflicten, gated toegang, niet-commerciële voorwaarden, naamsvermelding/extra voorwaarden (bijvoorbeeld CC-BY, CC-BY-SA of CC-BY-ND), permissieve licenties zoals Apache/MIT/BSD/CC0/ISC die op de modelkaart staan, en overig/onbekend. De classifier leest zowel het gedeclareerde licentieveld als de beperkingen: een onopgelost conflict in een van beide velden gaat vóór een permissief ogend modelkaartlabel; daarna worden gated toegang, niet-commercieel en naamsvermelding/extra voorwaarden apart weergegeven. Het permissieve signaal vereist een volledig licentie-identificatiemiddel (bijvoorbeeld MIT of Apache-2.0), geen toevallige deelstring zoals het woord `limited`. Dit is tekstuele triage, geen juridische conclusie. De labels bewijzen geen upstream datasetrechten, herkomst, Android-haalbaarheid of distributiegoedkeuring. Zoeken naar een model toont de talen waarvoor die onderzoekskandidaat relevant is, maar maakt het model niet beschikbaar of gedownload. Richtingcodes die een modelkaart noemt, zijn onderzoeksmetadata: ze bewijzen niet dat elk bron-doelpaar bestaat, dat de dialectkwaliteit geschikt is of dat de combinatie op Android werkt.

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
| Kurmanji | [amedcj/whisper-kurmanji](https://huggingface.co/amedcj/whisper-kurmanji) | 966,995,080 bytes; Apache-2.0 vermeld; snapshot `d7a083101e13012e3748f4c7d77234fa7d195441`; model.safetensors SHA-256 `7411638aad942d5d1038e0ff3b79f5632ad479c60ce98e2739f08c6cbdae5ada` | Checkpointbytes zijn nu reproduceerbaar, maar modelkaart noemt geen Common Voice-release-ID/evaluatiescore; trainingsdatarechten, dialectnauwkeurigheid, Android-runtime en geheugen blijven onbevestigd |
| Sorani | [roshna-omer/whisper-small-Kurdish-Sorani](https://huggingface.co/roshna-omer/whisper-small-Kurdish-Sorani) | circa 969 MB; Apache-2.0 vermeld; modelkaart noemt dataset `None` en zelfgerapporteerde WER 24,12% / orthografische WER 26,73% | Datasetrechten, evaluatieset, Sorani-variantdekking, Android-runtime en geheugen niet geverifieerd |
| Pasjtoe | [Sabtain-Dev/STT-Whisper-Pashto](https://huggingface.co/Sabtain-Dev/STT-Whisper-Pashto) | MIT op model- en datasetkaart; dataset 4.819 rijen / circa 1,45 GB; zelfgerapporteerde WER 43,83% | Rechtenherkomst, representativiteit, dialectdekking en Android-runtime nog niet onafhankelijk geverifieerd |

### Waarom de vertaalroute nog geblokkeerd is

- [Meta NLLB-200 distilled 600M](https://huggingface.co/facebook/nllb-200-distilled-600M) dekt de relevante taalcodefamilies, maar de gepubliceerde gewichten zijn circa 2,46 GB en de licentie is **CC-BY-NC-4.0**. Dat is geen vrijgaveklare kandidaat voor commercieel gebruik of een kleine Android-pack.
- [Google MADLAD-400-3B-MT](https://huggingface.co/google/madlad400-3b-mt) vermeldt Apache-2.0 en een lijst met onder meer ckb, ps, ku, nl en en. Het model card noemt geen dialectspecifieke kmr-code; de genoemde quantized GGUF is circa 1,65 GB, en de kaart waarschuwt dat kwaliteit per taal varieert en niet productiebeoordeeld is. Dit is een betere licentiekandidaat voor Sorani/Pasjtoe-onderzoek, maar nog steeds geen geïntegreerde Android-pack.
- De [ZamAI Pashto-English translator](https://huggingface.co/tasal9/ZamAI-Pashto-Translator-FacebookNLB-ps-en) vermeldt Apache-2.0 voor de afgeleide repository, maar noemt NLLB-200 3.3B als basismodel; de [upstream modelkaart](https://huggingface.co/facebook/nllb-200-3.3B) vermeldt CC-BY-NC-4.0. Licentiecompatibiliteit moet eerst worden opgelost.
- [Meta M2M100 418M](https://huggingface.co/facebook/m2m100_418M) vermeldt MIT en bevat volgens de modelkaart Pasjtoe, Nederlands en Engels, maar de volledige repository is circa 3,88 GB. Het model bevat geen Kurmanji- of Sorani-taalcode; zonder gequantiseerd Android-pakket, runtime- en toestelbewijs is dit alleen een Pasjtoe-onderzoekskandidaat, geen Tuddy-feature.
- Een licentielabel van een modelkaart is geen onafhankelijke audit van de dataset, gewichten, tokenizer en alle upstreamcomponenten. Er is geen modeldownload of externe runtime in Tuddy toegevoegd.

### Nieuwe kleine Sorani-MT-onderzoekskandidaten — 23 oktober 2026

Twee extra modelkaarten beschrijven afzonderlijke richtingen met Marian-checkpoints van 51,9M parameters (F32):

| Richting | Kandidaat | Modelkaart rapporteert | Blokker |
|---|---|---|---|
| Sorani `ckb` → Engels `en` | [TartarusXXX/ckb-en-marian-transformers](https://huggingface.co/TartarusXXX/ckb-en-marian-transformers) | BLEU 30,2383 / chrF 56,6711 uit trainingslogs; validatie/testrecept OpenLanguageData FLORES+ | Geen expliciete licentie zichtbaar op de modelkaart; auteur vraagt om dataset- en licentiecompatibiliteit te controleren vóór herdistributie |
| Engels `en` → Sorani `ckb` | [TartarusXXX/en-ckb-marian-transformers](https://huggingface.co/TartarusXXX/en-ckb-marian-transformers) | BLEU 14,2475 / chrF 45,1146 uit trainingslogs; validatie/testrecept OpenLanguageData FLORES+ | Dezelfde ontbrekende expliciete licentie en waarschuwing over dataset-/licentiecompatibiliteit |

De modelkaarten noemen InterdialectCorpus, Tatoeba, Wikimedia, TICO-19 en een Sorani-parallelcorpus als trainingsdataselectie. Dit is een bruikbare, kleine **research lead**, geen goedgekeurde modelpack: de licentie is niet vastgesteld, exacte gewicht-/tokenizer-hashes en datasetversies zijn niet vastgepind, de cijfers zijn niet onafhankelijk gereproduceerd en Android-runtime, geheugen, latency, native-speakerkwaliteit en vliegtuigmodusgedrag zijn onbekend. De twee richtingen zijn afzonderlijke checkpoints; geen van beide is in Tuddy gedownload, geïntegreerd of geactiveerd.

### Eén meertalige bron-naar-Engels-kandidaat voor kmr/ckb/pus — 23 oktober 2026

De [Helsinki-NLP/opus-mt-tc-bible-big-iir-en-modelkaart](https://huggingface.co/Helsinki-NLP/opus-mt-tc-bible-big-iir-en) noemt `kmr` (Kurmanji), `ckb` (Sorani) en `pus` (Pasjtoe, ISO 639-3) als brontalen met Engels (`eng`) als doeltaal. De kaart vermeldt Apache-2.0 en circa 0,2B parameters in F32; het exacte pakketformaat en de bestandsgrootte zijn niet vastgesteld. De kaart rapporteert een algemene multi-eng Tatoeba-score (BLEU 36,4 / chr-F 0,55062 over 10.000 zinnen), geen aparte score voor elk van deze dialecten. De trainingsmix omvat OPUS en een meertalige Bijbelcorpus. Daarom blijven corpusversies/rechten en afgeleide gewichtsprovenance **onopgelost** totdat de bronrechten en exacte modelrevisie onafhankelijk zijn vastgesteld. Het is bovendien alleen een mogelijke vertaalrichting naar Engels: geen ASR en geen bewezen Engels→kmr/ckb/pus-route. Niet downloaden of bundelen op basis van het modelkaartlabel alleen.

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

- [OpenVoiceOS/omnilingual-asr-ctc-300m-onnx](https://huggingface.co/OpenVoiceOS/omnilingual-asr-ctc-300m-onnx): Apache-2.0 Meta Omnilingual CTC model; the community ONNX export offers 328 MB INT8 (1.3 GB FP32), pinned to repository snapshot `bc55e1de19af8a907b0a010d06fc4637a2d5bf0c` and INT8 file SHA-256 `38a6cc569d46eb382f27d85b3208b883e834ff281b2057a606ec79e4ae501545`; the upstream training corpus is declared CC-BY-4.0. Meta's official [language-ID list](https://github.com/facebookresearch/omnilingual-asr/blob/main/src/omnilingual_asr/models/wav2vec2_llama/lang_ids.py) includes `ckb_Arab`, `pbt_Arab` and `pus_Arab`, but not `kmr_Latn`. This is only family-level language inventory evidence: the 300M CTC export has no target-specific Sorani/Pashto metrics, is not language-conditioned and has documented wrong-script failures. The 6.0 / 8.8 / 12.6 CER values in Meta's separate per-language table are for the 7B LLM-ASR system, **not** this 300M CTC export. It also lacks punctuation/capitalization, degrades beyond 40-second clips, and the published runtime uses an onnx-asr integration branch rather than a validated Android package. Research candidate only.
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


## Eigen zinnen voor Kurmanji, Sorani en Pasjtoe zonder model

Bij de vijfwoordenpreview staat **Schrijf eigen zinnen zonder model…**.
Deze route opent de bestaande tijdelijke correctiebeheerfunctie met de
gekozen taal als doeltaal. Je kunt één eigen zin of maximaal tien eigen
zinnen per batch schrijven, of je eigen JSON/CSV openen. De bron-/doeltaal
kan ook handmatig worden omgedraaid. De invoer is uitsluitend een door jou
zelf geschreven tekstpaar; Tuddy controleert de vertaling niet en voegt
geen ASR-, MT- of TTS-model toe.

De codes `kmr`, `ckb` en `ps` zijn alleen toegestaan in deze menselijke,
tijdelijke correctie-/phrasebookcatalogus. Ze worden **niet** toegevoegd
aan de ML Kit-tekstdoelen of Android-spraakondersteuning, en veranderen de
live vertaalroute niet. Oefenen blijft tekstueel voor een van deze talen
omdat een lokale stem daarvoor niet bevestigd is. Correcties blijven in
RAM tot het gesprek wordt gewist; alleen een bewuste JSON/CSV/HTML-export
kan een gekozen kopie bij een door de gebruiker aangewezen provider bewaren.

Dit is een bruikbare handmatige phrasebook-uitweg voorbij de vijf vaste
woorden, maar **geen vrije automatische vertaling of spraakherkenning**.
Issue #2 blijft open totdat de drie talen een rechtengecontroleerde,
Android-geïntegreerde offline ASR+MT-route hebben met native-speakerreview
en echte vliegtuigmodusacceptatie.

## Kurmanji/Sorani/Pasjtoe vinden zonder ondersteund ASR-/MT-model

Wanneer je in de **vertaaldoeltaalzoeker** een specifiek Kurmanji-, Sorani- of
Pasjtoewoord of de bijbehorende taalcode typt, toont Tuddy geen fictieve
automatische taalondersteuning. In plaats daarvan kun je direct kiezen:

- **Open 5 basisuitdrukkingen:** open het aparte offline paneel met vijf
  ingebouwde vaste uitdrukkingen voor de gevonden taal.
- **Zoek mijn eigen offline zinnen:** open uitsluitend het zelf opgeslagen
  privézinnenboek voor de gevonden taal, met door de gebruiker geschreven
  betekenissen. Lege resultaten worden niet automatisch ingevuld.

Beide opties stoppen eventueel lopende microfoonopname voordat een
offline-presentatie wordt geopend. Het kiezen van de snelkoppeling verandert
de actieve ML Kit- of ASR-taalkeuze niet, downloadt niets en maakt geen
free-form offline model actief. Voor de volledige ondersteunde dialecten,
modelrechten, vertaalvalidatie en fysieke toestelacceptatie blijven issues
#1 en #2 open. Zelfgeschreven vertalingen zijn niet geverifieerd voor
medische, juridische of andere hoogrisicotoepassingen.

## Generiek Koerdisch: geen dialect aannemen

Zoek je alleen op **Koerdisch**, **Kurdish**, **kurdi** of generieke Koerdische
schriftvormen zoals **کوردی** of **کردی**, dan biedt Tuddy **twee duidelijk
gelabelde offline-routes**: Kurmanji en Sorani. Elk heeft afzonderlijk de vijf
vaste basisuitdrukkingen en de eigen opgeslagen-zinnenroute.
De app kiest dus niet ongemerkt Sorani omdat de tekst in Arabisch schrift
geschreven is. Zoek je expliciet op **Kurmanji/kmr** of **Sorani/ckb**, dan
wordt alleen die concrete dialectoptie aangeboden. Ook hier worden geen
ML Kit-taalcodes geactiveerd of nieuwe woorden vertaald.

