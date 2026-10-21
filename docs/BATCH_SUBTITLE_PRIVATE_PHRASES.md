# Maximaal acht zelfgeschreven oefenkaarten uit één ondertitelbestand

Tuddy heeft naast **één ondertitelzin opslaan** ook **＋ Maak meerdere eigen
Kurmanji/Sorani/Pasjtoe-oefenzinnen…** in de tijdelijke ondertiteleditor.
De gebruiker kiest maximaal acht originele bronnummers (`1-3,7`), een
Nederlandse of Engelse eigen betekenis-taal en voor **iedere** bronregel
afzonderlijk Kurmanji (`kmr`), Sorani (`ckb`) of Pasjtoe (`ps`).
Iedere tekst blijft volledig zichtbaar, ook wanneer die te lang is om
direct te bewaren. De gebruiker bewerkt de zin naar maximaal 120 tekens
op één regel en schrijft per cue een eigen betekenis (max. 180 tekens).

De volledige selectie is een nieuwe, expliciet beoordeelde private
oefenset. Pas na het nalezen en een aparte checkbox schrijft Tuddy
via `PrivatePhraseDeck.saveNewBatch` **één** atomaire set: een enkele
ongeldige regel, duplicaat, niet-bestaand bronnr., ontbrekende taal,
volle 80-zinnenlijst of gelijktijdig toegevoegde identieke zin zorgt
ervoor dat **nul** nieuwe kaarten worden opgeslagen. Bestaande
persoonlijke betekenissen, favorieten, studieschema's en onderwerpen
worden niet stilzwijgend overschreven.

De bronregelnummers blijven gekoppeld aan de originele tijdelijke
ondertitelvolgorde, onafhankelijk van de zoekfilter. De ondertitel,
audio, timing, externe providers en live gesprekken worden niet
geschreven of gedeeld; alleen de bewust nagelezen zin en menselijke
betekenis blijven lokaal in **Mijn eigen zinnen** beschikbaar.

Dit is een gebruikersgerichte oefenfunctie, geen automatische
vertaling of audioherkenning. De openstaande modellicentie,
native-speaker-review en Android-toestelacceptatie uit issues #1/#2
blijven ongewijzigd.
