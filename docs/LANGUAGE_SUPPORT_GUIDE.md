# Taalondersteuning in Tuddy

Open in de app **Taal- en modelinstellingen → Bekijk alle taalondersteuning**. Het overzicht is doorzoekbaar op taalcode, Engelse of eigen naam, Nederlandse naam en veelgebruikte aliassen; accenten zijn niet vereist.

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

## Modelonderzoek: concrete kandidaten, nog geen ondersteuning

De taalondersteuningdialoog toont nu externe modelkaarten voor drie ASR-kandidaten. Ze zijn **niet** ingebouwd, gedownload of op Android gevalideerd:

| Taal | ASR-onderzoekskandidaat | Modelbestand / licentie volgens kaart | Open punt |
|---|---|---|---|
| Kurmanji | [amedcj/whisper-kurmanji](https://huggingface.co/amedcj/whisper-kurmanji) | circa 967 MB; Apache-2.0 vermeld | Trainingsdatarechten, dialectnauwkeurigheid, Android-runtime en geheugen nog niet geverifieerd |
| Sorani | [roshna-omer/whisper-small-Kurdish-Sorani](https://huggingface.co/roshna-omer/whisper-small-Kurdish-Sorani) | circa 967 MB; Apache-2.0 vermeld | Trainingsdatarechten, Sorani-variantdekking, Android-runtime en geheugen nog niet geverifieerd |
| Pasjtoe | [Sabtain-Dev/STT-Whisper-Pashto](https://huggingface.co/Sabtain-Dev/STT-Whisper-Pashto) | MIT vermeld; bestandsgrootte nog niet vastgesteld | Dataset-/gewichtenherkomst, dialectdekking en Android-runtime nog niet geverifieerd |

### Waarom de vertaalroute nog geblokkeerd is

- [Meta NLLB-200 distilled 600M](https://huggingface.co/facebook/nllb-200-distilled-600M) dekt de relevante taalcodefamilies, maar de gepubliceerde gewichten zijn circa 2,46 GB en de licentie is **CC-BY-NC-4.0**. Dat is geen vrijgaveklare kandidaat voor commercieel gebruik of een kleine Android-pack.
- De [ZamAI Pashto-English translator](https://huggingface.co/tasal9/ZamAI-Pashto-Translator-FacebookNLB-ps-en) vermeldt Apache-2.0 voor de afgeleide repository, maar noemt NLLB-200 3.3B als basismodel; de [upstream modelkaart](https://huggingface.co/facebook/nllb-200-3.3B) vermeldt CC-BY-NC-4.0. Licentiecompatibiliteit moet eerst worden opgelost.
- Een licentielabel van een modelkaart is geen onafhankelijke audit van de dataset, gewichten, tokenizer en alle upstreamcomponenten. Er is geen modeldownload of externe runtime in Tuddy toegevoegd.

**Volgende productgate:** selecteer per dialect een model met controleerbare herkomst en geschikte licentie, maak een realistische gequantiseerde Android-pack, integreer echte lokale ASR én MT, en pas daarna native-speakerreview en geautoriseerde fysieke vliegtuigmodusacceptatie toe. Tot dat moment blijft de app de drie talen als preview-only markeren.

## Scriptrichting bij eigen Sorani- en Pasjtoe-zinnen

De privé-zinnenlijst en de import-/verwijderpreview tonen eigen Sorani (`ckb`) en Pasjtoe (`ps`) tekst van rechts naar links. Het invoerveld, de oefenpoging en de betekenis die de gebruiker in Nederlands of Engels zelf schrijft blijven respectievelijk gericht op de geselecteerde taal en links-naar-rechts. Deze opmaak verandert geen tekst, translitereert niets en controleert geen spelling of vertaling.

## Automatische tekstrichting voor gemengde schriften

De algemene ondertitel-plakbewerker kiest nu de schrijfrichting op basis van het eerste Unicode-teken met een sterke links- of rechtsrichting, niet op basis van de telefooninstelling. Dat geldt voor de brontekst en de zelfgeschreven betekenis; het grote live-ondertitelkaartje gebruikt dezelfde regel met de herkende taalcode alleen als terugval voor tekst zonder richtingsinformatie. Tekst wordt niet herschreven of getranslitereerd.
