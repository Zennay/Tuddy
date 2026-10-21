# Unicode-veilig afbreken van eigen ondertitels

In **Lange ondertitelregels afbreken** kun je voor het geopende eigen
ondertitelconcept maximaal 32, 42 of 50 **zichtbare Unicode-tekenclusters**
per regel kiezen. Tuddy gebruikte eerder UTF-16-code-eenheden: emoji met
surrogaatparen en samengestelde letters met diakritische tekens telden dan
meerdere keren en braken tekst onnodig vroeg af.

De breedtebepaling gebruikt nu lokale Unicode-tekenclustergrenzen.
De daadwerkelijke regelafbreking blijft uitsluitend mogelijk op bestaande
**enkele gewone spaties** tussen woorden. Tuddy splitst geen woorden,
Unicode-tekencluster, Sorani/Pashto-schrift, emoji of niet-afbreekbare spaties.
De volgorde, tekstinhoud, milliseconde-tijdcodes en originele bestanden
blijven intact; alleen in een tijdelijk concept worden sommige spaties
expliciet naar regelafbrekingen omgezet.

De preview vermeldt hoeveel cues veranderen en hoeveel visueel te lang
blijven, ook wanneer een woord langer is dan de gekozen breedte. Bestaande
grenzen van acht regels per cue, max. 500 cues, snapshotvalidatie, Undo/Redo
en afzonderlijke Android SAF-opslag blijven gelden. Geen automatische
spraakherkenning, vertaling, netwerktoegang of permissiewijzigingen.
