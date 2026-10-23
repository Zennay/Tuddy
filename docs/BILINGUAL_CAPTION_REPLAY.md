# Tweetalig lokaal voorlezen van een afgeronde caption

**Tuddy → Toon gesprekszin** toont afgeronde gespreksteksten op het volledige
scherm. De gebruiker kan zelf kiezen:

- **Normaal / Rustig:** tempo voor het voorlezen van een hele zin of van twee zinnen.
- **Origineel → vertaling:** speel eerst de oorspronkelijke zin, daarna de
  afgeronde lokaal vertaalde zin.
- **Vertaling → origineel:** luister naar dezelfde twee zijden in omgekeerde
  volgorde.
- **Stop voorlezen:** stop direct, ook midden in de eerste van twee zinnen.

## Veilig verloop

De twee knoppen verschijnen uitsluitend bij een afgeronde lokaal vertaalde
caption met een aparte vertaling en twee verschillende taalcodes.
Elk tekstdeel wordt beperkt tot 400 tekens, zonder controletekens.
De tweede taal begint pas nadat Android de eerste voorleeszin als afgerond
heeft teruggegeven. Valt de lokale TTS-stem of de eerste stap weg, dan wordt
de tweede stap **niet** automatisch afgespeeld.

Voor elke taal controleert Tuddy de geïnstalleerde Android-stem opnieuw:
netwerkafhankelijke en niet-geïnstalleerde stemmen zijn uitgesloten. Er
wordt geen voice pack gedownload, geen cloudfallback gebruikt en geen
spraakherkenning of vertaling gestart. De huidige Android-stem kan optioneel
een indicatief tekstbereik terugmelden; deze markering is geen opgenomen
audiotijdcode, ASR-woorduitlijning of bewijs van correctheid van de vertaling.

Van caption wisselen, woordstappen inschakelen, handmatig de andere
weergavezijde kiezen, stoppen en de Activity verlaten stoppen de lopende
TTS-reeks. Het systeem bewaart geen audio of transcript en vraagt niet om
een extra microfoontoestemming.

**Niet verwarren met taalondersteuning:** deze functie levert geen
offline ASR/MT voor Kurmanji, Sorani of Pasjtoe. Daarvoor blijven
modelrechten, daadwerkelijke mobiele werking, native-speaker-evaluatie en
fysieke offline-acceptatie afzonderlijke gates (issues #1 en #2).


## Mijn eigen zinnen: twee talen afspelen

Ook in **Mijn gesprekskaart** (het persoonlijke Kurmanji-, Sorani- en
Pasjtoezinnenboek) zijn nu twee bewuste acties beschikbaar:
**Mijn zin → eigen betekenis** en **Eigen betekenis → mijn zin**.
Kies zelf het normale of rustige tempo. Beide teksten zijn door de gebruiker
zelf geschreven en niet door Tuddy of een moedertaalspreker gecontroleerd.
Tuddy leest achtereenvolgens de tekst in de betreffende taal, uitsluitend
wanneer Android daarvoor een vooraf geïnstalleerde niet-netwerkafhankelijke
stem aanbiedt. Ontbreekt de eerste stem, dan wordt de tweede kant niet alsnog
uitgesproken. Stemselectie, indicatieve woordhighlight en Stop blijven lokaal.

Dit is een offline presentatie-/studiefunctie, **niet** de ontbrekende vrije
spraakherkenning of machinevertaling voor kmr/ckb/ps.
