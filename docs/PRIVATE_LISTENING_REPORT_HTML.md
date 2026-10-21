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
