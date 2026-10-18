# Eigen audio met een SRT- of WebVTT-tijdlijn bekijken

Open in Tuddy een eigen **SRT/WebVTT** via de bestaande Android-bestandskiezer. Tik in de tijdelijke ondertiteleditor op **▶ Eigen audio met tijdcodes afspelen…** en kies via een tweede, afzonderlijke Android-bestandskiezer een audio-opname. Tuddy opent die opname met Android MediaPlayer, **zonder automatisch af te spelen**. Tik op **Afspelen**, **Pauze**, **Vorige** of **Volgende** om door de ondertitelregels te luisteren. De schuifbalk laat je direct naar een positie springen; tijdens het schuiven zie je de cue die bij die tijd hoort.

De grote ondertitelkaart volgt alleen de **begin- en eindtijd die in jouw SRT/VTT staat**. Bij overlappende regels geldt de laatst begonnen nog actieve cue; bij een gat verschijnt expliciet dat er geen regel actief is. Het is een luister- en montagehulp, geen spraakherkenning, karaoke-woordtijdlijn, audiodetectie of bewijs van juiste synchronisatie. Je kunt tijdcodes in de bestaande editor aanpassen en een **afzonderlijke kopie** opslaan; de audio-preview zelf verandert niets aan het bronbestand.

## Privacy en grenzen

- De gebruiker kiest het ondertitelbestand en het audiobestand **ieder afzonderlijk**. Niets speelt af zonder expliciete actie.
- De audio wordt **niet** gekopieerd naar het Tuddy-profiel, niet opgeslagen in een export en niet naar een Tuddy-server verzonden. Androids gekozen documentenprovider kan zélf een cloudbestand downloaden of synchroniseren: kies een offline lokaal bestand voor echte vliegtuigmoduservaring.
- De speler pauzeert zodra de app de voorgrond verlaat en wordt vrijgegeven als je het previewscherm sluit. Opnieuw kiezen van audio vervangt de vorige mediabron.
- Alleen Android-content-URI's via de bestandskiezer zijn toegestaan. Onbekende of langer dan zes uur durende opnames worden geweigerd; niet-ondersteunde audioformaten krijgen een foutmelding.
- Het is geen vervanging voor de ontbrekende fysieke Android-vliegtuigmodusacceptatie, native-speaker beoordeelde vrij gelicentieerde Kurmanji/Sorani/Pasjtoe ASR+MT, signed release of zCloud admission.

Geen testsuites, fictieve live audio-woorden of automatische netwerkfallback zijn aan dit productonderdeel toegevoegd.
