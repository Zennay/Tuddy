# Gedeelde lokale tekstgrens · speech → interim → vertaling → uitvoer

## Waarom

Android SpeechRecognizer, de lokale ML Kit-tekstvertaler en de handmatige
teksteditor voeren tekst aan bij één tijdelijk gesprek. Een providerresultaat
dat als *geslaagd* is gemeld, is daarmee niet automatisch een veilige
Unicode-string voor ondertitels, spraak, klembord of Android delen.
Een misvormde UTF-16-sequentie, verborgen besturingsteken of onbegrensde
provideruitvoer kan verwarrende tekst, vastgelopen UI of een onbedoelde
uitvoer veroorzaken.

## Eén consistente app-core-regel

`LocalTextBoundaryPolicy` is een stateless, RAM-only toelatingsgrens en
verwijdert geen inhoudelijke woorden. Hij weigert:

- lege of uitsluitend witruimtetekst en tekst over de relevante limiet;
- ongepaarde UTF-16-high/low-surrogaten;
- NUL en niet-afdrukbare ISO-besturingstekens behalve tabs en
  regelafbrekingen in echte tekstvelden;
- verouderde expliciete Unicode-bidi-overrides (U+202A–U+202E) waarmee
  de visuele volgorde van een bericht kan worden misleid.

**Normaal RTL-schrift**, de letters en diacritica ervan, joiners,
taaltekens en moderne directionele isolates blijven intact. Dit is
geen taalcorrectie, vertaalreview of filter voor beledigingen.

Dezelfde grens geldt aan de volgende bestaande productinterfaces:

| Pad | Maximaal | Gedrag bij weigering |
|---|---:|---|
| Android-spraakherkenningshypothese | 1.500 UTF-16 units | Probeer de volgende echte hypothese; voeg niets verzonnens toe |
| Handmatig getypte gesprekszin | 1.500 | Tekst blijft ter correctie staan; voeg geen regel toe |
| Voorlopige live ML Kit-vertaalhint | 1.500 | Toon de ongeldige hint niet; wacht op de definitieve regel |
| Definitieve lokale gespreksvertaling | 6.000 | Toon **Fout** op dezelfde regel; het origineel blijft en kan opnieuw geprobeerd worden |
| Android geselecteerde-tekstvertaling | 8.000 | Geen voltooid resultaat, kopie of deelactie beschikbaar |
| Android lokale TTS / ondertitelreeks | 400 per zin | Stop of weiger zonder audiouitvoer via een ongeldige zin |

Regelafbrekingen worden voor spraakvoorbeelden samengevoegd tot
witruimte; in vertaalteksten blijven ze bewaard. Er wordt nergens
stilzwijgend afgekapt. De bestaande revisie-, timeout-,
gespreksgeschiedenis-, lokale-model- en fysieke-taaklimieten blijven
gehandhaafd. Gecorrigeerde tekst wordt nog steeds expliciet door
de gebruiker gecontroleerd.

## Geen verkeerde acceptatieclaim

Een groene Android-build toont dat de integratie compileert. Alleen
een fysieke Android 12+-test kan de daadwerkelijke lokale herkenning,
offline-modellen, uitspraak en privacy op een toestel beoordelen.
De open gates voor NL↔EN in vliegtuigmodus, onafhankelijke review,
volledige Kurmanji/Sorani/Pasjtoe-modellen en licenties, UX-metingen,
getekende release en productie-admission blijven afzonderlijk.
