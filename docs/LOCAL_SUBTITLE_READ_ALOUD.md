# Lokaal voorlezen van een ondertitelreeks

In een geopend **Eigen ondertitelconcept · lokaal** kun je bij een geselecteerde
regel **Lees vanaf deze regel · maximaal 100 cues** kiezen. Kies eerst handmatig
een taal in de bestaande lokale-stemkiezer. Tuddy leest de geselecteerde cue en
de volgende regels in volgorde voor, met een zichtbare voortgangsteller. Je kunt vóór het starten kiezen tussen
normaal en rustig tempo; de keuze geldt voor de hele reeks.

Bij zowel **Hoor lokaal / Rustig** voor één cue als bij een reeks toont Tuddy
de **actuele cue** tijdens het voorlezen. Wanneer de geïnstalleerde Android-TTS-stem
tekstbereiken terugmeldt, markeert de app het bijbehorende tekstfragment. Sommige
stemmen geven geen bereik of slechts grove fragmenten door; dan blijft alleen
de actuele regel zichtbaar gemarkeerd. Tijdens het afspelen worden de startknoppen
uitgeschakeld om overlappende spraak te voorkomen. Tik **Open deze regel** om
een reeks te stoppen en de actieve cue in de editor te openen. Dit zijn door de
spraakdienst gemelde aanwijzingen, geen exacte fonetische timing of bewijs van
woordgrenzen. De bronondertitels en tijdcodes worden niet gewijzigd.

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

## Dezelfde volgmarkering in de fullscreen-gesprekszin

In **Tuddy · Toon gesprekszin** gebruiken de knoppen voor de originele zin en
de lokale vertaling dezelfde tijdelijke volgmarkering. De actieve tekst blijft
zichtbaar tijdens het spreken; een door de geïnstalleerde stem gemeld
tekstbereik wordt indicatief gemarkeerd wanneer beschikbaar. Stoppen,
naar een andere zin gaan, de weergave wisselen of de app verlaten stopt de
spraak en wist de tijdelijke markering. Ook hier wordt geen audio opgeslagen
en wordt geen netwerkstem of stemdownload gebruikt.

## Tweezijdig gesprek

In het tweezijdige gespreksscherm volgt de markering de zijde die je met
**Tegenover** of **Hier** laat voorlezen. De zichtbare tekst gebruikt dezelfde
normalisatie als de spraakinvoer, zodat teruggegeven offsets op de getoonde zin
passen. Wisselen van zijde, stoppen, een nieuwe beurt of de achtergrond in gaan
stopt de spraak en wist de tijdelijke markering. Ook hier blijft de markering
afhankelijk van wat de geïnstalleerde lokale stem werkelijk terugmeldt.
