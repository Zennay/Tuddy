# Opgeslagen oefengegevens wissen

Via **Taal- en modelinstellingen → Privacy · opgeslagen oefengegevens →
Wis opgeslagen oefengegevens…** kun je in één expliciete handeling de
app-private studiegegevens verwijderen.

Tuddy laat eerst een bevestiging zien met de actuele aantallen
**bewust opgeslagen basiswoorden**, **zelfgemaakte oefenzinnen** en
**favoriete taalparen** (bij een onleesbare privézinnenlijst wordt geen
onterecht aantal geclaimd). Pas na **Wis mijn oefengegevens** worden
de drie afzonderlijke lokale opslagbestanden gewist.

## Wat verdwijnt?

- Opgeslagen, handmatig gekozen basiswoorden uit de privéwoordenlijst.
- Eigen Kurmanji-, Sorani- en Pasjtoe-oefenzinnen met hun zelf ingevoerde betekenissen.
- Maximaal drie gepinde favoriete taalparen.

Deze actie verwijdert geen eerder geïnstalleerde ML Kit-tekstmodellen of
Android-spraakpakketten, en doet geen download of netwerkverzoek.
Het **huidige RAM-only gesprek**, de actieve taalrichting en eventuele
zelf geëxporteerde archiefbestanden blijven bestaan. Externe opslag
en de Android-klembordgeschiedenis vallen buiten de controle van Tuddy.

## Foutafhandeling

De drie private voorkeuren worden afzonderlijk geschoond met een
synchronisch commit-resultaat. Als één opslag niet gewist kan worden,
toont Tuddy **per categorie** welke opdracht mislukte, zodat de gebruiker
bewust opnieuw kan proberen; de app claimt dan niet dat alle data weg is.
Na de handeling worden open panelen gesloten en de getoonde aantallen
vernieuwd. **Annuleren**, naar de achtergrond gaan en sluiten nemen geen
bevestiging aan.

Geen automatische schoonmaak of toestelbrede reset; de gebruiker
houdt de regie. Een extern verspreide tekstkopie is niet terug te halen.
Dit is broncode-functionaliteit, geen getekende release of geverifieerd
fysiek toestelresultaat.
