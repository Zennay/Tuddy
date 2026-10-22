# Eigen offline taalkaart uit een live gespreksregel

Tuddy biedt bij een tijdelijke gespreksregel **＋ Maak eigen Kurdische/Pasjtoe-kaart**.
Dit is een **handmatige schrijffunctie**, geen offline vertaal- of spraakherkenningsmodel
voor Kurmanji, Sorani of Pasjtoe.

## Werkwijze

1. Maak of open een gespreksregel. Stop de microfoon voordat je deze bewerkt.
2. Kies **Maak eigen Kurdische/Pasjtoe-kaart**. De gespreksregel verschijnt uitsluitend
   als referentie, niet als automatisch gevalideerde bron/vertaling.
3. Kies **Kurmanji**, **Sorani** of **Pasjtoe**. Schrijf zelf de zin in de gekozen taal
   (maximaal 120 tekens). Kies daarna **Nederlands** of **Engels** en schrijf **zelf**
   de betekenis (maximaal 180 tekens).
4. Lees beide velden terug en kies bewust **Bewaar deze eigen privékaart**.
   De kaart verschijnt via **Mijn eigen offline zinnen** en kan daar apart worden
   herzien of verwijderd. **Annuleren** bewaart niets.

De twee velden beginnen altijd leeg. Tuddy kopieert geen herkende woorden,
modelvertalingen of eerdere privébetekenissen automatisch naar de kaart.
Een bestaande kaart met dezelfde taal en zin wordt niet overschreven: wijzig
die bewust in je zinnenboek. De algemene privézinnenlimiet en veiligheidsregels
gelden ook via deze route.

## Privacy en foutgevallen

- Het gesprek en de dialoginvoer bestaan alleen tijdelijk in het appgeheugen.
  De bronregel wordt niet meegeschreven naar je persoonlijke zinnenboek.
  Alleen de **twee door jou ingevoerde velden** en je gekozen talen worden
  na uitdrukkelijke bevestiging app-privé opgeslagen.
- De bevestiging wordt geweigerd als de microfoon actief is, de regel is
  verwijderd, de tekst/revisie is gewijzigd, het gesprek is gewist,
  de invoer ongeldig is of die kaart al bestaat. De bestaande kaart blijft intact.
- Bij achtergrondgebruik verdwijnen open opnameconversie en invoer zonder opslag.
  Er worden geen audio, gespreksgeschiedenis, cloudverzoeken of modelgewichten
  opgenomen in de kaart.
- Een zelfgeschreven betekenis is **niet** door een moedertaalspreker
  beoordeeld of door Tuddy geverifieerd. Deze functie verandert niets aan
  issue #2 voor volledig vrije offline Kurmanji/Sorani/Pasjtoe ASR+MT of aan
  issue #1 voor fysieke Android-vliegtuigmodusacceptatie.


## Meerdere regels

Voor een gestopte sessie kun je ook maximaal tien gespreksregels selecteren en
per regel zelf een privékaart schrijven. De batch wordt atomair opgeslagen
nadat alle velden compleet zijn:
[Meerdere eigen privékaarten uit gesprek of archief](PRIVATE_PHRASE_BATCH_AUTHORING.md).
