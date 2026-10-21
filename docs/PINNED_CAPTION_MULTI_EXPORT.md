## Nieuw: drie niveaus voor de tweetalige aanvuloefening

In **Aanvuloefening · HTML** kies je nu eerst expliciet hoeveel eigen
antwoordwoorden je wilt afdekken. **Licht** maskeert ongeveer één van elke
drie spatiegescheiden woorden, **Normaal** ongeveer de helft en **Uitdaging**
ongeveer twee van iedere drie. Voor zinnen met slechts twee zulke woorden
blijft precies het laatste woord verborgen; bij ongesegmenteerde schriftvormen
blijft de gehele antwoordzijde een invulstreep. Alle verborgen woorden zijn
alleen een **presentatiekeuze**; hun oorspronkelijke tekst en het volledige
antwoordblad blijven exact behouden. Taalcodelabels worden nooit weggehaald.

De native Android-dialoog toont voor iedere richting de invulhints van de
gekozen moeilijkheid vóór het opslaan. De HTML vermeldt het niveau en
bevat dezelfde gemaskeerde tekst. Als de gekozen moeilijkheid intussen
verandert of de tijdelijke selectie wijzigt tijdens de externe
Android-bestandskiezer, weigert Tuddy het oude bestand op te slaan.
Bestaande lokale, ongeverifieerde woorden en privacyvoorwaarden blijven
ongewijzigd: geen automatische score, geen audio, geen netwerk, geen
studieprofiel of extra toestelrechten.

---

## Nieuw: tweetalige aanvuloefening zonder netwerk

Kies **★ gemarkeerd → Exporteer selectie… → Aanvuloefening · HTML**.
De app maakt van exact de geselecteerde, echt afgeronde tweetalige zinnen
zelfstandige offline HTML-oefenkaarten. De oorspronkelijke vraag blijft
leesbaar, terwijl **elke tweede witruimte-gescheiden antwoordterm** tijdelijk
als invulstreep verschijnt. Voor schriftsoorten zonder spaties wordt de
hele antwoordzijde als lege invulregel aangeboden, niet kunstmatig in
woorden geknipt. **Bekijk volledig antwoord** toont per kaart de exacte
volledige zin na een bewuste tik; op papier staat een afzonderlijk
**antwoordblad** na de vragen. Het brontranscript zelf verandert nooit.

Deze route ondersteunt bron→vertaling, vertaling→bron en beide richtingen,
optionele taalcodelabels en het filter **Alleen mijn handmatig gecorrigeerde
antwoorden**. De eerder getoonde unieke-kaartenteller en de daadwerkelijke
bestandsvoorbeeldregels gelden ook voor de aanvuloefening. Dezelfde grenzen
(maximaal 80 oorspronkelijke markeringen, maximaal 160 kaarten, 256 KiB
uitvoer) en geldige brongegevens als voor de TSV-kaarten worden afgedwongen.
Ongeldige uitvoer wordt helemaal geweigerd, nooit afgekapt.

De app bewaart alleen na jouw expliciete Android-bestandskeuze en vergelijkt
na het terugkeren exact opnieuw de tijdelijke selectie, filters en bytes.
Het bestand heeft een strikte netwerkloze CSP, geen JavaScript en geen externe
bronnen. De gekozen bestandsprovider kan je tekst wel synchroniseren. De
weergegeven betekenis is eigen tekst of een **ongeverifieerde** lokaal
verkregen modelvertaling; Tuddy geeft geen automatische juistheidsbeoordeling.

---

## Nieuw: enkelzijdig afdrukbare vouwkaartjes

Naast TSV/CSV en het interactieve HTML-oefenblad heeft de expliciete
**Alleen gemarkeerde regels bewaren**-dialoog nu **Vouwkaartjes · HTML**.
Het bestand toont per geselecteerde, afgeronde vertaalzin de **bronzijde links**
en de **betekenis rechts** in één omlijnde kaart. Print het document op
**één zijde**, knip langs de buitenrand en **vouw de rechter betekenis
achter de linker bronzijde**. Er zijn geen onbetrouwbare duplexinstellingen,
externe scripts, fonts of netwerkverbinding nodig.

De bestaande instellingen blijven werken: bron→vertaling,
vertaling→bron of beide richtingen; taalrichtingfilter; optionele
taalcodelabels; uitsluitend je **zelf gecorrigeerde betekenissen** of
alle afgeronde lokale vertalingen. Dubbele betekenisparen worden op
dezelfde manier beperkt als bij TSV/CSV en het andere HTML-oefenblad.
Aanhalingstekens en HTML-tekens worden veilig ge-escaped en elke
kaartzijde kan automatisch een rechts-naar-links-tekstrichting kiezen.
Een beginnend reken- of plusteken wordt op papier getoond **zonder**
de extra spreadsheet-apostrof die bij TSV/CSV juist noodzakelijk is.

Het resultaat bevat alleen bewust gemarkeerde tekst en nooit ruwe
audio, audio-bestandsnamen of automatisch herkende tijdstempels.
Tuddy valideert de inhoud en herberekent het bestand nadat de
Android-documentkiezer sluit; gewijzigde of ingetrokken keuzes
kunnen dus niet stilzwijgend oude inhoud wegschrijven. Een gekozen
Android-provider kan de afgedrukte inhoud natuurlijk wel synchroniseren.
Dit zijn uitsluitend lokale modelvertalingen of **eigen correcties**,
geen onafhankelijk geverifieerde woordenlijst.

---

# Gemarkeerde zinnen bewust apart exporteren

Tuddy bewaart een live gesprek uitsluitend tijdelijk in het appgeheugen.
Tik bij afgeronde gesprekszinnen op de ster om maximaal 80 zinnen te
markeren. Open onder het gesprek **★ gemarkeerd**, bekijk welke uitspraken
meegaan en kies **Exporteer selectie…**. De export is niet beschikbaar
zolang de microfoon actief is.

Kies eerst de **taalrichting**: alle richtingen van dit gesprek of
precies één zichtbaar bron- en doeltaalpaar. Dit is een zuiver
lokaal selectiefilter: het verandert noch de sessie noch het
bewaarde markeringenoverzicht. Verander je de taalrichting tijdens
het externe bestandskiezen, dan wordt geen oud document alsnog
weggeschreven.

Selecteer ook bewust **welke inhoud** je mee wilt nemen: alle
gemarkeerde uitspraken, alleen reeds afgeronde lokale regels, of
uitsluitend regels met een werkelijk voltooide vertaling. De teller
laat vooraf zien hoeveel van je markeringen worden meegenomen; een
lege keuze wordt geweigerd. Niet-afgeronde vertalingen krijgen
nergens een verzonnen vertaalresultaat. Dit filter verandert de
tijdelijke markeringen of het echte gesprek niet.

Wil je meteen oefenen zonder iets te bewaren? Open **★ gemarkeerd** en
kies **Oefen ... gemarkeerde vertaalparen**. Alleen echt voltooide
tweetalige zinnen komen in de bestaande tijdelijke oefendialoog.
Zet desgewenst **Oefen alleen mijn handmatig gecorrigeerde antwoorden** aan
om modelantwoorden zonder eigen correctie ook binnen de oefenronde uit
te sluiten. Als er geen geschikte eigen antwoorden zijn, kan die
ronde niet gestart worden. Kies de richting, onthul antwoorden,
beoordeel jezelf en herhaal moeilijke kaartjes. De oefeningen blijven volledig in RAM, zijn niet gevalideerd
en verdwijnen zodra je sluit. De knop is uitgeschakeld tijdens microfoon-
opname of wanneer er geen bruikbare tweetalige markeringen zijn.
Dit verandert je markeringen, eigen archief of woordenschat niet.

## Zelf een afdrukbaar vragen- en antwoordenblad maken

Kies bij **★ gemarkeerd → Exporteer selectie…** het nieuwe formaat
**Oefenblad · HTML**. Net als voor TSV en CSV gaan uitsluitend echt
afgeronde, niet-lege tweetalige regels mee. Het bestaande zoek-/taalfilter,
de bewuste selectie, het ontdubbelen van identieke paren en de keuze
voor bron→vertaling, omgekeerd of beide richtingen blijven gelden.
Zelf geschreven studiecorrecties verschijnen waar ze exact bij de
huidige regel horen; kies desgewenst **Alleen mijn handmatig
gecorrigeerde antwoorden** om alle andere regels weg te laten.

Vooraf zie je dezelfde kaartteller en voor-/achterkanttekst als bij
TSV en CSV. Kies pas daarna een Android-bestandslocatie. Het lokale
HTML-document is tegelijk een **oefenkaartenset in de browser**: iedere
vraag toont zijn antwoord pas na bewust openen van **Toon antwoord**
(zonder scripts, internet of automatische onthulling). Bij **afdrukken**
verdwijnen deze openklapbare antwoorden uit het vragenblad; daar
verschijnen schrijflijnen en de **antwoorden** staan in een
afzonderlijk afdrukgedeelte met pagina-einde.
Zo kun je de vragen uitdelen zonder de antwoorden op de eerste pagina.
Het bestand heeft geen JavaScript, externe bronnen, gekoppelde
audiobestanden of netwerkaanvragen.
In tegenstelling tot het spreadsheetveilige TSV-/CSV-formaat toont het
HTML-blad een eigen zin die met `=`, `+`, `-` of `@` begint **zonder**
de extra formulebeschermende apostrof; het is HTML-tekst, geen
spreadsheetcel. Tekst wordt HTML-geëscapet en ieder antwoord
blijft leesbaar in zijn eigen tekstrichting (ook Sorani/Pasjtoe). De app bewaart geen nieuwe
studieresultaten en wijzigt de oorspronkelijke transcriptie niet.

De export is een volledig zelfstandige, maximaal 256 KiB grote
HTML-pagina met ge-escapete Unicode-teksten. Identieke studiekaarten
worden maar één keer meegenomen; bij twee richtingen verschijnen
beide vragen/antwoorden. Ongeldige/te grote documenten worden
volledig geweigerd. Een externe Android-bestandsprovider kan de
expliciet opgeslagen informatie alsnog synchroniseren of delen.
De antwoorden blijven ongeverifieerde modelvertalingen of
zelfgeschreven correcties, geen erkende vertalingen.

## Alleen mijn gekozen markeringen gebruiken

In **★ gemarkeerd** kun je nu maximaal 80 tijdelijke markeringen
doorzoeken op bron, vertaling en taalcode. Via **Taalrichting** kies je
ook één exact bron-/doeltaalpaar, of weer alle richtingen. Filter op
**Alle**, **Alleen afgeronde** of **Alleen voltooide vertalingen** en
toon de nieuwste regels eerst. Vink per zin aan of die mee mag in je volgende oefenronde
of export; dat is iets anders dan de ster verwijderen. Met **Voeg
zichtbare toe** en **Deselecteer zichtbare** kun je de huidige zoekresultaten
toevoegen of uit je selectie verwijderen, zonder eerder gekozen
niet-zichtbare regels te verliezen. Met **Kies uitsluitend zichtbare
regels** vervang je de hele selectie door precies je huidige
zoek-/filterresultaat. Dit voorkomt dat andere gemarkeerde zinnen
onbedoeld meegaan in een export. **Kies alle markeringen** herstelt de
oorspronkelijke volledige selectie.
Zoek- en filterkeuzes veranderen de oefen-/exportselectie niet automatisch.

**Verwijder alleen deze ster** haalt één regel uit je tijdelijke
markeringen, zonder de andere regels te verliezen. **Wis markeringen**
blijft een aparte bewuste actie. Bij beide verwijderacties biedt Tuddy
**↶ Herstel** voor de laatst verwijderde ster(ren), zolang de huidige
markeringenlijst open blijft en de originele zin nog in het gesprek
staat. Alleen die laatste verwijderactie kan worden hersteld en nooit
meer dan 80 sterren. Er is geen permanente herstelgeschiedenis. De teller en beide knoppen voor oefenen
en exporteren gebruiken uitsluitend de gekozen regels; bij een lege
keuze blijven ze uitgeschakeld. De keuze wordt bij het opnieuw openen
van de gemarkeerd-lijst naar alle beschikbare sterren gereset. Een
zelf gewijzigde selectie die tijdens de externe Android-bestandskiezer
verandert, blokkeert de oorspronkelijke opslag via de bestaande
snapshot-hercontrole. De bediening blijft scrollbaar op smalle telefoons.

## Mijn eigen studieantwoord corrigeren zonder gespreksbron te veranderen

Wanneer een gemarkeerde regel echt lokaal vertaald is, verschijnt bij
die zin de actie **✎ Corrigeer alleen studieantwoord**. Je ziet zowel
de oorspronkelijke uitspraak als de huidige **ongeverifieerde
modelvertaling**. Schrijf maximaal 400 geldige Unicode-tekens aan
één zelfgekozen betekenis en bevestig afzonderlijk met **Gebruik in
mijn oefenkaarten**. Een ongeldige of lege waarde houdt het
bewerkvenster open. Je kunt je eigen versie later aanpassen of met
**Gebruik weer modelvertaling** terugzetten.

De verbetering is bewust beperkt tot de huidige privésessie:
directe geselecteerde oefenkaarten en expliciet geëxporteerde
**TSV-leerkaarten** gebruiken het persoonlijk gecorrigeerde antwoord.
De voor-/achterkantpreview en de ruwe TSV-bestandspreview laten de
daadwerkelijk gekozen tekst zien. TXT, JSON, HTML en SRT/WebVTT
blijven uitsluitend de originele lokale transcript-/modeldata gebruiken,
zonder te suggereren dat het gesprek achteraf is gewijzigd.

Tuddy koppelt iedere correctie aan de exacte bronzin, modelvertaling
en taalrichting waarvoor die geschreven is. Wijzigt één daarvan,
dan past Tuddy geen oude correctie stilzwijgend toe. Zowel een
wijziging van correcties tijdens de Android-bestandskiezer als een
wijziging van selectie of gesprek maakt een eerdere export ongeldig.
Alle correcties blijven uitsluitend in RAM en verdwijnen bij **Wis
gesprek** of afsluiten. Een eigen uitleg is **niet onafhankelijk
geverifieerd**, geen automatisch verbeterde vertaling en geen
duurzaam leerprofiel.

## Alleen persoonlijk gecorrigeerde studiekaarten exporteren

Kies bij het **TSV of CSV**-formaat desgewenst **Alleen mijn handmatig
gecorrigeerde antwoorden**. Dan gaan uitsluitend de expliciet door
jou bewerkte vertaalparen uit de gekozen markeringen mee. De
weergave van aantallen, effectieve voor-/achterkant en de volledige
ruwe bestandsinhoud volgen precies dit filter. Zijn er geen bruikbare
persoonlijk gecorrigeerde regels, dan wordt het bestand geweigerd:
er worden nooit ongemerkt ongecorrigeerde modelantwoorden aangevuld.

Deze keuze verandert de transcriptgegevens, de bron-SRT/VTT en het
privé-JSON-archief niet. Een eigen correctie is nadrukkelijk nog steeds
geen door onafhankelijke moedertaalsprekers gecontroleerde vertaling.
Bij wijzigingen tijdens de Android-bestandskeuze wordt de eerdere
snapshot opnieuw afgewezen.

Selecteer daarna precies één uitvoerformaat:

- **TXT**: leesbare originele regels, taalpaar, tijdstip en alleen werkelijk
  voltooide vertalingen.
- **TSV-/CSV-leerkaarten**: importeer daadwerkelijk voltooide lokale modelvertaalparen
  in een kaartprogramma dat UTF-8-tekst met twee velden ondersteunt
  (voorzijde en achterzijde). Kies **Bronzin → lokale modelvertaling**,
  **Lokale modelvertaling → bronzin** of **beide richtingen** (twee kaarten per
  geldig paar). Status "Lokaal" zonder eigenlijke vertaling telt niet mee.
  Lege of nog niet vertaalde regels worden vooraf uit de teller gehaald
  en komen nooit als verzonnen achterkant in het bestand. Identieke
  bron/vertaling-combinaties binnen hetzelfde taalpaar tellen eenmaal;
  verschillende taalparen worden niet samengevoegd. Het scherm toont
  het aantal unieke tweetalige paren, het effectieve aantal kaartjes
  (bij beide richtingen dubbel) en een leesbare voor-/achterkant-preview.
  Met **Toon taalcode bij elke kaartzijde** wordt ieder veld optioneel
  voorafgegaan door bijvoorbeeld `[nl]` of `[en]`, ook als je de
  vraag- en antwoordrichting omdraait. De taalcode wordt uit het
  oorspronkelijke taalpaar gehaald, niet automatisch gedetecteerd.
  De bestandsinhoud en ruwe preview tonen exact dezelfde labels.
  Onbruikbare of onveilige taalcodes worden voor deze optie geweigerd.
  De exacte uitvoer is vóór opslaan in de ruwe bestandspreview te bekijken.
  Emoji en andere geldige Unicode-tekens blijven behouden; ongeldige
  losse UTF-16-surrogaten worden geweigerd in plaats van ongemerkt
  vervormd. Regeleinden en tabs in eigen teksten worden spaties;
  mogelijke spreadsheetformules krijgen een leidende apostrof, ook na
  onzichtbare opmaakkarakters. De werkelijke tekst (inclusief & en <)
  blijft platte UTF-8-data en wordt niet als HTML-geëscaped.
  Kies TSV met tabs als kolomscheiding, of CSV met komma's,
  dubbelgequote cellen, verdubbelde dubbele aanhalingstekens
  en CRLF-regelafsluitingen. Beide bestanden bevatten precies
  twee tekstkolommen zonder kopregel. De gekozen import-app kan
  de formulebeschermende apostrof anders tonen; inspecteer daarom
  het resultaat. Geen automatische kwaliteits-
  of taalcontrole. Het zijn modelantwoorden, niet jouw zelfgeschreven
  of door moedertaalsprekers geverifieerde betekenissen. Je leerkaarten blijven ook zonder verdere
  toestemming *niet* als app-privé studiekaarten opgeslagen.
- **HTML-oefenblad**: afzonderlijke vraag- en antwoordpagina's van uitsluitend
  je gekozen tweetalige leerkaarten, met dezelfde richting, correcties,
  taalcodekeuze en begrenzing als TSV/CSV. Bewuste SAF-opslag.
- **JSON**: dezelfde beperkte selectie in Tuddy's bestaande
  privéarchief-formaat, opnieuw te openen in de read-only archieflezer.
  Niet voltooide vertalingen blijven expliciet leeg.
- **HTML**: afdrukbare, zelfstandig leesbare tweetalige kaartjes; de
  tekst is HTML-geëscaped en haalt geen externe bronnen op.
- **SRT / WebVTT**: een **ruw ondertitelconcept** op basis van de
  geregistreerde eindtijd van zinnen. Kies zelf bron/vertaling/binnen
  beide, eerste tijdverschuiving en maximale weergaveduur. Dit zijn
  geen gemeten start-/eindtijden. Controleer en corrigeer ze in een
  video- of ondertiteleditor. Er wordt geen audio opgenomen of uitgeknipt.

Controleer eerst de lijst gemarkeerde regels, het bron-/vertaalfilter
en het formaat. Het bevestigingsscherm toont ook de berekende
bestandsgrootte en een korte inhoudssamenvatting van de meegenomen
regels. Via **Bekijk exacte bestandsinhoud…** open je afzonderlijk
de volledige, alleen-lezen tekst van het daadwerkelijk te bewaren
TXT-, TSV-, CSV-, JSON-, HTML-, SRT- of WebVTT-document. Dit gebeurt uitsluitend
in het tijdelijke appgeheugen; je keert daarna terug naar de keuzes.
Een groot document wordt per regel geladen zodat je ook bij veel
ondertitels kunt scrollen. JSON- en HTML-bestanden tonen bewust de
exacte technische tekst, niet een verzonnen gerenderde preview. Daarna verschijnt
de waarschuwing. Pas met **Kies opslag…** wordt de Android
bestandskiezer geopend. Tuddy controleert na terugkeer of dezelfde
zinnen, taalrichtingen, vertaalstatus en markeringen nog actief zijn.
Een gewijzigde selectie, een nieuw taalpaar, ander formaat of
een gewijzigd bron-/vertaalfilter, een gewist gesprek of een gestarte
microfoon breekt de overdracht af; maak dan een nieuwe keuze.
Bij annuleren wordt ook de tijdelijke ruwe preview gesloten.

Bij TSV neemt Tuddy uitsluitend werkelijk voltooide, niet-lege vertaalparen mee.\nDe richtingskeuze hoort bij de bestandskeuze-snapshot: verander je die tijdens\nhet opslaan, dan wordt de oude overdracht geannuleerd.\n\nBij SRT/WebVTT neemt Tuddy uitsluitend afgeronde lokale regels mee.
Met de handmatige keuze **Vertaling** worden zinnen zonder voltooide
vertaling vooraf uit de teller en het inhoudsoverzicht verwijderd;
de getoonde aantallen komen daardoor overeen met de uit te voeren
ondertitelregels.

De maximumgrenzen en invoerbeperkingen van elk bestaand formaat
blijven gelden. Sommige keuzes, bijvoorbeeld vertaling-alleen zonder
voltooide vertaling, leveren terecht geen bestand op. Exporteren maakt
geen automatische vertaling, sprekersherkenning, transcriptopslag of
synchronisatie. Markeringen verdwijnen bij **Wis gesprek**, na
geheugenverloop of bij het sluiten van de app.

**Privacy:** de gekozen Android-bestandsprovider kan je gesprek
mogelijk elders bewaren, delen of synchroniseren. Bewaar nooit
gevoelige inhoud zonder toestemming van de betrokkenen. Een lokaal
gegenereerd bestand is geen geverifieerde transcriptie, en een
broncodebuild is geen bewijs van fysieke offline toestelacceptatie.
