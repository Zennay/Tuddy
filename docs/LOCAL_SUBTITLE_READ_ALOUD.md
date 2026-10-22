# Lokaal voorlezen van een ondertitelreeks

In een geopend **Eigen ondertitelconcept · lokaal** kun je bij een geselecteerde
regel **Lees vanaf deze regel · maximaal 100 cues** kiezen. Kies eerst handmatig
een taal in de bestaande lokale-stemkiezer. Tuddy leest de geselecteerde cue en
de volgende regels in volgorde voor, met een zichtbare voortgangsteller. Je kunt vóór het starten kiezen tussen
normaal en rustig tempo; de keuze geldt voor de hele reeks.

Tijdens het voorlezen toont Tuddy ook de **actuele cue**. Wanneer de geïnstalleerde
Android-TTS-stem tekstbereiken terugmeldt, markeert de app het bijbehorende
tekstfragment in die cue. Sommige stemmen geven geen bereik of slechts grove
fragmenten door; dan blijft alleen de actuele regel zichtbaar gemarkeerd. Dit
zijn door de spraakdienst gemelde aanwijzingen, geen exacte fonetische timing
of bewijs van woordgrenzen. De bronondertitels en tijdcodes worden niet gewijzigd.

## Veilig gedrag

- Alleen reeds geïnstalleerde Android-TTS-stemmen die door het systeem als
  niet-netwerkafhankelijk worden gemeld, worden gebruikt. Er is geen online
  fallback en Tuddy downloadt geen stem.
- Elke cue moet niet-leeg zijn en maximaal 400 tekens bevatten. Een te lange
  regel blokkeert de hele reeks; er wordt niets stilzwijgend afgekapt of
  overgeslagen. Kort de regel in of splits hem in het tijdelijke concept.
- Maximaal 100 cues per reeks. De volgende reeks kan vanaf een later geselecteerd
  tijdstip worden gestart.
- **Stop**, het selecteren van een andere regel, het wijzigen van het concept of
  het verlaten van de app stopt de huidige reeks. De TTS-engine wordt afgesloten
  en er wordt geen audio of tekst door Tuddy opgeslagen.
- De gekozen stemtaal is een expliciete gebruikerskeuze. De uitspraak kan
  onnauwkeurig zijn; deze functie vertaalt niets en past geen tijdcodes aan.

De bestaande bronfile en het livegesprek blijven ongewijzigd. Voor export moet
je nog steeds afzonderlijk een nieuwe SRT/VTT-kopie bevestigen.
