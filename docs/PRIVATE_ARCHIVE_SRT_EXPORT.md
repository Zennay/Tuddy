# Eigen JSON-gespreksarchief als SRT-concept bewaren

Tuddy laat je via **Gesprekstools → Open JSON-archief…** een eerder
bewust opgeslagen privégesprek bekijken zonder het in een live
microfoonsessie te laden. In dit venster kun je nu optioneel
**Bewaar dit archief als SRT-concept…** kiezen.

Een aparte bevestiging toont hoeveel van de archiefregels werkelijk als
voltooid zijn aangemerkt. Alleen de afgeronde oorspronkelijke zinnen en
waar aanwezig afgeronde vertalingen worden opgenomen. Onvoltooide
vertalingen worden **niet** verzonnen of stilzwijgend als voltooid
gepresenteerd. De bestaande, strikt ingelezen JSON-archiefrecords gaan
rechtstreeks naar dezelfde begrensde SRT-conceptconverter als een
live gesprek.

## Grenzen van timing

Tuddy heeft alleen de tijd waarop Android een spraakherkenningsresultaat
leverde. De oorspronkelijke begintijd van geluid of woorden is onbekend,
ook voor geïmporteerde archieven. Het resultaat bevat dus uitsluitend
**geschatte tijdposities**, geen bewijs van audiosynchronisatie.
Lijn alle SRT-cues handmatig uit met het juiste audiobestand in een
video-editor.

## Privacy

Het archief wordt alleen gelezen via een door jou gekozen lokaal of
extern document. Het blijft in de bestaande read-only-weergave en
wordt niet aan de microfoon/tijdlijn toegevoegd. Na expliciete
exporttoestemming houdt Tuddy de gekozen begrensde SRT-bytes tijdelijk
in geheugen vast voor de Android-bestandskiezer. Wanneer de kiezer
opent, kan Android het archiefvenster sluiten; de bevestigde
exportopdracht kan toch worden afgerond wanneer de documentkiezer
terugkeert. Bij annuleren worden de tijdelijke bytes weggegooid.

De gekozen bestandsprovider kan het SRT-bestand buiten Tuddy opslaan,
synchroniseren of delen. Het later wissen van de gesprekstijdlijn
kan deze externe kopie niet intrekken. Er worden geen extra
Android-permissies, opnamebestanden of cloud-API's gebruikt.

Dit levert geen fysieke toestelacceptatie, getekende productie-APK of
onafhankelijk bewezen Kurmanji/Sorani/Pasjtoe ASR/MT.

## Zelfde SRT-exportkeuzes als een live gesprek

Ook bij een geopend JSON-archief kies je **Beide**, **Origineel** of
**Vertaling**, een ruwe startverschuiving en een maximale cue-duur.
Het scherm toont hoeveel archiefregels bij de huidige tekstkeuze
beschikbaar zijn. Alleen volledig afgeronde vertalingen tellen mee
voor **Vertaling**. Geen van deze keuzes doet alsof Tuddy de
werkelijke audiotijdstempels heeft gemeten.
