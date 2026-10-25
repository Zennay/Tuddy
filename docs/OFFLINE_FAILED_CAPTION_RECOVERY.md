# Mislukte offline vertalingen herstellen

## Waarom

Een lokale Android-spraakherkenning kan een tijdelijke caption opleveren
terwijl het ML Kit-tekstmodel voor die exacte taalrichting nog ontbreekt of
vertaling mislukt. Tuddy bewaart dan de oorspronkelijke regel in het
tijdelijke gesprek, met status **Model ontbreekt** of **Fout**. De gebruiker
hoeft die oorspronkelijke zin niet opnieuw in te spreken.

## Geen permanent vastgelopen vertaalregels

De lokale tekstvertaling heeft nu één **30-seconden-bovengrens** vanaf
het starten van een vertaalpoging. Die limiet geldt gezamenlijk voor
eventuele teksttaaldetectie, controle van reeds geïnstalleerde ML Kit-
modellen en de eigenlijke vertaaluitkomst. Een Android- of ML Kit-dienst
die geen callback terugstuurt, mag een ondertitel niet voor altijd op
**Vertalen…** laten staan.

Bij het verstrijken verandert uitsluitend de nog ongewijzigde,
tijdelijke regel naar **Fout** met een bericht dat de vertaling lokaal
te lang duurde. Het is nadrukkelijk *geen bewering* dat het model
ontbreekt of dat een vertaald resultaat inhoudelijk incorrect is.
De regel houdt zijn oorspronkelijke tekst en taalrichting. Een laat
aangeleverd resultaat van de oude poging wordt via de verhoogde
revisie geweigerd; het kan een nieuwe poging of menselijke wijziging
niet overschrijven. Een voltooid resultaat beëindigt meteen de
timer. Wissen, uit de geschiedenis verwijderen, oude regels wegwerpen
of de Activity verlaten ruimt de bijbehorende timers op.

De bestaande **↻ Probeer opnieuw**-knop herstart bewust één mislukte
regel, ook als je eerder de **oorspronkelijke tekst** zelf hebt
gecorrigeerd. Handmatig vastgestelde vertalingen worden hiermee
niet overschreven: alleen regels met status **Fout** of
**Model ontbreekt**, waarvan de huidige versie op het scherm
niet veranderd is, kunnen worden herstart. De herstelgroep voor
niet-gecorrigeerde regels blijft daarnaast bestaan.

## Herstel vanuit het gewone gesprek

1. De knop **Herstel N mislukte offline vertalingen…** verschijnt onder de
   gesprekshulpmiddelen als het actuele tijdelijke gesprek herstelbare fouten
   bevat. Hij is uitgeschakeld tijdens luisteren of modelbeheer.
2. Het paneel groepeert uitsluitend **Model ontbreekt** en **Fout** naar de
   *oorspronkelijke* bron- en doeltaalcodes van iedere regel; het actuele
   microfoontaalpaar hoeft dus niet hetzelfde te zijn.
3. Tik bewust bij één taalrichting op **Controleer lokaal en herstel maximaal
   6**. Er volgt eerst één **read-only** controle van reeds op het toestel
   geïnstalleerde ML Kit-tekstmodellen. Ontbreekt het model of is de status
   onbekend, dan wordt geen regel aangeraakt en opent Tuddy geen download.
   Voor bewuste voorbereiding via wifi bestaat **Offline startcheck**.
4. Na bevestigde beschikbaarheid worden hoogstens **zes ongewijzigde
   mislukte regels** van die taalrichting in hun bestaande tijdelijke
   geschiedenis opnieuw aangeboden aan dezelfde lokale vertaalpipeline.
   Bewerkte tekst, andere taalrichting, verwijderde regels, geslaagde
   vertalingen en handmatig gecorrigeerde regels worden niet vervangen.
   Bij een nieuwe poging verandert de interne vertaalrevisie, zodat oudere
   asynchrone resultaten de nieuwe poging niet kunnen overschrijven.
5. De melding geeft aan hoeveel regels opnieuw zijn aangevraagd, niet
   hoeveel daarvan al geslaagd zijn. Een overblijvende mislukte regel kan
   later na een nieuwe modelcontrole opnieuw worden geprobeerd.

De bestaande één-regel-knop **↻ Probeer opnieuw** en de aparte
tweeweg-tekstherstelactie blijven bestaan. Ook die routes negeren
handmatig gecorrigeerde of verouderde regels.

## Rechtstreeks één ontbrekend taalpaar via wifi voorbereiden

De herstelweergave biedt per historische bron→doeltaal nu twee **afzonderlijke**
acties. **Controleer lokaal en herstel maximaal 6** blijft een uitsluitend
lokale, read-only modelcontrole met een expliciete hervertaalactie voor
ongewijzigde mislukte regels. Hij downloadt niets.

**Ontbrekende tekstmodellen via wifi voorbereiden…** opent eerst een
tweede, niet-automatische toestemmingsdialoog met het precieze historische
taalpaar. Pas na **Bevestig wifi-modelvoorbereiding**:

1. Controleert Tuddy of het bron- en doeltaalpaar onder de bestaande
   ondersteunde ML Kit-teksttaalcodes valt. Het actuele microfoontaalpaar
   wordt niet gewijzigd.
2. Leest Tuddy de reeds aanwezige tekstmodellen **zonder netwerkactie**
   uit. Bij `READY` meldt het scherm dat opnieuw downloaden niet nodig
   is. Bij `UNKNOWN` of `UNSUPPORTED` wordt niets gedownload.
3. Alleen bij aangetoonde `MISSING` maakt Tuddy een tijdelijke ML Kit
   vertaalklant voor dat **ene gekozen** taalpaar en vraagt met
   `requireWifi()` de ontbrekende modellen aan. Er loopt hoogstens één
   expliciete modelactie tegelijk.
4. Na download rapporteert Tuddy niet zomaar “gereed”: de app leest
   onafhankelijk opnieuw uit welke tekstmodellen werkelijk aanwezig zijn.
   Het resultaat wordt alleen in het herstelpaneel getoond zolang dat
   venster, die sessie en dezelfde aanvraag nog actueel zijn.
5. Oude ondertitels worden nooit automatisch vertaald. Je moet zelf
   **Controleer lokaal en herstel maximaal 6** kiezen. Gewijzigde
   gespreksregels krijgen geen ongevraagde vervanging.

Android/ML Kit kan een eenmaal begonnen modeldownload na het sluiten
van de app voortzetten. Daarom betekent **Sluiten** nadrukkelijk
**niet** “modeldownload geannuleerd”. Gespreksinhoud en audio worden
niet meegestuurd; voorbereiding betreft enkel ML Kit-tekstmodellen,
nooit Android-spraakmodellen of ongereviewde Koerdisch/Pasjtoe-gewichten.

## Privacy en acceptatie

Alles werkt alleen met de al gekozen tekst uit het tijdelijke gesprek,
zonder nieuw bestand, upload, automatisch modeldownloadverzoek of
cloudvertaler. Het herstelpaneel bewaart geen eigen kopie en kiest nooit
automatisch een nieuwe taal voor bestaande regels. Een voltooide
hervertaling is nog steeds een *lokale modeluitkomst*, geen door een
moedertaalspreker gecontroleerde vertaling.

De broncode en bestaande CI-build kunnen deze flow integreren maar vormen
geen fysieke Android-vliegtuigmodusproef. De afzonderlijke acceptatie voor
Android 12+, volledig rechtengecontroleerde Kurmanji/Sorani/Pasjtoe-ASR+MT,
getekende APK en productie-toelating blijven open.
