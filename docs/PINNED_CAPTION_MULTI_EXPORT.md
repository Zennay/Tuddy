# Gemarkeerde zinnen bewust apart exporteren

Tuddy bewaart een live gesprek uitsluitend tijdelijk in het appgeheugen.
Tik bij afgeronde gesprekszinnen op de ster om maximaal 80 zinnen te
markeren. Open onder het gesprek **★ gemarkeerd**, bekijk welke uitspraken
meegaan en kies **Exporteer selectie…**. De export is niet beschikbaar
zolang de microfoon actief is.

Kies eerst de **taalrichting**: alle richtingen van dit gesprek of
precies één zichtbaar bron- en doeltaalpaar. Dit is een zuiver
lokaal selectiefilter: het verandert noch de sessie noch het
bewaarde markeringenoverzicht. Verander je de taalrichting tijdens
het externe bestandskiezen, dan wordt geen oud document alsnog
weggeschreven.

Selecteer ook bewust **welke inhoud** je mee wilt nemen: alle
gemarkeerde uitspraken, alleen reeds afgeronde lokale regels, of
uitsluitend regels met een werkelijk voltooide vertaling. De teller
laat vooraf zien hoeveel van je markeringen worden meegenomen; een
lege keuze wordt geweigerd. Niet-afgeronde vertalingen krijgen
nergens een verzonnen vertaalresultaat. Dit filter verandert de
tijdelijke markeringen of het echte gesprek niet.

Selecteer daarna precies één uitvoerformaat:

- **TXT**: leesbare originele regels, taalpaar, tijdstip en alleen werkelijk
  voltooide vertalingen.
- **TSV-leerkaarten**: importeer je daadwerkelijk lokaal voltooide vertaalparen
  in een kaartprogramma dat twee UTF-8-tabgescheiden velden ondersteunt
  (voorzijde en achterzijde). Kies **Bronzin → eigen betekenis**,
  **Eigen betekenis → bronzin** of **beide richtingen** (twee kaarten per
  geldig paar). Status "Lokaal" zonder eigenlijke vertaling telt niet mee.
  Lege of nog niet vertaalde regels worden vooraf uit de teller gehaald
  en komen nooit als verzonnen achterkant in het bestand. Identieke
  bron/vertaling-combinaties binnen hetzelfde taalpaar tellen eenmaal;
  verschillende taalparen worden niet samengevoegd. Het scherm toont
  het aantal unieke tweetalige paren, het effectieve aantal kaartjes
  (bij beide richtingen dubbel) en een leesbare voor-/achterkant-preview.
  De exacte uitvoer is vóór opslaan in de ruwe bestandspreview te bekijken.
  Regeleinden en tabs in eigen teksten worden spaties; mogelijke
  spreadsheetformules krijgen een leidende apostrof en HTML-speciale
  tekens worden als entiteit geschreven. Een studietool kan die tekens
  anders tonen afhankelijk van diens importinstellingen; kies twee
  kolommen en inspecteer het resultaat. Geen automatische kwaliteits-
  of taalcontrole. Je leerkaarten blijven ook zonder verdere
  toestemming *niet* als app-privé studiekaarten opgeslagen.
- **JSON**: dezelfde beperkte selectie in Tuddy's bestaande
  privéarchief-formaat, opnieuw te openen in de read-only archieflezer.
  Niet voltooide vertalingen blijven expliciet leeg.
- **HTML**: afdrukbare, zelfstandig leesbare tweetalige kaartjes; de
  tekst is HTML-geëscaped en haalt geen externe bronnen op.
- **SRT / WebVTT**: een **ruw ondertitelconcept** op basis van de
  geregistreerde eindtijd van zinnen. Kies zelf bron/vertaling/binnen
  beide, eerste tijdverschuiving en maximale weergaveduur. Dit zijn
  geen gemeten start-/eindtijden. Controleer en corrigeer ze in een
  video- of ondertiteleditor. Er wordt geen audio opgenomen of uitgeknipt.

Controleer eerst de lijst gemarkeerde regels, het bron-/vertaalfilter
en het formaat. Het bevestigingsscherm toont ook de berekende
bestandsgrootte en een korte inhoudssamenvatting van de meegenomen
regels. Via **Bekijk exacte bestandsinhoud…** open je afzonderlijk
de volledige, alleen-lezen tekst van het daadwerkelijk te bewaren
TXT-, TSV-, JSON-, HTML-, SRT- of WebVTT-document. Dit gebeurt uitsluitend
in het tijdelijke appgeheugen; je keert daarna terug naar de keuzes.
Een groot document wordt per regel geladen zodat je ook bij veel
ondertitels kunt scrollen. JSON- en HTML-bestanden tonen bewust de
exacte technische tekst, niet een verzonnen gerenderde preview. Daarna verschijnt
de waarschuwing. Pas met **Kies opslag…** wordt de Android
bestandskiezer geopend. Tuddy controleert na terugkeer of dezelfde
zinnen, taalrichtingen, vertaalstatus en markeringen nog actief zijn.
Een gewijzigde selectie, een nieuw taalpaar, ander formaat of
een gewijzigd bron-/vertaalfilter, een gewist gesprek of een gestarte
microfoon breekt de overdracht af; maak dan een nieuwe keuze.
Bij annuleren wordt ook de tijdelijke ruwe preview gesloten.

Bij TSV neemt Tuddy uitsluitend werkelijk voltooide, niet-lege vertaalparen mee.\nDe richtingskeuze hoort bij de bestandskeuze-snapshot: verander je die tijdens\nhet opslaan, dan wordt de oude overdracht geannuleerd.\n\nBij SRT/WebVTT neemt Tuddy uitsluitend afgeronde lokale regels mee.
Met de handmatige keuze **Vertaling** worden zinnen zonder voltooide
vertaling vooraf uit de teller en het inhoudsoverzicht verwijderd;
de getoonde aantallen komen daardoor overeen met de uit te voeren
ondertitelregels.

De maximumgrenzen en invoerbeperkingen van elk bestaand formaat
blijven gelden. Sommige keuzes, bijvoorbeeld vertaling-alleen zonder
voltooide vertaling, leveren terecht geen bestand op. Exporteren maakt
geen automatische vertaling, sprekersherkenning, transcriptopslag of
synchronisatie. Markeringen verdwijnen bij **Wis gesprek**, na
geheugenverloop of bij het sluiten van de app.

**Privacy:** de gekozen Android-bestandsprovider kan je gesprek
mogelijk elders bewaren, delen of synchroniseren. Bewaar nooit
gevoelige inhoud zonder toestemming van de betrokkenen. Een lokaal
gegenereerd bestand is geen geverifieerde transcriptie, en een
broncodebuild is geen bewijs van fysieke offline toestelacceptatie.
