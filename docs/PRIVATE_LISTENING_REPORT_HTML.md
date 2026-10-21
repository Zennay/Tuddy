## Nieuw · gedetailleerde bronregel-CSV voor eigen A–B-luisterfragmenten

Naast de bestaande compacte CSV kun je in **Mijn privé luisteroverzicht**
de knop **Bekijk volledige bronregel-CSV vóór opslaan** gebruiken.
Deze toont de werkelijk te bewaren CSV-rijen in bladzijden van maximaal
12 CSV-rijen met **Vorige/Volgende CSV-pagina**, zodat ook lange exports
op kleine Android-schermen inspecteerbaar blijven. Navigeren
verandert niets aan het volledige bestand. Elke passende cue heeft één
aparte rij voor de eigen SRT/VTT-ondertitelregel die **helemaal**
binnen een bewust
gemarkeerd A–B-fragment valt. De bronregelnummers blijven 1-gebaseerd,
met begin en einde op de eigen huidige milliseconde-tijdlijn. Elke rij
vermeldt ook het gekozen luisterfragment, diens begin/eind, de eigen
beoordeling en je optionele eigen volgende oefenstap. Zonder passende
regel schrijft de export een expliciete afwezigheidsmelding in plaats
van ten onrechte audioherkenning te suggereren. Dit is geen echte
audio-/ondertiteluitlijning en er wordt niets voorspeld of vertaald.

Met **Bewaar alle passende bronregels als CSV…** start pas de
expliciete Android-documentkeuze. Het bestand krijgt een andere
voorgestelde naam dan de compacte CSV. De output is strikt UTF-8,
altijd CSV-gequote, tegen formuleprefixen beschermd en begrensd op
128 KiB; te lange of ongeldige broninhoud laat de gehele export
veilig mislukken in plaats van bronregels stilzwijgend af te kappen.
Tijdens terugkeer uit de Android-bestandskiezer herberekent Tuddy
de gehele bytevolgorde en vergelijkt die met het goedgekeurde
RAM-concept. De bestaande selectiefilter (alle, nog oefenen of
onbeoordeeld) blijft van toepassing. De gekozen externe Android-
bestandsprovider kan je eigen tekst wel elders synchroniseren.

---

## Toegankelijke bronregels voor je zelfgekozen ondertiteltaal

Als je vóór het bewaren zelf een taal kiest bij de eigen ondertiteltekst,
neemt het HTML-luisterwerkblad die expliciete keuze nu mee in de bronregels:
elk getoond bronfragment krijgt een gevalideerd HTML-`lang`-attribuut en
een passende tekstoriëntatie. Dat helpt schermlezers bij bijvoorbeeld
Kurmanji, Sorani, Pasjtoe en andere ondersteunde taalkeuzes. De keuze is
geen automatische taaldetectie en verandert geen ondertitelinhoud. Zonder
geldige expliciete taalkeuze blijft de browser de richting uit de tekst halen.

---

## Nieuw: afdrukken van bronantwoorden vereist nu een bewuste keuze

Een offline **zelfstudie-luisterwerkblad** heeft onder de vragen een
checkbox **Print ook mijn bronregelblad (bewuste keuze)**.
De bronregels blijven via bewust openklappen op het scherm beschikbaar,
maar het complete bronregelblad wordt **niet standaard meegeprint**.
Ook als je al op de interne knop naar het bronregelblad hebt geklikt,
blijft het bronregelblad uit het afdrukvoorbeeld zolang de checkbox
niet is aangevinkt. Met vinkje krijgt de zelfgekozen SRT/VTT-brontekst
een aparte afdrukpagina voor eigen controle.

De **leerlingenversie zonder bronregels** verandert niet: daarin is
geen brontekst/privébeoordeling aanwezig en kan een printinstelling
dus ook geen verborgen antwoord alsnog tonen. Dit is geen garantie
dat de inhoud van zelfstudie-HTML geheim is; dat bestand bevat
bronregels en is alleen voor eigen gebruik. Voor delen kies je het
aparte antwoordvrije leerlingenbestand. Deze verbetering werkt
scriptvrij en offline, zonder nieuwe Android-machtigingen of
wijzigingen in het bewaren van de sessie.

---

## Nieuw: zelf invullen of zonder antwoordbronnen delen (luisterwerkblad)

In **Mijn privé luisteroverzicht → Bewaar luisterwerkblad…** kun je nu
kiezen tussen **Zelfstudie · bronregels zelf onthullen** en
**Leerlingenblad · zonder bronregels of privébeoordeling**. De keuze werkt
naast **Alle / Nog oefenen / Nog niet beoordeeld** en verandert je
bestaande audioroute niet. Er wordt geen audio meegestuurd.

Het zelfstandige offline HTML-luisterwerkblad heeft per geselecteerd
A–B-fragment een **antwoordveld**, een handmatige keuze **Nog oefenen /
Ging goed / Makkelijk** en **Wis mijn antwoord en inschatting**.
Het browserantwoord en de zelfbeoordeling zijn tijdelijk; Tuddy bewaart
deze niet en controleert geen uitspraak of betekenis. Bij printen
verschijnen schrijflijnen en lege zelfbeoordelingsvakjes, geen ingevulde
browserformulieren. De browser kan eventueel zelf eerder getypte velden
herstellen. Het bestand bevat geen JavaScript of netwerkbronnen.

De **zelfstudieversie** onthult eigen SRT/VTT-bronregels pas via een
bewuste uitklapactie. Het complete bronregelblad is op het scherm
aanvankelijk verborgen en wordt pas getoond na **Open compleet
bronregelblad**; bij afdrukken staat het op een aparte pagina.
De **leerlingenversie** bevat géén eigen bronregelteks­ten, verborgen
bronsleutel, privébeoordelingen, eigen oefennotities of selectiefiltertitel
in de HTML. Alleen gekozen audiotijdvakken, lege invulvelden en lege
beoordelingsopties komen mee; een gekozen tijdvak of aantal fragmenten
kan nog persoonlijke context zijn, dus controleer zelf wat je deelt.
De Android-bestandskiezer suggereert bij de leerlingenversie de naam
`Tuddy-luister-leerlingenblad-zonder-bronregels.html`.

Het eigen luisteroverzicht is nu ook verticaal scrollbaar op kleine
Android-schermen. Tijdens een bewuste opslag vergelijkt Tuddy
**alle actuele exportbytes en zowel selectie als doelgroep** met de
vooraf goedgekeurde inhoud. Zijn die veranderd, dan wordt de oude
export geweigerd. Een door de gebruiker gekozen bestandsprovider kan
een bestand wel synchroniseren. Dit is uitsluitend handmatige
SRT/VTT-bronstudie met eigen A–B-tijdkeuze, geen geverifieerde
audio-uitlijning of transcriptie.

---

## Nieuw: dezelfde gerichte CSV- en oefenbladselectie

De keuze **Alle fragmenten / Nog oefenen / Nog niet beoordeeld** geldt
voortaan zowel voor het afzonderlijke HTML-luisterwerkblad **als** voor
de privé-CSV. De knop **Bekijk mijn volledige CSV vóór opslaan** vermeldt
de gekozen categorie en toont exact de CSV-rijen die voor die categorie
zullen worden aangeboden. Categorieën zonder bijpassende eigen fragmenten
kunnen niet als CSV of werkblad worden geëxporteerd. De afzonderlijke
gewone HTML-/TXT-overzichten behouden juist hun bestaande complete
sessie-inhoud. Dit filter verandert de begeleide afspeelroute niet.

Wanneer je de categorie tijdens de Android-bestandskiezer verandert,
weigert Tuddy altijd de eerdere export, zelfs als twee categorieën
toevallig dezelfde fragmenten bevatten. Ook wijzigen van fragment,
beoordeling, zelfgeschreven notitie of berekende bytes blokkeert
de oude keuze. Er wordt nooit stilzwijgend een CSV zonder deze selectie
opgeslagen.

Een compact CSV-ondertitelvoorbeeld telt nu de **volledig binnen het
eigen A–B-tijdvak passende ondertitelregels**, ook wanneer de handmatig
gekozen audiomarkeringen niet precies op een ondertitelgrens liggen.
Bij meerdere regels toont de CSV de eerste en laatste volledig passende
bronregel en het **exacte aantal overgeslagen passende middenregels**;
het pretendeert geen volledige transcriptie. Zonder volledig passende
ondertitelregels blijft de melding over een puur handmatig audiotijdvak
zichtbaar. Er is nooit sprake van automatische audio-uitlijning. De generator en het privé-overzicht gebruiken
begrenste rekenkundige tijdverschuivingen: integer-overloop kan geen
vals gelijklopende ondertitelregels opleveren. CSV bevat alleen je
eigen geselecteerde tijden, beoordelingen, notities en expliciet
beperkte eigen SRT/VTT-brontekst. Geen automatische audio- of
spraakherkenning, nieuwe permissies, account, profiel of cloud-backup.

---

## Nieuw: mijn afdrukbare luisterwerkblad met bronregels als zelfcontrole

Na het maken van één tot vier eigen A–B-fragmenten kies je **Mijn privé
oefenoverzicht bekijken** → **Bewaar luisterwerkblad · vragen en aparte
bronregels…**. Dit maakt een apart **HTML-oefenblad** naast het bestaande
HTML-verslag, TXT-overzicht en de CSV. De Android-bestandskiezer vraagt
expliciet waar je het bestand bewaart; er wordt nooit automatisch geëxporteerd.

Vóór de export kies je bewust **Alle fragmenten**, **Nog oefenen** of
**Nog niet beoordeeld**. De beschikbare aantallen worden getoond;
een lege categorie kan niet worden bewaard. Die keuze geldt alleen
voor het te maken **oefenblad** en verandert de begeleide
afspeelroute niet. Ook de **gekozen selectie en aantallen** staan
in het afdrukbare HTML-bestand. Als de selectie of een beoordeling
wijzigt tijdens de Android-documentkiezer, wordt het oude concept
niet stilzwijgend opgeslagen.

In een gewone browser kun je per fragment eerst luisteren en zelf een
betekenis of uitspraak opschrijven. Via het native HTML-knopje
**Toon zelf gekozen bronregels** zie je pas daarna de eigen SRT/VTT-tekst
die qua tijd volledig binnen het gekozen A–B-fragment valt.
Er worden **geen woorden herkend of uit audio uitgelijnd**. Als er geen
passende ondertiteltekst bestaat, staat dat expliciet bij het fragment;
Tuddy verzint geen inhoud. De eigen beoordeling en volgende oefenstap
staan erbij. Het bestand bevat geen JavaScript, geluid, audiopad,
netwerkverzoeken of accountgegevens.

Bij **afdrukken** blijven de bronregels verborgen op het vragenblad.
De eigen SRT/VTT-bronregels verschijnen afzonderlijk in
**Bronregels voor zelfcontrole** op een nieuwe afdrukpagina. Per passage
toont de **korte uitklap-preview** maximaal 18 regels (de eerste negen
en laatste negen), met het **exacte aantal overgeslagen middenregels**.
Het afzonderlijke **Bronregels voor zelfcontrole** neemt nu juist
**alle volledig binnen de gekozen A–B-markeringen vallende ondertitelregels**
op, ook de tussenliggende. Een te groot HTML-document wordt geheel
geweigerd bij de bestaande 128 KiB-uitvoergrens; er wordt geen
stilzwijgend afgekapt antwoordblad als compleet aangeboden.
Alleen de **korte uitklap-preview** kort lange individuele regels
zichtbaar en Unicode-veilig in. Het volledige bronregelblad bewaart de
complete geschoonde tekst per passende bronregel (tot 8.192 tekens per
ingelezen regel); als de bron- of HTML-bestandsgrens wordt overschreden,
wordt de uitvoer geweigerd in plaats van stilzwijgend een onvolledig
antwoordblad te maken. De broninhoud in de sessie blijft ongewijzigd
en rechts-naar-links-tekst behoudt een passende tekstrichting. Bij meerdere luisterfragmenten kun je met
de links boven het werkblad direct naar een fragment springen; die
schermnavigatie verschijnt niet op de afdruk.

Ook bij deze export vergelijkt Tuddy bij terugkeer uit de
Android-documentkiezer de **complete actuele bestandsbytes** met de
tijdelijke conceptversie. Verandert de oefensessie, een A–B-fragment,
de beoordeling of een eigen notitie, dan weigert Tuddy het oude
oefenblad. De gekozen Android-provider kan de tekst wel zelf delen
of synchroniseren: bewaar alleen eigen, toegestane privé-inhoud.

---

# Mijn privé luisteroefening als HTML of TXT

## Milliseconden blijven zichtbaar in je eigen tijdmarkeringen

De weergegeven begin- en eindtijd van iedere handmatig gekozen
A–B-passage gebruikt nu **uu:mm:ss.mmm**, ook in een bewust bewaard
HTML-/TXT-overzicht en het CSV-bestand. Daardoor blijven twee momenten
binnen dezelfde seconde onderscheidbaar, zelfs bij fragmenten van
minder dan één seconde. Dit zijn uitsluitend eigen gekozen tijden,
geen gemeten uitspraak- of woordtijdstempels. Het geluid, het gekozen
ondertitelbestand en de oefenroute zelf worden niet gewijzigd.

Open een SRT/VTT-document en selecteer bewust je eigen audio in
**▶ Eigen audio met tijdcodes afspelen…**. Maak in het paneel voor
**audiomarkeringen en begeleide oefenroute** maximaal vier eigen
A–B-oefenfragmenten; beoordeel ze desgewenst als **Nog oefenen**,
**Gaat goed** of **Makkelijk**. Bij ieder oefenfragment kun je nu
ook één eigen **volgende oefenstap** typen (maximaal 240 tekens),
bijvoorbeeld „luister de laatste uitspraak nogmaals”. Die tekst
blijft uitsluitend tijdens de geopende audiosessie in het geheugen;
het is geen automatische feedback of vertaling. Als de begeleide
ronde werkelijk helemaal is afgespeeld, geeft het overzicht dit
apart aan.
Een niet-afgeronde ronde wordt nooit als afgerond beschreven.

Kies **Mijn privé oefenoverzicht bekijken · tekst zelf selecteren**.
Dit scherm toont de volledige samenvatting met eigen tijdvakken,
eventuele herkenbare brontekst en zelfbeoordeling. Na het bekijken
kies je bewust **Bewaar zelf HTML…** of **Bewaar als privé TXT-notitie…**, daarna pas in de Android-
bestandskiezer waar je het verslag opslaat. De HTML is zelfstandig
leesbaar en afdrukbaar, zonder scripts, externe lettertypen, media
of netwerkverzoeken. De tekst is veilig ge-escaped. De TXT-optie
bevat dezelfde informatie als eenvoudige UTF-8 tekst, handig voor
persoonlijke notities; er wordt niets automatisch gedeeld.
Ook teksten met geldige Unicode-emoji blijven leesbaar.

In het privé-overzicht verschijnt nu ook de eigen ondertiteltekst van de
handmatig gekozen fragmenten: bij maximaal acht bronregels worden **alle
originele regelposities** afzonderlijk getoond. Bij langere fragmenten
toont Tuddy bewust de eerste vier en laatste vier regels, met het
**exacte aantal niet-getoonde tussenregels**. Een bronzin langer dan
240 Unicode-codepunten krijgt zichtbaar `[ingekort]` in plaats van een
stilzwijgend verlies; emoji blijven heel. Dit is uitsluitend een
transparant fragmentoverzicht, geen volledige SRT-export. De bestaande
CSV houdt zijn compacte ondertitelvoorbeeld.

Tuddy kopieert hierbij nooit het geluidsbestand of zijn Android-URI,
maakt geen nieuwe audiofragmenten en schrijft geen herhaalplanning
weg. Het verslag gebruikt enkel de tijdelijke fragmentkeuzes,
zelfbeoordelingen en eventueel door jou getypte oefennotities.
Als je een fragment of bijbehorende audiomarkering verwijdert,
verdwijnt de gekoppelde oefennotitie eveneens. Wanneer je het
audiobestand wisselt, een fragment, beoordeling of eigen notitie
wijzigt, of het oefenscherm sluit tijdens de bestandskeuze, wordt
de oude export geweigerd. Heropen daarna het overzicht.

**Let op:** alle tijden komen uitsluitend uit jouw ondertitelbestand
of eigen handmatige audiomarkeringen. Het is geen automatische
spraakherkenning, vertaling, uitspraakbeoordeling of geverifieerde
audiouitlijning. Het externe opslagplatform dat jij kiest kan het
rapport elders bewaren of synchroniseren. Bewaar dus geen gevoelige
ondertiteltekst zonder toestemming van betrokkenen.


## Gerichte oefenroute zonder automatische beoordeling

In **Begeleide A–B-oefenroute** kun je nu kiezen tussen
**Alle fragmenten**, **Nog oefenen** (uitsluitend zelf als zodanig
beoordeeld) en **Nog niet beoordeeld**. De lijst toont vooraf hoeveel
eigen fragmenten daarvoor beschikbaar zijn. Een lege keuze kan niet
als route worden gestart. Je kunt tijdens de route niet ineens een
andere selectie aanzetten. Alle tijdvakken en herhalingen blijven
afhankelijk van je eigen SRT/VTT en gekozen audio; er is geen
automatische spraak- of taalniveaumeting.

Ook de bevestiging na het bewaren van een oefenfragment toont nu
weer de correcte ondertitel-/passagenaam en het aantal fragmenten.


## Mijn volgende luisterronde in het verslag

Het tijdelijke overzicht, de expliciete HTML-export en de TXT-notitie
tonen onder **Mijn volgende luisterronde** eerst zelf als **Nog oefenen**
gemarkeerde passages en daarna passages zonder eigen beoordeling.
Dit is uitsluitend een herordening van de eigen keuzes, geen
automatische herkenning, uitspraakscore of bewezen begrip. Bij
verwijderen of herbeoordelen wordt de lijst direct opnieuw berekend.
Als alles positief door de gebruiker is beoordeeld, wordt niet
beweerd dat iets geleerd is.

Een eigen oefennotitie heeft maximaal 240 Unicode-tekens: ook
emoji tellen als één zichtbaar teken en worden niet halverwege
afgekapt. Bij bewuste export wordt de inhoud opnieuw gecontroleerd.


## Mijn eigen A–B-beoordelingen als CSV

In hetzelfde **Mijn privé luisteroverzicht** kun je nu **Bekijk mijn volledige
CSV vóór opslaan** gebruiken: je ziet alle rijen, exact zoals Tuddy ze naar
UTF-8 schrijft. Met **Bewaar mijn oefenfragmenten als CSV…** kies je daarna
zelf in Android een documentprovider. Het CSV-bestand bevat voor maximaal vier
fragmenten zes kolommen: volgnummer, eigen begin- en eindtijd, eigen
beoordeling, zelfgeschreven volgende oefenstap en een kort herkenbaar
ondertitelvoorbeeld (of een melding dat er geen exacte brontekstmatch is).
Tijden zijn eigen markeringen/gecorrigeerde SRT/VTT-tijden, geen door
spraakherkenning gevalideerde woordtiming. Het CSV-formaat gebruikt expliciet
dubbel gequote velden en CRLF-rijen; spreadsheetformules uit gebruikersinvoer
worden niet uitgevoerd doordat een gevaarlijke beginwaarde als tekst wordt
gemarkeerd. Geldige emoji blijven behouden. De CSV bevat geen audio-URI,
geluidsbytes, accountgegevens of externe netwerkverzoeken.

Dezelfde veilige opslagregels gelden als voor HTML en TXT. Tuddy bewaart
het bestand nooit vanzelf en controleert het exacte CSV-bytebeeld opnieuw
na de Android-bestandskiezer; bij gewijzigde fragmenten, correcties of
beoordelingen moet je opnieuw beginnen. Een gekozen externe provider kan
natuurlijk wel synchroniseren. Je kunt de ruwe CSV vooraf ook zelf selecteren,
waarbij Androids eigen klembordapp de selectie kan verwerken.
