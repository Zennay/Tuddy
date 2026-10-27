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

## Geen stapeling van zware OCR-taken bij time-out of schermwissel

Een OCR-scan heeft al een zichtbare **20-secondenlimiet**. Die limiet
betekent echter niet dat de onderliggende Android/ML Kit-herkenning
daadwerkelijk is geannuleerd; de provider kan nog een afbeelding in
geheugen verwerken wanneer je een nieuwe kiest of het scherm sluit.

Tuddy laat daarom nog maar **één echt ingediende OCR-taak tegelijk
binnen dit app-proces** toe. De grens geldt ook wanneer een
Activity opnieuw wordt aangemaakt. Pas wanneer ML Kit een echte
voltooiingscallback terugstuurt (zowel succes als fout) komt de plek
vrij. Verlaat je het scherm of is de 20-secondenlimiet verlopen,
dan verdwijnen de oude tekst en voorvertoning direct zoals voorheen,
maar een nieuwe zware scan wordt duidelijk **geweigerd zolang de
oude taak nog actief is**. Dat voorkomt herhaald aanmelden van
grote afbeeldingen op een krap toestel, zonder te veinzen dat de
oude verwerking fysiek gestopt is.

Als de herkenning nog bezig is, krijg je de melding dat de vorige
offline-afbeeldingstaak eerst moet afronden. Ook als Android zelf
een **laag-geheugensignaal** geeft, begint er geen nieuwe scan.
Een bestand-leesfout of annulering voordat ML Kit echt aan de slag
ging, geeft de plek meteen vrij. Alle toelatingsinformatie is
uitsluitend anoniem en tijdelijk in RAM; de grens slaat **geen
beelden, OCR-tekst, URI's of gebruikersinformatie** op. Wanneer een
provider definitief geen callback levert, blijft die ene plek
gesloten tot de app opnieuw wordt gestart. Dat is een bewuste
fail-closed grens, geen bewijs van een geslaagde scan.

## Van gecontroleerde foto naar tekstbestand, klembord of Android-delen

De afbeeldingstekst hoeft niet eerst vertaald te worden om bruikbaar te
zijn. Na een expliciete OCR-actie, blokselectie, leesvolgorde en eventuele
handmatige correctie verschijnen drie **afzonderlijke** acties naast
`Controleer taal en vertaal lokaal…`:

- **Kopieer gecontroleerde tekst:** kopieert exact de toegelaten,
  nagekeken OCR-tekst als `text/plain` naar Androids klembord, met
  Androids gevoelige-inhoudmarkering. De gedeelde Tuddy-instelling
  `Uit / 1 minuut / 5 minuten` kan daarna **best-effort** proberen
  een nog ongewijzigd Tuddy-clip te wissen. Andere apps en
  klembordgeschiedenis kunnen tekst meteen behouden; dit is geen
  gegarandeerde verwijdering of privacy van Androids klembord.
- **Bewaar als .txt via Android…:** opent Androids eigen **bestand
  maken**-dialoog, met de neutrale voorgestelde naam
  `Tuddy-afbeeldingstekst.txt`. Alleen na jouw bevestiging van een
  doelmap schrijft Tuddy uitsluitend de gecontroleerde tekst als
  UTF-8 naar de geselecteerde document-URI. De gekozen foto zelf wordt
  niet meegeschreven. Een geannuleerde keuze maakt geen bestand vanuit
  Tuddy. Alleen zolang de bestandskiezer openstaat, bewaart de app één
  tijdelijke, expliciet goedgekeurde tekstkopie in RAM om de gekozen
  uitvoer te kunnen schrijven; die verdwijnt na het antwoord of bij
  vernietiging van het scherm. De externe provider kan het bestand later
  zelf synchroniseren.
- **Deel tekst via Android…:** vraagt eerst nogmaals expliciet of je
  de tekst **buiten Tuddy** wilt brengen. Alleen met `Open
  Android-deelmenu` krijgt de door jou gekozen ontvangende app een
  gewone `text/plain`-Intent met de tekst. Een ontvanger kan de
  tekst bewaren of uploaden. Een afgebroken bevestiging deelt niets.

Alle drie de uitgangen hergebruiken dezelfde bounded Unicode-beveiliging:
maximaal 8.000 UTF-16-tekens, geen ongeldige surrogaatparen,
gevaarlijke controlesymbolen of expliciete bidi-overrides. Onveilige
tekst wordt **geweigerd**, niet stilzwijgend ingekort. Er is geen nieuw
Android-recht, achtergrondbestand, server-endpoint, modeldownload of
automatische export toegevoegd. De vertaalroute blijft de bestaande
einmalige RAM-handoff en is volledig onafhankelijk van de drie
uitgaande Android-acties.

## Geselecteerde afbeeldingstekst lokaal terugluisteren

Na OCR en de handmatige blokselectie kun je je **gecontroleerde tekst
lokaal laten voorlezen** zonder een vertaling, kopie, Android-export of
netwerkactie. Kies eerst de taal waarin de tekst daadwerkelijk geschreven
is (Nederlands, Engels, Duits, Frans of Spaans) en tik daarna bewust op
**Lees na · normaal** of **Lees na · langzaam**. Die keuze is een
gebruikersinstelling voor deze tijdelijke OCR-weergave, geen automatisch
herkende taal of bewijs van een beschikbare stem.

De voorleesactie accepteert maximaal **2.400 tekens** van de actuele,
veilig gevalideerde OCR-tekst en deelt deze in ten hoogste 12 stukken
van maximaal 360 tekens op woordgrenzen. Een te lange tekst of een
afzonderlijk woord dat niet veilig past, wordt **geweigerd**: Tuddy
speelt niet stilzwijgend alleen de eerste helft af. Selecteer minder
OCR-blokken of kort het tekstveld zelf in en probeer opnieuw.
Het paneel toont de voortgang en biedt **Stop lokaal voorlezen**.

De bestaande `LocalCaptionSpeaker` staat uitsluitend al geïnstalleerde,
door Android als niet-netwerkafhankelijk gemelde TTS-stemmen toe. De
gedeelde Android-audiofocus- en headsetbewaking stopt de uitvoer bij
gemelde route- of focuswijzigingen; deze best-effort metadata garandeert
niet dat op een echt toestel nooit luidsprekeraudio hoorbaar is.
De gekozen tekst blijft in de RAM-only OCR-weergave. Er worden geen
audiobestanden, tekstvoorkeuren of inhoudslogs bewaard, en er wordt
geen offline model automatisch gedownload.

**Tekst wijzigen, OCR-blokken selecteren/verplaatsen, een nieuwe
afbeelding openen, het scherm verlaten of naar de achtergrond gaan**
stopt het voorlezen en laat late Android-TTS-callbacks niets meer
wijzigen. Dit is een luisterhulpmiddel voor de beoordeelde oorspronkelijke
tekst, geen vertaal- of herkenningsnauwkeurigheidsclaim en geen bewijs
van de nog vereiste fysieke Android 12+-vliegtuigmodusproef.

## Veilig corrigeren, terugdraaien en letterlijk zoeken/vervangen

Na OCR vormt Tuddy één tijdelijke correctiewerkruimte van de door jou
geselecteerde blokken, hun leesvolgorde en het handmatig bewerkte tekstveld.
**Ongedaan** en **Opnieuw** herstellen de volledige voorgaande combinatie;
dus ook een per ongeluk verwijderde blokselectie, omgewisselde volgorde
of overschreven handmatige correctie. De knoppen blijven beschikbaar als
je alle blokken hebt gedeselecteerd. Snelle toetsaanslagen zijn gegroepeerd
in één herstelbare bewerkingsstap; maximaal **16** versies worden uitsluitend
in het werkgeheugen gehouden. Een nieuwe foto, verlaten of naar de
achtergrond gaan wist de hele geschiedenis. Niets gaat naar opslag,
Androids saved instance state, de tekstvertaler of het klembord zonder
je eigen aparte actie.

Om een veelgemaakte OCR-fout in meerdere woorden tegelijk te corrigeren
gebruik je **Zoek letterlijk** en **Vervang met**. Tuddy gebruikt
hoofdlettergevoelige, letterlijke matches (geen regex of automatisch
genereren). De knop toont eerst het aantal treffers en de nieuwe
tekstlengte. Pas na **Pas bewust toe** verandert de huidige tekst.
Er zijn grenzen van 120 tekens per zoek/vervangveld, 100 treffers en
8.000 tekens totale beoordeelde tekst. Lege vervangtekst verwijdert
alleen de expliciet gezochte fragmenten. Ongeldige, te lange of
verouderde voorstellen worden niet toegepast en je kunt de laatste
toegepaste correctie weer ongedaan maken.

Elke tekstbewerking en herstelactie stopt eventuele lopende lokale
voorleesuitvoer meteen. Vertalen, voorlezen, opslaan, kopiëren en delen
blijven aparte expliciete acties op de actuele tekst. Het originele
beeld wordt niet bewerkt. Controleer het resultaat altijd zelf:
OCR-herkenning en zoeken/vervangen zijn geen bewijs van tekstkwaliteit
of volledige Android-vliegtuigmodusacceptatie.

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
   Voor een meerkolomsdocument of kassabon bepaalt Androids OCR niet
   altijd de juiste volgorde. Gebruik per blok **↑ Omhoog** of
   **↓ Omlaag** om een expliciete leesvolgorde te maken, zonder
   ongevraagde tekstcorrectie of netwerkwerk. De getoonde positie is
   de huidige leespositie; het oorspronkelijke OCR-bloknummer blijft
   zichtbaar. **Herstel oorspronkelijke OCR-volgorde** zet de volgorde
   terug zonder de huidige blokselectie te wissen.
   Als je de selectie of volgorde wijzigt, verschijnt opnieuw de
   letterlijk herkende tekst in precies die volgorde; eventuele eerdere
   handmatige edits in het tekstveld worden vervangen. Controleer na
   iedere wijziging de samengestelde tekst en pas pas daarna woorden aan.
   De limiet van 8.000 tekens geldt ook na herschikken.
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
blokselectie, gekozen leesvolgorde, eventueel bewerkte tekst en
voorvertoning; het scherm
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
