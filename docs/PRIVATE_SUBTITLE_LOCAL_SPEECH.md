# Geïmporteerde ondertitelregel lokaal laten voorlezen

Open bewust een SRT- of VTT-tekstbestand via
**Gesprekstools → Open SRT/WebVTT…**. Tik een cue aan om hem groot
te tonen. Kies daarna expliciet **de taal van de geselecteerde
tekst** in de taalkiezer en druk op **🔊 Hoor lokaal** of
**Rustig**. Tuddy doet geen automatische taaldetectie op het
bestand. Het is jouw keuze welke geïnstalleerde Android-stem
moet worden geprobeerd.

De app gebruikt dezelfde `LocalCaptionSpeaker` als bij
andere offline spraakknoppen: uitsluitend een door Android als
**al geïnstalleerd en niet-netwerkafhankelijk** gemelde stem is
toegelaten, zonder stemdownload of cloudfallback. Bij een
ontbrekende stem gebeurt er niets behalve een duidelijke melding.
Voorlezen werkt alleen voor één gekozen cue van maximaal
400 tekens. Een opgenomen ondertitelbestand kan regels bevatten
die langer zijn; daarvoor blijft de actie uitgeschakeld.

**■ Stop**, kiezen van een andere cue, wijzigen van de cue,
het appvenster naar de achtergrond brengen of de dialoog
sluiten beëindigen de oude spraak. Er wordt geen audio opgeslagen,
geen nieuw model opgevraagd, geen tekst gevalideerd en geen
microfoon gestart. De uitspraak kan fout zijn, zeker bij talen
zonder goede of door moedertaalsprekers beoordeelde stemmen.
Het opent geen live transcript of automatisch gesynchroniseerde
audio; tijdcodes blijven onbetrouwbare bestandsmetadata.
