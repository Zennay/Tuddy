# Conversation-first Android hoofdscherm

## Waarom

De native app had veel volledige-breedte studie-, import-, export- en
beheerknoppen tussen de luisteractie en de tijdelijke gespreksregels. Op
kleine Android-schermen konden die primaire ondertitels daardoor weinig of
geen resterende hoogte krijgen. De bestaande privacy- en modelbeperkingen
moeten tijdens vereenvoudigen ongewijzigd blijven.

## Nieuwe kernflow

1. **Kies spreektaal en doeltaal direct bovenaan.** Beide bestaande
   zoekbare pickers zijn nu zichtbaar zonder het volledige instellingenpaneel
   te openen. Alleen de ondersteunde Android-spraaklocale-kandidaten verschijnen
   aan de bronzijde. Kiezen installeert geen model of stem.
2. **Controleer offline gereedheid** via de bestaande afzonderlijke
   on-device spraak- en ML Kit-tekstmodelstatus. Dit vervangt geen fysieke
   vliegtuigmodus- of privacyacceptatie.
3. **Luister of typ** vanaf de twee primaire acties. Stop, Wis gesprek,
   Volg live/Pauzeer volgen, Leesmodus en Richting wisselen blijven direct
   bereikbaar. Wissel richting stopt eerst de microfoon, vraagt nieuwe
   modelcontrole en behoudt eerdere regels met hun eigen talen.
4. **Lees tijdelijke ondertitels** in dezelfde hoofdweergave. In een lege
   sessie verwijst de tekst rechtstreeks naar de bovenliggende bron-/doeltaal
   en de twee concrete startacties, zonder te suggereren dat taalkeuze
   volledige offline spraakdekking garandeert.
5. **Meer functies** opent een tot 300 dp begrensd, scrollbaar paneel voor
   alle bestaande studie-, conversatie-, transcriptbestands-, favorieten-
   en privacyopties. De taal-/modelinstellingen staan bovenaan dit paneel
   en gebruiken hun eigen begrensde instellingenweergave. Het paneel is
   standaard dicht en vraagt geen extra opslag of machtigingen.

## Gesprek bewust wissen

De directe **Wis gesprek**-actie vraagt nu eerst afzonderlijk of de gebruiker
de huidige tijdelijke gesprekstekst, correcties en markeringen wil verwijderen.
**Behoud gesprek**, buiten het venster tikken en de Android-terugactie laten
het gesprek intact. Pas na **Wis gesprek definitief** wordt precies de bestaande
alles-in-één opruimroute uitgevoerd: microfoon stoppen, vertraagde callbacks
ongeldig maken, tijdelijke export-/importconcepten annuleren en alle actieve
gespreksregels/markeringen wissen. Deze extra bevestiging beschermt tegen
onbedoelde dataverlies-taps zonder iets naar de cloud te bewaren. Externe,
reeds geëxporteerde kopieën of apart bewust opgeslagen privéwoorden/zinnen
worden niet teruggehaald of verwijderd.

Dit is een broncodewijziging met bestaande GitHub Android-build/CI als
integratiecontrole. Geen nieuw experiment, QA-suite of gemeten
taakprestatie is uitgevoerd. De brede UX-mijlpaal met externe benchmarks,
fysieke gebruikersfeedback en voor/na-metingen blijft apart open. Voor
productie blijven echte Android 12+ toestelproeven, lokale ASR/MT-rechten,
kwaliteit en ondertekende releasegoedkeuring vereist.
