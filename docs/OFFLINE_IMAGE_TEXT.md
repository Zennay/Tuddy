# Tekst uit zelfgekozen afbeelding · lokaal (Android 12+)

Begin op één van twee manieren:

- **Vanuit Android Galerij/Foto's/Bestanden:** deel bewust één foto of
  screenshot via **Delen → Tuddy**. Je komt op een privévoorvertoning.
  Tik apart **Herken gedeelde afbeelding · lokaal** om OCR te starten;
  alleen openen leest nog geen foto. Bij ontbrekende URI-rechten of meerdere
  gedeelde items kun je de eigen afbeeldingkiezer gebruiken.
- **Vanuit Tuddy:** open **Meer functies → Lees tekst uit afbeelding ·
  volledig lokaal** en kies bewust één foto in Androids fotokiezer.

- **Nieuwe directe cameraoptie:** kies in hetzelfde scherm **Maak foto met
  Android-camera**. Je toestel opent zijn eigen camera-app, zonder dat
  Tuddy zelf cameratoegang of een camera-/fotopermissie aanvraagt.
  Tuddy ontvangt enkel de **tijdelijke, kleinere Bitmap-voorvertoning**
  van die ene opname, geen fotobestand of galerijrechten.
  Bekijk de voorvertoning eerst: tik daarna bewust op **Herken deze
  camerafoto · lokaal** of op **Verwerp camerafoto**. Het openen van de
  camera alleen start dus **geen** OCR, modelcontrole of vertaling.
  Alleen na de tweede toestemming begint dezelfde begrensde, ingebouwde
  offline Latin OCR als voor een geselecteerde afbeelding.

  Dit is bewust een opnamesnelkoppeling met mogelijk **lagere resolutie**,
  niet een claim over volwaardige fotoscans of gegarandeerde
  OCR-nauwkeurigheid. Voor kleine letters, documenten of een complete
  pagina is de fotokiezer met een scherpe, bestaande afbeelding beter.
  Als Android geen camera heeft, geen voorvertoning teruggeeft of een
  te grote/ongeldige Bitmap levert, kan de gebruiker terugvallen op de
  eigen foto-/bestandskeuze. De externe camera-app bepaalt zelf
  eventuele opslag of cloudsynchronisatie: Tuddy slaat zelf **geen**
  camerafoto op en deelt er geen.

  De nog niet beoordeelde camerafoto vervalt bij naar de achtergrond
  gaan, sluiten, een nieuwe keuze of een systeemonderbreking. Na
  toestemming doorloopt de foto dezelfde OCR-tekstblokselectie,
  handmatige tekstcorrectie en eenmalige RAM-only-handoff naar de
  offline vertaler.


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
2. Bekijk de afzonderlijke lokaal herkende tekstblokken. Bij een korte
   afbeelding (samen maximaal 8.000 tekens) zijn de blokken vooraf
   aangevinkt, maar niets wordt automatisch vertaald. Bij een langer
   document blijft de selectie leeg: tik alleen de relevante stukken aan.
   Je kunt maximaal **8.000 tekens totaal** selecteren. De knoppen
   **Vul selectie aan** en **Wis blokselectie** helpen bij het kiezen.
   Als je de selectie wijzigt, verschijnt opnieuw de letterlijk herkende
   tekst; eventuele eerdere handmatige edits in het tekstveld worden
   vervangen. Controleer na iedere wijziging de samengestelde tekst.
3. Verbeter zelf verkeerde woorden in het bewerkbare tekstveld. De app
   kan maximaal **100 OCR-tekstblokken / 80.000 ruwe OCR-tekens**
   tijdelijk tonen; een afzonderlijk blok van meer dan 8.000 tekens
   moet je via een kleinere afbeeldingsuitsnede verwerken.
   De afbeelding wordt maximaal 20 seconden verwerkt. Te grote,
   ongeldige of niet veilig te lezen resultaten worden geweigerd,
   niet stilzwijgend afgekapt. Alleen de gekozen 8.000-tekenselectie
   kan aan de vertaler worden aangeboden.
4. Tik **Controleer taal en vertaal lokaal…**. Pas dan gaat de gecontroleerde
   tekst via een eenmalige, 15 seconden geldige **RAM-only**-handoff naar
   Tuddy's bestaande snelle vertaler. De Intent bevat alleen een token,
   nooit de foto of OCR-tekst.
5. Controleer bron- en doeltaal. **Vertaal lokaal** is een aparte handeling
   en gebruikt alleen reeds geïnstalleerde lokale ML Kit-tekstmodellen.
   Bij ontbrekende modellen blijft de vertaling dicht, zonder cloudfallback.
   Anders dan bij Androids tekstselectie is er **geen bronapp** om tekst
   stilzwijgend in terug te schrijven.

Achtergrondzetten, verlaten of een nieuwe afbeelding kiezen wist de
blokselectie, eventueel bewerkte tekst en voorvertoning; het scherm
bewaart geen afbeeldingen, OCR-blokken of transcripties.
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
