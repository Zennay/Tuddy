# Lokale SRT, WebVTT, LRC, CSV en TSV openen

Via **Gesprekstools → Open ondertitelbestand…** kiest de gebruiker zelf een
SRT-, WebVTT-, LRC-, getimed CSV- of getimed TSV-tekstbestand met de Android-documentkiezer. Tuddy leest
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
