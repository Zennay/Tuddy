# Eigen TTML/DFXP lokaal openen

Tuddy accepteert een bewust gekozen, geldige UTF-8 **TTML/DFXP**-tekstondertitel via **Open ondertitelbestand…**, **Plak of importeer…**, een expliciet gedeeld text/plain-bericht of een Android-contentdocument met TTML/XML-MIME. De app leest geen links of bestanden zonder gebruikersactie.

## Wat wordt ingelezen

- `<tt>` met `<body>`, optionele `<div>`-blokken en `<p begin="…" end="…">…</p>` met **absolute** tijdcodes.
- `<span>` binnen een regel is gewone tekst, `<br/>` wordt een regeleinde.
- Kloktijden `HH:MM:SS`, `HH:MM:SS.fff` (max. drie decimalen), seconden zoals `1.25s` en milliseconden zoals `1250ms`.
- Een `<head>` met metadata wordt genegeerd. Het XML-bestand wordt niet gerenderd: de uitvoer is alleen de tijdelijke lokale SRT/WebVTT-cuelijst, die je bewust kunt controleren en als **nieuwe kopie** bewaren.

## Veiligheid en compatibiliteit

- Maximaal 256 KiB geldige UTF-8, maximaal 500 cues in oplopende begintijd, maximaal 8 tekstregels per cue, 800 tekens per regel, 2.400 tekens per cue en tijden tot 99:59:59.999.
- Het volledige resultaat moet ook binnen beide bestaande SRT/VTT-uitvoergrenzen passen.
- Om onjuiste timing te voorkomen weigert Tuddy `dur`, inherited body/div/span timing, frame-/tick-gebaseerde tijden, tijdanimaties, nested timed spans en onbekende body-elementen. Cues vereisen beide eigen `begin` én `end`.
- DTD, entitydeclaraties, XML-commentaar, CDATA en overige declaraties of processing instructions worden geweigerd. Alleen standaard XML-escapes (zoals `&amp;`) mogen door de beperkte tekstparser worden omgezet.
- Geen externe XML-entiteiten, netwerkfetch, scripting, layout/styling, automatische vertaling, audioanalyse, opslag van de bron of achtergrondbestandsrechten.
- Een afgewezen bestand verandert het bestaande gesprek en ondertitelconcept niet.

## Voorbeeld

```xml
<?xml version="1.0" encoding="UTF-8"?>
<tt xmlns="http://www.w3.org/ns/ttml">
  <body><div>
    <p begin="00:00:01.000" end="00:00:03.500">Hallo <span>wereld</span>!</p>
    <p begin="4.0s" end="5500ms">Tweede regel<br/>ondertitel</p>
  </div></body>
</tt>
```

De twee cues krijgen uitsluitend de zelf aangeleverde timestamps; TTML-import bewijst geen spraakherkenning, audio-uitlijning, installatie van offline modellen of fysieke device-acceptatie.

## Bewust nieuw TTML-bestand bewaren

In de privé-ondertiteleditor is ook **Bewaar exacte tijdvakken als TTML…** beschikbaar. Na een volledige, apart bevestigde paginapreview vraagt Android waar de nieuwe `.ttml`-kopie moet komen. De auteurstijden worden in milliseconden bewaard; `<`, `>` en `&` worden XML-veilig escaped, echte tekstregelovergangen als `<br/>`. Als het nieuwe bestand niet opnieuw kan worden gelezen zonder verlies van tekst of tijdcodes, wordt bewaren geheel geweigerd. Na terugkomst uit Android wordt de bevestigde bytekopie nogmaals tegen het bewaarde concept gecontroleerd. Er is geen automatische overschrijving, netwerksynchronisatie door Tuddy of opname van audio.
