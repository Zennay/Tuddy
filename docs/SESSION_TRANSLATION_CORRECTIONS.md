# Eigen vertaalcorrecties opnieuw gebruiken binnen hetzelfde gesprek

Tuddy kan een bewust handmatig gecorrigeerde vertaling als *voorstel*
onthouden in het RAM van de huidige app-sessie. Dat maakt terugkerende
uitdrukkingen makkelijker te corrigeren zonder het on-device taalmodel
of je privéwoordenboek te veranderen.

## Zo werkt het

1. Laat een zin lokaal vertalen, of typ zelf een ondersteunde tekstzin.
2. Open **Corrigeer vertaling** voor een afgeronde regel en pas de
   tekst aan. De bestaande revisiecontrole voorkomt toepassing op een
   inmiddels gewijzigde regel.
3. Zet optioneel **Onthoud deze exacte correctie alleen voor dit gesprek**
   aan. De keuze staat standaard uit. Tik pas daarna op **Bewaar correctie**.
4. Als dezelfde **exacte oorspronkelijke tekst** later terugkomt met
   dezelfde bron- en doeltaal, verschijnt in het correctievenster een
   herkenbaar eigen voorstel. Tik op **Vul eigen voorstel in**, bekijk de
   volledige tekst en bevestig vervolgens zelf met **Bewaar correctie**.

Niets wordt automatisch als vertaling toegepast: het lokale taalmodel
blijft de gewone route. Een eigen voorstel is **geen geverifieerde**
machinevertaling, geen bewijs voor ondersteunde Koerdische/Pasjtoe taalparen
en geen basis om het spraakpakket- of offline toestelonderzoek over te slaan.

## Zelf controleren en wissen

Open **Gesprekstools → Tijdelijke vertaalcorrecties** om de exacte
oorspronkelijke tekst, taalrichting en menselijke vertaling terug te zien.
Je kunt afzonderlijke zinnen vergeten of na een aparte bevestiging alles
verwijderen. Maximaal 40 paren blijven in het geheugen; het oudste paar
verdwijnt wanneer dit maximum wordt bereikt. Per exacte zin en taalrichting
bestaat maar één actuele correctie. Alle tekst is begrensd, en opgeslagen
invoer wordt niet naar externe aanbieders gestuurd.

**Wis gesprek** wist ook deze voorstellen. Bij beëindigen van de
app-sessie verdwijnen ze eveneens. Geen SharedPreferences, database,
bestand, backup, modeldownload, netwerkverzoek of achtergrondproces
bevat deze correcties. Een bewuste export van een gespreksregel via de
bestaande aparte Android-dialoog blijft een zelfstandige keuze.

Geen automatische uitspraakherkenning voor Kurmanji, Sorani of Pasjtoe
is door dit onderdeel toegevoegd. De fysieke Android-acceptatie en het
onderzoek naar de rechten en kwaliteit van volledige on-device modellen
blijven open.

## Snelle toegang vanuit de live-ondertitel

In een afgesloten, lokaal vertaalde gespreksregel is onder de
bewerktaken **Bekijk mijn vorige correctie** zichtbaar wanneer precies
dezelfde originele zin en taalrichting eerder bewust zijn onthouden.
Deze snelkoppeling zet het exacte voorstel als bewerkbare tekst in het
bestaande correctievenster, maar slaat het **niet** direct op: controleer
de vertaling en tik vervolgens zelf op **Bewaar correctie**. Dezelfde
oude-revisiecontrole geldt als bij elke andere handmatige correctie.

## Eerdere correctie in de fullscreen-leesmodus

In **Groot lezen** verschijnt bij een afgesloten live-ondertitel met
precies dezelfde eerder onthouden zin en taalrichting de knop
**Bekijk mijn eerdere vertaalcorrectie…**. Deze opent het bestaande
revisie-beveiligde correctievenster met het eigen voorstel in het
invoerveld. Pas een tweede, expliciete tik op **Bewaar correctie**
wijzigt de betreffende regel. In een alleen-lezen privéarchief bestaat
deze actie niet en blijft iedere eerdere archiefregel onaangeraakt.

## Persoonlijke correcties lokaal terugvinden

In het beheervenster kan je lokaal zoeken naar brontekst, handmatig
vertaalde tekst of exacte taalrichting, met een zichtbaar resultaatcijfer.
De zoektekst blijft uitsluitend in het geheugen van het geopende venster.
Het wisselen van het filter wijzigt geen vertaling of bewaarde correctie;
ook bij een filter kan het gehele tijdelijke overzicht uitsluitend via
aparte wis-allesbevestiging worden gewist.

Ook dit zoeken gebruikt de bestaande niet-destructieve Unicode-toetsenbord-
normalisatie voor onder meer veelgebruikte Arabische/Persische yeh- en
kafvarianten, zodat eigen Sorani/Pasjtoe-tekst beter terug te vinden is.
De oorspronkelijk ingetypte letters en de opgeslagen correcties worden
hierdoor niet gewijzigd of automatisch gelijkgesteld bij hergebruik.


## Bewerkbare CSV uitwisselen, alleen na een bewuste keuze

Open **Gesprekstools → Tijdelijke vertaalcorrecties → Mijn eigen correcties
meenemen**. Je kunt nu naast de bestaande JSON-back-up een gewone UTF-8
CSV bewaren. De kolommen hebben een vaste volgorde:
`sourceCode,targetCode,original,translation`. Iedere rij bevat precies
één **zelfgeschreven**, niet-gevalideerde correctie voor één exacte
taalrichting. Komma's, aanhalingstekens en regeleinden binnen zinnen
worden volgens CSV-conventies ge-escaped. Excel en andere spreadsheetapps
kunnen het bestand bewerken; export van cellen die met
`=`, `+`, `-` of `@` beginnen wordt uit voorzorg geweigerd om
onbedoelde spreadsheetformules te voorkomen. De bestaande JSON-route
blijft bruikbaar voor die zinnen.

Via **Kies zinnen voor export…** kun je ook per correctie en per exact
taalpaar bepalen welke persoonlijke zinnen meegaan, afzonderlijk als
JSON of als CSV. De Android-documentkiezer wordt pas na een expliciete
exportactie geopend; een gekozen cloudbestandsprovider kan zelf
synchroniseren. Tuddy start geen automatische opslag of upload.

Met **Open eerder opgeslagen correcties…** kan een zelfgekozen JSON-
of CSV-bestand worden geopend. CSV-import vereist exact de vier kolommen,
strikte UTF-8, valide quoting en maximaal 40 unieke, begrensde paren.
Ongeldige gegevens worden zonder mutatie geweigerd. Controleer altijd
alle regels in de bestaande voorvertoning, selecteer desgewenst slechts
één taalpaar of individuele rijen, en kies daarna expliciet of je wilt
samenvoegen met handmatige conflictkeuzes of volledig vervangen.
De tijdelijke **Undo** blijft beschikbaar zolang de bestaande
voorwaarden gelden; de gespreksregels blijven onaangetast. Importeren
activeert geen model en valideert de menselijke vertaling niet.


Je kunt ook **Plak eigen correcties-CSV…** openen en de CSV-tekst
zelf in een begrensd invoerveld plakken. Tuddy leest het Android-klembord
niet automatisch en accepteert maximaal 48 KB tekst. Een geldige invoer
toont dezelfde selecteerbare importvoorvertoning als de bestandsroute;
pas een volgende bewuste keuze mag de RAM-correcties wijzigen.
Het sluiten van het plakveld gooit de ingevoerde tekst weg.


## Grote importvoorvertoningen gericht bekijken

Bij het bewust openen of plakken van een eigen correctie-JSON/CSV kun je
in de bestaande controlevoorvertoning nu ook zoeken op oorspronkelijke
zin, zelfgeschreven vertaling of exact taalpaar. De zoekfunctie
normaliseert veel voorkomende Arabisch/Perzische toetsvarianten alleen
voor weergave en selectie; de echte invoer wordt niet aangepast.

**Kies zichtbare** en **Haal zichtbare weg** werken uitsluitend op de
actuele zoekresultaten. Eerder aangevinkte, door het filter verborgen
regels blijven bewust geselecteerd en tellen mee in het zichtbare
totaal. **Kies alles** en **Kies niets** blijven beschikbaar voor het
volledige importbestand. Voor een merge/vervanging gebruikt Tuddy alleen
het uiteindelijke selectie-overzicht en blijven de bestaande
expliciete conflictkeuzes, maximale 40 zinnen en tijdelijke Undo geldig.
Geen automatische of gedeeltelijke import.
