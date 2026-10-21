# SRT/WebVTT, LRC, eigen tekst en documenten importeren

Tuddy laat je handmatig een volledig SRT- of WebVTT-document plakken, zonder dat de app zelf je Android-klembord uitleest.

## Eigen bestanden rechtstreeks kiezen via Android (SRT, VTT, LRC, CSV, TSV, TXT)

Je hoeft een eigen bestand niet eerst met een andere app te openen en te kopiëren:

1. Open **Plak eigen SRT/WebVTT-tekst…** of **Tweede ondertitelspoor zelf plakken…**. Kies onder de bestandsknop **SRT/VTT** voor een volledige ondertitel, of **Gewone zinnen** en de exacte CSV-/tabkolom- of tekstindeling van je bestand.
2. Tik op **📂 Kies eigen UTF-8 SRT, VTT, LRC, CSV, TSV of TXT-document…**. Android opent de systeemeigen documentkiezer. Jij kiest welk document en welke bestandsprovider je wilt gebruiken; Tuddy leest niet automatisch je bestanden of het klembord.
3. De Android-documentkiezer kan Tuddy naar de achtergrond sturen. De **Activity** bewaart daarom alleen tijdens deze expliciete bestandskeuze de vorige tijdelijke invoer en de geselecteerde invoermodus/tijdinstellingen in RAM. Na terugkeer leest Tuddy **maximaal 256 KiB** vanaf de bewust gekozen documentprovider, buiten de UI-thread. Bij een geldige keuze vervangt de bestandsinhoud uitsluitend het eerste tekstvak: oude losse betekenissen, zoekterm, regelselecties en paginapositie worden bewust gereset, maar de gekozen **handmatige start- en duurinstellingen blijven behouden**. Een geweigerde of afgebroken bestandskeuze heropent de **eerdere volledige invoer inclusief instellingen en selectie**, in plaats van alles te verliezen.
4. Controleer na de automatische terugkeer naar de juiste eerste- of tweede-spoorpreview de invoermodus en alle getoonde fouten, het exacte bronregelbereik en de doorzoekbare preview. Het gekozen bestand wordt alleen geaccepteerd door **dezelfde volledige parser en bestaande exportgrenzen** als bij handmatig plakken. Er wordt nog geen concept toegepast totdat je **Open gekozen regels** bevestigt.
5. De code weigert onleesbare, lege, te grote, binaire of ongeldige UTF-8-documenten, inclusief verboden controletekens, zonder de huidige invoer te vervangen. Vervolgens gelden ongewijzigd de maximaal 500 cues, veilige tijden en SRT-/WebVTT-exportcontrole.

De tijdelijke keuze en eerdere conceptinvoer verdwijnen wanneer de gebruiker de import afsluit; bij een ongeldig gemaakte sessie wordt een laat aangekomen documentresultaat genegeerd. Er worden geen leesrechten langdurig bewaard, geen bestand automatisch weggeschreven en geen nieuwe opslagpermissies aangevraagd. **Let op:** de door jou gekozen Android-bestandsprovider kan externe/cloudopslag gebruiken; Tuddy kan niet garanderen dat een gekozen provider offline blijft. De app verwerkt de geleverde tekst verder lokaal en vraagt geen eigen netwerkverbinding voor deze import. CSV met nieuwe regels **binnen een veld** wordt nog steeds geweigerd. Bestandsimport geeft geen ASR, automatische vertaling of echte audiotiming.


## SRT/WebVTT-bestanden rechtstreeks openen zonder de editor vast te zetten

Naast de handmatige bestandskeuze in de plakroute bestaan ook de
bestaande Android-knoppen **Open SRT/WebVTT…** en **Tweede
SRT/VTT-document toevoegen…**. Beide routes gebruiken nu dezelfde
streng begrensde, UTF-8-controlerende documentlezer als de eigen
plakimport. Android kan via een externe bestandsprovider trager zijn,
maar het inlezen en parsen gebeurt buiten de UI-thread; de live
microfoon of het live gesprek worden niet automatisch gestart of
aangepast.

- De eerste SRT/VTT-openactie accepteert expliciet een door jou
  gekozen bestand onafhankelijk van hoe de Android-provider het
  MIME-type rapporteert. De volledige inhoud moet alsnog aan de
  bestaande SRT/VTT-parserlimieten voldoen.
- Voor **het tweede spoor** keert eerst je oude tijdelijke
  ondertitelconcept terug. Pas als het nieuwe volledige document
  geldig is, biedt Tuddy de bestaande **samenvoeg-preview** aan.
  Bij annuleren of weigering blijft het eerdere concept ongewijzigd.
- In beide gevallen worden te grote bestanden (meer dan 256 KiB),
  ongeldige UTF-8, verboden controletekens en onvolledige SRT/VTT-cues
  geweigerd. Als je het gesprek wist, de editor wijzigt of een nieuw
  document kiest tijdens het laden, wordt een achterhaald resultaat
  niet meer toegepast.
- Dit is alleen **bewust gekozen lokale documentverwerking**.
  Android-documentproviders kunnen zelf externe/cloudopslag gebruiken.
  Tuddy vraagt geen blijvende URI-rechten of nieuwe permissies.

## Eigen LRC-lyrics-tijdcodes omzetten naar een ondertitelconcept

Selecteer **LRC: eigen begintijden per regel**. Typ/plak je eigen LRC-tekst,
of kies een `.lrc`-bestand via de bestaande expliciete Android-documentkiezer.

```text
[ar:Eigen artiest (optionele metadata)]
[00:01.25]Eerste zelfgeschreven zin
[00:04.50]Tweede zelfgeschreven zin
[00:07.00]Laatste zelfgeschreven zin
```

LRC biedt **begintijdcodes**, geen eindmomenten. Tuddy neemt iedere eigen
beginwaarde over en gebruikt **de volgende begintijd als einde**. De laatste
regel krijgt een **expliciet geschatte duur van drie seconden**, of minder
wanneer de maximale veilige tijdgrens bereikt is. Dit is geen gemeten audio-
of woorduitlijning. In de bestaande editor kun je de tijden handmatig
controleren, corrigeren en apart als SRT of WebVTT opslaan.

Het formaat moet strikt één tijdstempel `[mm:ss.xx]` of `[mm:ss.xxx]`
per niet-lege regel bevatten, in oplopende volgorde. Maximaal 500 cues,
800 tekens per tekstregel en maximaal 256 KiB volledig geldige UTF-8
worden geaccepteerd. Lege regels worden genegeerd. Bekende informatieve
LRC-metadata vóór de eerste cue (`ar`, `ti`, `al`, `by`, `length`) worden
bewust genegeerd; onbekende directives, `offset`-verschuivingen en meerdere
tijden op dezelfde regel zijn ongeldig in plaats van stilzwijgend
geïnterpreteerd. Het hele resultaat moet geldig exporteerbaar zijn als
SRT **én** WebVTT. De normale volledige preview, zoekfunctie, bronregelselectie
en handmatige bevestiging blijven verplicht. Dit bewijst geen offline
ASR/MT, taalinhoud of echt gesynchroniseerde audio.

## Direct openen

1. Stop eerst de live microfoon en tik op **Plak eigen SRT/WebVTT-tekst en bekijk voorbeelden…**.
2. Plak de volledige ondertiteltekst in het invoerveld. Geldige tijdregels hebben een begin- en eindtijd. Zowel SRT als WebVTT is toegestaan.
3. Bekijk het aantal gevonden regels en de eerste vijf tijdgecodeerde fragmenten. Alleen als de volledige tekst door de bestaande begrensde parser wordt geaccepteerd, wordt **Open tijdelijk concept** actief.
4. Bevestig expliciet. De regels openen in de bestaande lokale ondertiteleditor. Het gesprek, het bronbestand en de live transcriptie veranderen niet.
5. Je kunt fragmenten aanpassen, afspelen met eigen apart gekozen lokale audio, terugdraaien, oefenen of als **nieuwe** SRT/VTT-kopie opslaan.

## Van losse tekst een ondertitelconcept maken

1. Kies **Gewone zinnen**. Je kunt zelf kiezen tussen **Per regel** en **Per zin (schatting)**.
2. Bij **Per regel** wordt iedere niet-lege tekstregel een cue. Bij **Per zin** wordt op leestekens gevolgd door witruimte gesplitst; dit is een beperkte tekstheuristiek en kan verkeerde grenzen kiezen.
3. Kies zelf de starttijd (0, 5, 10, 30 of 60 seconden) en de geschatte duur per cue (2, 3,5, 5 of 7 seconden).
4. Bekijk de resultaten, zoek over het hele document en blader in pagina's van 15 cues. De getoonde tijdcodes zijn **handmatig gesimuleerd**, niet gebaseerd op audio/spraakherkenning.
5. Open het concept alleen na de bevestigingsknop, bewerk tekst en tijden zo nodig, en bewaar een aparte SRT/VTT-kopie met de bestaande documentkiezer. Er vindt geen automatische bestandsopslag plaats.

## Twee losse tekstvakken op je telefoon (zonder spreadsheet)

Als bronzin en eigen betekenis in twee losse tekstlijsten staan, hoef je geen tabs of spreadsheetindeling te maken.

1. Kies **Gewone zinnen** en vervolgens **Twee losse tekstvakken: bron en eigen betekenis**.
2. Plak of typ elke originele zin op een eigen regel in het **eerste vak**. Typ of plak daaronder in het **tweede vak** dezelfde hoeveelheid zelfgeschreven betekenissen, één betekenis per oorspronkelijke zin.
3. Tuddy koppelt regel 1 aan regel 1, regel 2 aan regel 2, enzovoort. **Ongelijke aantallen of een leeg veld** worden met een concrete melding (en zo nodig het eerste betrokken regelnummer) geweigerd. De app verschuift of vertaalt geen regels automatisch.
4. Kies de **geschatte** starttijd en duur per cue; beide lijsten worden één tijdelijk, doorzoekbaar tweetalig ondertitelconcept. Controleer tijd en tekst vóór het openen en later eventueel opnieuw in de bestaande editor.
5. De gezamenlijke bron- en betekenisinvoer is maximaal **256 KiB UTF-8**, maximaal **500 tekstparen**, en iedere bronzin/betekenis maximaal **800 tekens**. De bestaande SRT én WebVTT-exportlimieten gelden ook.
6. Sluiten of naar de achtergrond gaan verwijdert de niet-opgeslagen plakinhoud. Opslaan gebeurt alleen na een aparte Android-bestandskeuze, waarbij de gekozen provider zelf externe synchronisatie kan verzorgen.

De betekenis is uitsluitend door de gebruiker geschreven; deze route biedt geen automatische vertaling, ASR of audiotiming.

### Bestaande transcript-tijdcodes gebruiken (drie tekstkolommen)

Heb je zelf een transcript met begin- en eindtijden maar **geen** handmatig
geschreven vertaling, gebruik dan in **Plak zelf tekst…** de optie
**Gewone zinnen → Drie kolommen: begin · einde · eigen tekst**.

Plak precies drie tabgescheiden velden per oorspronkelijke regel:

```text
00:00:03.000[TAB]00:00:06.500[TAB]Zelf geschreven bronzin
00:00:06.500[TAB]00:00:09.200[TAB]Volgende eigen zin
```

Vervang `[TAB]` door een echt tabteken (bijvoorbeeld bij plakken vanuit
een eigen spreadsheet). Het tijdformaat volgt de bestaande handmatige
tijdparser, bijvoorbeeld `00:00:12.500` of `01:23,456`. Je ziet
**direct het oorspronkelijke regelnummer** wanneer een regel leeg is,
de begintijd of eindtijd ongeldig is, een tijdvak nul/negatieve lengte
heeft, de beginmomenten teruglopen, het aantal kolommen niet klopt
of de tekst ontbreekt/te lang is. Dezelfde specifieke feedback werkt
nu ook voor de bestaande vierkoloms-import.

Tuddy neemt bestaande begin- en eindtijden zonder schatting over,
plaatst één door jou geschreven tekstregel per cue en biedt de bestaande
volledige preview, zoekfunctie en expliciete selectie. Alleen na
**Open gekozen regels** verschijnt het resultaat in de tijdelijke
editor. Bij een selectie wordt de complete actuele bron opnieuw gelezen,
zonder verborgen wijzigingen in de originele SRT/VTT of het gesprek.
Er vindt geen echte audiouitlijning, ASR, vertaling, beoordeling of
netwerkverwerking plaats. De maximaal 500 regels en 256 KiB
bronbeperking, SRT/WebVTT-exportgrenzen en expliciete Android-bestandskeuze
blijven behouden.

# Handmatig tweetalige tekst uit twee kolommen

1. Kies **Gewone zinnen** en vervolgens **Twee handmatige tekstkolommen (tab gescheiden)**.
2. Plak per niet-lege regel twee velden uit bijvoorbeeld een eigen spreadsheet: `oorspronkelijke zin[TAB]zelfgeschreven betekenis`. Gebruik exact één tab om de kolommen te scheiden; twee lege/extra kolommen en lege tussenliggende spreadsheetrijen worden afgewezen in plaats van stilzwijgend overgeslagen.
3. Tuddy zet ieder door jou geschreven paar op één **tweeregelige** ondertitelkaart met dezelfde *geschatte* tijdsinstellingen. Bron en eigen betekenis blijven exact zoals aangeleverd, zonder automatische vertaling, taaldetectie of controle op juistheid.
4. Zoek lokaal door de complete preview, blader langs alle kaarten en open de volledige gekozen invoer pas na expliciete bevestiging. Vervolgens kun je in de bestaande editor aanpassen, combineren of apart als SRT/VTT opslaan.
5. Gebruik voor handmatige Kurmanji, Sorani of Pasjtoe gerust je eigen tekst, maar dit bevestigt **geen** offline ASR/MT-taalondersteuning of licentie-/native-speaker-review.

## Tijdcodes uit eigen spreadsheet bewaren (vier kolommen)

Heb je zelf nauwkeurige begin- en eindtijdcodes naast je tweetalige zinnen, dan hoeft Tuddy daar geen tijd te schatten.

1. Kies **Gewone zinnen** en **Vier kolommen: begin · einde · bron · betekenis**.
2. Plak per niet-lege rij exact vier velden, met tabs ertussen: `00:00:12.500[TAB]00:00:15.750[TAB]Eigen bronzin[TAB]Eigen betekenis`.
3. Alternatieve tijdnotatie is `mm:ss,mmm`; tijdvelden moeten binnen het geldige documentbereik vallen en **einde moet later zijn dan begin**. Lege tussenliggende spreadsheetrijen zijn ongeldig, zodat bestaande brontijd- en regelnummers niet onopgemerkt verschuiven. De volgorde moet chronologisch blijven. Overlappende tijden mogen zoals bij bestaande SRT/VTT voor overdub/captions, maar de editor laat die zichtbaar.
4. De app behoudt de opgegeven begin- en eindtijden zonder een audioanalyse, ASR of herschatting. De instelling voor geschatte starttijd en zinsduur wordt in deze modus niet getoond.
5. Bekijk de volledige regelpreview of kies eerst een exact bereik, open het concept en corrigeer desgewenst een bestaande tijdcode met de ondertiteleditor. Opslaan blijft expliciet via een nieuwe Android-bestandskeuze.

Deze eigen tijdcodes zijn **niet bewezen** aan een audio- of videobestand gesynchroniseerd; Tuddy kopieert uitsluitend de tijdcodes die je zelf invoert.

## Zelf begin en duur in seconden instellen

Bij **Gewone zinnen** zonder meegeleverde tijdcodes kun je nu
**Aangepaste begin- en duurinstelling…** kiezen. Typ een eerste
beginmoment tussen 0 en 21600 seconden (maximaal zes uur) en een
eigen duur per cue tussen 0,5 en 30 seconden. Zowel een punt als
een komma als decimaalteken wordt geaccepteerd, met maximaal drie
cijfers achter het scheidingsteken (bijvoorbeeld `97,250` en `4.5`).
Alle waarden worden binnen de bestaande maximale SRT/VTT-tijdcode
en cue-/documentgrenzen gecontroleerd; ongeldige cijfers blokkeren
de preview en overname totdat je ze corrigeert.

De oorspronkelijke vaste begin- en duurknoppen blijven beschikbaar
als je de optie weer uitzet. CSV/TSV met **eigen begin- en eindtijdcodes**
en volledige SRT/VTT-input nemen hun tijdcodes onveranderd over.
Handmatig ingevoerde begin-/duurwaarden zijn uitsluitend schattingen,
geen automatische spraakherkenning of bewezen audiosynchronisatie.
## Twee eigen tekstkolommen uit CSV zonder tijdcodes

Heb je een eigen CSV-bestand met uitsluitend twee kolommen (bronzin en
een door jou zelf geschreven betekenis), kies dan **Gewone zinnen →
CSV: brontekst, eigen betekenis (tijden geschat)**. Iedere fysieke rij
levert precies één tweetalige ondertitelkaart op. Gebruik het gewone
CSV-formaat: komma als scheidingsteken, quotes rondom tekst met komma's
en dubbele quotes voor een ingesloten aanhalingsteken.

```csv
"Goedemorgen, allemaal","Good morning, everyone"
"Een ""eigen"" voorbeeld","An ""original"" example"
```

Er mag geen kopregel in het fragment staan. Beide kolommen zijn verplicht;
lege rijen, onjuiste quotes en te lange tekstvelden melden het originele
regelnummer, zonder regels ongemerkt op te schuiven. Tijden komen
**uitsluitend** uit de apart handmatig gekozen begin- en duurinstellingen.
Tuddy vertaalt niets, herkent geen taal en synchroniseert geen audio.
Gebruik drie of vier CSV-kolommen wanneer je zelf tijdcodes aanlevert.
De volledige preview en gerichte bronregelselectie blijven beschikbaar.
## Bestaande transcript-tijden uit een CSV-spreadsheet plakken

Naast tabgescheiden drie- en vierkolomsinvoer ondersteunt **Plak zelf tekst**
nu **CSV: begintijd, eindtijd, eigen tekst** en
**CSV: begintijd, eindtijd, bron, betekenis**. Dit werkt voor eigen of
rechtmatig verkregen transcriptregels uit een spreadsheet.

Plak **zonder kolomkoppen** precies één ondertitelkaart per fysieke regel.
De parser accepteert komma's als scheidingsteken, volledig gequote velden
en verdubbelde dubbele aanhalingstekens voor een letterlijk aanhalingsteken.
Voorbeeld met drie kolommen (tijdcodes met komma moeten gequote zijn):

```csv
"00:00:01,500","00:00:04,000","Hallo, dit is mijn transcript"
"00:00:05,000","00:00:08,000","Nog een ""eigen"" zin"
```

Kies de vierkolomsmodus wanneer je een vierde kolom met een **zelfgeschreven
betekenis** aanlevert. Tuddy genereert geen vertalingen. Tijden worden
letterlijk overgenomen, niet uit audio afgeleid. CSV-velden met echte
regelovergangen worden bewust **niet** ondersteund: één kaart per fysieke
regel voorkomt onzichtbaar verschuivende bronnummers. Lege rijen, foute
kolomaantallen, ongeldige tijden en malformeerde quotes tonen een
rijgebonden melding. De bron blijft maximaal 256 KiB, 500 cues en
800 tekens per tekstkolom. De bron moet zowel als SRT als WebVTT geldig
blijven. Bestaande TSV-, SRT- en WebVTT-import, de volledige preview,
zoekfunctie en het kiezen van meerdere losse bronregelbereiken blijven
beschikbaar. Alleen expliciet gekozen rijen komen in de tijdelijke editor.
De Android-bestandskeuze bij later opslaan blijft een aparte handeling.
## Gevonden zinnen bewust als een eigen selectie overnemen

Bij het zoeken in de preview kun je nu op **Gebruik gevonden regels als selectie**
tikken. Tuddy vertaalt de momenteel **zichtbare zoekresultatenset** naar
een compacte, bewerkbare lijst originele bronnummers (bijvoorbeeld
`1-3,7,9-11`). De bestaande paginaweergave van 15 items beperkt dit niet:
alle resultaten binnen je huidige gekozen bronselectie tellen mee.
De selectie wordt **niet** automatisch aangepast als je de zoekterm
wijzigt; daarvoor moet je de knop bewust opnieuw gebruiken.
**Doorzoek opnieuw alle bronregels** zet de bronselectie terug naar
de volledige invoer. Je kunt de automatisch voorgestelde nummers vóór
het openen altijd zelf aanpassen in **Losse regels (optioneel)**.
Alle bronregels worden bij definitieve bevestiging opnieuw gelezen
en gecontroleerd. Zoeken noch deze selectieknoppen maken bestanden,
starten ASR of openen automatisch de editor.
## Meer dan één los bronregelbereik importeren

De plakpreview ondersteunt nu **Losse regels (optioneel)** voor bewust
gekozen niet-aaneengesloten originele bronregels. Bijvoorbeeld:

```text
1-5,8,12-16
```

Dit opent precies de oorspronkelijke rijen 1 t/m 5, 8 en 12 t/m 16
(in bronvolgorde) uit het **volledig opnieuw ingelezen** geldige document.
Een enkel nummer zoals `17` is ook toegestaan. Gebruik gewone
komma's en het minteken `-`, zonder impliciete jokertekens. Wanneer je
dit extra veld invult, vervangt het de eerdere **Van/Tot**-keuze;
wis het veld om weer alleen Van/Tot te gebruiken. Negatieve getallen,
nul, te hoge nummers, omgekeerde bereiken, een lege komma-groep of
overlappende/dubbele bronregels worden expliciet geweigerd.
De preview laat alle geselecteerde regels met **oorspronkelijke**
bronnummers en echte SRT/VTT-tijden zien; een zoekterm filtert alleen
wat je bekijkt en verandert de gekozen importregels nooit. Tuddy
herberekent de selectie op het actuele, volledige bronbestand bij
**Open gekozen regels**, zodat een eerdere preview geen onbedoeld
andere tekst laat binnenkomen. Ook wanneer er gaten in de gekozen
bronregels zijn, blijven de originele bron-tijdcodes behouden;
er is geen automatische tijdherverdeling, vertaling of netwerkverzoek.

## Een geselecteerd bereik uit een grote tekst openen

- Na het plakken toont Tuddy hoeveel geldige regels de complete bron bevat.
- Vul bij **Van regel** en **Tot (leeg = alle)** de gewenste regelnummers in. Het begint bij 1; een lege eindwaarde kiest tot de laatste bronregel. Ongeldige of omgekeerde bereiken worden afgewezen.
- De zoekfunctie en bladzijden van 15 kaarten tonen alleen het gekozen bereik, maar bewaren de **oorspronkelijke bronregelnummers**. Zoeken verandert nooit welke regels worden geopend.
- Bevestig met **Open gekozen regels**. De app leest de volledige actuele bron opnieuw, controleert opnieuw de gekozen grenzen en neemt precies dat bereik over in de tijdelijke editor, ook als een zoekfilter actief is.
- De niet-geselecteerde regels blijven buiten het tijdelijke concept. Deze handeling verwijdert of verandert niets in het bronbestand; een export blijft een aparte keuze via Android.

## Bron en eigen betekenis los bewerken na import

- In de bestaande ondertiteleditor open je een kaart en kies je **Handmatige tekstcorrectie**.
- Als die kaart uit precies twee tekstregels bestaat, toont Tuddy **twee aparte bewerkvelden**: de eerste tekstregel en de tweede zelfgeschreven betekenis. Bij een enkele of langere kaart blijft het gewone tekstveld beschikbaar. De app herkent of vertaalt hierbij geen talen.
- De twee velden blijven onafhankelijk wijzigbaar, elk maximaal 800 tekens. Bestaande tijden veranderen niet en opslaan van de nieuwe tekst blijft expliciet.
- Een ongeldige correctie sluit het venster **niet**: de ingevoerde tekst blijft zichtbaar en er verschijnt een concrete foutmelding. Corrigeer die tekst of annuleer zelf.
- Een geldige correctie wordt als één wijziging in de tijdelijke kopie toegepast en is via **Terug/Opnieuw** herstelbaar. Het geopende originele bestand blijft intact.

## Gerichte tekstcorrecties na import

In de tijdelijke ondertiteleditor open je **Zoek en vervang** (via het documentmenu) als een herhaalde schrijf- of vertaalkwestie in veel cues voorkomt.

1. Selecteer de **oorspronkelijke eerste en laatste regel** waar correcties zijn toegestaan. Leeg einde betekent alle regels tot het einde. Ongeldige of omgekeerde bereiken worden geweigerd.
2. Kies wat aangepast mag worden: **alle tekstregels**, **alleen de eerste regel** (zelf aangeleverde brontekst), of **alleen de tweede regel** (zelf geschreven betekenis). De tweede-regeloptie slaat eentalige cues zonder tweede regel over; er wordt nooit zelf een betekenis verzonnen.
3. Voer een letterlijke zoekterm en vervanging in, eventueel hoofdlettergevoelig. Bekijk **hoeveel cues en treffers** worden aangepast. Een scrollbare **vóór/na-preview toont de volledige tekst van iedere veranderde regel**, in bladerbare groepen van vijf. Bij een gewijzigde zoekterm, taalregel of selectie springt de preview terug naar het begin; er verdwijnen geen nog niet bekeken wijzigingen.
4. Bevestig afzonderlijk. Alle vervangingen vormen één bewerking in het tijdelijke concept; **Terug** maakt die ene wijziging ongedaan en **Opnieuw** kan haar terugzetten. Bestaande tijdcodes en regels buiten de gekozen selectie blijven onaangeroerd.
5. Bewaar pas later, indien gewenst, een nieuw SRT/VTT-bestand via de Android-documentkiezer. Originele bronbestanden of gesprekken veranderen niet.

Ook bij tweetalige kaarten bepaalt Tuddy **niet** welke taal de eerste of tweede regel werkelijk bevat; de volgorde blijft de verantwoordelijkheid van de gebruiker. Alle uitgaven blijven onder de bestaande 500-cue- en 256-KiB-limieten.

## Zelfgeschreven LRC-begintijden weer exporteren

Vanuit de tijdelijke ondertiteleditor kan je nu ook
**Bewaar eigen begintijden als LRC…** kiezen. De route is expliciet en
werkt zonder microfoon, netwerksessie, ASR of nieuwe opslagrechten.

1. Controleer je eigen ondertitelconcept. Elke cue moet **precies één
   tekstregel** hebben en de begintijden moeten **strikt oplopen**.
   Er mogen maximaal **500 cues** en **999 minuten** aan
   LRC-tijdsaanduiding worden meegenomen.
2. Kies **Bewaar eigen begintijden als LRC…**. Tuddy toont de
   **volledige werkelijk gegenereerde `[mm:ss.mmm]Tekst`-inhoud** en
   het exacte aantal UTF-8-bytes. De importparser moet het geheel
   kunnen lezen; anders wordt export geweigerd.
3. LRC heeft **geen einde-tijdveld**. Daarom worden eindtijden,
   eventuele overlap en audio-/woorduitlijning **niet geëxporteerd**.
   Na opnieuw inlezen bepaalt de bestaande LRC-import het einde uit
   de volgende begintijd en schat zij alleen het laatste einde.
   Als je die eindtijden wilt bewaren, kies dan **SRT, VTT, CSV of TSV**
   in plaats van LRC.
4. Na je expliciete bevestiging kies je zelf in Android de locatie
   voor een nieuwe `.lrc`-kopie. De Activity bewaart alleen de gekozen
   bounded snapshot tijdens deze externe bestandskeuze en berekent de
   volledige inhoud opnieuw voordat ze schrijft. Een latere,
   gewijzigde of ongeldige snapshot wordt niet stilzwijgend bewaard.
   Het origineel blijft intact.

Niet-ondersteunde tekens, ontbrekende regels, onjuiste tijdvolgorde of
overschreden 256 KiB worden geweigerd. De gekozen documentprovider
kan extern/cloudgesynchroniseerd zijn; Tuddy schrijft niet op de
achtergrond en vraagt geen blijvende URI-toegang. **Dit is een
handmatige lyrics-uitwisseling, geen bewijs van accurate audiotiming.**

## CSV/TSV met eigen tijdcodes weer opslaan en opnieuw importeren

Na bewerken van een tijdelijke ondertiteltijdlijn kun je nu naast SRT
en WebVTT ook **Bewaar tijdcodes als CSV…** of **Bewaar tijdcodes als TSV…**
kiezen in de bestaande editor. Dit is bedoeld voor eigen spreadsheet-
bewerkingen en weer importeren in Tuddy; er worden geen nieuwe betekenissen,
vertalingen of audiosynchronisatie gegenereerd.

1. Bewerk of voeg een ondertitelconcept toe via een bestaande SRT/VTT,
   handmatige CSV/TSV, LRC- of plakinvoerroute.
2. Kies **Bewaar tijdcodes als CSV…** (komma's, alle velden gequote) of
   **Bewaar tijdcodes als TSV…** (tabgescheiden). Het exportvenster
   toont **de volledige exacte UTF-8-inhoud**, alle bronregels, kolomaantal
   en bytegrootte vóór de Android-documentkiezer.
3. Wanneer **alle cues precies één tekstregel** bevatten, produceert
   Tuddy drie kolommen: `begin,einde,eigen tekst`. Wanneer **alle cues
   precies twee tekstregels** bevatten, krijgt elke cue vier kolommen:
   `begin,einde,eigen bron,eigen betekenis`. De tijdstempels worden
   exact in milliseconden geformatteerd. **Geen kopregel** wordt toegevoegd:
   het bestand past direct terug in de bestaande CSV/TSV-tijdimport.
4. Gemengde één-/tweeregelige fragmenten of tekst met meer dan twee
   regels worden geweigerd voor spreadsheetexport, niet stilzwijgend
   ingekort. SRT/VTT blijft beschikbaar voor volledige meerregelige cues.
   Tekst die een spreadsheet als formule kan uitvoeren (beginnend met
   `=`, `+`, `-` of `@`), verborgen Unicode-stuurtekens, tabs in
   een tekstveld of ongeldige Unicode leidt ook tot weigering: Tuddy
   verandert zulke bronnen **niet heimelijk** en biedt SRT/VTT als alternatief.
5. Bevestig de exacte preview en kies bewust je Android-opslagprovider.
   Na terugkeer controleert Tuddy opnieuw of de geëxporteerde bytes
   identiek zijn aan de vooraf bevestigde tijdelijke inhoud. Het
   oorspronkelijke ondertitelbestand wordt niet gewijzigd.
6. Importeer later met **Gewone zinnen → CSV: begintijd, eindtijd,
   eigen tekst** of **CSV: begintijd, eindtijd, bron, betekenis**,
   respectievelijk de bestaande drie- of vierkoloms tabopties voor TSV.
   De bestaande volledige preview en selectie bepalen afzonderlijk
   welke rijen als nieuw tijdelijk concept worden geopend.

Beide formaten zijn maximaal **256 KiB** en **500 cues**; de volledige
inhoud moet daarnaast exporteerbaar blijven als geldige SRT én WebVTT.
Er is geen automatische opslag, externe transcriptanalyse of extra
permissie. Een door jou gekozen externe Android-documentprovider kan
wel bestanden synchroniseren. Dit is een *handmatige uitwisselroute*,
geen bewijs van automatische vertaling of echte audiowoordtiming.

## Tweede spoor plakken en samenvoegen

1. Open een SRT/VTT-concept, vanuit een document of de plakroute.
2. Kies in de editor **Tweede ondertitelspoor zelf plakken…**.
3. Plak een tweede compleet document en beoordeel de korte preview.
4. Kies in de bestaande samenvoegpreview tussen **aansluiten**, **originele tijden behouden** of **overlappende tekstparen** (met optionele handmatige tijdsverschuiving).
5. Controleer het voorgestelde resultaat en geef apart toestemming om de samenvoeging alleen in het tijdelijke concept toe te passen. Undo/redo blijft bruikbaar.

## Grenzen en privacy

- Geen automatische clipboard-lezing, eigen netwerkclient, ASR, vertaling, cloudfallback of extra permissies. Bij bestandskeuze kan de externe Android-provider zelf een netwerkverbinding of cloudopslag gebruiken.
- Maximaal 256 KiB UTF-8 en 500 cues per bron; misvormde tijdcodes en niet-ondersteunde inhoud worden geweigerd. In de plattetekstroute zijn per cue maximaal 800 tekens toegestaan, en de resulterende SRT én WebVTT moeten binnen de exportlimiet passen.
- Een te grote plakactie wordt expliciet geweigerd, niet stilzwijgend afgekapt of vervangen door eerder geldige invoer. Handmatige paren mogen elk maximaal 800 tekens per kolom bevatten; lege/extra kolommen worden niet omgezet in een onjuiste betekenis.
- De geplakte inhoud blijft alleen in de geopende UI-sessie en verdwijnt bij sluiten of achtergrond. Geen automatische opslag.
- Opslaan is een aparte handeling via de Android-bestandskiezer; een gekozen documentprovider kan zelf cloudsync aanbieden.
- WebVTT-/SRT-tijdcodes en combinaties zijn **handmatig**. De app claimt geen gemeten woord- of audiouitlijning.
- Er is geen claim van fysieke Android-vliegtuigmodusacceptatie, release-ondertekening of volledige Kurmanji/Sorani/Pasjtoe-ASR/MT.
