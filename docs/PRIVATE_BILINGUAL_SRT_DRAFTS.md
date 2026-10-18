# Tweetalige SRT-ondertitelconcepten (offline)

Tuddy kan afgeronde zinnen uit de **huidige tijdelijke conversatie** als een
SRT-*concept* bewaren. Open **Gesprekstools** → **Bewaar SRT-ondertitelconcept…**,
lees de aparte toestemmingsvraag en kies dan zelf via de Android-documentkiezer
waar het .srt-bestand terechtkomt.

## Wat erin staat

- Voltooide originele zinnen, met de voltooide lokale vertaling waar aanwezig,
  in **een of twee regels per cue**.
- Geen lopende herkenningshypothesen, mislukte vertalingen, audio, automatische
  sprekeridentiteiten, optionele aantekeningen of privésleutels.
- In volgorde van de gespreksregels, met maximaal 500 items en 256 KiB output.
- Regeleinden en besturingstekens uit individuele zinnen worden verwijderd, zodat
  één zin geen extra SRT-cue kan injecteren.

## Grenzen van de tijdsaanduiding

Androids speech callbacks geven Tuddy de **tijd waarop een herkenningsresultaat
binnenkomt**. Ze geven **geen** begin- of eindtijden van uitgesproken woorden,
geen opnamebestand en geen synchronisatie met een video. SRT-cues worden daarom
gerangschikt volgens de voltooiingstijden; de eerste cue begint bij 00:00:00.
Waar teruglopende klokwaarden of gelijke timestamps voorkomen, bewaart Tuddy
de gespreksvolgorde met minimale tussenruimte. De standaard cue is maximaal
3,5 seconden en raakt een volgende cue niet. **Dit zijn alleen ruwe
montagehulpen, geen audiogetrouwe ondertitels.** Synchroniseer alles achteraf
zelf in je montageprogramma. Ook een afgeronde automatische vertaling kan
fouten bevatten; controleer de inhoud voordat je publiceert.

## Privacy

Het transcript blijft normaal uitsluitend in RAM. De export gebeurt
**niet** automatisch: pas na toestemming wordt een begrensd snapshot voor de
Android-bestandskiezer gemaakt. Bij een gewijzigde gespreksinhoud tussen
kiezen en terugkeren wordt het snapshot afgewezen. De gekozen externe
bestandsprovider kan het SRT-bestand synchroniseren. Eenmaal buiten Tuddy
kunnen wissen van het gesprek of het sluiten van de app dat bestand niet
intrekken. Er is geen cloudspraakfallback, netwerkexport of nieuwe Android-
permissie toegevoegd.

Dit is een **debug-/incubatiefunctie**. Voor productie ontbreken nog de
fysieke Android-vliegtuigmodusproef, vrije Koerdische/Pasjtoe-modellen,
licentie- en native-speakeracceptatie, en release-ondertekening.
