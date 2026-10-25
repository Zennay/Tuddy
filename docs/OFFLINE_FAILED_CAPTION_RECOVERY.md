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

## Een lopende vertaling zelf stoppen

Een gesprek met meerdere lokale spraakregels of tekstzinnen kan tijdelijk
**Bezig / Vertalen…** tonen. Je hoeft niet tot de 30-seconden-time-out
te wachten als je zo'n verwerking niet meer wilt laten weergeven.

- Kies bij één **Bezig**-regel expliciet **■ Stop alleen deze vertaling**.
  Alleen precies de zichtbare, nog ongewijzigde versie van die zin wordt
  op **Fout** gezet met de melding dat jij haar hebt gestopt. De
  originele tekst, taalrichting en jouw eerdere correcties blijven
  bewaard; er wordt niets automatisch opnieuw vertaald.
- In de gespreksfuncties staat, zolang er verwerking loopt, de knop
  **Stop de N lopende vertalingen…**. Die opent eerst een afzonderlijke
  bevestiging. Bij openen onthoudt Tuddy uitsluitend de regel-ID's en
  revisies van de op dat moment lopende vertalingen. Bij bevestigen
  worden alleen regels gestopt waarvan exact dezelfde revisie nog
  **Bezig** is. Inmiddels afgeronde, bewerkte of nieuwe zinnen worden
  niet meegenomen.
- Bij een bulkstop worden eerst **alle gekozen regels** ongeldig gemaakt
  en pas daarna hun wachtrijplaatsen en tijdelijke time-outs vrijgegeven.
  Daarom kan het leegmaken van de FIFO geen van die gekozen regels
  onbedoeld opnieuw starten. De microfoon blijft naar jouw bestaande
  keuze werken; deze knop activeert of pauzeert de spraakherkenning niet.
- Android ML Kit heeft niet altijd een intrekbaar, al begonnen
  vertaalverzoek. Oud Android-modelwerk kan achter de schermen nog
  afronden; het resultaat wordt door de verhoogde regelrevisie
  genegeerd. Een gebruikersstop mag niet als een modeldefect of
  bewezen afgebroken netwerktaak worden gerapporteerd.
- Wil je een gestopte zin alsnog lokaal vertalen? Tik bij die
  zin zelf opnieuw op **↻ Probeer opnieuw**. Het model moet nog
  steeds lokaal geïnstalleerd zijn; er is geen automatische download,
  cloudfallback, gespreksopslag of sessiebreed verborgen herstel.

## Veel zinnen tegelijk: veilige, begrensde offline verwerking

Wanneer Android meerdere definitieve gesprekszinnen kort na elkaar
oplevert, controleert Tuddy voortaan slechts **eenmaal tegelijk**
welke al geïnstalleerde tekstmodellen beschikbaar zijn per exacte
bron-/doeltaalrichting. Regels met een ander taalpaar krijgen hun
eigen controle. Het resultaat wordt uitsluitend aan de op dat
moment wachtende tijdelijke regels gegeven: **geen permanente
model-READY-cache**, en geen stilzwijgende download of netwerkvertaler.
Als iemand via de bestaande modelbeheerfunctie modellen voorbereidt
of verwijdert, worden eerdere controles ongeldig en volgt een
herstelbare foutmelding in plaats van een ongeverifieerde 'klaar'-status.

Daarna laat Tuddy maximaal **drie bijgehouden ML Kit-vertalingen
tegelijk** starten; extra zinnen komen in een volgordevaste tijdelijke
wachtrij. De zichtbare teller in het gespreksvenster toont hoeveel
regels momenteel in behandeling zijn. Een verloren model- of
vertaalcallback mag geen permanent vastgelopen wachtrij maken:
de bestaande 30-secondenlimiet omvat óók het wachten. Na een
timeout blijft de oorspronkelijke zin staan en werkt **↻ Probeer
opnieuw**. De app kiest niet zelf voor een andere taal, begint
geen download en stuurt niets naar een online vertaaldienst.

Een gebruikerscorrectie, verwijderde regel, oudste regel die door
de 500-regelslimiet vervalt of het bewust wissen van het gesprek
haalt desbetreffend werk uit de wachtrij. Een al aan Android ML Kit
overgedragen taak kan niet altijd worden ingetrokken; een laat
resultaat wordt daarom door de bestaande generatie- en
revisiecontroles geweigerd. In-flight controle en planning bevatten
alleen tijdelijke appgegevens; er wordt geen gesprek naar schijf
geschreven.

## Werkelijk gestarte ML Kit-taken blijven begrensd na Stop of timeout

De FIFO met maximaal drie actieve *zichtbare* vertalingen telt niet wat
Android intern nog verwerkt. Bij **Stop deze vertaling**, wachtrijannulering,
een nieuwe poging of na de **30-seconden-time-out** kan een al aan ML Kit
overgedragen taak alsnog doorrekenen. De zichtbare status blijft
terecht geannuleerd, maar het toestel heeft de oude berekening daarmee
niet bewezen gestopt.

Daarom geldt nu daarnaast één afzonderlijke toestelgebonden limiet:
**maximaal drie echt ingediende, nog niet door ML Kit afgeronde
vertaaltaken**, ongeacht of ze voor een definitieve ondertitel of een
tijdelijk live-voorbeeld zijn gestart. Een plek wordt pas vrijgegeven
als ML Kit werkelijk een terminale callback afgeeft (of als het
aanmaken van de taak mislukte voordat er iets was ingediend).
Een geannuleerde, verlopen of gecorrigeerde zin geeft dus niet
stilzwijgend ruimte voor extra modeltaken. Onderliggende late
resultaten blijven door de bestaande versiecontroles ongeldig.

Tijdelijke tussentijdse spraakvoorbeelden zijn **optioneel**: bij een
volle limiet worden ze zonder extra werk overgeslagen, zodat een
definitieve zin niet voorrang verliest. Als een definitieve
vertaalpoging zelf geen fysieke plek meer heeft, toont Tuddy een
**herstelbare lokale fout** in die bestaande regel. De gebruiker
kan wachten totdat Android werkelijk klaar is, of Tuddy bewust
afsluiten en opnieuw openen als een provider langdurig onbereikbaar
blijft. Dat laatste is geen bewijs dat al ingediende Android-werkzaamheden
onmiddellijk zijn afgebroken.

Het hoofdscherm toont zolang nodig **lokale modeltaken: N/3**.
Het door de gebruiker geopende, tekst- en audiovrije
apparaatrapport vermeldt eveneens het aantal nog actieve taken
in RAM. Er wordt geen model gedownload, geen cloudfallback gestart
en niets over zinnen, audio of gebruikersidentiteit opgeslagen in
deze fysieke limietteller.

## Eerder overgeslagen zinnen bewust in een kleine batch vertalen

Als **Pauzeer live vertalen · houd origineel** actief is, herkent Tuddy
uitspraken verder lokaal en bewaart de oorspronkelijke zinnen tijdelijk
met status **Gepauzeerd**. Hervatten van de live-vertaling zet oude
zinnen niet automatisch door naar ML Kit.

Met **Kies overgeslagen zinnen om lokaal te vertalen** kun je, terwijl
de microfoon uit is, zelf kiezen welke oorspronkelijke zinnen opnieuw
worden aangeboden. De lijst toont maximaal de laatste 48 **geschikte,
nog gepauzeerde** gespreksregels; oudere regels houden hun reeds
bestaande individuele **Vertaal deze zin alsnog**-actie. Selecteer
hoogstens zes zinnen uit **één oorspronkelijke taalrichting**.
Een gemengde selectie wordt niet toegestaan.

Tuddy controleert na jouw bevestiging eerst uitsluitend de **al
geïnstalleerde lokale ML Kit-tekstmodellen** voor dat taalpaar.
Een ontbrekend of onbekend model start nooit vanzelf een download:
de zinnen blijven Gepauzeerd en je kunt bewust via Offline startcheck
modellen voorbereiden. Na bevestigde modelbeschikbaarheid worden
alleen de nog exact ongewijzigde, eerder geselecteerde regels
aangeboden aan de bestaande lokale vertaalpipeline. Elke geselecteerde
regel krijgt een nieuwe revisie, zodat late eerdere callbacks en
latere correcties niet kunnen worden overschreven.

Deze actie start **geen microfoon**, schakelt de globale
vertaalpauze **niet** uit, vertaalt geen niet-geselecteerde regels,
en bewaart geen lijst of tekst op schijf. Een verzoek om zes
vertalingen is geen bewijs dat ze alle zes al zijn geslaagd.
Zonder fysiek Android-toestel is hiermee geen luchtmodus-,
modelkwaliteits- of onafhankelijk gebruikersbewijs geleverd.

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
