# Offline woordkaarten in de grote leesmodus

Open in Tuddy de **Leesmodus** of het **groot lezen van een eigen privéarchief**.

- Tik in een **afgeronde** originele zin of **lokaal afgeronde** vertaling op een zichtbaar woord. Tuddy gebruikt dezelfde kleine ingebouwde NL/EN/DE/FR/ES-woordenlijst als de gewone gesprekstijdlijn.
- Op een breed scherm werken woordkaarten in beide kolommen; in een smal scherm werken ze bij originele regels en hun vertaling onder elkaar. Bron- en doeltaalcodes worden afzonderlijk gebruikt. Sorani/Kurmanji/Pasjtoe krijgen **geen verzonnen vertaling of woordenboekbetekenis**.
- Bestaande lokale zoekmarkeringen blijven zichtbaar terwijl je op een woord tikt, ook bij een gevonden fragment.
- Houd een woord ingedrukt voor een tijdelijke preview; tik kort om het lokale woordvenster te openen. Toetsenbord/TalkBack kan het woordvenster via de semantische actie openen en zelf een woord invullen.
- De grote leesmodus toont deze kaarten **zonder automatische opslag**. Een nieuwe persoonlijke uitleg of woordenboekitem wordt niet stilzwijgend bewaard; gebruik daarvoor de bestaande expliciete bewaarroute buiten de grote leesmodus.
- Woorduitspraak is uitsluitend beschikbaar als de luistermicrofoon uit staat en een offline beschikbare Android-stem gekozen kan worden. De voorlopige live-spraakregel gebruikt bewust géén woordkaart, zodat gedeeltelijke herkenning niet als bevestigd wordt behandeld.
- Tekst, geselecteerde woorden, zoekterm en popup-vensters blijven binnen de huidige schermsessie. Er is geen cloudwoordenboek, netwerkfallback of permanente zoekindex.

Dit is een lees- en studieverbetering, **geen** ondersteuning voor volledige offline Kurmanji-, Sorani- of Pasjtoe-ASR/MT en geen vervanging voor fysieke Android-acceptatie.

## Zoeken met Sorani-/Pasjtoe-toetsenbordvarianten

De zoekbalk in de grote leesmodus gebruikt nu dezelfde tijdelijke **zoek-only**
Arabisch/Perzische spellingtolerantie als Tuddy's privéarchief en eigen kaarten:
Arabische/Persische yeh- en kaf-vormen, Unicode-presentatievormen, en optionele
klinkertekens/tatweel/ZWNJ worden genormaliseerd voor de match. Dit geldt voor
originele tekst, reeds lokaal vertaalde tekst en een eventuele handmatige uitleg.
Het handmatige sprekerfilter en de privéarchiefgrens blijven actief.

- De app verandert **geen enkele** originele letter in bron, vertaling,
  notitie, export, archief of bewaarde privékaart.
- Een trefferrij die alleen via een toetsenbordvariant werd gevonden verschijnt
  wel in de resultaten, maar alleen **letterlijke** overeenkomsten krijgen
  een kleurmarkering. Zo wordt geen verkeerde positie als exact woord gepresenteerd.
- Dit is geen dialectherkenning, automatische vertaling of nieuw offline ASR-model.
