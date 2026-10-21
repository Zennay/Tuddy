# ASS/SSA-ondertitels veilig importeren

Tuddy ondersteunt het bewust openen van eigen SubStation Alpha (.ssa) en Advanced SubStation Alpha (.ass) ondertitels. De uitkomst is een tijdelijk, lokaal bewerkbaar cue-overzicht dat dezelfde bestaande SRT/WebVTT-exportgrenzen gebruikt.

## Wat wordt gelezen

- Alleen de [Events]-sectie en de Format-regel die de kolommen benoemt.
- Alleen tijdgecodeerde Dialogue-regels; Start, End en Text moeten uniek zijn en Text moet de laatste kolom zijn, zodat komma's in ondertiteltekst behouden blijven.
- Tijden zoals 0:00:01.23 worden exact naar milliseconden omgerekend (centiseconden × 10).
- ASS-stylingblokken zoals {\\i1}, positionering en kleur worden verwijderd, niet uitgevoerd.
- \\N en \\n worden regeleinden; \\h wordt een vaste spatie.
- Letterlijke accolades die als \\{ en \\} zijn ge-escaped, blijven gewone tekst.

## Wat niet wordt uitgevoerd of beloofd

Tuddy voert geen ASS-scripts, effecten, animaties, externe referenties, audio of HTML uit. Vector-teken-cues (zoals {\\p1}...) worden overgeslagen in plaats van als proza weergegeven. Afbeeldingen, karaoke-timing, fonts, stijlen, fade/positionering en andere presentatie-effecten blijven niet behouden. De import bewijst geen spraakherkenning, vertaling of echte audio-uitlijning; tijden zijn uitsluitend de tijden uit het gekozen bestand.

## Grenzen

- Maximaal 256 KiB geldige UTF-8, 6.000 regels, 500 geaccepteerde cues.
- Begin- en eindtijd moeten geldig zijn; eindtijd moet na begintijd liggen en cue-begins moeten chronologisch niet dalen.
- Maximaal 8 tekstregels, 800 tekens per regel en 2.400 tekens per cue.
- De hele cue-lijst moet binnen de bestaande SRT- én WebVTT-exportgrenzen passen.
- Alleen de gebruiker kiest de bron. Geen automatische klembordlezing, geen achtergrondbestanden, geen URL-ophaalactie, geen netwerkvertaling en geen wijziging van het oorspronkelijke bestand.

## Voorbeeld

    [Script Info]
    ScriptType: v4.00+
    [V4+ Styles]
    Format: Name, Fontname, Fontsize, PrimaryColour, Bold, Italic
    Style: Default,Arial,20,&H00FFFFFF,0,0
    [Events]
    Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
    Dialogue: 0,0:00:01.23,0:00:03.45,Default,,0,0,0,,Hallo, wereld!
    Dialogue: 0,0:00:04.00,0:00:05.20,Default,,0,0,0,,Eerste regel\\N Tweede regel

De voorbeeldtekst opent met twee cues. De komma in Hallo, wereld! blijft behouden; de tweede cue krijgt twee tekstregels. De uitvoer is een gewone tijdelijke ondertitel-tijdlijn, geen gerenderde ASS-weergave.

## Waar te gebruiken

- **Open ondertitelbestand…** voor een door jou gekozen Android-document.
- **Plak of importeer ondertiteltekst…** voor een bewuste tekstpreview.
- **Delen → Tuddy** wanneer de andere app de complete ASS/SSA-inhoud echt als text/plain doorgeeft.

De bestaande strikte parser blijft de enige acceptatiepoort; ongeldige bestanden veranderen het huidige gesprek en het oorspronkelijke bestand niet.
