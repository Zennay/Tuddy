# Kopieer een afgeronde tweetalige caption

In **Tuddy · Leesmodus** staat bij een afgeronde lokale caption de actie
**Kopieer origineel + vertaling**. De app kopieert twee gelabelde regels:

- de brontaalcode met de oorspronkelijke herkende tekst;
- de doeltaalcode met de lokaal afgeronde vertaling.

De actie is handmatig en beschikbaar voor zichtbare, afgeronde captions in de
live-leesmodus en het privéarchief. Voorlopige spraak en vertalingen die nog
niet zijn afgerond krijgen geen kopieeractie. Er wordt geen nieuwe herkenning,
vertaling, opslag of netwerkverbinding gestart.

## Privacy en grenzen

- De payload is begrensd tot 16 KiB, met maximaal 4096 UTF-16-code-units per
  tekstveld en 80 tekens per taalcode. Controletekens worden verwijderd behalve
  tabs en nieuwe regels.
- Kopiëren schrijft bewust naar het Android-systeemklembord. Dat klembord valt
  buiten Tuddy's tijdelijke opslaggrens; andere apps of systeemfuncties kunnen
  de gekopieerde tekst lezen. De gebruiker beslist zelf of kopiëren gepast is.
- Het kopieerbericht waarschuwt voor deze grens. Er is geen automatische
  klembordmonitor, geen achtergrondkopie en geen cloudwoordenboek.
- De functie kopieert tekst zoals Tuddy die toont; ze certificeert geen
  herkenningsnauwkeurigheid of vertaalcorrectheid.

