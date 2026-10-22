# Meerdere eigen privékaarten schrijven vanuit een JSON-archief

Open **Open JSON-archief…**, zoek of blader naar regels en selecteer maximaal
tien regels met **Selecteer voor eigen kaart**. De selectie gebruikt de
oorspronkelijke archiefpositie en blijft behouden als je de zoekterm wijzigt.
Tik daarna op **Schrijf N eigen privékaarten…**.

## Schrijven en opslaan

- De wizard behandelt één geselecteerde regel per stap. De bronzin en een
  eventuele voltooide vertaling zijn alleen zichtbare, niet-geverifieerde
  context. Beide tekstvelden beginnen leeg en worden nooit vooraf ingevuld.
- Kies per kaart expliciet Kurmanji, Sorani of Pasjtoe voor de zelfgeschreven
  zin en Nederlands of Engels voor de zelfgeschreven betekenis.
- Ga met **Vorige kaart** en **Volgende kaart** door de selectie. De app bewaart
  nog niets terwijl je door de kaarten bladert.
- **Bewaar alle N kaarten** is alleen beschikbaar als iedere kaart twee geldige
  tekstvelden en beide taalcodes heeft, de zinnen uniek zijn binnen de batch én
  ten opzichte van het bestaande zinnenboek, en er ruimte is binnen de limiet
  van 80 kaarten.
- De opslag gebruikt één atomaire batchtransactie. Een conflict of een gewijzigd
  archief levert geen gedeeltelijke batch op; los de invoer op en probeer opnieuw.
  Annuleren verwijdert de tijdelijke invoer.

## Privacy en betrouwbaarheid

- De archiefregel is uitsluitend referentie. Geen tekst, modelvertaling, audio,
  timing, sprekerlabel of andere archiefmetadata wordt naar de eigen kaart gekopieerd.
- Er wordt geen vertaling of netwerkverzoek gestart. Alleen de zelfgeschreven
  zin, zelfgeschreven betekenis en gekozen taalcodes worden in het app-privé
  zinnenboek bewaard na de expliciete eindbevestiging.
- Sluiten/vervangen van het archief, achtergrondgebruik, actieve microfoon,
  een onleesbaar zinnenboek of verouderde archiefcontext maakt opslaan onmogelijk.
- De eigen betekenissen zijn niet door Tuddy of een moedertaalspreker
  geverifieerd. De bronbestanden en het geopende archief blijven ongewijzigd.
