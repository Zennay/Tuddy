# Compatibele SRT- en WebVTT-tekstbestanden openen

**Gesprekstools → Open SRT/WebVTT…** ondersteunt nu ook gewone
ondertitelbestanden van externe montageprogramma's, naast de door
Tuddy zelf gemaakte ondertitelconcepten.

De streng begrensde lezer accepteert een UTF-8-BOM, SRT-indexregels,
WebVTT-cue-identifiers, tijdstempels met **HH:MM:SS.mmm** of
**MM:SS.mmm** en gebruikelijke VTT-instellingen achter de tijdcode,
zoals `align:start`. VTT `NOTE`-, `STYLE`- en `REGION`-blokken
worden veilig overgeslagen: ze worden niet als ondertiteltekst
ingelezen, geëxporteerd of geïnterpreteerd.

De werkelijke gekozen cue-tekst wordt als gewone, niet-uitgevoerde
tekst weergegeven. Een WebVTT-marker of markup wordt dus geen
HTML/script, geen afgedwongen speaker-identiteit en geen
geverifieerde vertaling. Bij het exporteren van een nieuw
ondertitelconcept schrijft Tuddy bewust een eenvoudig,
portabel SRT/VTT-resultaat zonder overgenomen metadata of
onbetrouwbare opmaakclaims.

Strikte limieten blijven van kracht: maximaal 256 KiB aan
bestandsinhoud, 500 cue-regels, 8 tekstregels en 2400 tekens per
cue, geldige oplopende starttijden. Een onbekend of ongeldige
tijdcode leidt tot weigering; het bestaande live gesprek en
het oorspronkelijke externe bestand worden niet veranderd.
