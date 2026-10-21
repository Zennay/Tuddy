## Nieuw: gekozen TTML-ondertitelclips bewaren en delen

Selecteer in de ondertiteleditor bronregels of tijdvensters, kies TTML en bewaar de gekozen regels als een afzonderlijk `.ttml`-bestand. Of deel maximaal 24 gekozen TTML-cues als letterlijke XML-tekst (16 KiB), met aparte bevestiging. Bronbestand, overige tekst, audio en oorspronkelijke timing blijven intact, tenzij je zelf een compact fragment vanaf nul kiest. [Details](docs/TTML_DFXP_SUBTITLE_IMPORT.md).

## Nieuw: lokale TTML-kopie bewaren

Je kunt nu in de tijdelijke ondertiteleditor na een volledige preview een **nieuwe TTML-kopie** opslaan. De bewaarde tekst en begin-/eindtijden moeten exact door Tuddy's beperkte parser teruggelezen kunnen worden; anders wordt de uitvoer geweigerd. Android vraagt altijd eerst een opslaglocatie. [Beperkingen en voorbeeld](docs/TTML_DFXP_SUBTITLE_IMPORT.md).

## Nieuw: TTML/DFXP-ondertitels lokaal openen

Tuddy kan nu TTML/DFXP-tekstondertitels met expliciete `<p begin/end>`-tijden uit de Android-bestandenkiezer of een bewust gedeelde tekst/URI verwerken. De tijdelijke preview bevat alleen leesbare tekst, `<span>` en `<br/>`, en kan via de bestaande editor als nieuwe SRT/VTT-kopie worden bewaard. Externe XML-entiteiten, relatieve timing, scripts en andere media worden geweigerd. Geen cloudvertaling of bronwijziging. [Handleiding en beperkingen](docs/TTML_DFXP_SUBTITLE_IMPORT.md).

## Nieuw: eigen ASS/SSA-ondertitels lokaal openen

Open een zelf gekozen .ass- of .ssa-bestand via **Open ondertitelbestand…**, deel geldige platte tekst met Tuddy of plak het in de ondertitelbewerker. Tuddy haalt alleen getimede Dialogue-tekst uit de [Events]-sectie; styling, animatie, scripts, geluid en vector-tekeningen worden niet uitgevoerd. Bekijk en bevestig de tijdelijke preview voordat je een nieuwe SRT/WebVTT-kopie maakt. [Formaatgrenzen en details](docs/ASS_SSA_SUBTITLE_IMPORT.md).

## Nieuw: taalondersteuning zonder valse offlineclaims

Open **Taal- en modelinstellingen → Bekijk alle taalondersteuning** voor ML Kit-tekstdoelen, Android-spraaklocale-kandidaten en talen met alleen een beperkte woordenpreview. Voor Kurmanji, Sorani en Pasjtoe zijn nu ook openbare ASR-/MT-onderzoekskandidaten, modelgrootte en licentiewaarschuwingen zichtbaar, inclusief een MIT-gelicentieerde maar zeer grote M2M100-kandidaat voor Pasjtoe. De kaarten zijn geen geïntegreerde packs; ze worden alleen op jouw tik extern geopend en er wordt niets gedownload. De ondertitel-editor herkent ook automatisch RTL/LTR aan de tekstinhoud. [Details](docs/LANGUAGE_SUPPORT_GUIDE.md).

## Android Delen → Tuddy: ook LRC-lyrics met eigen tijdcodes

Naast complete SRT/WebVTT-tekst kun je nu eigen **LRC** met geldige `[mm:ss.xx]`- of `[mm:ss.xxx]`-markeringen als **platte tekst** delen. Tuddy opent de bestaande LRC-bewerkroute met een expliciete, tijdelijke preview; foutieve of te grote broninhoud wordt niet stilzwijgend geïmporteerd. LRC geeft alleen zelf vastgelegde begintijden; afgeleide eindtijden zijn niet met audio gemeten. Geen bijlagen, cloud, automatische vertaling of schrijfacties. [Meer uitleg](docs/MANUAL_SUBTITLE_PASTE.md).

---

## Nieuw · gedeelde ondertitels rechtstreeks als lokaal editorconcept

Deel vanuit een Android-app een complete **SRT/WebVTT als platte tekst** met **Delen → Tuddy**. De app controleert het volledige bericht lokaal met de bestaande strikte ondertitelparser en opent het alleen ter **bewuste controle en bevestiging** in de ondertiteleditor. Ongeldige of te grote inhoud (maximaal 256 KiB / 500 cues) wordt geen ondertitelconcept. Een al geopend ondertitelconcept wordt niet stilzwijgend overschreven. Gewone gedeelde tekst blijft beschikbaar via de bestaande snelle typtekstfunctie. Geen bijlagen, HTML, URI-bestanden, automatische vertaling, opslag of netwerkfallback. [Volledige handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

---

## Nieuw: een SRT/VTT-ondertitel exact op tijd splitsen

In de tijdelijke editor kun je nu een cue in twee tekstregels splitsen en
de **exacte tijdgrens zelf invoeren** (bijvoorbeeld `01:23.500`). Bij
bestaande meerregelige tekst wordt eerst de eigen regelovergang gebruikt;
anders kiest Tuddy de dichtstbijzijnde witruimtegrens rond het tekstmidden.
Laat de tijd leeg voor het midden van de bestaande cue. Een preview toont
de exacte voor/na-tijdlijn van beide delen; elk deel duurt minimaal 0,6 seconde en de nieuwe
grens mag het volgende cue-begin niet passeren. Ongeldige tijden of teksten
blijven bewerkbaar in het dialoogvenster in plaats van de invoer te verliezen.
De hele wijziging is één lokale Undo/Redo-stap; het bronbestand wordt niet
overschreven en de grens wordt niet uit audio herkend.
[Handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

Bij **Samenvoegen** laat een extra preview beide originele tijdvakken zien
en waarschuwt die expliciet wanneer een tijdgat verdwijnt of de cues
elkaar overlappen. Het resultaat behoudt de volledige tijdsdekking tot de
laatste van beide oorspronkelijke eindtijden. De bevestiging is één
Undo/Redo-bewerking in de tijdelijke editor; de originele bestanden blijven
onaangeroerd.

---

## Nieuw: herhaalde refreinen rechtstreeks uit eigen LRC-bestanden openen

Tuddy importeert nu ook gewone LRC-regels met meerdere expliciete
tijdstempels, zoals `[00:12.50][01:24.20]Mijn refrein`. Optioneel
kun je vóór de eerste eigen tekst precies één `[offset:+500]` of
`[offset:-250]`-header gebruiken om alle tijden expliciet maximaal
30 seconden later of eerder te plaatsen. In Android zie je welke
millisecondeverschuiving werkelijk is toegepast. Met
**− 0,5 s**, **Offset 0** en **+ 0,5 s** pas je die header rechtstreeks
aan in je tijdelijke, volledig herberekende LRC-concept. Je kunt ook
een exacte verschuiving zoals `-125` of `+750` milliseconden
invoeren en bewust bevestigen met **Pas toe**. Ongeldige
wijzigingen worden geweigerd; je originele Android-bestand verandert
niet. Meerdere of ongeldige offsetregels worden geweigerd. Iedere
tijdstempel wordt een eigen ondertitel-cue met dezelfde zelfgeschreven
tekst. De volledige reeks wordt chronologisch gesorteerd, waarna de
eindtijden uit het volgende begin worden afgeleid. In de Android-preview
staan de **uitgewerkte cue-nummers en tijden**, zodat selecteren geen
bronregels verwart met herhaalde voorkomens. Boven de grens van 500 cues,
meer dan 16 momenten op een regel of bij dubbele/ongeldige tijdstempels
wordt het volledige document geweigerd. Geen gefingeerde vertaling,
geluidsexport, netwerk of automatische audio-/woorduitlijning. De
bestaande lokale editor en expliciete Android-bestandskeuze blijven
verplicht.
[Voorbeeld en handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

---

## Nieuw: vier ondertitelbewerkingen op dezelfde tijdselectie

Exporteren, tijdcodes verschuiven, ongewenste ondertitels
verwijderen en letterlijke tekst corrigeren kunnen nu allemaal
beginnen met een handmatig **Vanaf/Tot**-tijdvenster. De
geselecteerde volledige oorspronkelijke cues worden zichtbaar
omgezet naar regelnummers; toepassing of bestandsschrijven is
altijd een afzonderlijke bewuste stap. De bestaande paginapreviews,
tijdcodegrenzen en Terug/Opnieuw blijven behouden.
[Meer informatie](docs/MANUAL_SUBTITLE_PASTE.md).

## Hersteld: gefilterd bestand exporteren wist geen andere editorregels

Wanneer je enkel geselecteerde SRT/VTT/CSV/TSV/LRC-regels naar een
bewust gekozen Android-bestand schrijft, blijft de **volledige
tijdelijke bewerkbare ondertiteltijdlijn** na terugkeer behouden.
Alleen de bestandsinhoud wordt gefilterd. Oudere code heropende
abusievelijk alleen de geselecteerde exportregels. De Activity
houdt tijdens de expliciete bestandskeuze nu gescheiden RAM-
snapshots bij voor het volledige editorconcept en de gekozen
exportbytes; originele files en live gesprek worden niet gewijzigd.
[Privacy en details](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: zelf tweetalige ondertitelregels vastzetten

Tuddy kan twee door jou gekozen SRT/VTT-regels nu ook **bewust
aan elkaar koppelen zonder voldoende tijdsoverlap**. Kies de
oorspronkelijke regel uit elk spoor, controleer de volledige eigen
teksten en tijdcodes, en bevestig. De bestaande documentbrede
tijdmatcher koppelt de overige regels opnieuw, maar kan die
handmatige combinatie niet overschrijven. Maximaal 32
chronologisch consistente één-op-één-paren; dubbele of
tegenstrijdige rijnummers, conflicterende uitgesloten cues en
niet-exporteerbare resultaten worden geweigerd.

Losse bronregels zijn met één knop over te nemen in de handmatige
koppelvelden. Ook een automatisch voorgesteld paar kun je bewust
vastzetten. Voor meerdere paren tegelijk accepteert de app strikt
begrensde bronpaarreeksen zoals `1:2,3-5:7-9` met een volledige
paginagewijze preview vóór expliciete overname. Alle handmatige
keuzes zijn individueel óf gezamenlijk te herstellen;
de complete bron-, paar- en resultaatpreview, Undo/Redo, RAM-
privacy en expliciete aparte Android-bestandsopslag blijven gelden.
**Geen automatische taalherkenning, spraakvertaling of echte
audio-uitlijning.** Een handmatig gecombineerd paar gebruikt
de **eerste spoor-tijdcodes**.
[Uitleg en stappen](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: controleer en corrigeer handmatig ondertitelkoppelingen

Bij het tijdelijk combineren van **twee eigen SRT/VTT-sporen**
kiest Tuddy nu over **de volledige tijdlijn** zo veel mogelijk
chronologische één-op-één koppelingen op basis van werkelijke
overlappende tijdvakken. De volledige koppelingen zijn te
beoordelen met hun oorspronkelijke bronregelnummers, volledige
teksten en overlaptijd. Ook het aantal ongekoppelde regels
in **beide** sporen staat in de preview. Je kunt nu **beide bronzinnen
doorzoeken**, op het **exacte oorspronkelijke tweede-spoorregelnummer**
filteren en **alle ongekoppelde originele regels** in beide sporen
afzonderlijk bekijken (met tekst- en spoorfilters). In de volledige
resultaatpreview kun je direct naar een resultaatregel springen.
Voor tweepuntscorrectie kun je per bronspoor ook zoeken, naar originele
regelnummers springen en lange oorspronkelijke cues volledig uitklappen.

Een voorgestelde inhoudelijk onjuiste koppeling kan de gebruiker
**handmatig uitsluiten**. Dat werkt per geselecteerd paar, per
eigen bronregelnummers zoals `2-5,9` of na een bewuste selectie
op **meerdere losse tijdvensters**. Zo'n regel uit het tweede spoor
blijft altijd als een losse ondertitel bestaan, terwijl de overige
koppelingen opnieuw worden gekozen. Afzonderlijk herstellen en
alles herstellen kan zonder verlies van het originele bronbestand. De gewone volledige voor/na-preview,
bestandsgrenzen, Undo/Redo en aparte Android SAF-save blijven
van kracht. Er wordt geen spraak/audio geanalyseerd.
[Zelf koppelingen nakijken](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: meerpunts synchronisatie van eigen ondertitels

Tuddy ondersteunt nu naast één handmatige offset en twee
ankerparen ook **2–8 bewust gekozen ankerparen** om een tweede
door de gebruiker gekozen SRT/WebVTT-spoor **per tijdssegment**
te corrigeren. In de native Android-dialog doorzoek je beide
oorspronkelijke documenten, neem je exacte bronregelnummers
over en voeg je tussenliggende ankers toe, of wijzig/verwijder
je bestaande ankers. Tuddy toont de volledige bronteksten
en originele begintijden en past strikt begrensde, met
integer milliseconden berekende interpolatie op de tweede
tijdlijn toe. Hierna volgt nog de volledige gekoppelde
preview vóór afzonderlijke RAM-toepassing en Android SAF-save.

Dit is **geen audioanalyse, spraakherkenning of automatische
vertaling**. De originele documenten blijven ongewijzigd.
[Meerpuntscorrectie en grenzen](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: subtitle-drift corrigeren met twee ankerparen

Als twee handmatig aangeleverde SRT/WebVTT-sporen **geleidelijk uit
synchronisatie lopen**, kan Tuddy nu het tweede spoor lineair
uitrekken of inkorten vanaf **twee door de gebruiker gekozen
overeenkomende cueparen**. De aparte dialog toont de complete vier
ankerzinnen en beide tijdafstanden; de daaropvolgende volledige
samenvoegpreview toont vijf resultaatcues per pagina.
De oorspronkelijke documenten en live transcriptie blijven intact.
Het gecorrigeerde concept verschijnt pas na expliciet akkoord, met
Undo/Redo en later een aparte Android SAF-bestandskeuze. Onmogelijke
of niet veilig exporteerbare tijdlijnen worden geweigerd; dit is
**geen automatische audio-uitlijning**.
[Stapsgewijze uitleg](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: twee handmatige spraaksporen nauwkeurig synchroniseren

Bij het toevoegen van een **tweede, eigen SRT/VTT-spoor** kan Tuddy
naast de vaste ±2/5/10-secondenpresets nu een **zelf ingetypte
milliseconde-offset** van maximaal ±600 seconden toepassen.
Als alternatief kiest de gebruiker **één beginregel in elk spoor**:
de app toont het verschil tussen de bestaande begintijden, en neemt
dat uitsluitend na een expliciete tik over. Er wordt geen media
geanalyseerd, geen taal herkend en geen vertaling gegenereerd.

De volledige samengevoegde ondertitellijst is **vijf cues per
pagina** te bekijken, met begin/eindtijden, gekoppelde tekst en
losstaande ongekoppelde regels. Identieke zelfgeschreven regels uit
twee sporen blijven allebei zichtbaar. Wijziging van ankerregels
vereist hernieuwde toestemming voor de verschuiving; een
ongeldige tijdlijn kan niet worden toegepast. Alles blijft tijdelijk
in RAM, met bestaande Undo/Redo en afzonderlijke Android SAF-save.
[Uitgebreide handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: zelf instelbare tijdverschuiving in de ondertiteleditor

Naast vaste verschuivingen kan de gebruiker nu zelf een positieve
of negatieve verschuiving invoeren, tot **600 seconden** met maximaal
drie decimalen (**1 ms resolutie**). Alleen bewust geselecteerde
ondertitelregels veranderen; de volledige SRT/VTT-export-, tijdlijn-
en Undo/Redo-beperkingen blijven intact. Ongeldige invoer en
niet-exporteerbare tijdlijnen worden niet toegepast.
[Werkwijze](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: meerdere losse tijdvensters in één bewerking

Tuddy kan maximaal 16 **zelf ingevulde niet-overlappende tijdvensters**
zoals `00:10-00:20;01:00-01:15` als één bronregelselectie
gebruiken. Dat werkt voor **SRT/VTT/CSV/TSV/LRC-fragmentexport**,
**gerichte tijdverschuiving**, **meerdere regels verwijderen** en
**letterlijke vervanging in één of twee eigen tekstregels**.
Volledige cues blijven in originele volgorde en verschijnen maximaal
één keer. Er is geen automatische audio-uitlijning, cloudspraak of
bestandswijziging zonder de bestaande aparte bevestigingen.
[Uitleg en voorbeelden](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: eigen tijdvenster rechtstreeks naar ondertitelselectie

Bij zowel **selectieve export** als **gerichte tijdverschuiving**
kun je nu een eigen vanaf/tot-tijdvenster invullen.
Tuddy selecteert op een expliciete knop alle **volledige**
ondertitelregels die dat tijdvenster overlappen en toont daarna
de oorspronkelijke regelnummers. De bestaande strenge preview,
tijd-/formaatlimieten, Terug/Opnieuw en Android-bestandskeuze
blijven intact; er wordt geen spraak of audio onderzocht.
[Werkwijze](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: verschuif uitsluitend eigen gekozen tijdcodes

Naast het verschuiven van de hele ondertiteltijdlijn ondersteunt
de privé-editor nu **gerichte tijdcorrecties voor losse originele
bronregels**, bijvoorbeeld `2-4,7,12`, met zes handmatige offsets.
Voor iedere betrokken regel zie je de volledige oude/nieuwe
tijdcodes en tekst in een doorbladerbaar voorbeeld. Teruglopende
begintijden of overschreden exportlimieten worden geweigerd.
Ongeselecteerde regels blijven exact ongewijzigd; correcties zijn
in één Terug/Opnieuw-stap te herstellen. Geen automatische
audio-uitlijning of bestandsopslag.
[Uitleg](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: exporteer precieze ondertitelranges zonder te zoeken

Selecteer in de tijdelijke ondertiteleditor nu zelf bronregels zoals
`1-5,9,12-16` om alleen dat fragment te bewaren. Je kunt ook alle
regels of juist de overige, niet-geselecteerde regels kiezen. Bij
zoeken neemt de aparte exportknop bewust de **oorspronkelijke
regelnummers uit alle zoekhits** als uitgangspunt. Een doorbladerbare
bronregelpreview toont wat je geselecteerd hebt; daarna volgt de
complete exacte bestandsinhoud voor SRT/VTT/CSV/TSV/LRC. De bestaande
tijdkeuzes, formaatgrenzen en expliciete Android-opslag blijven gelden.
[Handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: vijf formaten voor een zelfgekozen ondertitelfragment

Doorzoek je tijdelijke ondertitelconcept en kies **Bewaar alleen
gevonden regels…**. Je kunt nu uitsluitend die regels als
**SRT, WebVTT, CSV, TSV of LRC** exporteren; behoud de eigen tijden
of maak een compact fragment vanaf 00:00. Vooraf worden alle exacte
UTF-8-bytes per deelpagina getoond en opnieuw vergeleken vóór de
expliciete Android-bestandskeuze. CSV/TSV wordt geweigerd als
tekstkolommen niet veilig en volledig behouden kunnen worden.
LRC bewaart alleen eigen begintijden, geen eindtijden. De bestaande
limieten, privacy en Undo/Redo-editor blijven intact.
[Werkwijze](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: gerichte ondertitelcorrecties en volledige SRT/VTT-bestandspreview

De privé-editor kan een letterlijke zoek/vervangactie beperken tot
losse originele bronregels (`1-3,7,12`) en apart de eerste of tweede
eigen tekstregel corrigeren. Als hoofdlettergevoeligheid uitstaat,
wordt Unicode-case-insensitive gematcht, ook voor niet-ASCII letters;
vervangtekst met `## Nieuw: een SRT/VTT-ondertitel exact op tijd splitsen

In de tijdelijke editor kun je nu een cue in twee tekstregels splitsen en
de **exacte tijdgrens zelf invoeren** (bijvoorbeeld `01:23.500`). Bij
bestaande meerregelige tekst wordt eerst de eigen regelovergang gebruikt;
anders kiest Tuddy de dichtstbijzijnde witruimtegrens rond het tekstmidden.
Laat de tijd leeg voor het midden van de bestaande cue. Een preview toont
de exacte voor/na-tijdlijn van beide delen; elk deel duurt minimaal 0,6 seconde en de nieuwe
grens mag het volgende cue-begin niet passeren. Ongeldige tijden of teksten
blijven bewerkbaar in het dialoogvenster in plaats van de invoer te verliezen.
De hele wijziging is één lokale Undo/Redo-stap; het bronbestand wordt niet
overschreven en de grens wordt niet uit audio herkend.
[Handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

Bij **Samenvoegen** laat een extra preview beide originele tijdvakken zien
en waarschuwt die expliciet wanneer een tijdgat verdwijnt of de cues
elkaar overlappen. Het resultaat behoudt de volledige tijdsdekking tot de
laatste van beide oorspronkelijke eindtijden. De bevestiging is één
Undo/Redo-bewerking in de tijdelijke editor; de originele bestanden blijven
onaangeroerd.

---

## Nieuw: herhaalde refreinen rechtstreeks uit eigen LRC-bestanden openen

Tuddy importeert nu ook gewone LRC-regels met meerdere expliciete
tijdstempels, zoals `[00:12.50][01:24.20]Mijn refrein`. Optioneel
kun je vóór de eerste eigen tekst precies één `[offset:+500]` of
`[offset:-250]`-header gebruiken om alle tijden expliciet maximaal
30 seconden later of eerder te plaatsen. In Android zie je welke
millisecondeverschuiving werkelijk is toegepast. Met
**− 0,5 s**, **Offset 0** en **+ 0,5 s** pas je die header rechtstreeks
aan in je tijdelijke, volledig herberekende LRC-concept. Je kunt ook
een exacte verschuiving zoals `-125` of `+750` milliseconden
invoeren en bewust bevestigen met **Pas toe**. Ongeldige
wijzigingen worden geweigerd; je originele Android-bestand verandert
niet. Meerdere of ongeldige offsetregels worden geweigerd. Iedere
tijdstempel wordt een eigen ondertitel-cue met dezelfde zelfgeschreven
tekst. De volledige reeks wordt chronologisch gesorteerd, waarna de
eindtijden uit het volgende begin worden afgeleid. In de Android-preview
staan de **uitgewerkte cue-nummers en tijden**, zodat selecteren geen
bronregels verwart met herhaalde voorkomens. Boven de grens van 500 cues,
meer dan 16 momenten op een regel of bij dubbele/ongeldige tijdstempels
wordt het volledige document geweigerd. Geen gefingeerde vertaling,
geluidsexport, netwerk of automatische audio-/woorduitlijning. De
bestaande lokale editor en expliciete Android-bestandskeuze blijven
verplicht.
[Voorbeeld en handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

---

## Nieuw: vier ondertitelbewerkingen op dezelfde tijdselectie

Exporteren, tijdcodes verschuiven, ongewenste ondertitels
verwijderen en letterlijke tekst corrigeren kunnen nu allemaal
beginnen met een handmatig **Vanaf/Tot**-tijdvenster. De
geselecteerde volledige oorspronkelijke cues worden zichtbaar
omgezet naar regelnummers; toepassing of bestandsschrijven is
altijd een afzonderlijke bewuste stap. De bestaande paginapreviews,
tijdcodegrenzen en Terug/Opnieuw blijven behouden.
[Meer informatie](docs/MANUAL_SUBTITLE_PASTE.md).

## Hersteld: gefilterd bestand exporteren wist geen andere editorregels

Wanneer je enkel geselecteerde SRT/VTT/CSV/TSV/LRC-regels naar een
bewust gekozen Android-bestand schrijft, blijft de **volledige
tijdelijke bewerkbare ondertiteltijdlijn** na terugkeer behouden.
Alleen de bestandsinhoud wordt gefilterd. Oudere code heropende
abusievelijk alleen de geselecteerde exportregels. De Activity
houdt tijdens de expliciete bestandskeuze nu gescheiden RAM-
snapshots bij voor het volledige editorconcept en de gekozen
exportbytes; originele files en live gesprek worden niet gewijzigd.
[Privacy en details](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: zelf tweetalige ondertitelregels vastzetten

Tuddy kan twee door jou gekozen SRT/VTT-regels nu ook **bewust
aan elkaar koppelen zonder voldoende tijdsoverlap**. Kies de
oorspronkelijke regel uit elk spoor, controleer de volledige eigen
teksten en tijdcodes, en bevestig. De bestaande documentbrede
tijdmatcher koppelt de overige regels opnieuw, maar kan die
handmatige combinatie niet overschrijven. Maximaal 32
chronologisch consistente één-op-één-paren; dubbele of
tegenstrijdige rijnummers, conflicterende uitgesloten cues en
niet-exporteerbare resultaten worden geweigerd.

Losse bronregels zijn met één knop over te nemen in de handmatige
koppelvelden. Ook een automatisch voorgesteld paar kun je bewust
vastzetten. Voor meerdere paren tegelijk accepteert de app strikt
begrensde bronpaarreeksen zoals `1:2,3-5:7-9` met een volledige
paginagewijze preview vóór expliciete overname. Alle handmatige
keuzes zijn individueel óf gezamenlijk te herstellen;
de complete bron-, paar- en resultaatpreview, Undo/Redo, RAM-
privacy en expliciete aparte Android-bestandsopslag blijven gelden.
**Geen automatische taalherkenning, spraakvertaling of echte
audio-uitlijning.** Een handmatig gecombineerd paar gebruikt
de **eerste spoor-tijdcodes**.
[Uitleg en stappen](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: controleer en corrigeer handmatig ondertitelkoppelingen

Bij het tijdelijk combineren van **twee eigen SRT/VTT-sporen**
kiest Tuddy nu over **de volledige tijdlijn** zo veel mogelijk
chronologische één-op-één koppelingen op basis van werkelijke
overlappende tijdvakken. De volledige koppelingen zijn te
beoordelen met hun oorspronkelijke bronregelnummers, volledige
teksten en overlaptijd. Ook het aantal ongekoppelde regels
in **beide** sporen staat in de preview. Je kunt nu **beide bronzinnen
doorzoeken**, op het **exacte oorspronkelijke tweede-spoorregelnummer**
filteren en **alle ongekoppelde originele regels** in beide sporen
afzonderlijk bekijken (met tekst- en spoorfilters). In de volledige
resultaatpreview kun je direct naar een resultaatregel springen.
Voor tweepuntscorrectie kun je per bronspoor ook zoeken, naar originele
regelnummers springen en lange oorspronkelijke cues volledig uitklappen.

Een voorgestelde inhoudelijk onjuiste koppeling kan de gebruiker
**handmatig uitsluiten**. Dat werkt per geselecteerd paar, per
eigen bronregelnummers zoals `2-5,9` of na een bewuste selectie
op **meerdere losse tijdvensters**. Zo'n regel uit het tweede spoor
blijft altijd als een losse ondertitel bestaan, terwijl de overige
koppelingen opnieuw worden gekozen. Afzonderlijk herstellen en
alles herstellen kan zonder verlies van het originele bronbestand. De gewone volledige voor/na-preview,
bestandsgrenzen, Undo/Redo en aparte Android SAF-save blijven
van kracht. Er wordt geen spraak/audio geanalyseerd.
[Zelf koppelingen nakijken](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: meerpunts synchronisatie van eigen ondertitels

Tuddy ondersteunt nu naast één handmatige offset en twee
ankerparen ook **2–8 bewust gekozen ankerparen** om een tweede
door de gebruiker gekozen SRT/WebVTT-spoor **per tijdssegment**
te corrigeren. In de native Android-dialog doorzoek je beide
oorspronkelijke documenten, neem je exacte bronregelnummers
over en voeg je tussenliggende ankers toe, of wijzig/verwijder
je bestaande ankers. Tuddy toont de volledige bronteksten
en originele begintijden en past strikt begrensde, met
integer milliseconden berekende interpolatie op de tweede
tijdlijn toe. Hierna volgt nog de volledige gekoppelde
preview vóór afzonderlijke RAM-toepassing en Android SAF-save.

Dit is **geen audioanalyse, spraakherkenning of automatische
vertaling**. De originele documenten blijven ongewijzigd.
[Meerpuntscorrectie en grenzen](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: subtitle-drift corrigeren met twee ankerparen

Als twee handmatig aangeleverde SRT/WebVTT-sporen **geleidelijk uit
synchronisatie lopen**, kan Tuddy nu het tweede spoor lineair
uitrekken of inkorten vanaf **twee door de gebruiker gekozen
overeenkomende cueparen**. De aparte dialog toont de complete vier
ankerzinnen en beide tijdafstanden; de daaropvolgende volledige
samenvoegpreview toont vijf resultaatcues per pagina.
De oorspronkelijke documenten en live transcriptie blijven intact.
Het gecorrigeerde concept verschijnt pas na expliciet akkoord, met
Undo/Redo en later een aparte Android SAF-bestandskeuze. Onmogelijke
of niet veilig exporteerbare tijdlijnen worden geweigerd; dit is
**geen automatische audio-uitlijning**.
[Stapsgewijze uitleg](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: twee handmatige spraaksporen nauwkeurig synchroniseren

Bij het toevoegen van een **tweede, eigen SRT/VTT-spoor** kan Tuddy
naast de vaste ±2/5/10-secondenpresets nu een **zelf ingetypte
milliseconde-offset** van maximaal ±600 seconden toepassen.
Als alternatief kiest de gebruiker **één beginregel in elk spoor**:
de app toont het verschil tussen de bestaande begintijden, en neemt
dat uitsluitend na een expliciete tik over. Er wordt geen media
geanalyseerd, geen taal herkend en geen vertaling gegenereerd.

De volledige samengevoegde ondertitellijst is **vijf cues per
pagina** te bekijken, met begin/eindtijden, gekoppelde tekst en
losstaande ongekoppelde regels. Identieke zelfgeschreven regels uit
twee sporen blijven allebei zichtbaar. Wijziging van ankerregels
vereist hernieuwde toestemming voor de verschuiving; een
ongeldige tijdlijn kan niet worden toegepast. Alles blijft tijdelijk
in RAM, met bestaande Undo/Redo en afzonderlijke Android SAF-save.
[Uitgebreide handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: zelf instelbare tijdverschuiving in de ondertiteleditor

Naast vaste verschuivingen kan de gebruiker nu zelf een positieve
of negatieve verschuiving invoeren, tot **600 seconden** met maximaal
drie decimalen (**1 ms resolutie**). Alleen bewust geselecteerde
ondertitelregels veranderen; de volledige SRT/VTT-export-, tijdlijn-
en Undo/Redo-beperkingen blijven intact. Ongeldige invoer en
niet-exporteerbare tijdlijnen worden niet toegepast.
[Werkwijze](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: meerdere losse tijdvensters in één bewerking

Tuddy kan maximaal 16 **zelf ingevulde niet-overlappende tijdvensters**
zoals `00:10-00:20;01:00-01:15` als één bronregelselectie
gebruiken. Dat werkt voor **SRT/VTT/CSV/TSV/LRC-fragmentexport**,
**gerichte tijdverschuiving**, **meerdere regels verwijderen** en
**letterlijke vervanging in één of twee eigen tekstregels**.
Volledige cues blijven in originele volgorde en verschijnen maximaal
één keer. Er is geen automatische audio-uitlijning, cloudspraak of
bestandswijziging zonder de bestaande aparte bevestigingen.
[Uitleg en voorbeelden](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: eigen tijdvenster rechtstreeks naar ondertitelselectie

Bij zowel **selectieve export** als **gerichte tijdverschuiving**
kun je nu een eigen vanaf/tot-tijdvenster invullen.
Tuddy selecteert op een expliciete knop alle **volledige**
ondertitelregels die dat tijdvenster overlappen en toont daarna
de oorspronkelijke regelnummers. De bestaande strenge preview,
tijd-/formaatlimieten, Terug/Opnieuw en Android-bestandskeuze
blijven intact; er wordt geen spraak of audio onderzocht.
[Werkwijze](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: verschuif uitsluitend eigen gekozen tijdcodes

Naast het verschuiven van de hele ondertiteltijdlijn ondersteunt
de privé-editor nu **gerichte tijdcorrecties voor losse originele
bronregels**, bijvoorbeeld `2-4,7,12`, met zes handmatige offsets.
Voor iedere betrokken regel zie je de volledige oude/nieuwe
tijdcodes en tekst in een doorbladerbaar voorbeeld. Teruglopende
begintijden of overschreden exportlimieten worden geweigerd.
Ongeselecteerde regels blijven exact ongewijzigd; correcties zijn
in één Terug/Opnieuw-stap te herstellen. Geen automatische
audio-uitlijning of bestandsopslag.
[Uitleg](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: exporteer precieze ondertitelranges zonder te zoeken

Selecteer in de tijdelijke ondertiteleditor nu zelf bronregels zoals
`1-5,9,12-16` om alleen dat fragment te bewaren. Je kunt ook alle
regels of juist de overige, niet-geselecteerde regels kiezen. Bij
zoeken neemt de aparte exportknop bewust de **oorspronkelijke
regelnummers uit alle zoekhits** als uitgangspunt. Een doorbladerbare
bronregelpreview toont wat je geselecteerd hebt; daarna volgt de
complete exacte bestandsinhoud voor SRT/VTT/CSV/TSV/LRC. De bestaande
tijdkeuzes, formaatgrenzen en expliciete Android-opslag blijven gelden.
[Handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: vijf formaten voor een zelfgekozen ondertitelfragment

Doorzoek je tijdelijke ondertitelconcept en kies **Bewaar alleen
gevonden regels…**. Je kunt nu uitsluitend die regels als
**SRT, WebVTT, CSV, TSV of LRC** exporteren; behoud de eigen tijden
of maak een compact fragment vanaf 00:00. Vooraf worden alle exacte
UTF-8-bytes per deelpagina getoond en opnieuw vergeleken vóór de
expliciete Android-bestandskeuze. CSV/TSV wordt geweigerd als
tekstkolommen niet veilig en volledig behouden kunnen worden.
LRC bewaart alleen eigen begintijden, geen eindtijden. De bestaande
limieten, privacy en Undo/Redo-editor blijven intact.
[Werkwijze](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: gerichte ondertitelcorrecties en volledige SRT/VTT-bestandspreview

 of backslashes blijft letterlijk. De bewerking
heeft een paginavoorbeeld van **alle** gewijzigde tekst en blijft als
één actie herstelbaar.
Bestaande Van/Tot-correcties blijven bruikbaar zonder losse selectie.

Ook de SRT- en WebVTT-export toont nu eerst de complete exacte
UTF-8-inhoud in paginavorm, met bytegrootte en alle tijdcodes. De
opslagknop blijft geblokkeerd voor ongeldige documenten; vlak voor
Androids expliciete opslagkeuze worden de bytes opnieuw herleid en
vergeleken. Bronbestand en live spraak worden nooit overschreven.
[Details](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: meerdere eigen ondertitelregels in één keer verwijderen

In de tijdelijke ondertiteleditor kun je nu originele bronregels, zoals
`2-4,7,12`, als één herstelbare bewerking verwijderen. De dialog toont
de volledige tekst en tijdcodes van **elke** geselecteerde regel in
bladerbare pagina's, plus hoeveel regels overblijven. Wie eerst zoekt,
kan die oorspronkelijke zoekresultaatnummers expliciet overnemen;
zoeken alleen verandert de selectie nooit. Ongeldige of dubbele
ranges en het verwijderen van alle cues worden geweigerd. Overige
tekst/tijdcodes, bronbestanden en het live gesprek blijven intact;
bewaren vereist opnieuw een afzonderlijke Android-bestandskeuze.
[Instructies](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: paginavoorbeeld voor grote eigen ondertitelbestanden

De CSV/TSV- en LRC-exportdialoog toont niet meer alle 256 KiB ineens
in één tekstcomponent. De exacte bestandstekst is nu per begrensd
gedeelte volledig door te bladeren, met **Vorige** en **Volgende**.
Het gekozen bestand bevat uiteraard alle regels; alleen de weergave
is opgesplitst. Geen beperkingen versoepeld, geen automatische opslag
of vertaling. [Uitleg](docs/MANUAL_SUBTITLE_PASTE.md).

## Verbeterd: rechtstreeks SRT/VTT uit Android-bestandsproviders openen

De bestaande **Open SRT/WebVTT…**-actie en **Tweede SRT/VTT-document
toevoegen…** lezen sinds deze versie ook via de veilige 256-KiB-reader
op een IO-thread. De interface blijft bruikbaar terwijl een
geselecteerde Android-provider een document levert. Een ongeldig
tweede document vervangt nooit je huidige bewerkbare concept; alleen
geldige brondata gaan naar de bestaande samenvoeg-preview. Ook
providers die SRT/VTT niet als text/plain markeren worden ondersteund,
zonder de bestaande strikte parser, toestemming of privacygrenzen
te versoepelen. [Handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: handmatige LRC-begintijden weer opslaan

Tuddy kan bestaande `.lrc`-tekst met eigen beginmomenten niet alleen
importeren maar ook uit een bewerkt lokaal ondertitelconcept
**bewust als LRC-kopie exporteren**. In de editor kies je **Bewaar eigen
begintijden als LRC…** en bekijk je de complete ruwe
`[mm:ss.mmm]eigen tekst`-preview vóór Androids bestandskiezer.
Alleen enkelregelige cues en strikt oplopende tijdcodes worden
geaccepteerd. **LRC bewaart geen eindtijden**: gebruik SRT, VTT,
CSV of TSV wanneer die behouden moeten blijven. De bestaande
privacy- en bytegrenzen en expliciete opslagtoestemming blijven van
kracht. Geen automatische vertaling of audio-uitlijning.
[Details](docs/MANUAL_SUBTITLE_PASTE.md).

## Hersteld: Android-documentkiezer verliest geen tijdelijk ondertitelconcept

Bij een expliciete SRT/VTT/LRC/CSV/TSV/TXT-bestandskeuze kan Android
de huidige dialoog sluiten zodra de systeembestandskiezer zichtbaar wordt.
Daarom is de selectie nu **Activity-owned**, niet meer afhankelijk van een
door `ON_STOP` verwijderde Compose-dialoog. Zowel bij het eerste als
bij een tweede ondertitelspoor keert de gebruiker terug naar de
**volledige tijdelijke voorbeeldweergave**, zonder het live gesprek te
wijzigen. De oude eigen tekst, tijdinstellingen en selectie worden in
RAM hersteld bij annuleren of weigering. Na een geslaagde keuze wordt
uitsluitend de bewust gekozen brontekst vervangen, met behoud van
aangepaste start-/duurinstellingen en een nieuwe expliciete bronselectie.
Het inlezen blijft begrensd tot 256 KiB, op een IO-thread; er is
geen blijvende URI-toestemming of verborgen bestandsschrijfroute.
[Details](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: eigen ondertitel-CSV/TSV weer exporteren

Na de bestaande SRT/VTT-, LRC- of spreadsheetimport kun je vanuit
de **tijdelijke ondertiteleditor** je eigen begin- en eindtijden en
handmatige tekst weer als **bewerkbare CSV of TSV** opslaan. Tijdcodes
blijven behouden. Eén tekstregel per cue geeft drie kolommen; precies
twee handmatige tekstregels per cue geven vier kolommen met eigen
bronzin en eigen betekenis. Er wordt geen vertaling ingevuld.

Vooraf verschijnt de **complete ruwe bestandspreview**, inclusief
exact aantal UTF-8-bytes. Geen onzichtbare inkorting van meerregelige
cues, geen verborgen formule-uitvoering in spreadsheetcellen en geen
automatische opslag: een niet veilig/lossless exporteerbaar concept
wordt geweigerd en kan nog steeds als SRT/VTT worden opgeslagen.
Android vraagt een expliciete bestandsproviderkeuze, waarna de exact
bevestigde bytes opnieuw worden berekend. CSV/TSV bevat geen
kopregel en kan met de bestaande 3/4-koloms import terug worden geopend.
[Stappen en grenzen](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: eigen LRC-lyrics met beginmomenten importeren

Kies in de bestaande ondertitelimport **LRC: eigen begintijden per regel**.
Plak of selecteer via Android je eigen `[mm:ss.xx]Zin`-bestand. Tuddy
bewaart de expliciete begintijden, leidt ieder eindmoment af van het
**volgende begin** en schat alleen het laatste einde op **3 seconden**.
Kies met de bestaande volledige zoekpreview welke regels je wilt openen,
corrigeer tijden en exporteer desgewenst zelf een SRT/VTT-kopie.
Geen verborgen offsets, geen automatische vertaling of gemeten
audiowoorduitlijning; maximaal 500 eigen cues binnen de bestaande
256-KiB- en privacygrenzen.
[Uitleg](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: eigen ondertitel- en spreadsheetbestanden kiezen op Android

De bestaande offline concepteditor ondersteunt nu naast plakken ook een
**expliciete Android-documentkeuze**. Kies in het importvenster de passende
SRT/VTT-, CSV-, TSV- of gewone-tekstmodus, tik op **📂 Kies eigen UTF-8
SRT, VTT, CSV, TSV of TXT-document…** en selecteer zelf je bestand.
Tuddy leest maximaal **256 KiB UTF-8** op een achtergrondthread en houdt
de tekst alleen tijdelijk in het geheugen. Ongeldige/binaire/te grote
documenten laten je vorige invoer intact. Na een geldige keuze kun je de
volledige inhoud, tijdcodes, geselecteerde bronregels en zoekpreview
controleren en apart besluiten het concept te openen. Ook de bestaande
route voor een tweede handmatig gecombineerd spoor ondersteunt dit.

Er zijn geen permanente URI-rechten, extra permissies, automatische
cloudverwerking of stille schrijfopdrachten. Een **door de gebruiker
gekozen Android-documentprovider kan zelf online/cloudgesynchroniseerd
zijn**; de app zelf leest en verwerkt lokaal. De bestaande exportgrenzen,
veiligheidscontroles en expliciete toestemming blijven behouden. Geen
spraakherkenning, taalvalidatie of daadwerkelijke audiowoorduitlijning.
[Details](docs/MANUAL_SUBTITLE_PASTE.md).

## Nieuw: SRT/WebVTT en gewone tekst handmatig plakken

Kopieer je eigen SRT/WebVTT-tekst naar de tijdelijke ondertiteleditor
zonder eerst een bestand te maken. Of kies **Gewone zinnen** om eigen
tekst per regel of volgens geschatte leestekengrenzen om te zetten naar
losse ondertitelkaarten met **zelf gekozen, niet aan audio gekoppelde**
tijden. Voor mobiel gebruik zijn er ook **twee losse tekstvakken**:
originele zinnen en zelfgeschreven betekenissen, één op één per regel;
ontbrekende regels worden duidelijk aangegeven, niet verzwegen.
Je kunt ook **twee zelfgeschreven tekstkolommen** (bron + eigen
betekenis, gescheiden door een tab) per ondertitelkaart combineren, óf vier
kolommen plakken met **zelf opgegeven begin- en eindtijd** plus beide
tekstkolommen. Bestaande tijdcodes worden daarbij behouden, niet geschat.
De invoerkeuzes en tijdmenu's passen op smallere Android-schermen. Bekijk
het volledige bronmateriaal, filter op tekst en kies desgewenst een exact
bereik bronregels voordat je alleen die regels opent. Een tweede geplakt
spoor kan via de bestaande tijdmodi worden gecombineerd
met behoud van Undo/Redo. Niets leest automatisch het klembord, vertaalt,
synchroniseert of slaat zonder aparte toestemming op.

[Handleiding en privacygrenzen](docs/MANUAL_SUBTITLE_PASTE.md).

# Tuddy 🎙️

## Nieuw: eigen starttijd en kaartduur voor tekst zonder tijdcodes

De handmatige tekst- en twee-kolomsimport heeft naast vaste presets
nu ook **Aangepaste begin- en duurinstelling…**: stel de start in van
0 tot 21600 seconden en kies 0,5–30 seconden per kaart met een
decimale punt of komma (maximaal millisecondeprecisie). De uitgebreide
scrollpreview laat de **geschatte** timing vóór overname zien.
Bestaande SRT/VTT- en drie-/vierkoloms CSV/TSV-tijden blijven
ongewijzigd; dit is geen echte audio-uitlijning of herkenning.
[Handleiding](docs/MANUAL_SUBTITLE_PASTE.md).

---
## Nieuw: bestaande tijdcodes uit CSV-spreadsheets plakken

Naast de bestaande tabgescheiden import kun je eigen transcriptregels
nu ook als **drie- of vierkoloms CSV** plakken. Een CSV-veld met een
komma of aanhalingstekens wordt strikt gelezen; tijdcodes blijven
ongewijzigd. Bij ongeldige invoer noemt Tuddy de oorspronkelijke bronrij.
De volledige offline preview, expliciete deel-/meerbereikselectie en
bewuste Android-opslag blijven bestaan. Een CSV-regel is steeds één cue;
meerdere fysieke regels binnen een gequote veld worden niet ondersteund.
Er is geen automatische ASR, audiouitlijning of vertaling.
**Ook zonder tijdcodes:** kies twee eigen CSV-tekstkolommen (bron en
handmatige betekenis). Tuddy combineert ieder door jou geschreven paar tot
één tweetalige cue met uitsluitend handmatig geschatte tijden; geen ASR
of automatische vertaling. De app noemt de oorspronkelijke foutregel
als CSV-invoer niet klopt.

[Voorbeelden en beperkingen](docs/MANUAL_SUBTITLE_PASTE.md).

---

## Nieuw: van gevonden ondertiteltekst naar een exacte importselectie

Zoek in een eigen geplakte SRT/VTT- of transcriptbron en kies
**Gebruik gevonden regels als selectie**. Tuddy zet alle gevonden
originele bronregels om in een leesbaar en bewerkbaar bereik,
bijvoorbeeld `1-3,7,9-11`. Je kunt expliciet herstellen naar
**Doorzoek opnieuw alle bronregels**. De zoekfunctie bewaart niets
en bevestigt geen import namens jou; de bestaande aparte bevestiging
blijft nodig. Dit werkt ook met de nieuwe CSV-import.
[Meer uitleg](docs/MANUAL_SUBTITLE_PASTE.md).

---
## Nieuw: meerdere losse ondertitelbereiken in één import

In **Plak zelf tekst…** kun je nu met **Losse regels (optioneel)**
bewust een selectie invoeren zoals `1-5,8,12-16`.
Dat kiest uitsluitend die originele bronregels in tijdlijnvolgorde,
ook uit een groter SRT/VTT-document of eigen geplakte transcriptregels.
De bestaande **Van/Tot**-selectie blijft beschikbaar wanneer je het
extra veld leeg laat. De preview toont oorspronkelijke regelnummers;
zoeken wijzigt de gekozen import niet. Dubbele/overlappende of
ongeldige bereiken worden afgewezen; de bevestiging leest de
volledige bron opnieuw en importeert alleen de bevestigde rijen.
De originele documenten en tijdcodes worden niet herschreven.
[Exacte bediening](docs/MANUAL_SUBTITLE_PASTE.md).

---


## Nieuw: eigen transcript met begin/eindtijden direct plakken

In de tijdelijke ondertiteleditor kun je nu ook **drie eigen
tabgescheiden kolommen** importeren: begintijd, eindtijd en één
zelfgeschreven transcriptzin. Tuddy bewaart jouw timing exact en
verzint geen ontbrekende vertaling. De bestaande vierkoloms-import
(bron plus handmatige betekenis) en deze drie-kolomsvariant melden
nu **de precieze oorspronkelijke foutregel** bij ongeldige tijden,
lege rijen of een verkeerde kolomindeling. Je kunt alles vooraf
doorzoeken en enkel een expliciet geselecteerd deel openen.
[Werkwijze en privacy](docs/MANUAL_SUBTITLE_PASTE.md).

---


### LRC-tijdcorrectie: exact één cue of een aaneengesloten reeks (PR #226)

In de volledig lokale **Plak eigen ondertitels → LRC**-route kun je
uitgeschreven refrein-cues per individueel tijdstempel corrigeren.
Naast de bestaande gekozen ±30 seconden verschuiving zijn er nu twee
verschillende expliciete opties. **Eén exact cue-begin** vervangt alleen
de opgegeven oorspronkelijke starttijd; **Tweepuntscorrectie** verdeelt
de begintijden tussen twee bewust gekozen bronrijankers (0,5×–2×
snelheidsverhouding) zonder cues erbuiten aan te raken. Beide functies
gebruiken een volledig pagineerbare tijdlijn met originele én
gecorrigeerde start-/eindtijden, springen naar gewijzigde pagina's,
en vereisen een afzonderlijke bevestiging. De laatste acht bewuste
tijdacties zijn tijdelijk ongedaan te maken/opnieuw toe te passen.

Voor losse fouten verspreid over een langere eigen tijdlijn is ook een
**bulkveld met maximaal 32 expliciete `bronnummer=uu:mm:ss.mmm`-correcties**
beschikbaar, met volledige gecombineerde preview en alles-of-niets
bevestiging. Dubbele bronregels, tijdcodebotsingen en overschrijden van
de limiet worden geheel geweigerd.

Bovendien kan de primaire LRC-plakdialoog de **volledige
zelfgeschreven/gecorrigeerde LRC-bron**, inclusief `[offset:]`,
artiest-/titelmetadata en oorspronkelijke refreintags, als
**een apart nieuw .lrc-document** bewaren via de expliciete
Android SAF-bestandskiezer. Annuleren of mislukte opslag
heropent de tijdelijke eigen brontekst; de oorspronkelijke
door de gebruiker gekozen providerbron blijft ongewijzigd.

Alle wijzigingen blijven RAM-only totdat de gebruiker apart de
bestaande Android-bestandsopslag kiest; de oorspronkelijke gekozen
providerbron wordt niet overschreven. Onmogelijke tijden, gewijzigde
chronologische cue-identiteit en niet als SRT én VTT exporteerbare
uitkomsten worden volledig geweigerd. Er wordt geen audio gelezen,
spraak herkend, automatisch vertaald of cloudfallback gestart.
Eén correctierecept kan nu zelfgeschreven brontekst én betekenis
gericht aanpassen met `@bron:`, `@betekenis:` of `@alles:`
per regel. De preview toont exact welke tekstzijde door welke
regel gewijzigd wordt; er wordt geen taal herkend of vertaald.

Hergebruik een eigen reeks op documenten waar niet alle foutwoorden
voorkomen: de optionele instelling **Sla niet-gevonden
tekstcorrecties over** toont per regel wat daadwerkelijk veranderde,
maar weigert nog steeds onveilige correcties en een volledig
resultaat zonder wijziging.

Eigen correctiereeksen kunnen nu ook via aparte **Kopieer**- en
**Plak**-knoppen op het Android-klembord worden hergebruikt.
Niets wordt automatisch gelezen; externe klembordapps kunnen
expliciet gekopieerde tekst wel synchroniseren.

De lokale ondertiteleditor ondersteunt daarnaast tot **twaalf
expliciete tekstcorrecties** in één tijdelijke handeling via
`oude tekst => nieuwe tekst` per regel. De bestaande bronregelselectie,
Unicode-woordgrenzen en volledige vóór/na-preview blijven actief;
alle correcties slagen samen of worden samen geweigerd, met één
Undo/Redo-stap en zonder automatische bestandsopslag.

De lokale ondertiteleditor kan met **Zoek en vervang** nu ook **Alleen volledige woorden** kiezen. Dan blijven langere woorden intact: `kat` wordt bijvoorbeeld niet binnen `katten` veranderd. Ook Unicode-letters en gecombineerde accenten tellen mee. Controleer vóór overname de complete vóór/na-preview; niets wordt automatisch naar het originele bronbestand teruggeschreven.

Zie [de LRC-/ondertitelhandleiding](docs/MANUAL_SUBTITLE_PASTE.md).
**Deze wijzigingen zijn vooralsnog uitsluitend op open PR #226;
verplichte Android CI en de afzonderlijke device/licentie/release-gates
bepalen of ze naar main kunnen.**


## Verbeterde lokale woordherhaling zonder spraakherkenning

De optionele **geschatte woordvolger** kan nu ook korte woorden herhalen: bij een geschatte woordduur onder 0,75 s bevat de herhaling een begrensd stukje naburige audio binnen de zelfgekozen ondertitelcue. Te korte cues en vensters boven twee minuten blijven uitgesloten; handmatige tijdankers blijven leidend. Het Android-scherm maakt duidelijk dat dit alleen een **tijdcode-inschatting** is en de herhaling andere woorden kan omvatten. [Handleiding](docs/ESTIMATED_OFFLINE_SUBTITLE_WORD_FOLLOW.md).

---

## Nieuw · cue-relatieve handmatige woordankers hergebruiken

Naast absolute markeringen accepteert de offline woordvolger nu
ook `2=+00:00:01.250` als offset vanaf het begin van een cue.
De gebruiker kan zelf een relatieve ankerreeks kopiëren of in het
tijdelijke invoerveld zetten. Hergebruik op een andere ondertitel
vereist opnieuw volledige cue- en tijdcontrole en bewuste bevestiging;
gemengde absolute/relatieve invoer wordt geweigerd. De gebruiker
kiest alle tijden zelf: geen audioanalyse, ASR of cloud.

## Nieuw · eigen woordtijdankers terugplakken zonder audioanalyse

De optionele **geschatte woordvolger** kan maximaal 16 expliciet door
jou gemarkeerde tijdankers van één ondertitel als selecteerbare tekst
tonen, zoals `2=00:00:12.250;5=00:00:13.900`. Gebruik **Kopieer
woordankers** voor één-tik kopiëren naar het Android-klembord en
**Plak woordankers uit klembord…** om die tekst alleen na een
aparte tik in te lezen. Bewaar of kopieer die tekst zelf en plak hem
later in de invoer onder
**Handmatige woordankers** bij de **juiste** actieve cue.
Tuddy valideert de hele set tegen de huidige tekst en tijdgrenzen,
toont de exacte voorgestelde waarden en vervangt pas na
**Bevestig en vervang ankers van deze ondertitel** de tijdelijke
waarden. Een foutieve/te lange reeks kan nooit deels worden toegepast.

Dit is handmatige, terug te plakken tekst, geen gemeten woordtiming,
automatische opslag, audioherkenning of cloudfunctie. De bestaande
woordherhaling, springen en highlighting gebruiken de nieuwe
handmatige ankers na jouw bevestiging. Klembordapps kunnen tekst
zelf synchroniseren.

[Handleiding geschatte woordvolger](docs/ESTIMATED_OFFLINE_SUBTITLE_WORD_FOLLOW.md).

---

## Verbeterd: woordstappen respecteren expliciete zero-width-spaties

De geschatte woordvolger gebruikt nu ook bestaande `U+200B`-scheidingstekens
naast gewone Unicode-witruimte. Er worden geen woordgrenzen afgeleid of
verzonnen voor tekst die zelf geen scheidingstekens bevat.

[Handleiding](docs/ESTIMATED_OFFLINE_SUBTITLE_WORD_FOLLOW.md).

---

## Nieuw op PR #225: woordvolger met zelf getikte tijdankers

In de bestaande privé-audiospeler kun je naast de gelijkmatig geschatte
woordpositie nu maximaal **16 zelf gekozen tijdankers per ondertitelregel**
zetten. Typ een woordnummer (vanaf 2), luister naar je eigen audio en tik
op **Markeer dit woord op huidige audiotijd**. De lokale woordvolger,
Vorig/Volgend, woord- en woordgroepherhaling volgen deze eigen ankers;
tussen de ankers blijft een begrensde, gelijkmatige *schatting* gelden.
Oplopende ankers, voldoende tijd tussen woorden en de bestaande cuegrenzen
worden afgedwongen. Je kunt de markeringen per regel wissen; een nieuwe
audio/ondertitel of verschoven cueklok wist ze ook. Geen ASR, gemeten
spraakuitlijning, gewijzigde bronbestanden, netwerk of nieuwe rechten.
[Volledige uitleg](docs/ESTIMATED_OFFLINE_SUBTITLE_WORD_FOLLOW.md).

---

## Nieuw op PR #225: een eigen geschatte woordgroep herhalen

Bij **▶ Eigen audio met tijdcodes afspelen** en de optionele
**Geschatte woordvolger** kan je nu onder **Kies direct een woord**
een aaneengesloten woordgroep afbakenen met twee eigen woordnummers,
of **Start/Eind = huidig woord** aantikken tijdens het luisteren.
De exacte geselecteerde originele tekst is vooraf zichtbaar.
**↻ Herhaal gekozen woordgroep** speelt de begrensde ruwe tijdspanne
van 0,75–120 seconden opnieuw af via de bestaande lokale A–B-herhaling.
Oplopende, bestaande woordnummers en audio binnen hetzelfde cuevenster
zijn verplicht; niets wordt opgeslagen of opgenomen.

De woordvolger kan nu ook **tussen twee ondertitelregels** uitgezet
worden. Al deze tijden zijn uitsluitend gelijkmatig over eigen
SRT/VTT-regels geschat, niet gemeten of door ASR uitgelijnd.
Geen nieuwe machtigingen, externe AI of cloud.
[Bediening en beperkingen](docs/ESTIMATED_OFFLINE_SUBTITLE_WORD_FOLLOW.md).

---

## Nieuw: handmatig geschat woordfragment herhalen en volledig privé-luisterwerkblad

In de eigen SRT/VTT-audiospeler laat de optionele geschatte woordvolger
nu één zelfgekozen woordpositie **handmatig herhalen** via de bestaande
A–B-afspeellus. Alleen cue-vensters van minstens 750 ms worden gebruikt;
dit zijn gelijkmatig geschatte tijden, **geen gemeten of herkende
woordgrenzen**. De herhaling stopt afzonderlijk of bij uitschakelen
van de woordvolger, andere afspeelkeuze, gewijzigde tijdverschuiving
of verlaten van de speler. Er worden geen audiofragmenten gemaakt.

De bestaande PR #225 integreert daarnaast het privé-luisterwerkblad
en selectie-veilige CSV-export uit PR #224. Kies maximaal vier eigen
A–B-luisterpassages en exporteer bewust **alle**, **nog oefenen** of
**nog onbeoordeelde** als afdrukbaar HTML-luisterwerkblad met aparte
bronpagina, of als controleerbare UTF-8 CSV. Broncode, persoonlijke
beoordelingen en aantallen weggelaten regels zijn transparant;
oude gegevens worden niet na een gewijzigde Android-bestandskeuze
opgeslagen. Geen netwerk, ASR, automatische uitlijning of studieprofiel.

[Handleiding woordherhaling](docs/ESTIMATED_OFFLINE_SUBTITLE_WORD_FOLLOW.md) ·
[Handleiding luisterexport](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

# Tuddy 🎙️

## Nieuw: tik direct het gewenste woord in je ondertitel aan

In de optionele **geschatte woordvolger** kun je nu een doorbladerbare
lijst met de woorden van de actieve SRT/VTT-regel openen. Eén tik springt
naar het bijbehorende *geschatte* woordmoment; vorige/volgende-woord
blijft werken. Bij lange regels kun je ook direct een **woordnummer**
invoeren. De pagina en de naar rechts/links lopende woordtekst volgen
je expliciete keuze. Alles gebruikt uitsluitend de eigen cue-tijdcodes:
geen spraakherkenning, betrouwbare woordtiming of nieuwe permissies.
[Handleiding](docs/ESTIMATED_OFFLINE_SUBTITLE_WORD_FOLLOW.md).

---

## Nieuw: privé luisteroverzicht ook als CSV (inclusief woordvolger)

De optionele geschatte ondertitelwoordvolger behoudt nu de nieuwste
mogelijkheden uit de luisteroefenmodule. Je kunt naast HTML en TXT
je eigen A–B-fragmenten **bewust als zeskoloms UTF-8 CSV** exporteren,
na het bekijken van de volledige CSV-inhoud. Fragmentbeoordeling,
zelfgeschreven vervolgstap, tijdvak en kort ondertitelvoorbeeld blijven
lokaal tot je expliciet een Android-documentprovider kiest.
De bytes worden bij terugkomst opnieuw gecontroleerd; geen audio-URI,
nieuwe permissies, automatische ASR of automatische woorduitlijning.
[Meer informatie](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---


## Nieuw: optionele geschatte woordvolger bij eigen audio

In **▶ Eigen audio met tijdcodes afspelen…** kun je nu bewust
**Geschatte woordvolger proberen** aanzetten. Tuddy markeert dan
tijdelijk een woord per actieve SRT/VTT-regel volgens een **gelijke
verdeling van de door jou aangeleverde regeltijd**. Met vorige/huidige/
volgende woordstappen spring je handmatig naar geschatte audiomomenten. Dit is géén
gemeten woorduitlijning, echte transcriptie of automatische
spraakherkenning. De modus is standaard uit, wordt bij audio
wisselen/afsluiten gewist en kan op ieder moment weer uit om de
bestaande aanklikbare woordkaarten te gebruiken.
[Werking en beperkingen](docs/ESTIMATED_OFFLINE_SUBTITLE_WORD_FOLLOW.md).

---


## Privé luisteren: apart leerlingen-CSV zonder antwoorden of brontekst

In **Mijn privé luisteroverzicht → Leerlingenblad** is nu naast het HTML-werkblad ook een **volledige, lokaal voorgestelde leerlingen-CSV** beschikbaar. Elke rij toont alleen een bewust geselecteerd eigen A–B-tijdvak, de gekozen opdracht en een lege antwoordcel. Niet inbegrepen zijn ondertitelregels, luisterbeoordelingen, persoonlijke notities of audio. Privé TXT/CSV/HTML-exportknoppen blijven in de leerlingenmodus gesloten; voor een eigen brontekstrapport kies je bewust **Zelfstudie**. Na Androids bestandskiezer controleert Tuddy opnieuw de volledige bestandsbytes en actuele gebruikerskeuze. [Gebruik en privacygrenzen](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

## Offline luisteren · eigen opdracht kiezen

Een werkblad kan nu ook **per geselecteerd A–B-fragment** een andere
oefenopdracht bevatten. De algemene keuze blijft de standaard;
de gebruiker stelt maximaal vier afwijkende opdrachten bewust
per fragment in en controleert de export vóór opslaan.

Bij het eigen A–B-luisterwerkblad kun je nu kiezen tussen letterlijk
uitschrijven, kernwoorden noteren, zelf samenvatten of zelf de betekenis
opschrijven. De keuze verschijnt in het privé HTML-bestand of
antwoordvrije leerlingenblad. Transcriptievelden volgen de expliciet
ingestelde RTL/LTR-ondertiteltaal; er wordt niets automatisch vertaald
of nagekeken. [Handleiding](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

## 21 oktober · volledige bronregels veilig bestuderen en als CSV bewaren

Het zelfgekozen, offline **A–B-luisterwerkblad** toont maximaal 18 regels
in de kleine vragen-preview, maar zet **alle volledig passende brontekstregels**
in het afzonderlijke, bewust te openen bronregelblad. Lange regels worden
alleen in de korte preview ingekort; in het volledige bronregelblad
wordt te grote input veilig geweigerd in plaats van onvolledig
gepresenteerd. Zonder print-opt-in blijft dat antwoordblad buiten
de afdruk; leerlingenbestanden bevatten geen brontekst.

Daarnaast is er **Bekijk volledige bronregel-CSV vóór opslaan**:
een per 12 rijen doorbladerbaar CSV-document met één rij per
volledig binnen jouw eigen A–B-tijdvak passende ondertitelregel,
oorspronkelijke bronregel,
tijdcodes en eventuele eigen beoordeling/notitie. Via **Bewaar alle
passende bronregels als CSV…** kies je daarna zelf een Android-provider.
De bestaande compacte CSV blijft bestaan. Alle bronwaarden zijn
als tekst beschermd tegen spreadsheetformules; geen automatische
audio-uitlijning of netwerkfunctie. Bij terugkeer uit de Android-
bestandskiezer controleert Tuddy opnieuw de exacte goedgekeurde bytes.

[Volledige gebruiksuitleg](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

## 21 oktober · volledig luisterbronregelblad en correcte A–B-CSV

In de **offline luisteroefening** geeft de uitklapbare preview nog steeds
maximaal 18 brontekstregels weer, maar het apart te openen/optioneel
te printen **volledige bronregelblad** bevat nu alle ondertitelregels
die geheel binnen het door jou gekozen A–B-fragment liggen. Bij meer
dan één fragment kun je via de schermlinks direct naar een fragment
springen. Het antwoordblad wordt standaard **niet** afgedrukt; de
leerlingenexport bevat nog steeds helemaal geen bronregels. Bij een
te groot werkblad wordt de volledige export geweigerd, niet afgekapt.

Ook de eigen **CSV-ondertitelvoorbeelden** tonen nu ondertitelregels
die geheel binnen de zelfgekozen audiogrenzen vallen, zelfs wanneer
die markeringen niet op exact dezelfde milliseconde als een SRT/VTT-cue
liggen. De CSV vermeldt hoeveel passende tussenregels zijn weggelaten,
zonder een echte audio-uitlijning te claimen. Alleen expliciete Android
SAF-opslag; geen nieuwe netwerkroute of machtiging.

[Handleiding luisterwerkblad](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

## Nieuw: schermlezers volgen je zelfgekozen ondertiteltaal in het luisterwerkblad

Wanneer je zelf de taal van je ondertiteltekst kiest, neemt het afdrukbare
luisterwerkblad die taal mee in de HTML-bronregels. De regels krijgen een
gevalideerd taalattribuut en passende LTR/RTL-richting, zodat bijvoorbeeld
Sorani en Pasjtoe niet als Nederlandse tekst worden voorgelezen. Er wordt
geen taal automatisch herkend; zonder expliciete keuze blijft de richting
afhankelijk van de tekst.

[Handleiding](docs/PRIVATE_LISTENING_REPORT_HTML.md).

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

## Nieuw: exact geselecteerde privé-luister-CSV zonder verborgen regels

De selectie **Alle / Nog oefenen / Nog niet beoordeeld** geldt nu ook
voor de CSV-export. Android toont de echte geselecteerde rijen en weigert
de bewaarde versie na een verandering van selectie, zelfs wanneer
verschillende filters toevallig dezelfde rijen opleveren. De zelfstandige
HTML-luisterwerkbladexport gebruikt eveneens dezelfde categorie vóór én
na de bestandskiezer. Een CSV-bronexcerpt meldt exact hoeveel
tussenliggende regels zijn weggelaten, zonder vermeende audio-uitlijning.
Ongeldige tijdverschuivingen worden geweigerd en het privé-luisteroverzicht
kan geen valse cue-koppelingen meer tonen door integer-overloop.
Geen audio, netwerk, automatische scores of permanente oefenresultaten.
[Meer uitleg](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

## Nieuw: offline luisterwerkblad met ingeklapte bronregels en afdrukversie

Maak in de eigen SRT/VTT-audiospeler maximaal vier A–B-fragmenten en
kies bij **Mijn privé oefenoverzicht** nu ook **Bewaar luisterwerkblad…**.
In het scriptvrije HTML-bestand kun je eerst luisteren en notities maken;
per fragment onthul je pas zelf de bronondertiteltekst. Kies onafhankelijk
van de afspeelroute **alle**, alleen **nog oefenen**, of **onbeoordeelde**
fragmenten voor dat bestand. Lege categorieën zijn niet exporteerbaar.
Bij afdrukken
staan de vragen met schrijfruimte voorop en de gekozen SRT/VTT-bronregels
op een aparte zelfcontrolepagina. Lange passages vermelden zichtbaar
hoeveel tussenliggende regels zijn weggelaten. Eigen beoordeling en
oefennotitie gaan mee, nooit audio, URI of spraakherkenningsresultaten.
De bestaande expliciete Android-bestandskeuze en byte-hercontrole
blijven verplicht. [Details](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

# Tuddy 🎙️

## Nieuw: beter leesbare eigen passages in privé-luisterverslagen

Voor geselecteerde A–B-fragmenten toont Tuddy nu maximaal acht
bijbehorende originele SRT/VTT-bronregels afzonderlijk met hun
bronnummers; voor grotere passages worden de niet-getoonde regels
expliciet geteld. Lange zinnen worden zichtbaar en op hele
Unicode-tekens ingekort. Dit komt in het privé-overzicht en de
bewuste HTML/TXT-export, niet als verborgen audio-analyse.
[Handleiding](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

## Nieuw: exacte milliseconden op eigen A–B-luisterkaartjes

Eigen gekozen audiofragmenten behouden nu ook in de HTML-, TXT- en
CSV-export de volledige **uu:mm:ss.mmm**-precisie. De eerdere weergave
op hele seconden maakte korte of dichtbije passages lastig van elkaar
te onderscheiden. De wijziging wijzigt geen audio, ondertitelbestanden,
opslagrechten of (niet-gevalideerde) bronuitlijning.
[Uitleg](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---

## Nieuw: mijn A–B-luisterkaarten als CSV voor eigen overzichten

Naast HTML en TXT heeft het tijdelijke privé-luisteroverzicht nu een
**volledige CSV-preview** en een afzonderlijke, bewuste **Bewaar CSV…**-keuze.
De zes kolommen bevatten eigen fragmenttijden, zelfbeoordeling, eigen notities
en alleen een begrensd ondertitelvoorbeeld, nooit audio of een audio-URI.
Het UTF-8-bestand is begrensd, spreadsheetformula-veilig, kan door een
Android-provider gesynchroniseerd worden en wordt na de documentkiezer
exact opnieuw gecontroleerd op gewijzigde brondata. Het is geen
automatisch begrip-/uitspraakrapport.
[Details](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---


## Nieuw: mijn offline luisteroefenoverzicht zelf als HTML of TXT bewaren

Bekijk na je eigen SRT/VTT-audiooefening **Mijn privé oefenoverzicht**
met maximaal vier A–B-fragmenten, handmatig gekozen beoordelingen,
een optionele eigen oefennotitie (maximaal 240 tekens) per fragment
en uitsluitend écht afgeronde begeleide rondes. Met **Bewaar zelf HTML…**
kun je dit bestaande overzicht nu afzonderlijk als offline afdrukbaar
HTML-bestand óf als eenvoudige UTF-8 TXT-notitie bewaren. Tuddy neemt geen audio of audio-URI mee en controleert
bij terugkeer uit de bestandskiezer dat het overzicht niet gewijzigd is.
De opslagprovider kan de tekst zelf synchroniseren; tijdcodes zijn niet
automatisch met audio gecontroleerd.
De begeleide A–B-route heeft nu ook een aparte keuze **Nog niet
beoordeeld** naast alle fragmenten en de zelf als lastig gemarkeerde
fragmenten; een lege selectie kan niet worden afgespeeld.
Het privé-overzicht geeft ook een eigen vervolgrondje met eerst de
door jou als lastig beoordeelde en daarna nog onbeoordeelde fragmenten.
Oefennotities bewaren Unicode-tekens zonder emoji halverwege af te kappen.
[Bediening en privacy](docs/PRIVATE_LISTENING_REPORT_HTML.md).

---


## Offline invuloefeningen: direct naar kaarten en antwoord met vraagcontext

In het zelfstandige **Aanvuloefening · HTML** kun je zonder scripts of internet met **Vorige kaart / Volgende kaart** door een heel oefenboek bladeren. Op het zelfstudie-antwoordblad staan nu **de oorspronkelijke vraag en het volledige antwoord samen**, met overeenkomstig ronde-/kaartnummer en een teruglink naar de vraag. Bij afdrukken verdwijnt de navigatie en blijft het antwoordblad alleen beschikbaar na de bestaande bewuste printkeuze. Een **leerlingenblad** bevat ook nu geen volledige antwoorden. De bestaande expliciete Android-export en maximale uitvoergrootte blijven gehandhaafd. [Details](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw · eigen Unicode-woordgrenzen blijven intact in invulhints

De meerrondige, tweetalige HTML-aanvuloefening herkent nu ook
niet-afbreekbare spaties en expliciete zero-width-spaties (U+200B)
als **door de schrijver zelf gekozen woordgrenzen**. Spaties en
scheidingstekens blijven intact in de opgaven; een tekst zonder
zulke grenzen wordt niet kunstmatig in woorden opgesplitst.
Losstaande leestekengroepen zoals `...` of `—` blijven zichtbaar
en tellen niet mee als te verbergen antwoordwoord; de juiste
invulpositie verschuift daardoor niet. De Android-hintpreview en
zelfstandige offline HTML gebruiken dezelfde tokenisering.
Zonder netwerk, audio-uitlijning of automatische taalbeoordeling.

De native Android-voorbeeldweergave voor kaartvragen, antwoorden
en invulhints gebruikt nu ook de echte LTR/RTL-schriftrichting,
los van Nederlandse UI-labels. De offline HTML-export en de
leerlingenvariant behouden hun bestaande privacygrenzen.

Ook de getypte en afgedrukte **antwoordruimte** volgt voortaan de expliciet gekozen LTR/RTL-antwoordtaal, met HTML-`lang` voor schermlezers. [Uitleg bij de invuloefening](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## RTL-richting en schermlezers in tweetalige HTML-oefenkaarten

Wanneer je taalcodes zoals `[ar]`, `[ps]` of `[ckb]` toont, houdt
Tuddy het Latijnse label apart van de zin en bepaalt de tekstrichting uit
de taal-/scriptcode. De daadwerkelijke zin krijgt ook een passend HTML-`lang`
attribuut, zodat schermlezers de juiste taal kunnen gebruiken. Dit geldt
voor vragen, invulhints en beide antwoordweergaven, ook als de zichtbare
taalcodes uit staan.

[Handleiding voor de tweetalige export](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw: afwisselende kaartvolgorde met identieke antwoordnummering

Bij **Aanvuloefening · HTML** kies je voortaan ook de
**kaartvolgorde**: oorspronkelijke zinsvolgorde of
**Afwisselende kaartvolgorde per ronde**. Voor dezelfde selectie
en dezelfde rondes blijft de volgorde volledig reproduceerbaar:
ronde 1 behoudt de bronvolgorde, ronde 2 draait de kaartvolgorde
om en ronde 3 begint op een verschoven positie. Bij maar één kaart
kan de volgorde natuurlijk niet wijzigen. Er worden geen random
shuffles, nieuwe zinnen of vertalingen gemaakt.

In Android verschijnt de **werkelijke kaartvolgorde per gekozen ronde**
als de oorspronkelijke kaartposities vóór de bestandskeuze. De
HTML-generator en de Android-preview gebruiken exact dezelfde
ordefunctie, zodat het afzonderlijke zelfstudieantwoordblad nog
steeds bij dezelfde kaartnummers hoort. De volgordekeuze staat
in de tijdelijk vastgehouden `PendingPinnedCaptionExport` en
wordt na de Android-documentkiezer vergeleken met de actuele
keuze en **gehele exportbytes**. Een gewijzigde kaartvolgorde
wordt dus niet ongemerkt als eerder gekozen bestand opgeslagen.

De optie werkt met één ronde of het volledige drie-ronden-boek,
met bestaande moeilijkheden, zelfstudie en echt antwoordvrije
leerlingenbladen. Er zijn geen extra permissies, scripts, cloudcalls
of leerscore-opslag nodig.

---

## Nieuw: echte schrijflijnen en optioneel antwoordblad bij afdrukken

Een HTML-aanvuloefening levert op papier nu daadwerkelijk **twee of vier
zichtbare schrijflijnen per vraag** op, ook wanneer de printer geen
achtergrondafbeeldingen of -kleuren afdrukt. Dat was voorheen slechts
een hoge lege ruimte met één onderste lijn.

**Zelfstudie-HTML wordt zonder antwoordsleutel afgedrukt totdat je
zelf expliciet het vakje `Print ook het antwoordblad (bewuste keuze)`
aanvinkt.** Deze schakelaar staat in het opgeslagen offline
HTML-bestand onder de vragen. Zonder vinkje verschijnt het
antwoordblad **niet in de afdruk of afdrukvoorbeeld** — ook niet als
je het antwoordblad eerder via de browserlink hebt geopend. Met
vinkje krijgt het volledige antwoordblad een eigen afdrukpagina
met dezelfde ronde- en kaartnummers. Het blijft op het scherm
alleen zichtbaar na bewuste navigatie.

Let op: zelfstudie-HTML **bevat** de complete antwoorden ook wanneer
ze niet worden afgedrukt. Gebruik voor het delen van een bestand
met leerlingen daarom altijd de aparte optie **Leerlingenblad · geen
antwoorden in bestand**, niet de zelfstudieversie. Ook andere
browsers kunnen eerder ingevulde velden soms herstellen. Geen
JavaScript, netwerkverzoeken, beoordelingsdatabase of nieuwe
Android-permissie. De Android-exportdialoog meldt nu expliciet
dat afdrukken van het antwoordblad standaard uit staat.

---

## Nieuw: drie aanvulrondes over dezelfde zelfgekozen tweetalige zinnen

Bij **★ gemarkeerd → Exporteer selectie… → Aanvuloefening · HTML**
kun je voortaan kiezen tussen **Ronde 1**, **Ronde 2**, **Ronde 3** en
**Alle drie rondes · volledig oefenboek**. De ronde verandert bewust
**welke afzonderlijke woorden ontbreken**, niet de bronzinnen,
eigen correcties, moeilijkheid of gedeelde inhoud. Eén zelfstandig
offline HTML-bestand kan alle drie de rondes bevatten; iedere ronde
begint dan op een nieuwe afdrukpagina. Het volledige antwoordblad
voor een zelfstudieboek blijft apart en is in de browser pas zichtbaar
na een bewuste actie.

De algoritmen zijn **vast en herhaalbaar**, niet willekeurig.
Bij *Licht* ontbreekt ongeveer één van drie met per ronde wisselende
posities; bij *Uitdaging* ongeveer twee van drie; bij *Normaal*
ongeveer de helft, met een aanvullende derde variant. Erg korte
zinnen en schriftvormen zonder aangeleverde spaties kunnen niet
altijd drie verschillende patronen opleveren: Tuddy verzint geen
woordgrenzen. Bij het tonen van de eerste letter wordt een antwoord
van maar één teken **niet volledig onthuld**. Dezelfde keuze geldt
voor **zelfstudie** en het **leerlingenbestand zonder complete antwoorden**.

Het Android-exportvenster toont voor ieder zichtbaar bronpaar
de **echt gegenereerde hints per gekozen ronde** en het juiste
totale aantal kaarten. De bestandsbytes bevatten de gekozen
rondetitel, alle kaarten met zichtbare rondenummers en de bijbehorende
antwoorden uitsluitend wanneer **Zelfstudie** is gekozen. De
rondekeuze is opgenomen in `PendingPinnedCaptionExport` en in
de byte-herberekening wanneer de Android-documentkiezer terugkeert.
Verandert de ronde tussentijds, dan weigert Tuddy de oude export.
Geen scripts, opslag van oefenbeoordelingen, automatische taalcontrole,
netwerk, nieuwe machtigingen of gefabriceerde vertalingen.

---

## Nieuw: veilige leerlingenversie van de tweetalige invuloefening

Bij **★ gemarkeerd → Exporteer selectie… → Aanvuloefening · HTML** kies je
nu expliciet voor **Zelfstudie · antwoorden na bewuste keuze** of
**Leerlingenblad · geen antwoorden in bestand**. In de zelfstudieversie
moet je ieder individueel antwoord openklappen; ook het volledige
antwoordblad blijft in de browser verborgen totdat je via
**Open volledige antwoordblad** er bewust naar gaat. Op papier blijft
het volledige antwoordblad op een aparte pagina aanwezig.

De **leerlingenversie** neemt géén afzonderlijk volledig antwoord,
openklapantwoord of verborgen HTML-antwoordblad op in het geëxporteerde
bestand. Ze bevat uitsluitend de gekozen oorspronkelijke vraagtekst,
de bewust ingestelde onvolledige invulhints, tijdelijke invoervelden
en de zelfbeoordelingsopties. De werkelijke inhoud van een vraag kan
vanzelfsprekend tekst bevatten die een docent ook als antwoord zou
beschouwen; controleer daarom je eigen bronzinnen en de gekozen
hintmodus voordat je deelt. Tuddy verzint of beoordeelt de vertaling
niet. De Android-preview meldt nadrukkelijk dat de achterzijde in
de leerlingenversie ontbreekt. Het voorgestelde bestandslabel is
**Tuddy-leerlingenblad-zonder-antwoorden.html**, duidelijk verschillend
van de zelfstudieversie.

De keuze van ontvanger is onderdeel van de tijdelijke exportsnapshot
en de opnieuw berekende bytes bij terugkeer uit de Android-documentkiezer.
Als de bestemming, bronselectie, hints, moeilijkheid, correcties of
schrijfruimte intussen wijzigen, wordt de oorspronkelijke opslag
geweigerd. Geen extra permissies, JavaScript, netwerkverzoeken,
automatisch gegenereerde antwoorden of centrale leerscores. Een
door de gebruiker gekozen Android-bestandsprovider kan het bestand
wel extern synchroniseren.

---

## Nieuw: zelf beoordelen binnen het zelfstandige offline oefenblad

Iedere kaart in het geëxporteerde **Aanvuloefening · HTML** heeft nu naast
het eigen antwoordveld de tijdelijke beoordeling **Nog oefenen** of
**Ik wist het**. Het resultaat wordt niet automatisch berekend, opgeslagen
als leerscore of teruggeschreven naar Tuddy: je kiest zelf op basis van het
bewust opengeklapte antwoord. **Wis mijn antwoord en beoordeling** reset
alleen het antwoord en de keuze van die ene kaart via standaard HTML.
De rest van het oefenblad en de originele gesprekstekst blijven ongewijzigd.

Bij afdrukken verschijnen in plaats van de tijdelijke radio-knoppen twee
**lege** keuzevakjes, met schrijfruimte volgens de in Android gekozen
instelling. Geen browser-JavaScript, netwerk, automatische nakijkfunctie,
centrale voortgang, scripts of nieuwe permissies. Browsers kunnen zelfstandig
formulierwaarden herstellen; de bestandsprovider kan een door jou gekozen
export extern bewaren.

---

## Nieuw: oefenblad hergebruiken en ruime schrijfruimte kiezen

Je kunt in een zelfstandig geëxporteerd **Aanvuloefening · HTML** nu voor
iedere kaart **Wis mijn antwoord op deze kaart** kiezen. De knop reset alleen
het ingevulde tekstveld van die kaart via gewone HTML-formuliersemantiek,
zonder JavaScript, verzenden of netwerktoegang. Het oorspronkelijke antwoord
blijft apart uitklapbaar; je wijzigt nooit de bronzin of de eigen
studiecorrectie. Browsers kunnen zelfstandig reeds getypte tekst herstellen.

Kies in de Android-exportdialoog ook **Hoeveel schrijfruimte wil je op papier?**
met **Normaal · twee schrijflijnen** of **Ruim · vier schrijflijnen**. Deze
keuze bepaalt de hoogte van de invulvelden in de browser en de lege ruimte
bij het afdrukken. Bij printen verdwijnen de tekstvakken en de wisknoppen;
het antwoordblad staat nog steeds op een afzonderlijke pagina. De gekozen
ruimte wordt op het HTML-document vermeld en hoort bij de exacte
byte-/toestemmingscontrole na de Android-bestandskiezer: een gewijzigde
keuze maakt een lopende export ongeldig. Er is geen automatische
taalcorrectie, leerscore, gedeeld account of app-opslag toegevoegd.

---

## Nieuw: direct invullen en zelfgekozen invulhints zonder internet

De geëxporteerde **Aanvuloefening · HTML** is nu ook op de telefoon of laptop
als invulwerkblad te gebruiken: typ je eigen antwoord in het tekstvak onder
elke hint en open alleen wanneer je wilt het volledige antwoord. Er is geen
automatische nakijkfunctie, JavaScript, formulierverzending, netwerkbron of
nieuwe app-toestemming. Bij **afdrukken** verdwijnen tekstvakken en verschijnen
schrijflijnen; het antwoordblad blijft op een aparte pagina. Tuddy bewaart
getypte browserantwoorden niet, al kan de browser zelf formuliertekst herstellen.
Een extern gekozen bestandsprovider kan het HTML-bestand wel synchroniseren.

Naast **Licht / Normaal / Uitdaging** kies je in Android voortaan het soort hint:
**Alleen invulstrepen**, **Eerste teken van verborgen woorden** of
**Woordlengte als invulstrepen**. Dit zijn uitsluitend hints voor door
witruimte gescheiden woorden; voor ongesegmenteerde scripts blijft de
hele antwoordzijde verborgen. Lengtehints tonen maximaal 16 strepen per
woord, geen automatische taalanalyse. Je ziet de daadwerkelijke hint voor
het opslaan, in het HTML-bestand staat de gekozen optie vermeld, en
verandering van de optie tijdens de bestandskiezer blokkeert oude opslag.
Originele zinnen, eigen correcties, de exacte antwoordteksten en
selectie-/privacygrenzen blijven ongewijzigd.

---

## Nieuw: lokale tweetalige aanvuloefening met volledig antwoordblad

Kies nu voor **Licht**, **Normaal** of **Uitdaging** om respectievelijk
ongeveer één derde, de helft of twee derde van de antwoordwoorden
zelf in te vullen. De keuze is in de native Android-preview te bekijken,
verschijnt op het oefenblad en wordt opnieuw gecontroleerd bij de
Android-bestandskiezer. Er worden nooit woorden uit het echte gesprek
verwijderd; de antwoorden blijven apart beschikbaar.

Maak bij **★ gemarkeerd → Exporteer selectie…** een **Aanvuloefening · HTML**
uit je eigen selectie van afgeronde tweetalige zinnen. Iedere kaart toont
de bronzin en een antwoord waarvan om de andere spatiegescheiden term
tijdelijk is vervangen door een invulstreep. Voor schriftsoorten zonder
spaties wordt geen kunstmatige woordgrens verzonnen. Het volledige antwoord
is in de browser pas na een tik zichtbaar; bij printen verschijnt het
op een **afzonderlijke antwoordpagina**. Bekijk vóór opslaan in de app
de werkelijke aanvulhints, kaartenteller en exacte bestandspreviews.

De functie werkt in beide taalrichtingen, met eigen tekstcorrecties,
optionele taalcodes en een limiet op geselecteerde kaarten. Alles
blijft lokaal, scripts-/netwerkvrij en wordt pas na bewuste
Android-bestandskeuze opgeslagen. Geen spraakopnames, permanente
studieresultaten of claims over geverifieerde vertalingen.
[Handleiding](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw: tweetalige vouwkaartjes op één afdrukzijde

Uit bewust gemarkeerde voltooide vertaalparen kun je naast TSV/CSV
en het interactieve HTML-oefenblad nu **Vouwkaartjes · HTML** kiezen.
In het privé-bestand staan vraag en betekenis naast elkaar:
éénzijdig afdrukken, langs de buitenrand knippen en de rechter
betekenis achter de linker vraag vouwen. Beide taalrichtingen,
eigen correcties, taalcodekeuze, ontdubbelen en rechts-naar-links-tekst
werken via de bestaande selectie; geen scripts, accounts, audio,
cloud of automatische export.
[Handleiding](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

# Tuddy 🎙️

## Nieuw: afdrukbaar privé-oefenblad uit eigen tweetalige zinnen

Maak uit de bewust geselecteerde **★ gemarkeerde** gesprekszinnen nu
ook **Oefenblad · HTML**: een zelfstandig bestand met per vraag een **Toon antwoord**-knop
via native HTML-details, geheel zonder scripts of netwerk. Op papier
blijven de antwoorden verborgen bij de vragen en staan ze op een
afzonderlijke afdrukpagina. Dezelfde keuze voor bron→vertaling, omgekeerde kaarten
of beide richtingen, optionele taalcodes, alleen persoonlijk
gecorrigeerde antwoorden en ontdubbeling als bij TSV/CSV blijft intact.
Alles gebeurt in RAM en pas na expliciete Android-bestandskeuze.
Geen ongevraagde opslag, netwerk, nieuwe machtigingen of
geverifieerde vertaalclaims.
[Handleiding](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw: leerkaartbestand uitsluitend uit handmatig gecorrigeerde antwoorden

Selecteer bij de TSV- of CSV-export **Alleen mijn handmatig gecorrigeerde
antwoorden** om alle overige modelvertalingen buiten het bestand te
houden. De selectie, kaartenaantallen en ruwe preview volgen het filter;
zonder eigen correcties is er geen export. Eigen antwoorden zijn niet
extern gecontroleerd.
[Gebruik](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw: corrigeer een modelvertaling alleen voor je eigen studie

Bij **★ gemarkeerd** kun je op **✎ Corrigeer alleen studieantwoord**
tikken, de modelvertaling vergelijken met je zelf geschreven betekenis
en maximaal 400 Unicode-tekens invoeren. Alleen tijdelijke oefenkaarten
en de expliciete TSV-/CSV-leerkaartenexport gebruiken deze eigen correctie;
het echte gesprek en TXT/JSON/HTML/SRT/WebVTT blijven onveranderd.
Het antwoord is ongetoetst en wordt bij gewijzigde brontekst of
modelvertaling niet blind overgenomen. Je kunt een correctie wissen
en terugkeren naar het oorspronkelijke modelantwoord.
[Handleiding](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw: bepaal welke gemarkeerde regels je oefent of bewaart

Onder **★ gemarkeerd** kun je zoeken, op voltooiing én exact
bron-/doeltaalpaar filteren, nieuwste eerst tonen en per zin
de oefen-/exportselectie aanvinken. Kies uitsluitend de zichtbare zoekresultaten, voeg zichtbare regels toe of
deselecteer ze zonder andere sterren te verliezen. Verwijder desgewenst
een losse ster en herstel de laatste verwijderactie zolang het scherm open is. Oefenen en bestandskeuze
werken alleen met de expliciet gekozen subset. Er wordt niets
automatisch geüpload of opgeslagen.
[Bediening](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw: oefen gemarkeerde tweetalige zinnen zonder export

Open **★ gemarkeerd** en kies **Oefen ... gekozen vertaalparen**.
De bestaande lokale oefenkaartjes gebruiken alleen je geselecteerde,
daadwerkelijk afgeronde lokale modelvertalingen, die niet onafhankelijk
op inhoud gecontroleerd zijn. Je kunt nu ook **uitsluitend je handmatig
gecorrigeerde studieantwoorden oefenen** zonder overige modelantwoorden.
Draai de vraag/antwoordrichting
om, onthul pas na het nadenken, beoordeel jezelf en oefen moeilijke
kaarten nogmaals. Alles verdwijnt bij sluiten; er wordt niets
toegevoegd aan je vaste studieprofiel.
[Privacy en bediening](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw: exporteer gemarkeerde gesprekszinnen in zeven formaten

Markeer maximaal 80 regels in je tijdelijke live gesprek en bekijk ze
onder **★ gemarkeerd**. Via **Exporteer N gekozen…** kies je bewust
voor TXT, een opnieuw importeerbaar privé-JSON-archief, afdrukbaar HTML,
een ruw SRT/WebVTT-ondertitelconcept met instelbare tijden of UTF-8
TSV-/CSV-leerkaarten met uitsluitend voltooide, ongeverifieerde lokale
modelvertalingen. Bij TSV of CSV kies je bron naar vertaling, omgekeerd of
beide richtingen; gelijke paren worden ontdubbeld. Voor gemengde
taalrichtingen kun je optioneel `[taalcode]` bij elke kaartzijde
zetten; de exacte labels zijn vóór opslag zichtbaar.
Kies expliciet één taalrichting of alle richtingen, en tussen alle
gemarkeerde regels, alleen afgeronde regels en werkelijk vertaalde
regels; bekijk vóór het opslaan aantallen, bestandsgrootte en de
beoogde inhoud. Bekijk desgewenst de
exacte ruwe bestandsinhoud in een lokaal, scrollbaar leesvenster.
SRT/WebVTT telt alleen zinnen die werkelijk in dat ondertitelconcept
terechtkomen.
Alle niet-gemarkeerde én niet-geselecteerde regels blijven buiten het bestand. De app
controleert bij terugkeer uit de Android-bestandskiezer of markeringen
en zinnen nog identiek zijn en schrijft anders niet. Er is geen
automatische synchronisatie met audio, geen nieuwe spraak- of
vertaalclaim en geen stilzwijgende opslag.
[Bediening en privacy](docs/PINNED_CAPTION_MULTI_EXPORT.md).

---

## Nieuw: bewaar zelf gekozen ondertitelzinnen als privé-oefenzin

In de eigen SRT/VTT-audiospeler kun je een actieve Kurmanji-, Sorani-
of Pasjtoe-zin als bewerkbaar concept openen, er zelf een betekenis
in Nederlands of Engels bij schrijven en deze pas na een tweede
expliciete toestemming bewaren in **Mijn eigen zinnen**. De app
genereert geen vertaling en overschrijft geen bestaande zin.
[Uitleg](docs/LOCAL_SUBTITLE_AUDIO_PREVIEW.md).

---


## Nieuw: tik op ondertitelwoorden tijdens je eigen audio

In de SRT/VTT-audiospeler kun je nu een teksttaal kiezen en meteen op
een woord in de actieve ondertitel tikken. De bestaande lokale woordkaart
toont alleen beperkte gecontroleerde basisbetekenissen en laat je een
**zelf bedachte betekenis** na expliciete toestemming toevoegen aan
**Mijn eigen woorden**. Dat is een blijvende app-private woordnotitie,
terwijl het audio/SRT-bestand en luisterfragmenten tijdelijk blijven.
Geen cloud-vertaling of automatische taalherkenning.
[Bediening en privacy](docs/LOCAL_SUBTITLE_AUDIO_PREVIEW.md).

---


## Nieuw: mijn privé luisteroverzicht na een oefenroute

Open in de eigen-audiospeler de audiomarkeringen en kies **Mijn privé
oefenoverzicht bekijken**. Je ziet actuele fragmenten, begin/eindtijden,
eventuele brontekst en je persoonlijke beoordeling. Een begeleide route
telt alleen als afgerond wanneer alle herhalingen echt voorbij zijn.
De tekst kan desgewenst met Android-tekstselectie door jou worden
gekopieerd; Tuddy slaat niets automatisch op of deelt niets.
[Bediening en privacy](docs/LOCAL_AUDIO_TIME_BOOKMARKS.md).

---


## Nieuw: maak A–B-oefenfragmenten direct vanuit zoekresultaten

In de offline SRT/VTT-audiospeler kun je bij **⌕ Zoek passage** nu
**A-start** en **B-einde** kiezen bij twee zelf gevonden ondertitelregels.
Bekijk bovenaan de exacte tijdcode en brontekst, bevestig vervolgens
**Bewaar A–B-passage** om je bestaande tijdelijke oefenroute aan te vullen,
zonder handmatig in de audio te schuiven of de bron te wijzigen.
Begrensd, geen auto-afspelen en geen netwerk-ASR.
[Handleiding](docs/LOCAL_AUDIO_TIME_BOOKMARKS.md).

---


## Nieuw: hele ondertitelpassage als begeleid oefenfragment

Een langere zin verdeeld over verschillende SRT/VTT-regels kun je nu
in de bestaande eigen-audiospeler met **A-start** en **B-einde**
afgrenzen en direct met **Bewaar deze complete A–B-passage** toevoegen
aan de begeleide luisterroute. Tuddy gebruikt uitsluitend je bestaande
zelf opgegeven tijdcodes (0,75 s–5 min, maximaal vier kaartjes en acht
markeringen), toont de bronpassage en laat jou de volgorde en 1×/3×/5×
herhalingen kiezen. Dit start geen audio, wijzigt geen documenten en
gebruikt geen netwerk-ASR. [Handleiding](docs/LOCAL_AUDIO_TIME_BOOKMARKS.md).

---


## Nieuw: ondertitelregel meteen aan je offline oefenroute toevoegen

In **▶ Eigen audio met tijdcodes afspelen…** kun je nu een actieve
ondertitel of een gevonden zoekresultaat met één tik als oefenfragment
bewaren. Tuddy neemt alleen de bestaande SRT/VTT-begin- en eindtijd,
met je huidige handmatige tijdcorrectie; het bewaart de selectie
in de bestaande route van maximaal vier fragmenten en acht audio-A/B-markeringen.
Bekijk de tekst bij je oefenfragment, kies zelf de volgorde en oefen
desgewenst 1×, 3× of 5× per fragment. Dubbelen, te korte/lange
of buiten-audio vallende regels worden niet toegevoegd.
Er wordt niets opgenomen, herkend, opgeslagen, geüpload of automatisch
afgespeeld. [Bediening en grenzen](docs/LOCAL_AUDIO_TIME_BOOKMARKS.md).

---


## Nieuw: acht vrije audiomarkeringen en A–B-herhaling zonder ondertitelgrenzen

In **▶ Eigen audio met tijdcodes afspelen…** kun je nu
**▾ Markeer audiomomenten en herhaal vrij A–B** openen. Tik op
**Markeer huidige audiopositie** om maximaal acht momenten in je
zelfgekozen audio te onthouden, spring met één tik terug naar een eerder
moment of verwijder die markering individueel.

Kies bij twee markeringen **Kies A** en **Kies B**. Als B minstens
0,75 seconde en maximaal 5 minuten ná A ligt, kun je het exacte
audiofragment in een lus beluisteren, óók wanneer jouw SRT/VTT geen
ondertitel op die precieze tijdstippen heeft. Het bestaande herhalen
van een enkele ondertitelregel of een A–B-passage van meerdere
ondertitelregels blijft apart beschikbaar; slechts één van de drie
herhaalmodi speelt tegelijk. Een zoek-/tijdsprong buiten de actieve lus
stopt de lus.

Dit is **handmatige offline audiobediening** en geen automatische
spraakherkenning, woorduitlijning of vertaling. De markeringen worden
niet in een SRT/VTT geschreven, niet gedeeld of opgeslagen: andere
audio kiezen of dit scherm sluiten wist alles. De door jou gekozen
Android-bestandsprovider kan zelf cloudtoegang gebruiken. Zie
[handleiding](docs/LOCAL_AUDIO_TIME_BOOKMARKS.md).

---

## Nieuw: een A–B-passage met meerdere ondertitelregels herhalen

Voor langere zinnen of gesprekjes kun je nu binnen **▶ Eigen audio met
tijdcodes afspelen…** de geavanceerde **A–B-passageoefening** openen.
Spring eerst naar de gewenste beginregel en kies **A-start**. Spring
vervolgens naar dezelfde of een latere regel en kies **B-einde**. Kies
**↻ Herhaal mijn A–B-passage**: de speler springt naar het begin van A en
herhaalt tot na de eindtijd van B. Je kunt het herhalen op elk moment
uitzetten. Gewoon één regel herhalen blijft daarnaast beschikbaar.

De A–B-selectie is alleen bruikbaar als alle geselecteerde regels en
het hele fragment (minimaal 0,75 seconde, maximaal 5 minuten) binnen de
gekozen audio vallen. De huidige tijdelijke handmatige
tijdsverschuiving geldt ook hier, zonder originele audio of SRT/VTT te
wijzigen. Alle A/B-selecties verdwijnen na audio wisselen of sluiten; bij
een gewijzigde offset stopt de oude herhaling zodat geen onverwacht
fragment wordt herhaald. Dit is persoonlijke luisteroefening zonder
automatische audioanalyse, spraakherkenning of externe opslag.

Heb je precies de juiste passage gevonden, dan kun je binnen hetzelfde
A–B-paneel kiezen voor **Maak A–B als afzonderlijk ondertitelconcept…**.
Tuddy toont eerst hoeveel regels worden meegenomen, de geselecteerde
A- en B-regels en het nieuwe einde wanneer de ondertiteltijdlijn vanaf
**00:00** begint. Pas na je eigen bevestiging gaan uitsluitend de
geselecteerde, eventueel handmatig tijdgecorrigeerde ondertitelregels
naar de tijdelijke editor. Je kunt daar bewust een **nieuw** SRT/VTT-
bestand kiezen via Android **Opslaan als**. Er wordt geen audioclip gemaakt,
geen origineel overschreven en geen derde partij benaderd.

## Nieuw: handmatig ondertitels gelijkzetten met je eigen audio

Als de gesproken tekst niet gelijkloopt met de SRT/VTT-tijdcodes, open
**▶ Eigen audio met tijdcodes afspelen…** en gebruik de tijdcorrectie in
stappen van **0,25, 1 of 5 seconden** (tot maximaal **±30 seconden**).
Een *positieve* correctie toont ondertitels later dan in het bronbestand.
De actieve ondertitel, zoeken en springen naar een regel en één regel
herhalen volgen dezelfde tijdelijke correctie.

Wil je vanaf één herkenbaar beginpunt precies gelijkzetten? Pauzeer
bij het begin van een zin, open **⌕ Zoek passage**, zoek de betreffende
ondertitel en kies **Gebruik als tijdanker op huidige audiopositie**.
Deze actie wordt geweigerd als de benodigde verschuiving groter dan
30 seconden is. Met **Herstel de oorspronkelijke tijdcodes (0 s)**
keer je direct terug naar het originele timingoverzicht.

Dit is standaard een luisterhulpmiddel in het huidige scherm:
Tuddy bewerkt geen originele SRT/VTT-cues, bewaart geen offset op schijf
en doet geen automatische spraak-/woorduitlijning. Sluiten of andere
audio kiezen wist de correctie. De gekozen Android-bestandsprovider
kan zelf mediabestanden downloaden of synchroniseren.

Wil je de handmatig gecorrigeerde tijdcodes tóch verder bewerken of
bewaren? Kies **Neem deze correctie over in mijn tijdelijke
ondertiteleditor…**. Tuddy controleert dan of **alle** regels binnen de
toegelaten tijdsgrenzen blijven; een ongeldige verschuiving wordt
geweigerd in plaats van regels af te kappen. Je ziet de hoeveelheid
regels en oude/nieuwe begin- en eindtijden en moet de omzetting
**afzonderlijk bevestigen**. De app keert pas daarna terug naar de
bestaande tijdelijke editor, waar je zelf desgewenst een **nieuwe**
SRT/VTT-kopie kiest via Android Opslaan als. Het brondocument wordt
nooit automatisch gewijzigd.

## Nieuw: eigen audio beluisteren met SRT/VTT-tijdcodes

Open een SRT/VTT in de tijdelijke ondertiteleditor en kies **▶ Eigen audio
met tijdcodes afspelen…**. Selecteer zelf een audio-opname, speel af of pauzeer,
schuif naar een audiotijd of spring naar de vorige/volgende cue. Voor 
uitspraak en luisteroefeningen kun je tussen 0,75×, 1× en 1,25× 
schakelen en de tijdcode van één ondertitelregel herhaald afspelen. Met **⌕ Zoek passage**
zoek je lokaal in je ondertiteltekst en spring je naar de tijdcode van de
gevonden regel. De grote
ondertitelkaart volgt uitsluitend de tijden uit jouw document en laat gaten
zonder actieve ondertitel eerlijk zien. Niets wordt automatisch afgespeeld,
opgeslagen of met een server gesynchroniseerd door Tuddy. Een gekozen
Android-bestandsprovider kan zelf cloudtoegang gebruiken; echte offline
werking vergt een lokaal aanwezig audiobestand. Dit is geen woord-timing of
automatische synchronisatie. [Handleiding](docs/LOCAL_SUBTITLE_AUDIO_PREVIEW.md).


## Nieuw: twee handmatige tijdankers voor een complete ondertiteltijdlijn

In de SRT/VTT-editor kun je nu **⌁ Tijdlijn uitlijnen met twee ankers…**
gebruiken als vaste verschuivingen niet voldoende zijn. Geef zelf de
gewenste begintijd van de eerste en laatste regel op; Tuddy schaalt alle
tussenliggende tijden en duren proportioneel, toont het resultaat vooraf
en legt de wijziging vast als één herstelbare tijdelijke bewerking.
Er is geen automatische audio-/videoanalyse of ongewenste bestandsopslag.
[Handleiding](docs/OFFLINE_TWO_ANCHOR_SUBTITLE_TIMELINE.md).

## Nieuw: ondertiteltijdlijn bekijken en rechtstreeks corrigeren

In het bestaande offline SRT/VTT-document zie je met
**⌁ Bekijk de ondertiteltijdlijn…** direct waar regels overlappen,
dezelfde tijdcode hebben, kort na elkaar staan of een lange tussenruimte
hebben. Filter per categorie en tik een signaal om de juiste regel
te openen, waarna je zelf de exacte tijdcodes kunt bijstellen.
Dit overzicht wijzigt niets, start geen audioanalyse en bewaart niets.
[Bediening en grenzen](docs/OFFLINE_SUBTITLE_TIMELINE_INSPECTOR.md).

## Nieuw: ondertiteltijden exact aanpassen per regel

In de bestaande offline SRT/VTT-editor kun je voor één ondertitel
het begin en einde nu exact invoeren, inclusief milliseconden.
Kies bewust om alleen die regel te wijzigen of alle volgende tijden
mee te verschuiven op basis van het gewijzigde begin of einde.
Bekijk vooraf de impact en overlap, gebruik Terug/Opnieuw en bewaar
uitsluitend op verzoek een nieuwe kopie. Er is geen automatische
audio-/videosynchronisatie of nieuwe permissie.
[Bediening en privacy](docs/EXACT_OFFLINE_SUBTITLE_TIMING.md).

## Nieuw: bestaande SRT/VTT-tijdcodes offline corrigeren

In de ondertiteleditor kun je nu **alle tijdcodes samen schalen en verschuiven**
met een direct voorbeeld van eerste/laatste tijdcode en eventuele overlap.
Kies het anker, pas de verandering alleen op je tijdelijke concept toe,
gebruik Terug/Opnieuw en bewaar desgewenst afzonderlijk een nieuwe kopie.
De tekst verandert niet en Tuddy meet geen audio/video-synchronisatie.
[Bediening en privacy](docs/OFFLINE_SUBTITLE_RETIMING.md).

## Nieuw: standaard WebVTT-bestanden met taalheader openen

Tuddy opent nu ook gangbare WebVTT-bestanden met `Kind:`- en
`Language:`-headerregels vóór de cues. Onbekende instructies zoals
externe tijdmapping worden niet stilzwijgend overgenomen.
[Ondersteuning en grenzen](docs/WEBVTT_IMPORT_COMPATIBILITY.md).

## Nieuw: alleen gevonden ondertitels als bestand bewaren

Zoek in een zelf geopend SRT/VTT-bestand en kies vervolgens
**Bewaar alleen gevonden regels…**. Kies tussen de originele
tijdcodes en een vanaf 00:00 aaneengesloten selectie. Vooraf zie
je het aantal regels en de uitvoergrootte; opslag vereist nog steeds
een afzonderlijke Android-bestandskeuze. Het oorspronkelijke document
blijft intact.
[Werking en grenzen](docs/FILTERED_OFFLINE_SUBTITLE_EXCERPT.md).

## Nieuw: afdrukbaar oefenblad van eigen ondertitels

Je kunt geselecteerde regels uit een bewust geopend SRT/VTT-bestand
ook als veilig, volledig offline HTML-oefenblad bewaren. Eerst de
tijdcode, starthint en schrijfruimte; daarna een apart antwoordblad.
Alleen een afzonderlijke Android-bestandskeuze schrijft een nieuwe
kopie. Tijdcodes en tekst zijn niet automatisch geverifieerd.
[Uitleg](docs/OFFLINE_SUBTITLE_PRINTABLE_WORKSHEET.md).

## Nieuw: eigen SRT/VTT-ondertitels oefenen

Gebruik een bewust geopend ondertitelbestand nu ook als tijdelijke
oefenkaarten: filter regels, onthul de volledige tekst op verzoek en
oefen moeilijke zinnen opnieuw. Er wordt geen leerprofiel of
automatische vertaling van gemaakt en alle antwoorden blijven in RAM.
[Handleiding](docs/OFFLINE_SUBTITLE_RECALL.md).

## Nieuw: meerdere ondertitelbestanden offline combineren

Open een eigen SRT/VTT-bestand en kies **Tweede SRT/VTT-document
toevoegen…** om een tweede bestand via Android te selecteren.
Je bepaalt zelf of de nieuwe regels achteraan aansluiten of hun
bestaande tijdstempels behouden. Bekijk vooraf de overlap, voeg
alleen toe aan het tijdelijke bewerkbare concept en bewaar uitsluitend
na een afzonderlijke keuze een nieuwe kopie. Tijdcodes zijn niet
automatisch met audio gesynchroniseerd.
[Bediening en privacy](docs/OFFLINE_MULTIFILE_SUBTITLE_COMPOSITION.md).

## Nieuw: acht weken studieplanning bewust als agenda bewaren

In **Mijn offline studieplanning** kun je nu na een aparte keuze een
`.ics`-bestand opslaan met uitsluitend **datums en aantallen** van je
bestaande herhaalplannen. Kies zelf welke ingeschakelde kaartgroepen
meetellen; geen woorden, eigen betekenissen of gesprekken verlaten de app.
Een externe provider of agenda-app kan het bestand wel synchroniseren;
de export wordt niet automatisch bijgewerkt.
[Uitleg en privacy](docs/PRIVATE_STUDY_CALENDAR_ICS_EXPORT.md).

## Nieuw: mijn privéthema-indeling bewust overzetten

De thema-manager kan nu naast je bestaande eigen zinnen een
**afzonderlijke thema-back-up** bewaren of herstellen.
Bewaar op het nieuwe toestel eerst je zelfgeschreven zinnen,
daarna de thema-indeling. Een import wordt streng gecontroleerd
en vervangt pas na een afzonderlijke bevestiging de thema's,
nooit je eigen teksten. Het JSON-bestand bevat themanamen en
herleidbare SHA-256-identiteiten: dat is **geen encryptie**.
[Uitleg en privacy](docs/PRIVATE_PHRASE_TOPIC_BACKUP.md).

## Nieuw: mijn offline herhalingen acht weken vooruitzien

In **Vandaag offline oefenen** toont **Bekijk mijn geplande
herhalingen · 8 weken** nu per dag hoeveel basiswoorden,
zelfgeschreven zinnen en eigen woordbetekenissen je vrijwillig
hebt ingepland. Achterstallige kaarten tellen alleen bij vandaag;
de kalender verandert niets, vraagt geen extra toestemming en
stuurt geen meldingen.
[Werking en privacy](docs/PRIVATE_EIGHT_WEEK_STUDY_CALENDAR.md).


## Nieuw: favorieten en thema’s bij voorbereide teksten

Bij **Gebruik mijn voorbereide eigen tekst…** kun je nu ook
filteren op je eerder bewaarde gespreksfavorieten en zelfgekozen
privéthema's. Daardoor vind je snel de eigen NL/EN-betekenis
die je als **onverstuurd** concept wilt overnemen.
De indeling blijft ongewijzigd en er vindt geen automatische
Kurmanji/Sorani/Pasjtoe-vertaling plaats.
[Bediening en privacy](docs/PREPARED_PRIVATE_TWO_WAY_DRAFTS.md).


## Nieuw: alle geplande kaarten in één gemengde offline ronde

In **Vandaag offline oefenen** kun je nu basiswoorden, eigen zinnen
en zelfgeschreven woordbetekenissen door elkaar oefenen zonder tussen
drie schermen te wisselen. De antwoorden verschijnen pas na een tik.
Je beoordeelt elke kaart afzonderlijk; die keuze wordt uitsluitend
in de al bestaande privéplanning bewaard. Overslaan laat de kaart
onbeoordeeld. Er ontstaat geen gezamenlijk leerprofiel of
automatische vertaalgarantie.
[Werking en privacy](docs/MIXED_OFFLINE_DAILY_REVIEW.md).


## Nieuw: een eigen voorbereide zin gebruiken in een tekstgesprek

In het offline **tweeweg-tekstgesprek** kun je nu bewust een
eerder zelfgeschreven **Nederlandse of Engelse betekenis** van
een privézin als **onverstuurd concept** overnemen. Een bestaande
concepttekst wordt nooit zonder bevestiging vervangen. De
Kurmanji-/Sorani-/Pasjtoe-bronzin wordt niet als ondersteund
automatisch tekstmodel gepresenteerd. Alleen na jouw aparte
verzendtik probeert Tuddy een ondersteunde lokale tekstvertaling.
[Bediening en veiligheidsgrens](docs/PREPARED_PRIVATE_TWO_WAY_DRAFTS.md).


## Nieuw: zelf je kaarten op papier oefenen

Kies bij **Bewaar mijn kaarten van vandaag…** ook voor een afdrukbaar
oefenblad: eerst alleen de vragen met ruimte voor jouw antwoorden,
daarna de eigen betekenissen op een aparte sleutelpagina.
Het bestand werkt geheel offline en gebruikt alleen de bewust gekozen
kaarten die vandaag aan de beurt zijn. De betekenissen bij
zelfgeschreven woorden of zinnen blijven ongeverifieerd.
[Uitleg](docs/PRINTABLE_DAILY_PRACTICE_WORKSHEET.md).


## Nieuw: alleen de oefenkaarten van vandaag afdrukken

Vanuit **Vandaag offline oefenen** kun je bewust één of meer
leergroepen kiezen en daarvan een zelfstandig, offline HTML-studieblad
bewaren via de Android-bestandskiezer. Alleen de vandaag aan-de-beurt
zijnde kaarten worden meegenomen. Als het lokale materiaal tijdens
de bestandskeuze verandert, weigert Tuddy de verouderde export.
Zelfgeschreven betekenissen blijven ongeverifieerd en de externe
opslaglocatie is jouw verantwoordelijkheid.
[Meer informatie](docs/DUE_TODAY_OFFLINE_STUDY_BOOK.md).


## Nieuw: één studieoverzicht voor alle drie je eigen oefenstromen

**Vandaag offline oefenen** toont nu ook de vrijwillige herhaalplanning
voor je **eigen woordbetekenissen**, naast bewaarde basiswoorden en
zelfgeschreven Kurmanji-/Sorani-/Pasjtoe-zinnen. Bekijk de gezamenlijke
vandaagteller, open een van de drie kaartreeksen of beheer de
toestemming en opslag per onderdeel. Niets start automatisch en
eigen betekenissen zijn niet onafhankelijk gecontroleerd.
[Handleiding en beperkingen](docs/THREE_LANE_OFFLINE_STUDY_HUB.md).


## Nieuw: één privéthema als afdrukbaar zinnenboek

Na het kiezen van een zelfgemaakt thema kun je nu alleen die zinnen
naar een zelfstandig offline HTML-zinnenboek exporteren.
Kies bewust of je filter actief blijft en selecteer de taal.
De app weigert een verouderd momentbeeld na het openen van
de Android-bestandskiezer. Eigen teksten en betekenissen zijn
ongeverifieerd; een externe provider kan de export synchroniseren.
[Bediening en privacy](docs/PRINT_SELECTED_PRIVATE_PHRASE_TOPIC.md).


## Nieuw: mijn eigen woordbetekenissen later opnieuw oefenen

Zelfgeschreven woordbetekenissen kunnen nu, na **aparte toestemming**,
aan een privé herhaalplanning meedoen. Je ziet je antwoord pas
na een tik, kiest zelf of het moeilijk of bekend was en krijgt
woorden op latere dagen terug. Een gewijzigde betekenis begint opnieuw;
je kunt alle datums wissen zonder je notities kwijt te raken.
Geen account, cloud, microfoon of kwaliteitsclaim over eigen vertalingen.
[Uitleg en privacy](docs/PRIVATE_OWN_WORD_REVIEW.md).


## Nieuw: mijn eigen gesprekszinnen per thema ordenen

In **Mijn eigen offline zinnen** kun je nu zelf maximaal twaalf
privéthema's maken, hernoemen en verwijderen. Deel elke zelfgeschreven
Kurmanji-, Sorani- of Pasjtoe-zin bewust in, filter erop en oefen
alleen het gekozen thema. Verwijdering van een thema laat de zinnen
en herhaaldatums intact. Deze lokale indeling staat los van export
en bevat geen automatisch vertaalde of gecontroleerde betekenissen.
[Handleiding en privacy](docs/PRIVATE_PHRASE_TOPICS.md).


## Nieuw: eigen ongecontroleerde woordbetekenissen oefenen

Bij **Mijn eigen offline woorden** kun je nu ook zelfgeschreven woorden
oefenen, zonder ze eerst aan een gecontroleerd woordenboek toe te voegen.
Kies een taal, onthul je eigen betekenis op verzoek, draai de vraagrichting
om en beoordeel jezelf. Er zijn maximaal twintig kaarten per ronde, met
bladeren naar volgende woorden. **Scores worden niet opgeslagen.** Dit
zijn geen geverifieerde vertalingen of taalvaardigheidsresultaten.
[Bediening en privacy](docs/PRIVATE_OWN_WORD_FLASHCARDS.md).


## Nieuw: alles wat je vandaag offline kunt oefenen

Op het hoofdscherm opent **🧠 Vandaag offline oefenen** één overzicht van
je vrijwillige herhaalschema's voor basiswoorden en zelfgeschreven
Kurmanji/Sorani/Pasjtoe-zinnen. Je ziet het aantal kaarten dat vandaag
klaarstaat, kunt beide onderdelen direct oefenen of beheren, en ziet
anders de eerstvolgende geplande datum. Toestemming blijft per onderdeel
afzonderlijk; Tuddy start geen microfoon, modeldownload, account of
automatische herinnering. [Zo gebruik je het overzicht](docs/UNIFIED_DAILY_STUDY_HUB.md).


## Nieuw: eigen Kurmanji-, Sorani- en Pasjtoe-zinnen herhalen

Naast vrij oefenen kun je je **zelfgeschreven** zinnen vrijwillig voor
herhaling plannen. Pas na aparte toestemming blijven per zin enkel een
lokale hash, herhaaldatum en niveau bewaard. De eigen betekenis blijft
verborgen tot je haar aantikt. Bekende zinnen komen later terug;
lastige morgen. Je kunt de planning afzonderlijk wissen.
**Eigen betekenissen zijn niet automatisch vertaald of gecontroleerd.**
[Bediening en privacy](docs/PRIVATE_PHRASE_REVIEW_SCHEDULE.md).


## Nieuw: vandaag offline woorden herhalen (vrijwillig)

Naast een gewone oefenronde kun je je opgeslagen basiswoorden nu apart
voor **privé herhaalplanning** aanmelden. Elk woord verschijnt bij eerste
gebruik, en na je eigen antwoord later opnieuw. **Nog oefenen** plant
morgen; **Ik wist het** verschuift naar 2/4/7/14/30 dagen.
Je ziet de betekenis pas na een tik. Herhaaldatums en niveaus blijven
uitsluitend op je toestel, worden niet automatisch geback-upt en kunnen
zonder je woorden te verliezen worden gewist. Standaard staat dit uit;
geen account, pushmelding, cloud of taalvaardigheidsclaim.
[Bediening en gegevens](docs/PRIVATE_SCHEDULED_WORD_REVIEW.md).


## Nieuw: eigen JSON-archief groot lezen

Open bewust een eerder opgeslagen privé-JSON-archief, zoek in
de regels en tik **▣ Lees gevonden archiefregels groot**.
De bestaande tweetalige leesmodus toont uitsluitend deze
zoekresultaten groter, bij voldoende schermbreedte naast elkaar.
Sluiten brengt je terug naar hetzelfde archief, zonder iets
in het live gesprek te importeren of automatisch op te slaan.
[Bediening en privacy](docs/PRIVATE_ARCHIVE_FULLSCREEN_READER.md).

## Nieuw: woord bekijken zolang je het indrukt

De v0.4-functie **vasthouden-en-loslaten** is terug als tijdelijke
offline preview: houd een woord in een ondertitel of getypt
gesprek vast om snel de beperkte ingebouwde betekenis en je
eigen, niet geverifieerde uitleg te zien. Laat los om te
sluiten. **Kort tikken** opent nog steeds de uitgebreide
woordkaart, inclusief bewerken en lokaal voorlezen.
[Bediening en privacy](docs/TRANSIENT_OFFLINE_WORD_PEEK.md).

## Nieuw: oefen een afgerond gesprek zonder iets op te slaan

Vanuit het live gesprek (na stoppen van de microfoon) en vanuit
de tweeweg-typmodus kun je nu **🧠 Oefen dit gesprek** openen.
Alleen werkelijk **lokaal voltooide** bron/vertaling-paren komen
in aanmerking. Oefen in groepjes van twaalf, wissel de
vraagrichting, onthul antwoorden pas na een tik en beoordeel
jezelf. Optioneel lokaal voorlezen, hoogstens één herkansing
per moeilijke kaart. De voortgang verdwijnt bij sluiten:
**geen nieuwe archivering of automatische vertaalgarantie**.
[Bediening en privacy](docs/EPHEMERAL_CAPTION_STUDY.md).

## Nieuw: bewaarde woorden lokaal horen en oefenen

In **Mijn opgeslagen woorden** kun je elk lokaal bewaard woord
met **🔊 Hoor woord**, **Rustig** en **Stop** uitspreken met een
bestaande geïnstalleerde Android-stem. Dezelfde bediening
zit in **Oefen opgeslagen woorden**. Bij woordwissel,
achtergrond of actieve live microfoon stopt de audio; tijdens
luisteren met de microfoon zijn de knoppen uitgeschakeld.
Geen stemdownload, cloudspraak of nieuwe opslag.
[Werking en beperkingen](docs/OFFLINE_SAVED_WORD_AUDIO.md).

## Nieuw: luister naar je eigen offline oefenkaart

Tijdens **Oefen mijn eigen zinnen** kun je de zichtbare vraag
op verzoek met een lokaal geïnstalleerde Android-stem laten lezen.
Het **antwoord** is pas na **Toon mijn betekenis** afspeelbaar,
normaal of rustig. Kaart-/richtingwissel, beoordelen en
achtergrond sluiten het vorige geluid. Ontbrekende lokale stemmen
betekenen stilte, geen cloudfallback; jouw zinnen zijn niet
automatisch gecontroleerd.
[Bediening en beperkingen](docs/OFFLINE_PRIVATE_PRACTICE_VOICE.md).

## Nieuw: afgeronde ondertitels en handmatig woorden voorlezen

In **▣ Toon deze zin groot** kun je na een tik het afgeronde
origineel of de daadwerkelijk lokaal voltooide vertaling laten
voorlezen. Bij **▸ Lees woord voor woord** kun je ook het
geselecteerde tekstwoord op normaal of rustig tempo laten horen.
Geen automatische audiotiming, extra modeldownload of
netwerkspraak: bij ontbrekende geïnstalleerde offline Android-stem
wordt niets ingesproken. Iedere nieuwe zin/woordselectie en
achtergrondgang stopt het vorige geluid.
[Lees hoe dit werkt](docs/OFFLINE_AUDIO_COMPLETED_CAPTIONS.md).

## Nieuw: offline gesprekskaarten op verzoek voorlezen

De grote eigen-zinnenkaart én de vijf ingebouwde voorbeeldwoorden
bieden nu **🔊 Zin/Bronwoord**, **🔊 Betekenis/Doelwoord**, **Rustig**
en **Stop**. De app gebruikt alleen een stem die Android zelf als
reeds geïnstalleerd en niet-netwerkafhankelijk meldt. Als een
Kurmanji-, Sorani- of Pasjtoe-stem ontbreekt, blijft de knop zonder
online uitwijkroute stil en verschijnt een melding. Wisselen van
kaart, achtergrond en sluiten stoppen het geluid. Eigen betekenissen
zijn **niet geverifieerd**.
[Handleiding en privacy](docs/PRONOUNCE_OFFLINE_PHRASE_CARDS.md).

## Nieuw: handmatig woord voor woord lezen

In een grote, afgeronde gesprekskaart kun je bewust **▸ Lees woord voor woord**
kiezen. Blader met **← Woord** en **Woord →** door de bestaande oorspronkelijke
tekst of een daadwerkelijk lokaal voltooide vertaling. Elke taal begint bij
het eerste tekstwoord, en nieuwe zinnen starten opnieuw. De markering is
**niet** gesynchroniseerd met audio en stelt geen uitgelijnde vertaling of
native-speakercontrole voor.
[Bediening en beperkingen](docs/MANUAL_WORD_STEP_CAPTION.md).

## Nieuw: vijf basiswoorden extra groot presenteren

De vijf ingebouwde offline Kurmanji-, Sorani- en Pasjtoe-woorden
kun je vanuit het basiswoordenbord nu **▣ groot tonen**, met
**Vorige / Volgende**, bronkeuze Nederlands/Engels,
volgordewissel en groter/kleiner schrift. Alleen de bestaande vijf
niet-onafhankelijk-gecontroleerde voorbeelden; geen extra taalmodel
of microfoon. [Meer informatie](docs/FULLSCREEN_OFFLINE_STARTER_CARDS.md).

## Nieuw: mijn offline gesprekskaarten terugvinden

Je kunt eigen Kurmanji-, Sorani- en Pasjtoe-zinnen nu tegelijk
doorzoeken, met meerdere zoekwoorden en optioneel alleen favorieten.
**Toon groot** bladert uitsluitend door de gefilterde resultaten
(inclusief de gekozen taalvolgorde), niet stilzwijgend door andere
privézinnen. De teller laat zien of er meer dan 80 kaarten zijn;
**Toon volgende kaarten** laat die op verzoek zien. Alle betekenissen
blijven door jezelf geschreven en ongeverifieerd.
[Bediening en privacy](docs/PRIVATE_PHRASE_DISCOVERY.md).

## Nieuw: mijn eigen uitlegregels doorzoeken

Heb je in de huidige sessie één of meer eigen betekenissen bij
ondertitels gezet? Met **✎ N eigen uitleg** vind je alle persoonlijke
uitlegregels terug in één tijdelijke, doorzoekbare weergave.
Zoek op herkende tekst, modelvertaling of je eigen betekenis;
open een volledige tweetalige kaart of wijzig/wis de eigen notitie.
Een onvoltooide vertaling wordt niet als afgerond gepresenteerd.

Dit overzicht is alleen een **read-only index van het lopende
gesprek**, geen opgeslagen woordenboek, automatische controle
of nieuwe export. Achtergrond/sluiten beëindigt het overzicht en
de bestaande privacy- en modelgates blijven intact.
[Bediening en privacy](docs/SESSION_MEANING_INDEX.md).

## Nieuw: eigen uitleg naast een afgeronde gesprekszin

Tuddy heeft nu een tweede, onafhankelijke mogelijkheid om een zin te
verduidelijken: na het stoppen van de microfoon kun je bij een
gespreksregel op **✎ Eigen uitleg** drukken en je eigen korte
interpretatie (max. 280 tekens) toevoegen. De originele woorden en de
automatische offline modelvertaling blijven zichtbaar en onveranderd.
Je notitie krijgt overal de waarschuwing **niet geverifieerd**,
ook in de grote tweetalige gesprekskaart en leesmodus. Je kunt haar
later wijzigen of wissen.

Een notitie verdwijnt wanneer je de originele zin corrigeert, die
regel verwijdert of het gesprek wist. Hij blijft alleen in het
tijdelijke sessiegeheugen; er is geen opslag, netwerk, nieuw model,
verificatieclaim of automatische export. **De notitie verschijnt niet
in bestaande gesprek-/SRT-/WebVTT-bestanden of deelacties.**
[Werking en privacygrenzen](docs/SESSION_MANUAL_MEANING.md).


**Tuddy** is the Android-first, privacy-focused realtime translation and language-learning app previously prototyped as **LiveLingo**.

It aims to display speech and its translation together like synchronized lyrics, with a selected target language, automatic source-language detection, scrollable conversation history, and compact word cards. The project prioritizes newcomer and refugee language access.

**Language picker search:** both the spoken-source and translation-target pickers match language codes, native/English labels, Dutch localized names, and common aliases. Accents and combining marks are ignored, so `Francais` finds `Français`, while `Japans`, `Koreaans`, `Engels`, and regional Arabic aliases are searchable. Search improves discovery only; it does not imply additional offline model support.

## Nieuw: een afgeronde gesprekszin groot tonen

Tik in de gesprekstijdlijn bij een afgeronde zin op
**▣ Toon deze zin groot** om zonder delen of exporteren tijdelijk
een leesbare schermvullende kaart te tonen. Kies origineel, vertaling
of beide, grotere tekst en bladeren door andere afgeronde zinnen.
De microfoon wordt gestopt, er wordt niets automatisch bewaard.
[Uitleg en grenzen](docs/FULLSCREEN_SINGLE_CAPTION_CARD.md).

## Nieuw: afdrukbaar offline tweetalig zinnenboek

Bij **Mijn eigen offline zinnen** kan de gebruiker een zelfstandig
HTML-zinnenboek bewaren van zelfgeschreven Kurmanji/Sorani/Pasjtoe-zinnen
en hun eigen NL/EN-betekenissen, als afdrukbare tweetalige kaarten.
Kies bewust één taal of alle talen en de Android-opslaglocatie.
Zinnen en betekenissen worden **niet als geverifieerd gepresenteerd**;
er is geen internet of extra model nodig. Het externe bestand is
geen Tuddy-back-up en valt niet onder het wissen van de app.
[Bediening en privacy](docs/PRIVATE_PRINTABLE_PHRASEBOOK.md).

## Nieuw: eigen gesprekskaarten direct zoeken

Op het hoofdscherm opent **▣ Mijn gesprekskaarten · direct zoeken en tonen**
een snelle, volledig offline zoekweergave voor bewust opgeslagen
Kurmanji-, Sorani- en Pasjtoe-zinnen. Je kunt rechtstreeks een grote
tweetalige kaart laten zien of je eigen zinnen beheren. Dit gebruikt
geen live transcriptopslag, spraakherkenning, cloud of automatische
vertaling. Eigen betekenissen zijn niet geverifieerd.
[Meer informatie](docs/OFFLINE_BILINGUAL_PHRASE_PRESENTER.md).

## Nieuw: eigen offline woordenboek

Naast de kleine ingebouwde woordenlijst kunnen gebruikers nu zelf
**woordbetekenissen** invoeren, zoeken, bewerken en wissen. Een tik op
een ondertitel toont een eigen notitie als deze bestaat en biedt een
expliciete knop om een betekenis toe te voegen. Eigen woorden kunnen
ook met de offline oefenkaarten worden geleerd; de app houdt deze
notities duidelijk gescheiden van de gecontroleerde basiswoorden.
Alles blijft privé op het toestel totdat de gebruiker zelf iets invoert.
[Werking, grenzen en privacy](docs/PRIVATE_OWN_WORD_NOTES.md).

## Nieuw: tweetalige SRT-ondertitelconcepten

Bij **Gesprekstools → Bewaar SRT-ondertitelconcept…** kun je van
voltooide originele en lokaal vertaalde zinnen een SRT-bestand maken.
Dit is alleen na aparte privacytoestemming en via je eigen Android-
bestandskiezer; er is geen automatische export. De cue-tijden zijn
**ruwe schattingen gebaseerd op het moment dat spraakherkenning klaar
was** en absoluut geen audio-/woord-synchronisatie. Controleer de
vertaling en lijn de tijden handmatig uit in je montageprogramma.
[Werking en privacygrenzen](docs/PRIVATE_BILINGUAL_SRT_DRAFTS.md).

## Nieuw: één privéondertitel verwijderen

Per ondertitel kun je met **🗑 Wis deze regel** een afzonderlijke
gespreksregel na expliciete bevestiging uit het tijdelijke geheugen wissen.
Dit bewaart de andere gespreksturns en opgeslagen woorden. De bevestiging
controleert de actuele tekst en revisie voordat de regel wordt verwijderd;
oude lokale vertaalcallbacks kunnen die regel niet herstellen. Nog niet
voltooide export- en deelacties worden ingetrokken, maar bestaande externe
kopieën en klembordinhoud kunnen niet achteraf worden gewist.
[Privacy en bediening](docs/PRIVATE_SINGLE_CAPTION_DELETE.md).

## Geen automatische vertaalmodeldownloads meer

Een nieuwe ondertitel, handmatig getypte zin of correctie probeert
niet langer stilzwijgend een ML Kit-teksttaalmodel op te halen.
Tuddy controleert eerst de werkelijk geïnstalleerde modellen voor
de herkende bron en het gekozen doel. Ontbreekt iets, dan toont
de zin **Model ontbreekt** met de benodigde talen. De gebruiker
kiest daarna zélf of hij het ontbrekende model via wifi voorbereidt.
Ook bij tijdelijk onbekende modelstatus is er geen cloudfallback.
[Exact gedrag en grenzen](docs/EXPLICIT_MODEL_DOWNLOAD_ONLY.md).

## Nieuw: uitspraak oefenen met lokaal Android-voorlezen

Bij elke ondertitel kun je via **🔊 Lokaal voorlezen · kies tempo**
de oorspronkelijke zin of een afgeronde vertaling laten uitspreken,
in normaal of rustig tempo. De app biedt alleen al geïnstalleerde,
niet-netwerkafhankelijke Android TTS-stemmen aan. Ontbreekt een
bruikbare lokale stem, dan blijft voorlezen uit en volgt **geen**
online fallback. Een eigen stopactie beëindigt de uitspraak.
Er wordt niets als audio bewaard.
[Bediening en beperkingen](docs/OFFLINE_PRONUNCIATION_CHOICES.md).

## Nieuw: tweetalig lezen naast elkaar

In **⛶ Leesmodus** komen het werkelijk herkende origineel en de
lokale vertaling op brede schermen (vanaf 600 dp) in twee kolommen
**naast elkaar**. Je kunt daar zelf tussen kolommen of de normale
weergave onder elkaar wisselen. Spraak-/vertalingsstatus, eigen
sprekerlabels en de live-woordfocus blijven zichtbaar; onvoltooide
vertalingen worden niet ingevuld met verzonnen tekst. Op smalle
schermen blijft de vertrouwde verticale layout.
[Uitleg en privacygrenzen](docs/BILINGUAL_PARALLEL_READING.md).

## Nieuw: privé-oefenkaarten zonder internet

Met **🧠 Oefenkaarten** oefen je de woorden die je bewust hebt
bewaard uit de ingebouwde woordenlijst en je eigen notities
voor Kurmanji, Sorani en Pasjtoe. Wissel tussen kaarten voor
woorden, eigen zinnen of alles. Bekijk eerst het antwoord en
geef zelf **Ken ik ✓** of **Nog eens** aan. Kies nu ook
**Antwoord typen** om een woord of eigen zin letterlijk te reconstrueren,
je invoer zonder netwerk met je opgeslagen antwoord te vergelijken,
en moeilijke kaarten te herhalen of over te slaan. Dit beoordeelt
alleen tekstgelijkheid, niet de werkelijke betekenis of uitspraak.
Eigen betekenissen zijn niet door Tuddy vertaald of gecontroleerd.
Geen score of getypt antwoord wordt opgeslagen of verstuurd.
Na het onthullen van een kaart kun je het opgeslagen woord of je eigen zin
ook normaal of rustig **lokaal laten voorlezen** met een al geïnstalleerde
Android-stem; zonder passende offline stem blijft Tuddy stil. Sluiten of
wisselen van kaart stopt de uitspraak.
[Bediening en grenzen](docs/OFFLINE_STUDY_FLASHCARDS.md).

## Nieuw: bewegende woordfocus in het gewone gesprek

De actuele voorlopige spraakregel krijgt nu ook buiten de fullscreen
leesmodus een bewegende nadruk op het **laatste herkende tekstwoord**.
Het hergebruikt dezelfde Unicode-tekstfocus als de grote leesmodus,
maar met compactere letters. Dit is geen exacte audio-tijdsynchronisatie
en markeert de vertaling niet op verzonnen woordposities.
[Bediening en grenzen](docs/INLINE_LIVE_WORD_FOCUS.md).

## Nieuw: 5 offline basiswoorden direct bereikbaar

Het gespreksscherm heeft nu **🌍 5 basiswoorden · Kurmanji, Sorani,
Pasjtoe**. Eén tik toont alle vijf bestaande, exact gebundelde
basiswoorden (hallo, bedankt, ja, nee, water) in de gekozen taal,
met Nederlandse of Engelse brontekst. Dit werkt zonder microfoon,
wifi of taalmodel en biedt een route naar de eigen offline
oefenzinnen. Het is **geen** vrije vertaalmachine, geen bewijs van
spraakondersteuning of moedertaalsprekeracceptatie.
[Overzicht en grenzen](docs/COMMUNITY_STARTER_PHRASEBOARD.md).


## Nieuw: Syrische en Palestijnse Arabische spraakkandidaten

Naast algemeen Arabisch zijn `ar-SY` en `ar-PS` nu apart kiesbaar
als **Android-regiokandidaten** uit het originele LiveLingo-v0.4-
bronarchief. Tuddy controleert per kandidaat de werkelijk aanwezige
lokale Android-spraakpakketten en weigert cloudfallback. ML Kit biedt
nog steeds uitsluitend generiek Arabische tekstvertaling, **geen**
bewezen dialect-specifieke ASR/MT-dekking.
[Bediening en beperkingen](docs/LEVANTINE_SPEECH_CANDIDATES.md).

## Nieuw: optionele limiet voor microfoonopvang

Onder **Taal- en modelinstellingen → Privacy** kun je een
maximale luistersessie van **5, 15 of 30 minuten** kiezen,
of **Geen** voor bestaande werking. De timer loopt door
over automatische herkenner-herstarts en stopt bij
aflopen echt de on-device microfoon; het tijdelijke
gesprek blijft behouden. Een wijziging tijdens luisteren
begint opnieuw vanaf het moment van kiezen.
Alleen de gekozen minutenwaarde wordt privé opgeslagen.
[Werking en grenzen](docs/MICROPHONE_SESSION_LIMIT.md).

## Nieuw: optioneel trilsignaal bij nieuwe spraakzin

Via **Taal- en modelinstellingen → Toegankelijkheid** kun je een
kort trilalarm aanzetten als de lokale Android-herkenner een
**afgeronde** ondertitel oplevert. Maximaal één puls per drie
seconden, niet bij voorlopige woorden of vertaalupdates.
Standaard uit; de voorbeeldtrilling wordt alleen op je eigen
tik afgespeeld. Er worden geen woorden, audio, tijdstempels
of extra persoonlijke gegevens opgeslagen. Android kan haptiek
dempen; dit is geen garantie op geslaagde herkenning.
[Uitleg en grenzen](docs/COMPLETED_CAPTION_HAPTICS.md).

## Nieuw: optionele privéweergave

Via **Taal- en modelinstellingen → Privacy · privéweergave** kun je
Androids schermbeveiliging voor Tuddy aan- of uitzetten. Indien aan,
vraagt de app het besturingssysteem screenshots, standaard
schermopnames en recente-appsvoorbeelden te blokkeren. De keuze
blijft lokaal op het toestel bewaard; geen transcript of audio
wordt voor deze functie opgeslagen. Externe camera's of andere
toestelafwijkingen kunnen nooit volledig worden tegengehouden.
[Bediening en beperkingen](docs/PRIVATE_SCREEN_SHIELD.md).


## Nieuw: offline startcheck vanaf het hoofdscherm

De knop **Offline startcheck** laat zien of de gekozen Android-spreektaal
daadwerkelijk lokaal beschikbaar is én of de benodigde ML Kit-tekstmodellen
al op dit toestel staan. Je kunt beide statussen apart of samen opnieuw
controleren. Ontbrekende pakketten kunnen uitsluitend via een expliciete
wifi-downloadknop worden voorbereid. Het venster leidt ook naar de
taalinstellingen en naar de modus **Typ zin zonder microfoon**.
Android 12 meldt geen verifieerbare spraakpakketlijst en wordt daarom
niet als 'klaar' weergegeven. De controle bewijst geen werkende
vliegtuigmodus of vrij te vertalen Koerdische/Pasjtoe spraak.
[Bediening en beperkingen](docs/OFFLINE_START_CHECK.md).

## Nieuw: lokaal opgeslagen oefengegevens bewust wissen

Onder **Taal- en modelinstellingen → Privacy** kun je na afzonderlijke
bevestiging alle privébasiswoorden, zelfgemaakte oefenzinnen en favoriete
taalparen tegelijk verwijderen. Tuddy meldt per opslagcategorie
eventuele mislukte verwijderingen; het huidige gesprek, de actieve
taalkeuze en eerder gemaakte externe exports blijven intact.
[Bediening en privacygrenzen](docs/PRIVATE_STUDY_DATA_ERASURE.md).

## Nieuw: favoriete taalparen

Gebruik **☆ Favoriete taalparen** om maximaal drie privécombinaties van
spreektaal en vertaaltaal vast te leggen, te kiezen of te verwijderen.
Een wissel stopt de microfoon, houdt het oude gesprek met oorspronkelijke
taalrichting intact en vraagt daarna om een nieuwe controle van spraak-
én tekstmodel. Er wordt niets automatisch gedownload. Alleen de taalcodes
worden privé bewaard, geen transcript of stemgegevens.
[Bediening en grenzen](docs/FAVORITE_LANGUAGE_PAIRS.md).

## Nieuw: tekst vanuit een andere Android-app delen

**Delen → Tuddy** verschijnt voor één platte tekst in het Android-deelmenu.
Ook via **tekst selecteren → Tuddy** kan geselecteerde tekst uit andere
Android-apps via de systeemactie 'Tekst verwerken' worden geopend.
De gedeelde tekst wordt eerst als **bewerkbaar, tijdelijk concept** getoond;
pas na **Vertaal lokaal** volgt de bestaande offline ML Kit-tekstvertaling.
Annuleren of de app verlaten wist het open concept. Ongeldige en te lange
inhoud wordt geweigerd; geen automatische URL-/bijlagenimport, cloudspraak
of externe bestandsopslag.
[Privacy, bediening en beperkingen](docs/ANDROID_TEXT_SHARING.md).

## Nieuw: offline tekstmodelopslag beheren

Via **Taal- en modelinstellingen → Beheer offline tekstmodelopslag**
zie je de echt geïnstalleerde ML Kit-tekstmodellen. Ongebruikte modellen
kun je **per taal, na afzonderlijke bevestiging** verwijderen. Het actuele
bron-/doeltaalpaar blijft beschermd. De actie is uitgeschakeld bij luisteren,
modelvoorbereiding of een al lopende opslagactie; er is geen automatische
opruiming en geen netwerkspraak. Je huidige gesprek en privéwoorden blijven
staan, maar later gebruik kan opnieuw een modeldownload via wifi vragen.
Dit beheert geen Android-spraakpakketten en belooft geen gemeten opslagwinst.
[Uitleg en beperkingen](docs/OFFLINE_TEXT_MODEL_STORAGE.md).
Nog geen fysiek toestelbewijs of getekende release.

## Nieuw: zinnen handmatig corrigeren

Bij een definitieve gespreksregel kun je met **✎ Corrigeer tekst**
de oorspronkelijke zin in het tijdelijke gesprek herstellen en de
tekst lokaal opnieuw laten vertalen. Voor regels met **Fout** of
**Model ontbreekt** is er ook **↻ Probeer opnieuw**, zonder de tekst
opnieuw in te typen. Oude, vertraagde resultaten kunnen de nieuwere
correctie niet overschrijven; een handmatige correctie krijgt een
zichtbaar label. De microfoon moet uit staan. Dit is geen echte
spraakmodelverbetering of menselijke tolkvalidatie.
[Bediening, privacy en grenzen](docs/CAPTION_CORRECTIONS.md).

## Nieuw: zin typen zonder microfoon

Tik in het gesprekscherm op **✍ Typ zin · zonder microfoon**. Hiermee open
je direct een privacyvriendelijk invoervenster zonder Android-microfoon,
zelfs op telefoons zonder on-device spraakherkenning. Je kiest eerst in de
instellingen de bron- en doeltaal. Een ingevoerde zin wordt exact volgens
de bestaande invoergrenzen als originele ondertitel in het tijdelijke
gesprek toegevoegd. De app probeert met de bestaande ML Kit-pijplijn
**lokale tekstvertaling** voor dat taalpaar; indien het model nog ontbreekt
kan de eenmalige download eerst wifi vragen. Mislukt de vertaling, dan
blijft de invoer als onvoltooide regel herkenbaar.

Het invoervenster kan alleen worden geopend wanneer de microfoon uitstaat.
Annuleren, gesprek wissen of verlaten van de app verwijdert de tijdelijke
concepttekst. Er is geen stilzwijgende server-spraakherkenning, opslag van
de concepttekst of onbewezen vrije Koerdische/Pasjtoe-vertaling. De optie
**Zin typen** onder de modelinstellingen gebruikt dezelfde invoerfunctie.
Een medische of juridische vertaling blijft ongeschikt zonder menselijke
controle. Fysieke Android-acceptatie en release/signing staan apart.

## Nieuw: mobiel transcript-eerst gesprekscherm

Op kleinere telefoons blijft de live ondertiteltijdlijn zichtbaar: **Luister,
Wis gesprek, Volg live, ⛶ Leesmodus en ⇄ Wissel richting** blijven bereikbaar.
De knop **Gesprekstools · export, sprekers en archief** opent een begrensd,
intern scrollbaar paneel voor de bestaande spreker-navigatie, eigen TXT/JSON-
export, privéarchief en eventueel apart gelabelde export, plus nieuwe
archiefacties. Het paneel is standaard gesloten; ook de taal- en
modelinstellingen blijven afzonderlijk uitklapbaar. Beide grote panelen
sluiten elkaar om niet tegelijk de ruimte boven de live tekst op te eisen.
Alle bestaande privacybevestigingen, documentkiezers en rechten blijven
onveranderd. Dit is broncode, geen fysiek geïnstalleerde/release-APK.

## Nieuw: schermvullend live meelezen

Open **⛶ Leesmodus · schermvullende ondertitels** voor een rustige weergave
van de oorspronkelijke spraak en, zodra werkelijk lokaal voltooid, de
vertaling in grote kaarten. Live tussenresultaten blijven expliciet
onafgerond. Wanneer je zelf omhoog scrolt, stopt het automatisch volgen;
met **Volg live** spring je terug naar de nieuwste regel. **■ Stop
microfoon** stopt de reeds actieve herkenning ook vanuit deze weergave;
**Sluiten** keert terug zonder bestaande ondertitels te wissen.
In de leesmodus staat bij elke afgeronde regel het handmatig gekozen
label **A/B/onbekend**. Via **Alle sprekers** kun je de tijdelijke tijdlijn
filteren op A, B of nog onbekend; de originele live tussenregel verschijnt
uitsluitend in de ongefilterde tijdlijn, want die heeft nog geen stabiel
sprekerlabel. Alle labels en filters blijven RAM-only; ze zijn geen
automatische stem- of persoonsidentificatie.
Het scherm gebruikt dezelfde gekozen ondertitelgrootte, bewaart geen
gesprekken, kent geen automatische speaker-ID en vraagt geen extra
Android-rechten. Een fysieke toestelproef en getekende release blijven open.

## Nieuw: veilige tweeweg-gesprekken

Met **⇄ Wissel richting** kun je bij ondersteunde spreektaalparen zonder
gespreksverlies de oorspronkelijke spreek- en vertaaltaal omdraaien.
De microfoon stopt eerst; de nieuwe richting vereist opnieuw controle van
de echte offline spraak- en tekstmodellen op het apparaat. Iedere voltooide
ondertitel blijft zijn **eigen** taalrichting tonen. 59 tekstdoelen zijn
niet hetzelfde als 59 on-device spraaktalen: de knop is alleen beschikbaar
voor dertien expliciet kiesbare regionale spraakkandidaten: elf oorspronkelijke talen (NL/EN/DE/FR/ES/AR/FA/UK/TR/RU/UR), met voor Arabisch ook de afzonderlijke historische regiocodes ar-SY en ar-PS. Android moet het exacte lokale spraakpakket voor iedere kandidaat daadwerkelijk melden; de picker bewijst dat niet. Zie
[de handleiding voor tweeweg-gesprekken](docs/TWO_WAY_OFFLINE_CONVERSATION.md).
Dit bewijst geen telefoonacceptatie of Koerdische/Pasjtoe vrije vertaling.

## Nieuw: één regel uit een privé-archief kopiëren of delen

In het eigen **read-only JSON-archief** kunnen gebruikers na selectie
van een **afgeronde** tweetalige regel kiezen voor
**Gebruik deze archiefregel…**. Een aparte toestemming is vereist voordat
die ene regel naar het Android-klembord of de deelkiezer kan gaan.
De payload meldt dat de inhoud afkomstig is uit een **niet onafhankelijk
geverifieerd, door de gebruiker gekozen archiefbestand**.
De huidige live-sessie wordt niet gewijzigd; de actie wordt geannuleerd
bij achtergrond, gesprek wissen of archief sluiten.
[Privacy en bediening](docs/PRIVATE_ARCHIVE_EXCERPTS.md).

## Nieuw: privégesprek exporteren als JSON

Naast de bestaande tekstexport heeft het gesprekscherm nu **Bewaar JSON…**.
Je kiest zelf via Android waar de JSON met maximaal 500 afgeronde regels
wordt opgeslagen.
Met **Open JSON-archief…** kun je een zelf gekozen, eerder opgeslagen bestand
begrensd en alleen-lezen bekijken, zonder het lopende gesprek te vervangen.
Het archief heeft nu ook een **tweetalige zoekbalk**, resultaatentelling en
per uitspraak datum/tijd; zoekopdrachten blijven volledig lokaal in RAM.
Ongeldige, gewijzigde of te grote archieven worden geweigerd; de inhoud
is door de gebruiker aangeleverd en geen bewijs van echte spraakopname. Per regel blijven originele taal, doeltaal, tijdstempel,
originele tekst en een eventuele **voltooide** vertaling gekoppeld.
Nog lopende of mislukte vertalingen worden expliciet `null`, niet stilzwijgend
als voltooid beschouwd. Export is opt-in; er wordt niets automatisch
gesynchroniseerd of bewaard. Het gekozen bestandsplatform kan **wel**
privégesprekken buiten Tuddy synchroniseren. Lees
[de JSON-format- en privacyuitleg](docs/PRIVATE_TRANSCRIPT_JSON.md).
Dit is broncode-functionaliteit, geen bewijs van een toestelinstallatie.

## Status — 2026-10-17

**Native Android debug APK available; prototype NOT device-validated or released.**
Source `main@b1a298a907f8ff2f7459b9c934f56fafb462f502` passed hosted
[Android APK CI #39064114978](https://github.com/Zennay/Tuddy/actions/runs/39064114978)
(`testDebugUnitTest` + `assembleDebug`).
Artifact **`Tuddy-debug-apk`**, GitHub ID **12112063195**, contains the debug APK,
its SHA256 receipt and the exact source-commit receipt. **APK SHA256:**
`fdc2dc8d4ec7de7293afca9ce3ed1aad832e21c3d0ba1aedef2210bc3b120bf6`.
This checksum is for `app-debug.apk`, not for the enclosing ZIP.
A successful hosted build does **not** prove Android 12+ airplane-mode behavior,
microphone usability, speech model coverage, device performance or release readiness.

### Implemented and validated in source

- Native Android on-device-only `SpeechRecognizer`, continuous captioning across
  sentences with bounded retries for silence and fail-closed permission/language
  errors. Background, stop and session clearing cancel queued restarts.
- ML Kit **on-device text** translation for 59 supported target languages
  after required model downloads. Offline speech recognition coverage remains
  independent and device-/speech-pack-specific.
- Bilingual live/interim/final captions; 'Volg live' respects the user's pause,
  follows growing interim text and works with a 500-line RAM-only history.
- The chosen spoken source and text target survive process restarts as two
  validated private preference codes (restoring unknown/stale codes falls back
  to reviewed defaults); microphone audio, captions and word lookups remain
  RAM-only and are never restored from storage.
- Small, offline, explicitly **five-phrase** NL/EN → Kurmanji/Sorani/Pashto
  preview. This is NOT arbitrary-sentence translation or offline recognition.
- Tap-to-define bilingual text with accessible manual word entry, Unicode
  selection and a small curated offline NL/EN/DE/FR/ES glossary. Unknown words,
  Arabic/Kurdish/Pashto dictionary coverage and sensitive medical/legal text
  never receive invented explanations. Captions preserve per-line language IDs.
- Source integration PRs [#9](https://github.com/Zennay/Tuddy/pull/9),
  [#10](https://github.com/Zennay/Tuddy/pull/10),
  [#11](https://github.com/Zennay/Tuddy/pull/11),
  [#12](https://github.com/Zennay/Tuddy/pull/12),
  [#13](https://github.com/Zennay/Tuddy/pull/13),
  [#14](https://github.com/Zennay/Tuddy/pull/14),
  [#15](https://github.com/Zennay/Tuddy/pull/15),
  [#16](https://github.com/Zennay/Tuddy/pull/16) and
  [#17](https://github.com/Zennay/Tuddy/pull/17). Current integrated main
  has **29/29** Python quality checks passing in an isolated checkout.
  These complement, not replace, hosted Android unit tests.

### New source feature: on-device speech-pack preparation (P1 #2 partial)

- Android 13+ now has a user-initiated **Controleer spraak** action
  reporting Android's actual installed/pending/downloadable/online-only
  on-device source language packs. Unavailable coverage stays unknown.
- A missing supported pack gets an explicit **Download via wifi** action.
  The device recognizer manages model provisioning; Android 14+ progress
  is visible, Android 13 requests must be checked again. Status is
  invalidated after a language switch or background lifecycle.
- It is still **not** free-form Kurmanji, Sorani or Pashto ASR/translation.
  Those languages remain visibly limited to the five-phrase preview.
  See [offline speech pack guide](docs/OFFLINE_SPEECH_PACKS.md).
- Device execution and end-to-end airplane-mode speech remain **unverified**;
  issue #2 stays open until independently licensed local ASR + MT actually
  works for all three communities.

### Additional offline setup: actual ML Kit text-model inventory

A second operator action, **Controleer tekst**, now queries the on-device
ML Kit downloaded translation models for the selected pair. The screen
shows exactly which non-English models are missing. After **Model via wifi**
Tuddy automatically reads the inventory again; completing a download
request is not by itself a readiness claim. Stale callbacks are canceled
on Stop, backgrounding and language changes. Model inventory and speech
readiness remain independent.

See [offline text readiness](docs/OFFLINE_TEXT_MODEL_READINESS.md).

### Open release and product gates

1. Use [docs/DEVICE_VALIDATION.md](docs/DEVICE_VALIDATION.md) with the exact
   APK checksum above on a **physical Android 12+ device**. Record ten
   airplane-mode utterances, multiple continuous restarts, model setup,
   language/permission lifecycle, TalkBack word card and session privacy;
   do not claim a device result until that evidence exists.
2. Finish full historical Android v0.4 source integration. The original
   ZIP remains in ChatGPT Library at
   `/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip`;
   the imported conversation and phrasebook modules do NOT constitute a
   wholesale migration of its previous UI/viewmodel/speech architecture.
   See [the v0.4 source migration map](docs/TUDDY_V0_4_SOURCE_MIGRATION.md)
   for current equivalents and the remaining boundaries.
3. Deliver separately licensed and on-device-tested free-form Kurdish
   Kurmanji, Sorani and Pashto **speech recognition AND translation**
   with native-speaker/medical/legal accuracy review. Five beginner phrases
   are not that implementation. Keep [issue #2](https://github.com/Zennay/Tuddy/issues/2)
   open and labels honest.
4. Keep [issue #1](https://github.com/Zennay/Tuddy/issues/1) and zCloud production
   admission/release holds open until an actual device acceptance and owner
   approval; no automatic VPS workload or signed production deployment.

Notion project: https://app.notion.com/p/3f89e19ac955810aa4a5e53871966f8f

Earlier project tracking: https://github.com/Zennay/zCloud/issues/1344 and https://github.com/Zennay/zCloud/issues/1350

## Privacy and accuracy

No cloud transcription fallback is intended. Text/model downloads may require internet once. Accuracy for legal, asylum, and medical contexts is not guaranteed; a qualified interpreter or native-speaker review remains essential.


### Offline woorden bewaren en oefenen (nieuw)

Tik op een woord in een afgeronde oorspronkelijke of vertaalde ondertitel om de
**lokale woordkaart** te openen. Alleen als de kleine, handmatig gecontroleerde
NL/EN/DE/FR/ES woordenlijst de betekenis daadwerkelijk bevat, verschijnt
**Bewaar woord**. Bewaren is uitdrukkelijk opt-in. Via **Mijn woorden** kun je
woorden offline terugvinden, jezelf oefenen door betekenissen te onthullen,
woorden afzonderlijk verwijderen of na bevestiging de hele lijst wissen.

Alleen maximaal 120 unieke `taal|woord`-sleutels worden opgeslagen in private
Android appvoorkeuren; de betekenis komt telkens opnieuw uit de ingebouwde
woordenlijst. Er wordt **geen gesprek, audio, contextzin, providerantwoord,
persoonlijke identiteit of gespreksgeschiedenis** mee opgeslagen, en er is geen
cloud-woordenboek of netwerkverzoek. Android-backup blijft uitgeschakeld.
Gesprekken blijven standaard RAM-only; de bestaande expliciete export is een
afzonderlijke gebruikersactie. Niet bekende woorden en Koerdische/Pasjtoe
uitdrukkingen mogen geen verzonnen definities krijgen. Apparaatacceptatie en
native-speaker review zijn nog niet bewezen.


### Instelbare, beter leesbare ondertitels

Onder **Taal- en modelinstellingen → Ondertitelgrootte** kies je
**Normaal / Groot / Extra groot**. Dit past onmiddellijk de live
oorspronkelijke tekst én vertaling, het leesbare privé-archief en de
tijdelijk gemarkeerde zinnen aan. Alleen de schermvoorkeur wordt
privé onthouden, nooit het transcript of audio. De modeluitkomst blijft
gelijk. [Gebruik en privacy](docs/CAPTION_READING_SIZE.md). Echte
Android-toegankelijkheidsacceptatie blijft nog afzonderlijk vereist.

### Tijdelijke markeringen voor een belangrijk gespreksmoment

Je kunt nu een afgeronde tweetalige uitspraak **☆ Markeer** geven,
via **★ gemarkeerd** direct terugvinden, naar de oorspronkelijke
tijdlijnpositie springen en de geselecteerde regels bewust als TXT
opslaan. Maximaal 80 markeringen horen alleen bij dit gesprek en
verdwijnen bij **Wis gesprek**, als de oudste zin uit het geheugen
rolt of bij app-afsluiting. Er is geen automatische opslag of netwerk,
en lopende vertalingen zijn nooit als afgerond gelabeld.
[Lees de privacy- en gebruiksinstructies](docs/SESSION_CAPTION_BOOKMARKS.md).

### Zelfgekozen leesbaar woordenblad

Via **Mijn woorden → Bewaar leesbaar woordenblad…** kun je na een
expliciete Android-bestandskeuze een TXT maken met uitsluitend
zelf opgeslagen, door het ingebouwde NL/EN/DE/FR/ES-miniwoordenboek
herkende woorden en hun reeds aanwezige betekenissen. Dit is een
**lees- en printbestand**, geen vervanging van de JSON-herstelback-up.
Het bevat geen gesprek, audio, zelfgeschreven oefenzinnen of
leerresultaten. Een gekozen externe bestandsprovider kan synchroniseren.
[Uitleg en privacy](docs/PRIVATE_VOCABULARY_STUDY_SHEET.md).

### Nieuwe offline woordoefening

Via **Mijn woorden → Oefen opgeslagen woorden** kun je nu echt oefenen
met maximaal 20 eerder bewaarde, lokaal gecontroleerde woorden per ronde.
Na **Toon betekenis** beoordeel je zelf **Ik wist het** of **Nog oefenen**;
lastige woorden keren maximaal tweemaal terug. Je ziet je voortgang en
eindoverzicht, kunt een nieuwe ronde doen of de volgende set oefenen.
Oefenresultaten blijven alleen in RAM gedurende het oefenvenster;
gesprekken, audio, scores en leerhistorie gaan nooit naar een server
of het bestaande JSON-back-upbestand. Dit is geen automatische
spaced-repetition-over-dagen of formele taaltoets.
[Uitleg en beperkingen](docs/OFFLINE_WORD_PRACTICE.md).

### Woorden oefenen én back-uppen op eigen verzoek

De bestaande knop **Mijn woorden** biedt nu ook **Bewaar back-up…** en
**Herstel back-up…** voor alleen de zelf opgeslagen taal/woord-sleutels
uit het kleine lokale, handmatig gecontroleerde woordenboek.
Herstel controleert eerst bestandsversie, bekende woorden, dubbelen en
UTF-8-inhoud, toont hoeveel woorden geladen worden en vraagt vervolgens
apart om de bestaande lijst werkelijk te vervangen. Annuleren raakt de
woorden niet. Definities komen altijd uit de lokaal geïnstalleerde
woordenlijst, niet uit het gekozen bestand. Geen gesprekken, audio of
persoonlijke zinnen worden geëxporteerd of geïmporteerd. Android vraagt
zelf om een opslaglocatie; een externe bestandsprovider kan wel
synchroniseren. Zie [woordback-uphandleiding](docs/PRIVATE_WORD_BACKUP.md).
Fysieke toestelacceptatie en release-APK blijven afzonderlijk vereist.

### Voorlezen van afgeronde vertalingen (op eigen verzoek)

Bij een definitieve lokaal vertaalde regel kun je nu op
**Spreek vertaling uit** tikken. Tuddy vraagt pas dan de standaard
Android TextToSpeech-dienst om **reeds geïnstalleerde** stemmen te tonen.
De app accepteert uitsluitend stemmen die die dienst zelf als
`isNetworkConnectionRequired=false` en niet als 'not installed' markeert,
voor de herkende doeltaal. Als geen passende stem beschikbaar is, wordt
er niets afgespeeld; Tuddy start geen stemdownload, server-TTS of
stilzwijgende online fallback. De microfoon moet uitstaan voor de knop
werkt. **Stop stem**, Stop, achtergrond en app-afsluiting ruimen de
spreekdienst op.

Dit is een **Android-gerapporteerde offline-capaciteit**, geen
onafhankelijk bewijs dat alle apparaat-/fabrikantspraakdiensten volledig
zonder netwerk werken. Een fysiek toestel en vliegtuigmodus zijn nog
niet gevalideerd. Niet alle 59 ML Kit-teksttalen hebben lokaal een
geïnstalleerde Android-stem; Kurmanji/Sorani/Pasjtoe krijgen geen
verzonnen tekst- of spraakondersteuning. TTS-uitspraak is ongeschikt als
enige basis voor gevoelige medische, juridische of asielbesluiten.


### Eigen Kurmanji-, Sorani- en Pasjtoe-zinnen offline bewaren

Open onder **Offline basiswoorden** de knop
**Mijn eigen Kurmanji/Sorani/Pasjtoe zinnen**. Je kunt daar maximaal
80 persoonlijke zinnen met je **eigen** Nederlandse of Engelse
betekenis typen, bewust opslaan, terugzoeken, individueel verwijderen
of na bevestiging allemaal wissen. Kurmanji gebruikt Latijns schrift;
Sorani en Pasjtoe krijgen expliciet RTL-tekstweergave. De enige
permanente data zijn door jou bewust ingevoerde zinnen in de
app-private Android-opslag; Android-backup blijft uitgeschakeld.
Uit het gesprek wordt **niets automatisch opgeslagen** of naar deze
lijst overgenomen; de app verstuurt geen van deze zinnen naar een server.

Deze zelfgeschreven en niet-gecontroleerde notities worden **nooit
voorgesteld als automatisch vertaalde resultaten**. De apart gelabelde
vijf ingebouwde beginnerzinnen blijven dezelfde beperkte,
handmatig gereviewde preview. Er worden geen Koerdische/Pasjtoe ASR-
of ML Kit-vertaalmodellen gedownload of voorgewend. Vrije offline
spraak en vertaling voor alle drie talen, native-speaker controle,
modellicenties en toestelacceptatie uit
[issue #2](https://github.com/Zennay/Tuddy/issues/2) blijven open.
Gebruik eigen notities zonder menselijke tolk niet voor gevoelige
medische, juridische of asielbeslissingen.


### Privé back-up en herstel voor eigen zinnen

In **Mijn eigen offline zinnen** kun je nu expliciet **Bewaar back-up…**
kiezen. Android vraagt via de Storage Access Framework-bestandskiezer
welke locatie het JSON-bestand krijgt; de app schrijft nergens anders.
**Herstel back-up…** accepteert alleen het eigen
versie-1-formaat, maximaal 50 KiB / 80 zinnen, strikte UTF-8,
de bekende taal- en betekenisvelden en slechts de originele zelf
ingevoerde teksten. Beschadigde, onbekende en gedupliceerde gegevens
worden geweigerd voordat er opslag verandert. Een geldig bestand laat
eerst het aantal ingelezen zinnen zien en vraagt apart of bestaande
privézinnen werkelijk moeten worden vervangen; annuleren verandert niets.

Geen extra opslag- of internetmachtiging en geen automatische
achtergrondback-up. Een door jou gekozen cloudbestandsprovider kan
de back-up buiten Tuddy synchroniseren. Exporteer geen gevoelige
persoonsgegevens. De zelf ingevulde betekenissen zijn en blijven
niet-gecontroleerde persoonlijke notities, **geen** nieuwe vertaalmodellen.


### Eén afgeronde tweetalige zin bewust delen

Bij een afgeronde lokaal vertaalde ondertitel verschijnt nu **Deel deze zin…**
naast de bestaande kopieerknop. Eerst volgt een afzonderlijke
privacybevestiging: de getoonde zin kan daarna buiten Tuddy terechtkomen.
Pas na **Open deelvenster** geeft Tuddy alleen die ene originele tekst
plus de daadwerkelijk afgeronde vertaling aan het standaard
Android-deelvenster door. Geen volledig gespreksarchief, transcript
in opbouw, onvoltooide vertaling of automatische achtergrondverzending.
Als de zin of sessie intussen verandert, vervalt de toestemming en moet
je opnieuw kiezen. Na weggaan uit de app verdwijnt het openstaande
toestemmingsvenster. De gekozen externe app kan tekst zelf opslaan,
doorsturen of synchroniseren: deel geen vertrouwelijke persoonsgegevens.
Het openen van Androids chooser bewijst niet dat er werkelijk iets is
verzonden. Deze wijziging levert uitsluitend broncode; fysieke
toestelacceptatie, taalmodelbeschikbaarheid en getekende APK blijven apart.


### Handmatig sprekers onderscheiden in het live gesprek

In de live ondertiteltijdlijn staat bij iedere afgeronde regel **Spreker: onbekend**.
Met **Wijzig spreker** doorloop je handmatig **A → B → onbekend**. Dit is geen
automatische sprekerherkenning: Tuddy beweert niet dat een stem aan een persoon
gekoppeld is. Boven het gesprek kun je met **Volgende A** en **Volgende B**
langs de gemarkeerde gespreksregels navigeren zonder de live opname te resetten.
De labels bestaan uitsluitend in het geheugen van de huidige sessie,
verdwijnen bij **Wis gesprek** of wanneer een oude zin uit de tijdlijn
verdwijnt, en worden niet toegevoegd aan bestaande tekst-/JSON-exporten.
Een ander account, cloud, sprekeridentificatie of nieuwe Android-rechten
is hiervoor niet nodig. Dit is uitsluitend broncode: device- en releasegates
blijven gelden.


### Volledig gesprek met zelfgekozen A/B-sprekers bewaren

De bestaande tekst- en JSON-export blijven **zonder sprekerlabels**. Wie een
afgeronde gespreksregel handmatig als A of B heeft gemarkeerd, krijgt daarnaast
**Bewaar gesprek met sprekerlabels…**. De app stelt een afzonderlijke
privacybevestiging vóór het Android-bestandsscherm verplicht; de gebruiker
kiest zelf het opslagdoel. Het begrensde tekstbestand bevat uitsluitend
afgeronde gesprekkenregels, inclusief A/B/onbekend-labels uit de huidige
tijdelijke sessie. Een gewijzigd transcript of label tussen voorbereiden
en bevestigen weigert de verouderde export. Sprekerlabels zijn nooit
biometrisch herkend, geen echte persoonsidentiteit, niet gesynchroniseerd
door Tuddy en niet in het normale archief opgeslagen. Een gekozen externe
bestandsprovider kan documenten wel opslaan of synchroniseren.
