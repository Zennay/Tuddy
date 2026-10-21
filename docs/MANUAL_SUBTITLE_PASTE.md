# SRT/WebVTT, LRC, eigen tekst en documenten importeren

Tuddy laat je handmatig een volledig SRT- of WebVTT-document plakken, zonder dat de app zelf je Android-klembord uitleest.

## Een ondertitel op een zelf gekozen tijdstip splitsen

In **Eigen ondertitelconcept · SRT/WebVTT** kies je bij een regel **Splitsen**.
De editor vult twee tekststukken voor je voor: bij bestaande meerregelige
tekst heeft een handmatige regelovergang voorrang, anders kiest Tuddy de
dichtstbijzijnde witruimtegrens rond het tekstmidden. Je kunt beide delen
vóór bevestiging vrij aanpassen. Laat **Exacte splitsingstijd** leeg om de
cue precies in het midden te delen, of voer zelf een tijdcode in zoals
`01:23.500` of
`00:01:23.500`. Komma en punt zijn toegestaan voor milliseconden.

De preview toont de huidige cue en de duur van beide voorgestelde delen.
Beide nieuwe cues moeten minimaal 0,6 seconde duren en de splitsgrens mag
niet voorbij het begin van de volgende cue vallen. Een ongeldige tijd,
te korte helft, lege/ongeldige tekst of onveilige volledige tijdlijn wordt
geweigerd; de editor blijft open zodat je de invoer kunt herstellen. De
split is één tijdelijke Undo/Redo-bewerking. Het oorspronkelijke bestand
wordt nooit overschreven en er wordt geen audio geanalyseerd of automatisch
gesynchroniseerd.


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

Het formaat accepteert nu **één tot zestien expliciete tijdstempels vóór
dezelfde tekstregel**. Een tijdstempel heeft precies het formaat
`[mm:ss.xx]` of `[mm:ss.xxx]`. Dit is handig voor eigen, herhaalde refreinen
waarbij meerdere keren exact dezelfde eigen tekst wordt gebruikt:

```text
[ar:Eigen artiest (optionele metadata)]
[00:01.25][01:10.50]Een zelfgeschreven refrein
[00:04.50][01:13.75]De volgende eigen refreinregel
[00:17.00]Eenmalige eigen tekst
```

De ene fysieke bronregel met twee tijdstempels wordt **twee afzonderlijke
cues**. Tuddy sorteert alle expliciete begintijden na de volledige import
chronologisch; bovenstaande regels worden dus `00:01.25`, `00:04.50`,
`00:17.00`, `01:10.50`, `01:13.75`. Het eindmoment van elke cue wordt
berekend uit het **volgende chronologische begin**. De laatste cue krijgt
een expliciete duurinschatting van hoogstens drie seconden. Deze volgorde
is in de native preview zichtbaar; kiezen op nummer verwijst daarom naar
een **uitgewerkte chronologische cue**, niet naar de oorspronkelijke
fysieke LRC-regel.

Maximaal 500 uitgewerkte cues, 16 tijdstempels per regel, 800 tekens
per tekstregel en 256 KiB geldige UTF-8 zijn toegestaan. Dubbele
tijdstempels (ook op andere regels), ongeldige chronologie na
samenvoegen, ontbrekende teksten en te veel items worden **volledig
geweigerd**; niet stilzwijgend overgeslagen. Lege regels worden
genegeerd. Informatieve LRC-metadata vóór de eerste cue (`ar`, `ti`,
`al`, `by`, `length`) worden genegeerd; onbekende directives worden
geweigerd. Optioneel kan **precies één eigen `[offset:+500]` of
`[offset:-250]`-header vóór de eerste tekstregel** staan. Deze waarden
zijn letterlijke **milliseconden**, geen seconden: positief verschuift
alle eigen begintijden **later** en negatief **eerder**. Alleen een
gehele waarde tussen **-30000 en +30000** wordt geaccepteerd.
Een resulterende negatieve of buiten-limiet tijd, meerdere offsetregels,
ongeldige syntax of een offset ná een tekstregel weigert het hele
document. In de Android-voorbeeldweergave staat de daadwerkelijk
verwerkte, expliciete offset naast de tijdcodes; er wordt niets uit
audio afgeleid en geen stille verschuiving uitgevoerd. Het
resultaat moet veilig als volledige SRT **én** WebVTT exporteerbaar zijn.

### De hele LRC-tijdlijn bewust 500 ms verschuiven

Als het eigen LRC-document geldig is, zie je naast de chronologische
cue-preview de actieve offset en drie knoppen: **− 0,5 s**,
**Offset 0** en **+ 0,5 s**. Je kunt ook **Exacte offset in ms**
invullen, bijvoorbeeld `-125` of `+750`, en die bewust met **Pas toe**
activeren. Alleen gehele waarden van -30000 tot +30000 milliseconden
worden geaccepteerd. De invoer blijft tijdens het typen in RAM en
wijzigt de tijdlijn pas na de bevestigingsknop. Iedere tik past alleen de ene zichtbare
`[offset:...]`-header in je **tijdelijke invoer** aan. Bij een offset
van nul verdwijnt die regel volledig; de zelfgeschreven zinnen en
expliciete oorspronkelijke tijdstempels veranderen niet. De
uitgewerkte tijdlijn wordt meteen opnieuw samengesteld, met dezelfde
maximumgrenzen en chronologische selectie. Als de verschuiving een
negatieve tijd, dubbele/onveilige tijd of ongeldige export oplevert,
weigert Tuddy de wijziging en blijft je oude eigen tekst staan. Je
kunt de toegevoegde header en resulterende tijdstempels vóór
overname zelf bekijken. Het **oorspronkelijke gekozen bestand** wordt
nooit ongemerkt aangepast of overschreven.

Volledige preview, zoeken, eigen cue-selectie en bewuste Android-keuze
blijven vereist. Tijdcodes zijn aangeleverd, **niet gemeten**, en
zeggen niets over correcte spraak-/woorduitlijning, ASR of MT.

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

## Lange CSV/TSV- en LRC-exportvoorbeelden zonder vastlopende interface

Wanneer je een groot eigen ondertitelconcept exporteert, wordt er **niet
meer één gigantisch tekstblok** in het bevestigingsvenster getekend.
Het venster toont nu de **werkelijke bestandsbytes als UTF-8-tekst
in doorbladerbare, begrensde gedeelten**. Tik op **Vorige** en
**Volgende** om alle delen van het volledige exportdocument te bekijken.
De pagina's behouden originele regelovergangen; de scrollpositie start
bij een volgende pagina opnieuw bovenaan. Het weergegeven gedeelte is
alleen een voorbeeld: na de aparte Android-documentkeuze wordt altijd
de **complete vooraf bevestigde snapshot** opnieuw berekend en
opgeslagen, niet alleen de getoonde pagina.

Deze leesbaarheidverbetering geldt zowel voor drie-/vierkoloms
**CSV/TSV** als eigen **LRC**-beginmomenten. De bestaande maximaal
500-cue- en 256-KiB-grenzen, broncontroles en expliciete toestemming
blijven ongewijzigd. Het zegt niets over echte ASR of audio-uitlijning.

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

## Tweede spoor plakken, handmatig uitlijnen en samenvoegen

1. Open een SRT/VTT-concept vanuit een document of de plakroute.
2. Kies in de editor **Tweede ondertitelspoor zelf plakken…** of selecteer via de bestaande expliciete Android-documentkiezer een tweede SRT/VTT.
3. Kies **aansluiten**, **originele tijden behouden** of **combineer twee teksten bij overlappende tijdcodes**. Voor het laatste zijn drie handmatige uitlijnroutes:
   - De bestaande presets van -10, -5, -2, 0, +2, +5 of +10 seconden.
   - **Eigen seconden tweede spoor**: typ bijvoorbeeld `-7,250` of `+12.125`. Punt en komma zijn toegestaan, met maximaal drie decimalen, maximaal **±600 seconden** (0 mag). De offset wordt exact in milliseconden berekend; bij ongeldige invoer wordt **toevoegen** uitgeschakeld in plaats van op de oude preset terug te vallen.
   - **Twee eigen startankers**: typ één **origineel 1-gebaseerd regelnummer** uit elk spoor, of blader door beide oorspronkelijke sporen (**drie regels per pagina**) en tik op de gewenste bronregel. De bladerlijst toont korte hints; de volledige zelfgeschreven tekst verschijnt als gekozen anker. Tuddy toont eerst de **volledige zelfgeschreven tekst, bronregelnummers en bestaande begin-/eindtijden van beide gekozen cues**, plus het verschil tussen hun **cue-begintijden**. Beoordeel zelf of ze overeenkomen. Kies dan bewust **Neem deze twee startmomenten als verschuiving**; wijzigen van de rijnummers blokkeert de koppeling totdat je opnieuw berekent, een eigen verschuiving invoert of een preset bewust kiest. Alleen bestaande 1–500-regelnummers en verschillen binnen ±600 seconden zijn geldig.
4. Bekijk de **volledige voorgestelde gecombineerde lijst** met starttijd, eindtijd en eigen tekst, in pagina's van vijf regels. Ook niet-gekoppelde regels en eventuele **identieke tekst in beide sporen** blijven zichtbaar; er wordt geen tweede eigen tekst stilzwijgend weggelaten. Een kandidaat wordt alleen aan één origineel gekoppeld wanneer de eigen tijdsintervallen minstens de helft van de kortste duur werkelijk overlappen. Dit is geen bewijs dat beide teksten bij dezelfde zin horen.
5. Bevestig expliciet **Voeg alleen toe aan tijdelijk concept**. Tuddy herberekent het exacte resultaat vanuit de huidige bron, offset en importkeuze; ongeldig tijdverloop, negatieve starttijden, meer dan 500 resulterende cues of niet-exporteerbare SRT/VTT worden geweigerd. Undo/Redo blijft bruikbaar.
6. Een bestand wordt **pas** geschreven na een latere bewuste **Bewaar kopie…**-actie in Android SAF, nooit door synchroniseren of een preview. Een zelfgekozen Android-provider kan wel synchroniseren.

Het uitlijnen gebeurt **uitsluitend op zelf aangewezen ondertitelbeginwaarden**, zonder audioanalyse, spraakherkenning, modelvertaling, woordtiming, toegangsuitbreiding of automatische bestandsopslag. De originele eerste en tweede documenten blijven intact.

### Bewust koppelen met behoud van alle bronregels

Bij **Combineer twee teksten bij overlappende tijdcodes** wordt nu de
**volledige tijdvolgorde van beide bestanden** bekeken, niet alleen
elk volgend cuepaar afzonderlijk. Tuddy zoekt onder de strikt
toegestane koppelingen naar zo veel mogelijk één-op-één paren en
kiest bij gelijk aantal de grootste gezamenlijke **werkelijke
tijdsoverlap**. Elke koppeling respecteert de bronvolgorde in
**beide** sporen. Een paar telt alleen mee wanneer ten minste
**de helft van de kortste eigen cue-duur** overlapt. Dit helpt
bij korte overlappende ondertitels waarvoor een lokale keuze
een later overeenkomstig paar zou blokkeren.

De samenvoegpreview toont hoeveel oorspronkelijke regels in
**beide** sporen ongekoppeld blijven. Via **Controleer elke
bronregel-koppeling** kun je voor alle voorgestelde paren in
pagina's van vier de **oorspronkelijke rijnummers, volledige eigen
teksten, originele begintijd en aangepaste begintijd van spoor twee,
en gedeelde milliseconden** bekijken. Dit blijft ook bij de aparte
tweepuntscorrectie zichtbaar, zodat je het effect van de handmatige
uitlijning op het werkelijke tweede bronbestand kunt beoordelen.
Ongekoppelde regels uit het tweede spoor blijven als
**zelfstandige ondertitelregels** bestaan; ongekoppelde regels
uit het eerste spoor blijven ongewijzigd. Zelfs **identieke
authored tekst** in beide sporen wordt niet stilzwijgend
weggegooid. De volledige samengevoegde kopie blijft apart
doorbladerbaar in pagina's van vijf.

Gebruik **Controleer elke bronregel-koppeling** om tot maximaal 500
voorgestelde bronparen gericht te onderzoeken: typ een letterlijk
woord of zinsdeel uit **één van beide originele teksten** (zoeken
negeert hoofdletters), en/of vul het **exacte oorspronkelijke
regelnummer uit spoor 2** in. De twee filters beperken uitsluitend
de vier-paren-per-pagina-weergave. Het echte koppelen, uitsluiten,
tijdcorrecties en de volledige resultaatpreview blijven gebaseerd
op het ongewijzigde bronbestand en dezelfde expliciete keuzes.
Een niet-gekoppelde bronregel toont terecht geen zoekresultaat
in de lijst van **gekoppelde** paren.

Via **Inspecteer niet-gekoppelde bronregels** kun je nu daarnaast
**alle losse originele regels uit beide sporen**, met hun werkelijke
1-gebaseerde bronnummers, volledige teksten en oorspronkelijke
begin-/eindtijden, vijf per pagina inspecteren. De aparte
**Zoek in losse originele brontekst**-invoer en de keuzeknoppen
**Beide / Spoor 1 / Spoor 2** filteren uitsluitend wat op het
scherm staat. Met **Ga naar resultaatregel** spring je in de
complete samengevoegde tijdlijn direct naar een regel (1–500)
zonder honderd keer op Volgende te hoeven tikken. Ook handmatig
uitgesloten tweede-spoorregels blijven hier vindbaar. Een
tijdsverschuiving of tweepuntscorrectie maakt de **bron** niet
anders: de daadwerkelijk toegepaste tijden blijven apart
zichtbaar in de volledige resultaatpreview. Deze inspectie
schrijft, vertaalt of verwijdert niets.

### Zelf twee oorspronkelijke bronregels bewust koppelen

Wanneer twee eigen SRT-/WebVTT-sporen inhoudelijk bij elkaar horen,
maar weinig of helemaal geen overlappende tijdstempels hebben, kan
de gebruiker nu **exacte oorspronkelijke bronnummers in beide
sporen** kiezen. Dit is **handmatige inhoudelijke koppeling**, geen
uit audio afgeleide timing, machinevertaling of taalherkenning.

1. Open bij **Combineer twee teksten bij overlappende tijdcodes**
   het gedeelte **Koppel zelf twee volledige oorspronkelijke bronregels**.
   Vul een eerste-spoor- en een tweede-spoor-bronregelnummer in.
   Vanuit **Inspecteer niet-gekoppelde bronregels** kun je bij een
   losse cue ook **Neem originele regel over voor handmatig koppelen**
   aantikken. Vanuit **Controleer elke bronregel-koppeling** kunnen
   de twee bronnummers van een automatisch voorgesteld paar samen
   worden overgenomen. Geen van deze knoppen past al een paar toe.
2. Lees beide **volledige eigen bronteksten** met hun echte originele
   begin-/eindtijden. Alleen **Voeg dit exacte eigen bronpaar toe aan
   het voorstel** verankert dat paar in het tijdelijke voorstel.
   De brontekst van spoor 1 krijgt dan de tekst van spoor 2 als
   tweede regel; de tijdcodes van **spoor 1** bepalen bewust de
   tijdcodes van de samengevoegde cue. De originele tweede-spoortijd
   blijft zichtbaar bij broninspectie en wordt niet als gemeten
   synchronisatie voorgesteld.
3. Je kunt **maximaal 32** handmatige paren kiezen. Elk oorspronkelijk
   bronnummer mag hoogstens één keer voorkomen; in beide sporen moet
   de oorspronkelijke chronologische regelvolgorde overeenkomen.
   Hetzelfde tweede-spoor-nummer kan niet tegelijk bewust uitgesloten
   en handmatig gekoppeld zijn. De app weigert ongeldige keuzes of
   niet volledig exporteerbare documenten, zonder de bestaande
   geldige voorstellen te verliezen.
4. Voor **alle overige** bronregels blijft de begrensde documentbrede
   één-op-één-tijdmatcher actief. Zijn keuzes worden rond de bewust
   vastgezette paren opnieuw bepaald; niet-gekoppelde regels blijven
   geheel zichtbaar in het overzicht van losse bronregels.
5. Via de vier-paren-paginareview of de aparte lijst van handmatige
   koppelingen kun je zo'n verankering met **Herstel automatische
   koppeling voor dit paar** weer wissen. Daarna kan de tijdmatcher
   opnieuw een eventueel andere combinatie voorstellen. Ook
   maximaal 32 vastgezette paren zijn met vier per pagina door te
   bladeren. De volledige resultaatpreview houdt vijf cues per
   pagina én de bestaande directe sprong naar een resultaatregel.

### Meerdere zelfgekozen bronparen in één invoer

Gebruik bij dezelfde handmatige samenvoeging **Bronpaarreeksen** voor
een zorgvuldig gecontroleerde bundel. Bijvoorbeeld:

```text
1:2,3-5:7-9,10:12
```

Dit betekent letterlijk: broneerste spoor **1→2**, **3→7**,
**4→8**, **5→9** en **10→12** in het tweede spoor. Een enkele
notatie heeft steeds links **eerste-spoor-rij: tweede-spoor-rij**;
bij een bereik moeten **beide delen exact evenveel originele regels**
omvatten. Met maximaal 512 tekens en maximaal 32 nieuwe én
al toegevoegde paren wordt nooit een onjuist ontbrekend rijdeel
bijgemaakt. De rijnummers moeten in **beide bronbestanden strikt
oplopen**; nul, dubbele, overlappende, omgekeerde, buiten-bestandse
of met bestaande handmatige koppelingen conflicterende keuzes
worden volledig geweigerd.

Wanneer de bundel geldig is, bekijk je alle **volledige originele
teksten met hun oorspronkelijke begintijden**, per vier voorgestelde
paren, vóór **Neem hele gecontroleerde bronpaar-bundel over**.
De knop is alleen beschikbaar als óók de huidige uitgesloten
regels, eerder gekozen handmatige paren, oorspronkelijke
chronologische regelvolgordes en hele SRT/VTT-uitvoer akkoord zijn.
De bundel wordt in één keer aan het **tijdelijke** voorstel
toegevoegd; hij wordt niet vanzelf als bestand opgeslagen. Per
paar kan hij daarna worden hersteld; **Herstel de tijdmatcher voor
alle handmatige paren** wist in één bewuste actie alle vastgezette
paren, niet de oorspronkelijke ondertitels of uitsluitingskeuzes.

**Modus wisselen:** handmatige paren en uitsluitingen gelden
alleen binnen de modus **Combineer twee teksten bij overlappende
tijdcodes**. Als je bewust overschakelt naar aansluiten of
originele tijdcodes naast elkaar behouden, worden deze
niet-relevante instellingen niet op de andere modus toegepast;
een terugkeer naar de overlapmodus kan je keuzes binnen
de huidige tijdelijke import opnieuw gebruiken.

**Definitieve toepassing blijft afzonderlijk:** de laatste
**Voeg alleen toe aan tijdelijk concept**-actie herberekent alle
handmatige paren, tijdcorrecties, uitsluitingen, oorspronkelijke
bronrijen, koppelingen en volledige exportlimieten vanaf de huidige
tijdelijke documenten. Een mismatch wordt geweigerd. Undo/Redo
en de los gekozen Android SAF-bestandslocatie blijven nodig;
originele bronbestanden worden nooit stilzwijgend gewijzigd.

### Een verkeerd voorgestelde koppeling handmatig afwijzen

Open in dezelfde tijdelijke samenvoegpreview **Controleer elke
bronregel-koppeling** en kies bij een voorgesteld paar **Laat tweede
bronregel als losse cue staan**. Tuddy sluit dan precies deze
oorspronkelijke **regel uit spoor twee** van verdere automatische
koppelingen uit. Het hele tijdlijnvoorstel wordt opnieuw opgebouwd,
zodat andere bronparen nog wel op volgorde gekoppeld kunnen worden.
De uitgesloten regel **blijft compleet aanwezig** met eigen
tekst en tijdcodes, nu als zelfstandige cue; er wordt niets verwijderd.
Dit werkt ook als twee eigen teksten identiek zijn.

Je kunt meerdere originele regels ineens behouden als losse cues:
vul bijvoorbeeld `2-5,9,12-15` in bij **Bronregels tweede spoor**
en tik daarna op **Houd deze bronregels los, zonder tekstverlies**.
Dezelfde expliciete actie werkt met **maximaal 16 niet-overlappende
zelf getypte tijdvensters** zoals
`00:10-00:20;01:30-01:45`: eerst **Neem volledige bronregels uit
tijdvensters**, controleer daarna de getoonde rijnummers en bevestig
pas dan het uitsluiten. De selectie gebruikt de oorspronkelijke
1-gebaseerde bronregelnummers van uitsluitend het tweede eigen
spoor, niet de eventuele zoekresultaten of reeds gekoppelde lijst.
Ongeldige, dubbele en overlappende rijbereiken zijn verboden.
Geen enkele invoer leest audio of verwijdert tekst.

Bij één of meer uitsluitingen verschijnt een aparte doorbladerbare
lijst met vier oorspronkelijke cueverwijzingen per pagina. Gebruik
**Sta koppeling van regel weer toe** om één uitsluiting terug te draaien
of **Sta alle eerder uitgesloten koppelingen weer toe** om opnieuw
vanaf alle originele bronregels te koppelen. Als meer losse regels
de bestandsgrootte of 500-cue-grens zouden overschrijden, wordt
toepassen geblokkeerd totdat je de selectie corrigeert; het
oorspronkelijke concept wordt nooit stilzwijgend ingekort.
De hele zichtbare lijst wordt bij definitieve bevestiging opnieuw
berekend, inclusief handmatige uitsluitingen en werkelijke koppelingen.

De algoritmische koppeling kijkt **alleen** naar geschreven
tijdstempels, niet naar de werkelijke betekenis van woorden,
sprekerherkenning, audio of media. Inspecteer de bronparen
zelf vóór bevestiging. Het model past niets automatisch toe.
Bij definitieve toestemming worden de hele gewijzigde
tijdlijn én de bronkoppelingen opnieuw vastgesteld en met
de goedgekeurde preview vergeleken. De bestaande SRT/VTT-
exportgrenzen, Undo/Redo en aparte Android SAF-save gelden
ook hier.

### Meerdere tijdankers voor ondertitels met ongelijk verloop

De bestaande keuze voor één exacte offset en de optie voor twee
handmatig gekozen ankerparen blijven werken. Voor sporen waarvan
het tijdverloop **in verschillende gedeelten een ander tempo**
heeft, kies je in **Combineer twee teksten bij overlappende
tijdcodes** de nieuwe knop **Meerdere tijdankers: corrigeer
2–8 bronparen…**.

1. Selecteer zelf twee tot acht bij elkaar horende
   oorspronkelijke bronregelparen uit de eerste en tweede
   SRT-/WebVTT-sporen. De lijsten van beide sporen zijn
   tekstdoorzoekbaar en tonen telkens drie oorspronkelijke regels
   met het echte originele bronnummer en de complete tekst op
   verzoek. Een tik op **Neem oorspronkelijke regel over** vult
   enkel een invoerveld in; hij koppelt niets automatisch.
2. Klik **Voeg dit bronankerpaar bewust toe** om een extra,
   chronologisch passende overeenkomst te plaatsen, ook tussen
   twee bestaande ankers. Met **Wijzig beide oorspronkelijke
   rijen** kun je een bestaand anker vervangen en met
   **Verwijder alleen anker** een extra anker herstellen.
   Minimaal twee ankerparen blijven nodig. Je kunt ook een
   complete, zelfgeschreven ankerreeks gebruiken zoals
   `1:1,100:99,240:237,500:497`. Deze invoer bevat de
   **oorspronkelijke 1-gebaseerde bronregelnummers** links uit
   spoor één en rechts uit spoor twee, geen milliseconden.
   Tuddy weigert ongeldige, dubbele, niet oplopende of
   onexporteerbare reeksen. Van de volledige voorgestelde
   reeks zie je eerst **alle originele zinnen en begintijden**
   voordat **Vervang alle ankers bewust door deze reeks**
   je eerdere selectie verandert. Er wordt niets automatisch
   gekoppeld op basis van tekst, audio of vertaalkennis.
3. De app toont voor ieder zelfgekozen anker **beide volledige
   oorspronkelijke teksten én begintijdstempels**. Voor ieder
   opeenvolgend paar toont hij de originele tweede-spoor-duur
   en de gewenste eerste-spoor-duur. De app berekent geen
   bij elkaar horende inhoud of taal: dat beoordeel je zelf.
4. De begrensde **segmentgewijze lineaire interpolatie**
   verplaatst alle begin- en eindtijden van het tweede
   spoor. Voor cues vóór het eerste of na het laatste anker
   blijft de verhouding van het dichtstbijzijnde segment
   van toepassing. Per anker mag het oorspronkelijke
   tijdverschil maximaal **±600 seconden** zijn en per
   aangrenzend segment is de schaal beperkt tot
   **0,5×–2×**, met strikt stijgende originele bronnummers
   én anker-begintijden. De app weigert omgekeerde, verdwenen
   of te korte tijdvakken en volledige kopieën die niet in
   zowel SRT als VTT veilig exporteerbaar zijn.
5. In de ankerselector verschijnt eerst de **complete
   oorspronkelijke tweede-spoortijdlijn**, met per bronregel
   (vijf per pagina) de **oude en nieuwe begin-/eindtijd**
   en de hele eigen brontekst. Je kunt direct naar een originele
   spoor-2-regel springen. Dat is een berekening uit de
   vier tot zestien handmatig gekozen bronnummers; er wordt
   geen audio herkend. **Neem alle tijdankers over** slaat
   nog niets op. Controleer daarna ook de volledige
   samengevoegde tijdlijn en de afzonderlijke originele
   ongekoppelde bronregels.
   Met de andere knop **Voeg alleen toe aan tijdelijk
   concept** bevestig je de op dat moment geheel
   opnieuw berekende combinatie. Dezelfde bronbestanden,
   ankerkeuzes, eventuele handmatige inhoudelijke koppelingen
   en uitgesloten rijen worden opnieuw gecontroleerd.
   Bewaren vereist later apart een bewuste Android-bestandskeuze.

De twee routes voor **tijd** en **inhoudelijke** koppeling zijn
verschillend: segmentankers verschuiven alleen tijdcodes van
het tweede spoor; een afzonderlijk handmatig bronpaar bepaalt
welke originele teksten op één tweetalige cue komen. Bij zo'n
tweetalige cue blijven de tijden van **spoor één** leidend.
Deze route werkt zonder internet, audioherkenning, automatische
vertaling of ongevraagde bestandsopslag. Een gekozen externe
Android-documentprovider kan zelf synchroniseren.

### Tijdverloop corrigeren met twee handmatig gekozen ankerparen

Als twee ondertitelsporen **aan het begin samenvallen maar verderop
geleidelijk uit elkaar lopen**, is een constante tijdverschuiving
ontoereikend. Kies in **Combineer twee teksten bij overlappende
tijdcodes** de aparte knop **Twee cueparen: corrigeer geleidelijk
tijdverloop…**.

1. Kies een **vroeg** overeenkomend cuepaar: één oorspronkelijk
   regelnummer uit het eerste en één uit het tweede spoor. Kies daarna
   een **later** overeenkomend cuepaar. Je kunt de vier regels intypen
   of per spoor **op originele tekst zoeken** (letterlijke, niet-hoofdlettergevoelige
   filter; tot 100 tekens), **naar een oorspronkelijk bronregelnummer springen**
   of **drie gefilterde regels per pagina doorbladeren**. Met **Lees volledige regel**
   kun je lange of meerregelige brontekst uitklappen voordat je bewust **Kies vroeg**
   of **Kies later** aantikt. De keuze blijft altijd gekoppeld aan het originele
   1-gebaseerde bronregelnummer; zoeken, springen, uitklappen en bladeren
   wijzigen nooit zelf een ankerpaar. De knop **Vul eerste en laatste
   regel per spoor in** is slechts een suggestie: controleer zelf of
   beide bronzinnen echt corresponderen.
2. De app toont van **alle vier de expliciet gekozen broncues** de
   volledige tekst en begintijd. Daarnaast zie je de originele
   tijdafstand en de gewenste afstand van de twee gekozen ankerparen.
   Dit is géén automatisch vastgestelde inhoudsrelatie.
3. Met **Neem twee ankerparen over** accepteer je slechts de gekozen
   configuratie; de app verandert op dat moment nog niets in je
   tijdelijke ondertitelconcept.
4. In de bestaande samenvoegpreview blader je daarna door de
   **volledige resulterende tijdlijn**, vijf regels per pagina,
   inclusief niet-gekoppelde cues. Pas **Voeg alleen toe aan tijdelijk
   concept** past deze gecontroleerde versie als één herstelbare
   Undo/Redo-stap toe. Voor een nieuwe bestandskopie blijft een aparte
   Android SAF-save nodig.

De tweede tijdlijn wordt **evenredig uitgerekt of samengedrukt** op
basis van twee expliciet gekozen startmomentparen. Bijbehorende
eindtijden schuiven mee; een klein verschil in framesnelheid kan zo
handmatig worden gecorrigeerd. Buiten de twee ankers wordt dezelfde
lineaire functie toegepast, zonder werkelijke audio- of woorduitlijning
te claimen.

Vereisten: minstens twee chronologisch verschillende bronregels in
beide sporen; per anker maximaal **600 seconden** verschil tussen
eigen beginmomenten; tijdschaal tussen **0,5× en 2×**; alle
resulterende tijden, tekst en complete SRT **én** WebVTT moeten binnen
de bestaande bestandsgrenzen blijven. Ongeldige, niet-chronologische
of buiten de grenzen liggende resultaten worden geweigerd. Je kunt
altijd bewust terugkeren naar **één constante verschuiving**.
Geen automatische opslag, cloudspraak, taalidentificatie of
bestandswijziging van de oorspronkelijk gekozen bronnen.

## Meerdere ondertitelregels bewust verwijderen (tijdelijke editor)

Bij een lange eigen SRT/VTT, LRC, CSV/TSV of geplakte tekst hoef je niet
langer ieder ongewenst segment afzonderlijk te openen en te wissen:

1. Open het eigen ondertitelconcept in de bestaande editor en kies
   **✂ Meerdere eigen ondertitelregels verwijderen…**.
2. Typ de **oorspronkelijke 1-gebaseerde regelnummers** in, bijvoorbeeld
   `2-4,7,12`. Die nummers horen bij de *huidige volledige tijdelijke
   lijst*, niet bij de zoekresultaten of de volgorde na eerdere deleties.
   Als je eerder op tekst zocht, kun je bewust **Neem gevonden regels als
   verwijderselectie** kiezen; alleen de volledige set van gevonden
   oorspronkelijke rijnummers wordt dan overgenomen.
3. Bekijk vóór bevestiging elke gekozen regel met originele tijdcodes
   en volledige tekst, in overzichtelijke pagina's van vijf. De preview
   vermeldt ook hoeveel ondertitels blijven staan. De zoekfunctie
   verandert de selectie nooit ongemerkt.
4. Kies **Verwijder gekozen regels tijdelijk**. De overige eigen
   tijdcodes en tekst blijven gelijk. Met **Terug** herstel je deze hele
   bewerking in één keer en met **Opnieuw** pas je hem opnieuw toe.
5. Alleen een latere **Bewaar kopie…** via de expliciete Android-
   bestandskiezer schrijft een nieuw document; het bestaande origineel
   en het live gesprek blijven ongewijzigd.

De selectie is strikt begrensd tot maximaal 500 cues, staat geen
dubbele/overlappende of omgekeerde intervallen toe, en kan nooit de
laatste cue verwijderen. Tuddy valideert de overblijvende volledige
tijdlijn opnieuw als exporteerbare SRT én WebVTT en berekent de exacte
geselecteerde regels bij bevestiging opnieuw. De actie doet geen
automatische inhoudsanalyse, vertaling, bestandsschrijfactie of netwerk-
aanroep. Bewerkte bestanden blijven uitsluitend in het tijdelijke RAM-
concept tot de gebruiker zelf opslag kiest.

## Zoek en vervang in losse bronregels, niet alleen één blok

De bestaande **Zoek en vervang · tijdelijk** heeft naast de **Van/Tot**
velden nu **Losse bronregels (optioneel)**. Bijvoorbeeld `1-3,7,12`:
alleen die oorspronkelijke nummers mogen worden aangepast. Als een
losse selectie is ingevuld gaat die vóór Van/Tot; maak het veld leeg
om het oude aaneengesloten bereik weer te gebruiken. Foute, dubbele,
overlappende of buitenbereik-nummers worden afgewezen, niet
automatisch hersteld. Via **Kies gevonden bronregels als bereik**
kun je na expliciet zoeken ook *alle* bijbehorende oorspronkelijke
rijnummers overnemen (niet alleen de zichtbare previewpagina).

Het blijft een **letterlijke** zoek- en vervangactie, geen regex,
vertaling of automatische correctie. De gebruiker kan kiezen voor
de complete cue, alleen de eerste eigen bronregel of alleen de tweede
zelfgeschreven betekenisregel. Een doorbladerbare **volledige
voor/na-preview** toont voor iedere getroffen cue exact wat verandert.
Bij bevestiging wordt de selectie opnieuw berekend uit het dan
actuele tijdelijke concept. De tijdcodes blijven staan, de volledige
SRT-/VTT-exportgrenzen worden hercontroleerd en de wijziging is in
één keer te herstellen via **Terug/Opnieuw**. Niet-geselecteerde cues
blijven byte-voor-byte gelijk.

## SRT en WebVTT: volledige exacte preview vóór nieuw document bewaren

Kies in de tijdelijke editor **Bewaar kopie SRT…** of **Bewaar kopie
VTT…**. In plaats van een enkele algemene waarschuwing toont Tuddy
nu de **complete werkelijke UTF-8-bestandsinhoud** met alle originele
tijdcodes en teksten, opgesplitst in korte doorbladerbare pagina's.
Je ziet zowel de totale bytegrootte als het aantal cues. Niet
exporteerbare documenten krijgen geen actieve opslagknop. Bij
**Kies opslaglocatie…** worden alle bytes opnieuw uit het huidige
concept berekend en met de getoonde inhoud vergeleken. Alleen na
die bevestiging vraagt Android om een bestandsprovider, die
eventueel zelf online kan synchroniseren. Geen automatische cloud-
spraakverwerking, schrijfacties vóór toestemming of verifieerbare
audio-/woordtiming wordt hierdoor geclaimd.

## Alleen gevonden regels als SRT, VTT, CSV, TSV of LRC bewaren

Zoek in het tijdelijke ondertitelconcept op een woord of zin en kies
**↳ Bewaar alleen gevonden regels als SRT/VTT…**. De bestaande
zoekselectie toont nog steeds de eigen oorspronkelijke regels; er
worden geen niet-gevonden ondertitels meegenomen. In het vernieuwde
expliciete exportvenster kun je kiezen uit **SRT, WebVTT, CSV, TSV of
LRC**. De knopnaam verwijst om compatibiliteitsredenen nog naar
SRT/VTT, maar alle vijf opties worden in het venster aangeboden.

Kies **Behoud oorspronkelijke tijdcodes** als je de oorspronkelijke
begins/uitloop wilt behouden, of **Maak aaneengesloten excerpt vanaf
00:00** om de geselecteerde regels zonder tussengelegen pauzes te
exporteren (0,25 s tussen de kaarten). Dit is een handmatige
tijdlijnbewerking, **geen gemeten audio-uitlijning**. Het is geen
automatische tekst- of spraakvertaling.

- **SRT/VTT** bevatten de volledige eigen tekst en begin-/eindtijden.
- **CSV/TSV** zijn alleen beschikbaar als alle gekozen cues één
  tekstregel (3 kolommen) of allemaal precies twee regels (4 kolommen)
  hebben; onveilig spreadsheetinvoer en ongeldige tekst worden
  geweigerd. Ze zijn zonder kopregel direct opnieuw importeerbaar.
- **LRC** is uitsluitend bedoeld voor één eigen tekstregel per cue
  met strikt oplopende begintijden. **Eindtijden gaan verloren**;
  gebruik SRT/VTT/CSV/TSV als je ze wilt behouden.

Het exportvenster laat **alle werkelijk te schrijven UTF-8-bytes**
zien in doorbladerbare deelpagina's, plus de bron-/excerptlengte en
bestandsgrootte. Niet veilig exporteerbare combinaties zetten de
opslaan-knop uit in plaats van inhoud stilzwijgend weg te laten.
Vlak vóór opslag herberekent Tuddy de exacte export op basis van
de actuele tijdelijke selectie, en de gedeelde Android-bestandsroute
controleert de snapshot nogmaals na de bewuste providerkeuze.
Het origineel blijft ongewijzigd. Een provider die je zelf kiest
kan extern synchroniseren.

## Bewaar eigen originele bronregels — ook zonder zoekopdracht

Vanuit de tijdelijke editor kies je **Kies zelf bronregels om te bewaren…**.
Vul een reeks zoals `1-5,9,12-16` in. Je kunt ook op **Alle regels**
tikken, of vanuit een actieve zoekopdracht **Bewaar gevonden regels als
SRT, VTT, CSV, TSV of LRC…** kiezen. Die tweede route initialiseert
de selectie met de oorspronkelijke regelnummers van *alle* zoekhits
uit het hele document. Binnen beide routes kun je bewust terug naar
die gevonden selectie of de **overige niet-geselecteerde regels** nemen.

Het venster laat de originele regelnummers, begin-/eindtijden en
volledige tekst van de gekozen regels in pagina's van zes zien. Een
losse range wordt begrensd tot 1–500 cues; dubbelingen, overlap,
omgekeerde of onbestaande nummers zijn een fout, geen stille
correctie. Alleen een geldige selectie leidt tot een volledige,
paginagewijze preview van het werkelijke UTF-8-uitvoerbestand.
Daarna kies je oorspronkelijke tijdcodes of een compact fragment
en het uitwisselformaat SRT, WebVTT, CSV, TSV of LRC. Formaat-
specifieke inhouds- en bytegates blijven intact; LRC bevat
uitsluitend beginmomenten. Elke wijziging van je selectie past
onmiddellijk de preview aan. Op het moment dat je wilt opslaan
worden selectie en exacte uitvoerbytes opnieuw berekend; pas
daarna opent Android de expliciete bestandsprovider.

De bronfile, onbewerkte regels en live transcriptie veranderen niet.
De gekozen bestandsprovider kan zelf cloudsynchronisatie aanbieden.
Dit is geen automatische vertaling of gecontroleerde audio-uitlijning.

## Verplaats alleen geselecteerde ondertitel-tijdcodes

In de tijdelijke editor kies je **⏱ Alleen gekozen bronregels in tijd
verschuiven…**. Typ één of meerdere originele regelnummers, bijvoorbeeld
`2-4,7,12`. Bij een actieve zoekopdracht kun je alle gevonden
oorspronkelijke bronnummers bewust overnemen. Kies vervolgens
**−5, −2, −0,5, +0,5, +2 of +5 seconden**. Alle oorspronkelijke
teksten en alle niet-geselecteerde cue-tijden blijven ongewijzigd.

De volledige tijdlijn wordt na de voorgestelde wijziging opnieuw
gecontroleerd: een negatief begin, te laat einde, omgekeerde of
nulduur, teruglopende begins bij grensregels of onveilige SRT/VTT
wordt geweigerd. Overlappende opeenvolgende cues worden geteld
en getoond, maar al aanwezige overlap blijft toegestaan volgens de
bestaande ondertitelexportregels. Het dialoogvenster toont voor
**iedere gekozen bronregel** in pagina's van vijf de volledige eigen
tekst én de oorspronkelijke en nieuwe begin-/eindtijd. Er is geen
afleiding van audio of herkenning van spreker/taal.

Pas de verschuiving bewust toe met **Verschuif gekozen regels
tijdelijk**. De hele correctie is één **Terug/Opnieuw**-stap en
schrijft nog niets naar je bronbestand. Een nieuw bestand bewaren
vraagt later afzonderlijk om jouw Android-bestandsprovider. Er zijn
geen nieuwe permissies, accounts of netwerkverzoeken.

## Bronregels uit een gekozen tijdvenster

### Meerdere tijdvensters in één selectie

Bij **Kies zelf bronregels om te bewaren…**, **Alleen gekozen
bronregels in tijd verschuiven…**, **Meerdere eigen ondertitelregels
verwijderen…** en **Zoek en vervang · tijdelijk** kun je in het extra
veld met een `vanaf-tot;vanaf-tot`-voorbeeld meerdere losse
intervallen typen:

```text
00:10-00:20;01:00-01:15;02:35-02:42.500
```

Kies daarna bewust **Neem cues uit alle gekozen tijdvensters**.
Tuddy selecteert complete ondertitels die één of meer intervallen
overlappen en toont de **unieke oorspronkelijke bronregelnummers**
voordat je iets toepast of bewaart. Ook een lange cue die meerdere
gekozen vensters raakt, verschijnt maar één keer. Invoer heeft
maximaal **16 intervallen / 512 tekens**, elk met een echte
starttijd vóór de eindtijd. Lege, overlappende of omgekeerde vensters
zijn ongeldig; aangrenzende vensters zijn toegestaan. De volgorde
van de getoonde cues blijft de originele bronvolgorde, ook als
tijdvensters in een andere volgorde zijn ingevoerd. De bestaande
enkele **Vanaf/Tot**-selectie en handmatige bronregelnummers blijven
beschikbaar.

Dit werkt in alle vier handmatige acties: **selectief exporteren**, **tijd verschuiven**, **regels verwijderen** en **letterlijk tekst vervangen**. De huidige, originele rijselectie wordt pas na de expliciete knop vervangen; daarna gelden nog steeds de bestaande volledige voor/na-preview en Undo/Redo. Geen venster wijzigt vanzelf de keuze.

Dit doet **geen** audioanalyse of afkappen van captions.
Het exportdialoog biedt nog steeds uitsluitend na volledige
bestandsvoorbeeldcontrole een eigen Android SAF-opslagkeuze;
de tijdverschuiving blijft een expliciete herstelbare editorbewerking.


In **Kies zelf bronregels om te bewaren…** en **Alleen gekozen
bronregels in tijd verschuiven…** kun je naast regelnummers ook een
**Vanaf**- en **Tot**-tijd invullen, in `mm:ss`,
`mm:ss.fff` of `hh:mm:ss.fff` (een komma is ook toegestaan).
Bijvoorbeeld vanaf `01:15` tot `02:10`.

Kies daarna bewust **Neem overlappende cues uit tijdvenster** of
**Selecteer cues die tijdvenster overlappen**. Een ondertitel wordt
geselecteerd als zijn bestaande begin vóór het einde van het
venster ligt **én** zijn einde na het begin van het venster ligt.
Zo worden ook lange captions met een begin vóór het venster
meegenomen wanneer zij nog gedeeltelijk binnen het venster vallen.
Dit kiest **de volledige cue**: eigen tekst en tijdcodes worden
niet ingekort of herschreven, en er wordt geen audio gelezen.

Een ongeldig, leeg of omgekeerd venster en een venster zonder
overlappende cues activeren de selectieknop niet. Het resultaat
wordt bewust omgezet naar exact dezelfde strikt begrensde
originele 1-based bronregelreeksen als bij handmatige invoer.
Na het selecteren blijft de bestaande volledige bron-/export-
preview of vóór/na-tijdcodepreview verplicht, evenals de
separate expliciete bevestiging. Taalmodel, vertaling,
netwerktoegang en extra toestemmingen zijn niet vereist.

## Na fragmentexport: het volledige tijdelijke concept blijft intact

Wanneer je alleen gevonden regels, een handmatig regelbereik of een
tijdvenster als SRT/VTT/CSV/TSV/LRC bewaart, hoort het **opgeslagen
bestand** alleen de gekozen regels te bevatten. Dat betekent **niet**
dat de geopende tijdelijke editor met die subset mag worden vervangen.

De Activity-owned Android-opslagroute bewaart daarom afzonderlijk,
uitsluitend in RAM tijdens de bewust geopende documentkiezer:

- het **volledige huidige bewerkbare ondertitelconcept**, dat na
  Androids DocumentsUI wordt heropend, ook bij annulering of
  mislukte opslag;
- de **expliciet gekozen te exporteren cues** met alle exacte
  vooraf bevestigde bytes, die bij terugkeer worden herberekend en
  uitsluitend naar de gekozen bestandsprovider worden geschreven.

Dat geldt nu identiek voor **SRT, VTT, CSV, TSV én LRC**,
inclusief een selectie uit zoeken, handmatige bronranges, of
het complement daarvan. Het herstellen van de volledige editor
betekent geen wijziging van de eigen originele bronfile;
het reeds bevestigde exportbestand kan bewust kleiner zijn.
Bij verlopen sessietoestemming, ongeldig formaat of gewijzigde bytes
wordt niet alsnog opgeslagen. Een gekozen externe Android-provider
kan zelf online synchroniseren. Undo-geschiedenis in de dialoog
wordt niet als langdurige opslag bewaard.

## Handmatige tijdcorrectie tot op de milliseconde

In **Alleen gekozen bronregels in tijd verschuiven…** werken de zes
vaste knoppen (±0,5, ±2 en ±5 seconden) nog steeds. Daarnaast kun je
nu in **Eigen seconden** een zelfgekozen verschuiving opgeven, zoals
`-7,250`, `+12.345` of `0,125`. Positief schuift later, negatief
eerder. Een punt of komma is toegestaan, met maximaal drie decimalen;
de berekening gebeurt **exact in milliseconden** en rondt niet via
floating point af.

Het bereik is **-600 tot +600 seconden**, exclusief nul.
Een ongeldig getal schakelt de bevestiging uit in plaats van terug
te vallen op een eerder gekozen preset. Een preset opnieuw kiezen
wist de handmatige waarde bewust. De volledige gekozen bronregels
worden vóór toepassing met oude en nieuwe tijden getoond; ieder
ongewijzigd cue blijft intact. Een verschuiving die een tijd
negatief maakt, een bronregel voorbij de volgende schuift of niet
als volledige SRT én VTT exporteerbaar is, wordt geweigerd.
De verandering blijft één Undo/Redo-stap in RAM; bestandsopslag
vereist nog steeds apart expliciete toestemming.

## Tijdvensters ook voor verwijdering en tekstcorrecties

Hetzelfde eigen **Vanaf/Tot**-tijdvenster kan nu vanuit de
tijdelijke editor worden omgezet naar een bronregelselectie in
**Meerdere eigen ondertitelregels verwijderen…** en in
**Zoek en vervang · tijdelijk**. De selectie wordt pas
overgenomen als je expliciet op de bijbehorende knop tikt.
De app kiest complete cues waarvan de bestaande tijden het
gevraagde venster overlappen; hij knipt geen regels of tijdcodes
automatisch af.

Daarna zie je bij verwijderen alle gekozen originele tekstregels
en tijden, inclusief het aantal resterende cues, vóór de bewuste
bevestiging. Bij Zoek/Vervang zie je de **volledige voor/na-tekst**
van iedere daadwerkelijk getroffen originele cue. In het
laatste venster blijft de keuze voor eerste regel, tweede eigen
betekenis of volledige tekst gelden. Een invalid tijdvenster
wijzigt de selectie niet, en bestaande Undo/Redo en
SRT/VTT-limieten blijven actief.

Dezelfde selectiemethode bestaat dus in vier acties:
**export**, **tijdverschuiving**, **verwijdering** en
**gerichte letterlijke vervanging**. Elke actie herberekent
zijn huidige bronregels bij bevestiging en raakt het live gesprek
en oorspronkelijke bestand niet.

## Alleen geselecteerde LRC-cues handmatig verschuiven

In **LRC: eigen begintijden per regel** kun je een eigen LRC-bron (ook
een refreinregel met meerdere tijdstempels) openen en de uitgewerkte
chronologische cue-nummers kiezen via **Van/Tot** of
**Losse regels** (bijvoorbeeld `3-5,9`). Dit is geen audiomatching.

Vul onder **Alleen geselecteerde regels ± milliseconden** een expliciete
waarde in, bijvoorbeeld `+1200` of `-750` (maximaal ±30.000 ms).
De app berekent een volledige, gebladerde oude/nieuwe tijdlijn van alle
cues, inclusief gewijzigde **eindtijden van aangrenzende cues**.
Alleen na **Pas deze geselecteerde LRC-verschuiving bewust toe**
verandert het tijdelijke tekstconcept in het invoervak. Het origineel
bij de Android-bestandsprovider wordt nooit aangepast.

Zowel de correctie voor het hele LRC-spoor als een correctie van
geselecteerde cues heeft nu **↶ LRC ongedaan** en **↷ Opnieuw**
binnen dezelfde invoer. Maximaal de laatste acht bewuste tijdacties
blijven uitsluitend in RAM bewaard, zodat je meerdere correcties kunt
vergelijken zonder opnieuw een eigen bronbestand te openen. Vrij
handmatig typen in het ruwe tekstvak begint uitdrukkelijk een nieuwe
tijdactiegeschiedenis; onbedoeld terugzetten van daarna geschreven
eigen tekst wordt zo voorkomen. Sluiten van het dialoogvenster
verwijdert deze tijdelijke geschiedenis.

Bij lange bestanden van maximaal 500 cues toont de preview
**Vorige wijziging**, **Volgende wijziging** en
**Ga naar eerste gewijzigde regel**. Die knoppen springen niet
alleen naar aangepaste begintijden, maar ook naar aangrenzende
regels waarvan de **eindtijd** verschuift doordat een volgend
LRC-begin veranderde. Normaal pagineren door alle regels blijft
beschikbaar, ook voor ongewijzigde cues.

De bestaande [offset:]-header blijft exact behouden. De wijziging
past alleen de door de gebruiker gekozen timestamp-tags aan, ook
wanneer meerdere refreinmomenten op één fysieke bronregel staan;
eigen lyrics, titel, artiest en overige toegestane metadata blijven
ongewijzigd. Een verschuiving die de totale chronologische volgorde
zou omkeren, dubbele tijdstempels zou geven, een start vóór nul zet
of een ongeldige SRT/VTT-uitkomst veroorzaakt, wordt **geheel geweigerd**.
De selectie blijft na zo'n weigering behouden; er is geen gedeeltelijke
overschrijving of verkapte fallback. De toekomstig opgeslagen
SRT/VTT-eindtijden volgen nog steeds uit het volgende LRC-begin
(laatste eindtijd blijft geschat), niet uit opgenomen audio.

## Compact kiezen tussen vier LRC-correctiegereedschappen

De invoer toont na het kiezen van **LRC** niet meer alle grote
correctiepanelen tegelijk. Kies bewust één optie:
**Verschuif selectie**, **Meerdere exacte tijden**,
**Eén exact tijdstip** of **Twee tijdankers**.
Alleen het gekozen formulier en zijn huidige voor/na-preview
worden getoond. De gebruiker kan op elk moment wisselen zonder
de eigen brontekst of de nog niet toegepaste conceptinvoer
van een ander gereedschap stilzwijgend te verwijderen.
De afzonderlijke expliciete bevestigingen en de bestaande
ongedaan/opnieuwgeschiedenis blijven ongewijzigd.

## Gecorrigeerde LRC-bron mét metadata als nieuw bestand bewaren

In de primaire **Plak eigen SRT/WebVTT-tekst → LRC**-dialoog
vind je na een volledig geldige LRC-invoer ook
**Bewaar deze volledige LRC-bron als nieuw .lrc-bestand…**.
Dat bewaart het **actuele, eventueel gecorrigeerde eigen
UTF-8-brondocument** met alle expliciete refreintags, lyrics,
toegestane artiest-/titelmetadata en `[offset:]` als bronkopie.
Dit verschilt bewust van een uit SRT/VTT-cues gereconstrueerde
LRC-export: bij die eerdere route kunnen bronheaders niet
worden teruggevonden.

De Android **CreateDocument**-bestandskiezer vraagt apart
om een nieuwe opslaglocatie. Alleen na een expliciete keuze
en hercontrole van de complete begrensde bronmomentopname
worden bytes geschreven; het oorspronkelijk gekozen bestand
wordt niet door Tuddy aangeraakt. De inhoud blijft tijdens
DocumentsUI uitsluitend als tijdelijke RAM-snapshot beschikbaar.
Na annuleren, een ongeldige providerlocatie of mislukte opslag
wordt het bewerkbare eigen concept met een duidelijke melding
opnieuw geopend. Een externe Android-provider kan zelf
cloudsync aanbieden; Tuddy doet dit niet. Een handmatig
gewijzigde of ongeldige volledige LRC, misvormde UTF-8,
meer dan 500 uitgewerkte cues of 256 KiB wordt geweigerd.
De functie bewaart dus alleen een eigen brondocument en
voegt geen geautomatiseerde vertaling of audioverwerking toe.

## Meerdere foutieve LRC-begintijden in één bewuste correctie

Als verscheidene eigen LRC-tijdstempels verkeerd zijn,
gebruik **Meerdere eigen LRC-begintijden tegelijk corrigeren**.
Schrijf maximaal 32 unieke oorspronkelijk genummerde cue's met
hun nieuwe absolute `uu:mm:ss.mmm`-tijdcode, bijvoorbeeld:

`1=00:00:02.000;4=00:01:07.250`

Een puntkomma **of** een nieuwe regel scheidt toewijzingen.
Met **Vul gekozen cues met huidige begintijden in** maakt Tuddy
optioneel een bewerkbaar vertrekpunt van maximaal 32 reeds
geselecteerde oorspronkelijke cues. Alleen bewust aangepaste
waarden leveren een wijziging op; er wordt geen tijd berekend
uit muziek, spraak of ondertitelbetekenis.

De complete gewijzigde tijdlijn is per vijf regels te bekijken,
met een directe sprong naar vorige of volgende gewijzigde
begin-/eindtijd. De knop **Bevestig alle exacte LRC-tijdcorrecties**
past alle opgegeven correcties **atomair** op de tijdelijke lokale
LRC-tekst toe; eerdere waarden zijn via de bounded Undo/Redo
terug te halen. Ongeldige nummers, dubbele nummers, meer dan
32 opdrachten of 512 invoertekens, ongeldige tijden, een
verandering die cues omwisselt, tijdcodebotsingen en
onexporteerbare SRT/VTT-resultaten worden als geheel geweigerd.
Een te lange plakactie wordt zichtbaar geweigerd en kan niet
stilzwijgend een eerder geldige opdracht toepassen. Het
door de gebruiker gekozen originele bestand blijft onveranderd.

## Eén oorspronkelijk LRC-begin exact corrigeren

Wie slechts één verkeerd LRC-tijdstempel heeft, hoeft niet langer
alle geselecteerde cues met dezelfde offset te verschuiven:
**Corrigeer één foutief LRC-begin exact** biedt een afzonderlijke
1-gebaseerde originele **bronregel** en gewenste absolute
**uu:mm:ss.mmm-begintijd**. Met **Vul eerste geselecteerde cue en
zijn huidige tijd in** wordt eerst uitsluitend een uitgangspunt
ingevuld, zonder direct iets te veranderen. Een correctie van
minuten kan hiermee ook wanneer de ±30-seconden-groepscorrectie
niet geschikt is.

De volledige voor/na-preview toont ook de mogelijk veranderde
**eindtijd van de onmiddellijk voorgaande cue**; je kunt
naar de eerste of volgende gewijzigde pagina springen en alle
bronregels bekijken. Pas **Bevestig deze ene exacte LRC-tijdcorrectie**
wijzigt het tijdelijke concept. Dezelfde bounded Undo/Redo-functie
geldt. De actie wordt geheel geweigerd bij gelijke oude/nieuwe
tijd, niet-bestaand bronnummer, dubbele of omgekeerde tijdstempel,
negatieve tijd, ongeldige metadata of niet-exporteerbare volledige
SRT/VTT. Het bestand bij de gekozen provider blijft intact.

## Tweepuntscorrectie van een eigen LRC-ondertitelreeks

Naast een constante verschuiving kunnen twee **expliciet door jou
gekozen oorspronkelijke bronregels** een aaneengesloten tijdvak
opnieuw verdelen. Stel in de LRC-plakdialoog eerst **Van regel**
en **Tot regel** in of kies één aaneengesloten lijst met minstens
twee uitgewerkte chronologische cue-nummers. Losse niet-aangrenzende
regels worden bewust geweigerd.

Bij **Tweepuntscorrectie** kun je de twee bestaande begintijden
eerst laten invullen en vervolgens de gewenste nieuwe begintijden
bewerken, bijvoorbeeld van `00:00:12.000`/`00:00:35.000`
naar `00:00:11.500`/`00:00:34.000`.
Alle tussenliggende cues worden uitsluitend volgens hun bestaande
LRC-begintijden tussen de twee door jou gekozen tijdankers opnieuw
verdeeld. Het vroegere en latere anker blijven exacte tijdstempels.
Het toegestane tijdsverloop is 0,5× tot 2× het originele interval.

De **volledige oude/nieuwe preview** omvat iedere bronregel en toont
ook gewijzigde eindtijden van de cue vóór het eerste anker. Blader
vijf regels tegelijk of spring naar het volgende gewijzigde blok.
Pas na **Bevestig deze volledige tweepuntscorrectie** verandert
de lokale LRC-tekst. Dezelfde beperkte **ongedaan/opnieuw**
bediening geldt als bij één losse tijdsverschuiving.

Originele teksten, eigen refreinherhalingen, overige tijdstempels
buiten de ankers, `[offset:]` en toegestane metadata worden
niet herschreven. Waar de correctie een tijdlijn zou omkeren,
starttijden zou laten samenvallen, een tijd buiten het veilige
bereik zou brengen of export als gehele SRT én VTT zou verhinderen,
wordt het voorstel **volledig geweigerd**. Er zijn geen
geanalyseerde audiowoorden, vertalingen of automatisch
herkende overeenkomsten en het originele providerbestand
wordt niet overschreven.

## Grenzen en privacy

- Geen automatische clipboard-lezing, eigen netwerkclient, ASR, vertaling, cloudfallback of extra permissies. Bij bestandskeuze kan de externe Android-provider zelf een netwerkverbinding of cloudopslag gebruiken.
- Maximaal 256 KiB UTF-8 en 500 cues per bron; misvormde tijdcodes en niet-ondersteunde inhoud worden geweigerd. In de plattetekstroute zijn per cue maximaal 800 tekens toegestaan, en de resulterende SRT én WebVTT moeten binnen de exportlimiet passen.
- Een te grote plakactie wordt expliciet geweigerd, niet stilzwijgend afgekapt of vervangen door eerder geldige invoer. Handmatige paren mogen elk maximaal 800 tekens per kolom bevatten; lege/extra kolommen worden niet omgezet in een onjuiste betekenis.
- De geplakte inhoud blijft alleen in de geopende UI-sessie en verdwijnt bij sluiten of achtergrond. Geen automatische opslag.
- Opslaan is een aparte handeling via de Android-bestandskiezer; een gekozen documentprovider kan zelf cloudsync aanbieden.
- WebVTT-/SRT-tijdcodes en combinaties zijn **handmatig**. De app claimt geen gemeten woord- of audiouitlijning.
- Er is geen claim van fysieke Android-vliegtuigmodusacceptatie, release-ondertekening of volledige Kurmanji/Sorani/Pasjtoe-ASR/MT.
