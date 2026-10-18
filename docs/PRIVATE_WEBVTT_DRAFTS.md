# WebVTT-ondertitelconcepten

Tuddy kan vanuit **afgeronde live-gespreksregels** en vanuit een
**expliciet gekozen, read-only JSON-archief** behalve SRT nu ook WebVTT
(`.vtt`) opstellen. De bestaande keuze voor origineel, vertaling of
beide teksten, plus de handmatige verschuiving en maximale tijd per
regel, werkt voor beide formaten.

## Bediening

1. Zet de microfoon uit voordat je een live gesprek exporteert.
2. Open **Gesprekstools → Bewaar ondertitelconcept (SRT/WebVTT)…**.
   Bij een zelf geopend archief gebruik je de overeenkomstige archiefactie.
3. Kies in het bevestigingsvenster **SRT** of **WebVTT**, de gewenste
   tekstvariant en de grove tijdinstellingen.
4. Bevestig dat de inhoud en ruwe timing niet geverifieerd zijn en
   kies vervolgens zelf de Android-bestandslocatie voor `.srt` of `.vtt`.

Geen media wordt gelezen of gesynchroniseerd. De app kent alleen het
moment waarop Android een zin als gereed teruggeeft. Die tijd is
**geen audio-woorduitlijning**, niet het begin van de opname en geen
betrouwbare ondertiteltiming. Corrigeer alle tijdcodes in een editor
voordat je het bestand gebruikt bij een video.

WebVTT gebruikt `WEBVTT` als eerste regel en tijdcodes met
`00:00:00.000 --> 00:00:03.500`. SRT behoudt komma's en de bestaande
tijdscodering. Beide ondersteunen maximaal 500 begrensde regels.
Onvoltooide vertalingen worden nooit verzonnen: in de modus
**Vertaling** ontbreken regels zonder werkelijk voltooide tekst.

Export blijft een uitdrukkelijk handmatige actie via Androids
bestandskiezer. De gekozen opslagprovider kan de inhoud elders
synchroniseren; Tuddy start geen upload. De bestaande controle dat een
live snapshot niet is veranderd voordat het wordt geschreven blijft ook
voor WebVTT actief. Bij privé-archiefexport blijft het vooraf gekozen
momentbeeld intact wanneer de externe bestandskiezer de app op de
achtergrond zet. Tuddy bewaart geen audio of gesprekstekst automatisch.

De nieuwe export betekent **niet** dat de drie Koerdische/Pasjtoe
doeltalen vrije offline ASR of modelvertaling ondersteunen. Toestelproef,
modelrechten en productiesigning blijven aparte acceptatiegates.
