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


## Fullscreen gesprekszin: dezelfde bewuste kopieeractie

In **Tuddy · Toon gesprekszin** is **Kopieer deze tweetalige caption**
beschikbaar als de getoonde zin al lokaal is vertaald en beide tekstzijden
beschikbaar zijn. Dit gebruikt exact dezelfde bounded en geschoonde
`PrivateCaptionCopy`-payload als de gewone live-leesmodus: twee regels met
de brontaal en doeltaal, nooit een onvoltooid interim of verborgen archief.
De gebruiker tikt zelf op de knop. Er verschijnt een blijvende waarschuwing
dat Androids systeemklembord buiten de RAM-only privacygrens van Tuddy valt
en door andere apps kan worden gelezen. Andere kaart selecteren wist enkel
de zichtbare melding, niet de door Android beheerde inhoud van het klembord.
Er is geen automatische clipboardsynchronisatie of nieuwe netwerktoegang.

## Fullscreen gesprekszin: bewust delen via Android

Bij een lokaal afgeronde tweetalige caption toont **Tuddy · Toon gesprekszin**
ook **Deel deze tweetalige caption**. De actie maakt dezelfde begrensde,
geschoonde `PrivateCaptionCopy`-tekst als de kopieeractie en opent uitsluitend
het standaard Android-deelvenster. Jij kiest zelf een bestemming; er wordt
niet automatisch een ontvanger gekozen of een bericht verstuurd. De gekozen
app ontvangt de twee gelabelde tekstregels en kan die vervolgens verwerken of
online versturen. Deel daarom alleen tekst die je bewust met die app wilt delen.
Niet-afgeronde of ongeldige captions worden geweigerd. Dit is geen nieuwe
vertaling, opslag, spraakherkenning of netwerkverbinding van Tuddy zelf.
