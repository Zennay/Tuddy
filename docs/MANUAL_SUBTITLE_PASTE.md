# SRT/WebVTT plakken zonder bestand

Tuddy laat je handmatig een volledig SRT- of WebVTT-document plakken, zonder dat de app zelf je Android-klembord uitleest.

## Direct openen

1. Stop eerst de live microfoon en tik op **Plak eigen SRT/WebVTT-tekst en bekijk voorbeelden…**.
2. Plak de volledige ondertiteltekst in het invoerveld. Geldige tijdregels hebben een begin- en eindtijd. Zowel SRT als WebVTT is toegestaan.
3. Bekijk het aantal gevonden regels en de eerste vijf tijdgecodeerde fragmenten. Alleen als de volledige tekst door de bestaande begrensde parser wordt geaccepteerd, wordt **Open tijdelijk concept** actief.
4. Bevestig expliciet. De regels openen in de bestaande lokale ondertiteleditor. Het gesprek, het bronbestand en de live transcriptie veranderen niet.
5. Je kunt fragmenten aanpassen, afspelen met eigen apart gekozen lokale audio, terugdraaien, oefenen of als **nieuwe** SRT/VTT-kopie opslaan.

## Tweede spoor plakken en samenvoegen

1. Open een SRT/VTT-concept, vanuit een document of de plakroute.
2. Kies in de editor **Tweede ondertitelspoor zelf plakken…**.
3. Plak een tweede compleet document en beoordeel de korte preview.
4. Kies in de bestaande samenvoegpreview tussen **aansluiten**, **originele tijden behouden** of **overlappende tekstparen** (met optionele handmatige tijdsverschuiving).
5. Controleer het voorgestelde resultaat en geef apart toestemming om de samenvoeging alleen in het tijdelijke concept toe te passen. Undo/redo blijft bruikbaar.

## Grenzen en privacy

- Geen automatische clipboard-lezing, netwerk, ASR, vertaling, cloud of extra permissies.
- Maximaal 256 KiB UTF-8 en 500 cues per bron; misvormde tijdcodes en niet-ondersteunde inhoud worden geweigerd.
- De geplakte inhoud blijft alleen in de geopende UI-sessie en verdwijnt bij sluiten of achtergrond. Geen automatische opslag.
- Opslaan is een aparte handeling via de Android-bestandskiezer; een gekozen documentprovider kan zelf cloudsync aanbieden.
- WebVTT-/SRT-tijdcodes en combinaties zijn **handmatig**. De app claimt geen gemeten woord- of audiouitlijning.
- Er is geen claim van fysieke Android-vliegtuigmodusacceptatie, release-ondertekening of volledige Kurmanji/Sorani/Pasjtoe-ASR/MT.
