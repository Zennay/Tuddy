# Tekst uit zelfgekozen afbeelding · lokaal (Android 12+)

Begin op één van twee manieren:

- **Vanuit Android Galerij/Foto's/Bestanden:** deel bewust één foto of
  screenshot via **Delen → Tuddy**. Je komt op een privévoorvertoning.
  Tik apart **Herken gedeelde afbeelding · lokaal** om OCR te starten;
  alleen openen leest nog geen foto. Bij ontbrekende URI-rechten of meerdere
  gedeelde items kun je de eigen afbeeldingkiezer gebruiken.
- **Vanuit Tuddy:** open **Meer functies → Lees tekst uit afbeelding ·
  volledig lokaal** en kies bewust één foto in Androids fotokiezer.

De fotokiezer verleent toegang tot die ene keuze, niet tot je hele fotobibliotheek.
Sommige door Android aangeboden fotoproviders beheren ook cloudfoto's:
selecteer een lokaal aanwezige afbeelding als je iedere externe overdracht
wilt uitsluiten en controleer de toestand van je toestel zelf.

Een gedeelde foto kan van een cloud-gesynchroniseerde externe fotoprovider
komen. De provider kan zelf netwerkverkeer veroorzaken voordat Tuddy de
tijdelijke URI ontvangt: de offlinegarantie geldt voor Tuddy's verwerking,
niet voor iedere externe bronapp.

Tuddy gebruikt het **in de APK gebundelde ML Kit Latin Text Recognition v2**
model. De OCR-functie kan daardoor tekst uit een afbeelding halen zonder
een modeldownload op het toestel, nieuwe camera-/fotopermissies, opname
of Tuddy-netwerkverzoek. We ondersteunen hier alleen **Latijns schrift**;
geen betrouwbare Sorani, Arabische-schrift Kurmanji of Pasjtoe OCR-claim.

## Van foto naar vertaling

1. Kies een afbeelding of bevestig expliciet de gedeelde afbeelding.
   Tuddy leest alleen de gekozen content-URI en
   verkleint te grote pixelafmetingen vóór verwerking. Ongeldige, zeer grote
   of onleesbare afbeeldingen worden geweigerd.
2. Bekijk de lokaal herkende tekst. Verkeerde woorden kun je zelf verbeteren.
   Voorlopige OCR-tekst blijft in het geheugen, is beperkt tot 8.000 tekens
   en krijgt een zichtbare tijdslimiet van 20 seconden.
3. Tik **Controleer taal en vertaal lokaal…**. Pas dan gaat de gecontroleerde
   tekst via een eenmalige, 15 seconden geldige **RAM-only**-handoff naar
   Tuddy's bestaande snelle vertaler. De Intent bevat alleen een token,
   nooit de foto of OCR-tekst.
4. Controleer bron- en doeltaal. **Vertaal lokaal** is een aparte handeling
   en gebruikt alleen reeds geïnstalleerde lokale ML Kit-tekstmodellen.
   Bij ontbrekende modellen blijft de vertaling dicht, zonder cloudfallback.
   Anders dan bij Androids tekstselectie is er **geen bronapp** om tekst
   stilzwijgend in terug te schrijven.

Achtergrondzetten, verlaten of een nieuwe afbeelding kiezen wist de
voorvertoning; het scherm bewaart geen afbeeldingen of transcripties.
Een reeds gestarte native OCR-taak kan nog uitlopen, maar verouderde
callbacks mogen geen tekst meer tonen of overdragen.

De bestaande optionele screenshotschild-instelling geldt ook voor het
OCR-scherm. Bij de eerste weergave herstelt Tuddy de Android-windowbescherming
voordat privé-inhoud verschijnt. De systeemfotokiezer en externe camera's
vallen buiten Tuddy's screenshotschild.

Dit is een nieuwe optionele afbeeldingstekstfunctie, **geen** bewijs van
fysieke Android 12+-vliegtuigmodusacceptatie of complete offline Kurmanji,
Sorani en Pasjtoe spraak-/vertalingsmodellen. Issue #1/#2 en alle signing,
native-speaker- en release-gates blijven open. Toestelgedrag en de
gebruikte exact-main APK moeten afzonderlijk fysiek worden beoordeeld.
