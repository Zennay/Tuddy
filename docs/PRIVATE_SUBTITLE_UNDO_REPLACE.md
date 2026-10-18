# Privé SRT/WebVTT-editor: terugdraaien en letterlijk vervangen

In de bestaande **zelf geopende ondertitelkopie** kan de gebruiker nu de laatste twintig geslaagde bewerkingen stap voor stap **Terug** draaien of opnieuw toepassen. Dit omvat tekstcorrecties, begin/eindtijden, een halve seconde globale verschuiving, splitsen, samenvoegen en verwijderen. Een nieuwe bewerking na terugdraaien maakt de oude *Opnieuw*-tak ongeldig. De bestaande knop **Herstel** verwijdert bewust de volledige bewerkingshistorie en zet alleen de oorspronkelijk geopende cues terug.

Dezelfde editor heeft een zelfstandige **Vervang…**-workflow: voer exact te zoeken tekst en letterlijke vervangtekst in (ieder maximaal 100 tekens), zie hoeveel ondertitelregels een treffer bevatten, en bevestig afzonderlijk. Dit is hoofdlettergevoelig en geen regex, spellcheck, audio-timing, vertaler of betekenisverificatie. Een ongeldige of te grote wijziging wordt als geheel geweigerd. Voor iedere gewijzigde regel blijven de bestaande per-cue grenzen gelden: maximaal 2400 tekens, acht regels, niet-leeg, geen verborgen controlecodes. Ook de complete SRT-snapshot moet onder de bestaande 256-KiB-limiet passen. Na goedkeuring is de volledige vervanging **één terugdraai-stap**.

## Privacy en release
- Alle vorige versies blijven uitsluitend in RAM tijdens het geopende document, maximaal twintig snapshots. Sluiten/verlaten van de editor wist de historie; er is geen nieuwe automatische opslag.
- Het originele externe bestand wordt nooit aangepast. **Bewaar kopie SRT…** of **Bewaar kopie VTT…** houdt de bestaande onafhankelijke expliciete Android-bestandslocatie/toestemmingsstap.
- Teksten, private cues of historie worden niet naar de server, een cloudspraakmodel, logs, analytics, externe accounts of een transcriptarchief verstuurd.
- Geen nieuwe permissies, microfoonloop, native speaker ondersteuning, licentieclaim, gesigneerde APK, QA-suite of deviceacceptatie toegevoegd.
- Relevante code: `PrivateSubtitleDraftEditor.replaceLiteral` valideert de wijziging, `PrivateSubtitleFileDialog` bedient undo/redo/confirmatie. Bestaande exact-HEAD Android-build en release-gates blijven verplicht.
