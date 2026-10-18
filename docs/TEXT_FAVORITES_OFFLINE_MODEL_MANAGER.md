# Offline tekstmodellen voor alle favoriete taalrichtingen

Ga naar **⇄ Tweeweg-tekstgesprek → ★ Favorieten →
✓ Check alle favorieten · offline modellen…**.

Deze nieuwe beheerweergave leest de actuele, lokaal op het toestel
gedownloade ML Kit **tekstvertaal**modellen voor elk van maximaal zes
bewaarde brontaal/doeltaalparen. De controle gebeurt achtereenvolgens
zonder download of verzending van gesprekstekst. Een favoriet is slechts
een taalcodekeuze, geen modelbeschikbaarheidsverklaring.

Met **Bereid ontbrekende modellen via wifi voor…** opent een
afzonderlijk bevestigingsscherm met de concrete lijst van maximaal
zes taalparen. De gebruiker beslist pas **daar** om ontbrekende
modellen via wifi voor te bereiden.

- Elk paar wordt vlak vóór een mogelijke download opnieuw lokaal
  gecontroleerd. Een al compleet offline paar wordt overgeslagen.
- Ontbrekende modellen worden serieel voorbereid via de bestaande
  `TypedConversationModelController.prepareViaWifi()`, die
  `DownloadConditions.requireWifi()` gebruikt.
- Na iedere download wordt het daadwerkelijke lokale modelbezit
  opnieuw gecontroleerd. Alleen `READY` telt als klaar.
- Bij onbekende status of mislukte download stopt de resterende reeks:
  geen automatische nieuwe downloads na een fout.
- Sluiten maakt alle nog uitstaande callback-vervolgstappen ongeldig.
  Een download die ML Kit al in behandeling heeft genomen, kan
  desondanks later afronden.
- De status en voortgang blijven alleen in de geopende Compose-sessie.
  Geen transcript, gebruikte tekst, modelstatus of downloadlijst
  wordt als eigen app-telemetrie opgeslagen.

Dit is specifiek voor de bestaande, door ML Kit ondersteunde
**tekst**talen. Deze functie installeert of controleert geen
Android **spraakherkennings**pakket en is geen bevestiging dat
vliegtuigmodus, microfoon of vertaalnauwkeurigheid op een fysiek
toestel zijn geaccepteerd.

**Kurmanji, Sorani en Pasjtoe** blijven buiten de lijst van
ondersteunde ML Kit-teksttalen. Volledige gelicentieerde on-device
ASR+MT, native-speakerreview, fysieke Android 12+ acceptatie en
productieondertekening zijn onafhankelijke releasegates.

Er zijn geen nieuwe app-permissies, cloudspraakfallbacks, nieuwe
QA-suites of handmatige QA-rondes toegevoegd.
