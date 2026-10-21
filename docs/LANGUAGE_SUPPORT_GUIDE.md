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
- Een vaste woordenlijst is geen vrije vertaling en bewijst geen ASR.
- Volledige Kurmanji/Sorani/Pasjtoe-ondersteuning vereist een geschikt gelicentieerd on-device model, native-speakerbeoordeling en fysieke vliegtuigmodusacceptatie.

De gids is informatief en doorzoekbaar; zij vervangt geen toestelcontrole, modellicentiecontrole of native-speakerbeoordeling.
