# Lokale SRT, WebVTT, LRC, CSV, TSV, SBV en JSON3 openen

Via **Gesprekstools → Open ondertitelbestand…** kiest de gebruiker zelf een
SRT-, WebVTT-, LRC-, getimed CSV-/TSV- of YouTube SBV-tekstbestand met de Android-documentkiezer. Tuddy leest
maximaal 256 KiB strikt geldige UTF-8 en toont maximaal 500 regels in
een tijdelijke, doorzoekbare leesweergave. **Groot lezen** toont grotere
tekst; vorige/volgende en de lijst werken ook bij eigen LRC-lyrics.
De knop **Tweede ondertitelbestand toevoegen…** gebruikt dezelfde
begrensde import wanneer de gebruiker bewust twee sporen combineert.

## LRC: alleen aangeleverde begintijden

De bestaande strikte LRC-parser accepteert tijdtags zoals
`[00:12.50]eigen tekst` en toegestane metadata en offset (binnen zijn
bestaande grenzen). Twee begintijden op één lyricregel worden twee
afzonderlijke regels op hun eigen chronologische plaats. Een volledige
LRC is geen SRT: Tuddy leidt het einde van een regel af van de volgende
begintijd; **de laatste regel krijgt een geschat einde na 3 seconden**.
Dat is geen gemeten woord-/audio-uitlijning, geen herkende spraak en geen
bevestigde vertaling. Ongeldige, dubbele of omgekeerde starts, controles,
te grote bestanden en andere misvormde invoer worden geweigerd.

Na het openen werkt de vertrouwde **tijdelijke SRT/WebVTT-editor**
met zoeken, corrigeren, handmatige tijdswijzigingen, lokale
audio-voorvertoning en bewuste export. Alleen cue-tekst en afgeleide
start-/eindtijden gaan mee naar die editor: LRC-metadata, meerdere
fysieke tijdtags per lyricregel en de oorspronkelijke offsetheader
worden **niet verliesvrij bewaard bij een SRT/VTT-export**. Wil je de
authored LRC-bron inclusief metadata en offset behouden of bewerken?
Gebruik **Plak of importeer ondertiteltekst…**,
kies daar de LRC-modus en sla uitsluitend na een afzonderlijke
Android-opslagbevestiging een nieuwe volledige LRC-bronkopie op.
Het originele gekozen document wordt niet overschreven.

## Bestandskeuze binnen de handmatige editor

Ook vanuit **Plak of importeer ondertiteltekst… → Kies document…**
kan de gebruiker zelf een bronbestand kiezen. Na een geldige, begrensde
UTF-8-documentread probeert Tuddy buiten de UI-thread exact dezelfde
SRT/VTT-, LRC- of getimede CSV/TSV-parser als bij **Open ondertitelbestand…**.
Alleen bij bewezen geldig formaat wordt de passende invoermodus gekozen.
Bij onbekende maar leesbare platte tekst blijft de eerder door de gebruiker
gekozen handmatige modus behouden. Een teruggekeerde bestandskiezer
**past nooit direct een cue toe**: de oorspronkelijke tekst, gekozen modus
en volledige voorvertoning moeten nog apart gecontroleerd en bevestigd
worden. Dit werkt eveneens wanneer een tweede ondertitelspoor handmatig
uit een apart bestand wordt gekozen.

## CSV en TSV: eigen tijdcodes heropenen

Het bestaande **Bewaar tijdcodes als CSV/TSV…** in de privé-editor
schrijft drie kolommen (begintijd, eindtijd, brontekst) of vier kolommen
(dezelfde drie plus een handmatig ingevoerde tweede tekstregel). Via de
bestandskiezer kunnen die eigen exports nu rechtstreeks terug de editor
in. Een CSV-veld met komma's en aanhalingstekens wordt uitsluitend via
de bestaande strikte CSV-regelparser gelezen; TSV gebruikt tab-gescheiden
velden. Er is geen bestandsnaam- of MIME-vertrouwen: uitsluitend geldige
velden, exacte bestaande tijdcodes en een opnieuw als SRT én VTT
exporteerbaar geheel worden geaccepteerd. Een ongeldige rij weigert
het hele document. Een gewone woordenlijst zonder tijdkolommen
wordt dus niet stilzwijgend van bedachte timing voorzien.

**Geen header of spreadsheetformules uitvoeren:** de eigen export
bevat geen koprij, en de importer verwacht ook geen koprij.
De eigen tijdregels en eventuele tweede tekstkolom worden als
ongeverifieerde inhoud behandeld; Tuddy voert geen formule uit.
Alleen een expliciet gekozen nieuwe kopie kan de data bewaren.

## YouTube SBV / SubViewer handmatig openen

Een bewust gekozen YouTube SBV-bestand gebruikt eigen begin- en eindtijden,
zoals:

```text
0:00:01.000,0:00:03.500
Hier staat een eigen ondertitel.

0:00:04.000,0:00:06.000
Dit is de volgende.
```

**Open ondertitelbestand…** en **Tweede ondertitelbestand toevoegen…**
herkennen deze strikt begrensde, blanco-regel-gescheiden tekst.
Alleen het exacte tijdformaat `H:MM:SS.mmm,H:MM:SS.mmm`,
niet-lege tekstblokken en chronologisch oplopende starts worden geaccepteerd.
Bij ongeldige of dubbele tijdvakken met niet-positieve duur wordt het
hele bestand geweigerd. Een import neemt uitsluitend door de gebruiker
aangeleverde teksten en begin-/eindtijden over; de app opent nooit zelf
een YouTube-video, URL of automatisch downloadbare ondertitels.
Gebruikers kunnen de geïmporteerde regels lokaal lezen, corrigeren,
als tweede spoor verwerken of met aparte bevestiging als SRT/VTT
bewaren. Uit SBV blijven geen oorspronkelijke YouTube-metadata
of audio-identiteit als geverifieerd bewijs bestaan.

## SBV zonder bestanden plakken, delen en exporteren

De handmatige knop **Plak of importeer ondertiteltekst…** kan nu ook
complete `H:MM:SS.mmm,H:MM:SS.mmm` SBV-blokken lokaal lezen.
In de Android-deelactie **Delen → Tuddy** wordt alleen letterlijk
geselecteerde `text/plain`-SBV-inhoud met geldige timestamps aan de
private preview doorgegeven. Een onvolledige of ongeldige SBV krijgt
geen automatische opname, spraakherkenning of tijdschatting.
Ook wanneer in de handmatige editor een SBV-document via **Kies document…**
wordt geopend, wordt de invoermodus pas voorgesteld nadat de hele
bestandsinhoud met de strikte SBV-parser is gecontroleerd.
De originele bron blijft onaangeroerd.

Na handmatig bewerken verschijnt **Bewaar exacte tijdvakken als YouTube SBV…**.
Die route toont eerst de exacte UTF-8-bestandsinhoud in pagina's en
vereist vervolgens afzonderlijk een Android-opslaglocatie. Het exportformaat
bewaart per cue **start, einde en letterlijke tekst**, zonder koptekst
of veronderstelde audio-/video-identiteit. De nieuwe export wordt vóór
opslaan opnieuw als SBV geparsed en byte-voor-byte gecontroleerd tegen
de bevestigde preview. Cues met ongeldige regels, tijdcodes of
lege tussenregels worden volledig geweigerd; SRT/VTT blijven mogelijk.
De Android-provider kan extern synchroniseren, maar Tuddy doet dit
niet zelf. Het originele bestand wordt niet overschreven.

## YouTube JSON3-ondertitels uit een eigen bestand

**Open ondertitelbestand…** ondersteunt ook expliciet geselecteerde YouTube
timed-text **.json3**. Tuddy vraagt geen URL, YouTube-account of
internetverbinding op. Het aangeleverde `events[].tStartMs` en
`dDurationMs` bepalen uitsluitend de cue-start en -eindtijd;
`segs[].utf8` wordt per event samengevoegd tot één tekstblok.
Dit zijn geen bewezen woordtijden of spraak- en vertaalresultaten.
Layout-events zonder tekst worden niet getoond. JSON3 is niet
uitwisselbaar met een Tuddy-privégespreksarchief.

Een geldig document bevat maximaal 256 KiB strikt leesbare UTF-8,
3.000 JSON-events en 500 bruikbare cues met chronologische start,
positieve duur, begrensde tekst en tijden die als SRT én VTT bewaard
kunnen worden. Bij ongeldige inhoud wordt het volledige document
geweigerd. De tekst staat alleen tijdelijk in de bestaande lokale
ondertitel-editor; correcties en nieuwe SRT/VTT-kopieën zijn afzonderlijke
gebruikeracties. Geen video, geluid, model, import-URL of automatische
netwerkactie. De oorspronkelijke JSON3 blijft onaangeroerd.

## JSON3 zonder document plakken of delen

Ook complete door de gebruiker aangeleverde JSON3-tekst kan nu via de
handmatige ondertitel-editor of Androids `text/plain`-deelmenu als
tijdelijk, bevestigingsplichtig ondertitelconcept worden geopend.
In die directe UI-route geldt **64 KiB** als grens; de asynchrone
Android-bestandskiezer behoudt de 256-KiB-documentgrens. Alle
aangeleverde tijden blijven oorspronkelijk. De gebruiker kan een
geldige JSON3 kopiëren, eerst privé inspecteren en daarna expliciet
als nieuwe SRT/VTT-kopie bewaren. Ongeldige JSON3 wordt niet tot
gewone automatische gesprekstranscriptie of vertaling gedegradeerd.

## Veiligheid en privacy

Dit is uitsluitend het bewust gekozen document, nooit de actuele
live-gespreksbron. Het inlezen gebeurt buiten de UI-thread met
begrensde geheugengrootte. De bestandskiezer kan een externe
Android-provider tonen; deze kan zelfstandig synchroniseren.
Tuddy volgt geen URL of bijlage, vraagt geen blijvende URI-machtiging,
start geen microfoon, downloadt geen model en doet geen serververtaling.
Ongeldige bestanden wijzigen het huidige gesprek, eigen privékaarten
of reeds geopende ondertitels niet. Het tijdelijke concept wordt bij
sluiten of achtergrond gewist. Alleen expliciete opslag via Android
kan een nieuwe kopie maken.

Ook deze Android-codelevering bewijst geen fysieke vliegtuigmodusacceptatie,
complete vrij gelicentieerde Koerdische/Pasjtoe offline ASR+MT,
native-speakerreview of getekende productierelease.
