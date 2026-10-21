# Lokale SRT, WebVTT en LRC openen

Via **Gesprekstools → Open SRT/VTT/LRC…** kiest de gebruiker zelf een
SRT-, WebVTT- of LRC-tekstbestand met de Android-documentkiezer. Tuddy leest
maximaal 256 KiB strikt geldige UTF-8 en toont maximaal 500 regels in
een tijdelijke, doorzoekbare leesweergave. **Groot lezen** toont grotere
tekst; vorige/volgende en de lijst werken ook bij eigen LRC-lyrics.
De knop **Tweede SRT/VTT/LRC-document toevoegen…** gebruikt dezelfde
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
Gebruik **Plak eigen SRT/VTT/LRC-tekst en bekijk voorbeelden…**,
kies daar de LRC-modus en sla uitsluitend na een afzonderlijke
Android-opslagbevestiging een nieuwe volledige LRC-bronkopie op.
Het originele gekozen document wordt niet overschreven.

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
