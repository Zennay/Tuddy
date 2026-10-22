# Zoeken in eigen Sorani-, Pasjtoe- en Kurmanji-zinnen

## Wat verandert

De zoekvelden voor **Mijn eigen offline zinnen**, **Mijn offline gesprekskaarten**
en **Zoek in mijn eigen zinnen** kunnen nu beperkte toetsenbordverschillen
herkennen. De Arabische yeh (ي), alef maksura (ى) en Perzische yeh (ی)
mogen bij het zoeken onderling overeenkomen. Hetzelfde geldt voor Arabische
kaf (ك) en Perzische keheh (ک). De zoekfunctie negeert optionele Arabische
klinkertekens (harakat), tatweel en ZWNJ. Unicode-presentatievormen worden
voor het zoeken genormaliseerd.

- Dit gebeurt **uitsluitend tijdens het zoeken** naar bestaande,
  door de gebruiker zelfgeschreven kaarten.
- Je opgeslagen Sorani-/Pasjtoe-/Kurmanji-tekst, CSV/JSON-export,
  duplicaatcontrole, spelling, studietijden en betekenissen veranderen niet.
- De resultaten zijn dezelfde bestaande privékaarten, geen kunstmatig
  vertaalde of automatisch gegenereerde zinnen.
- Niet alle mogelijke verschillen in spelling en dialect worden gelijkgemaakt:
  h/ە, hamza en afzonderlijke betekenisdragende letters blijven onderscheiden.
- Zoekindexen bestaan niet op de server of in nieuwe lokale opslag;
  filteren gebeurt direct in RAM.

De bestaande fysieke Android-vliegtuigmodusacceptatie (issue #1) en
rechten/gecontroleerde volledige offline ASR+MT (issue #2) zijn hierdoor
**niet** afgerond.
