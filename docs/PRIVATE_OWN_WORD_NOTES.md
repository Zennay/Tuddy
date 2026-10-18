# Eigen offline woordbetekenissen

De knop **✎ Mijn eigen woorden · offline betekenissen** opent een apart,
lokaal woordenboek met zelf ingevoerde woorden en betekenissen. De gebruiker
kan zelf de woordtaal kiezen, een notitie bewaren, in de eigen woordlijst
zoeken, een betekenis wijzigen of een eigen notitie na bevestiging wissen.

## Onderweg bij een ondertitel

Tik in een originele of vertaalde ondertitel op een woord. De bestaande
**lokale woordkaart** toont de beperkte, gecontroleerde basiswoordenlijst
alleen wanneer dat woord daar echt in staat. Een aparte sectie toont
desgewenst een **mijn eigen notitie**; die is altijd duidelijk als
ongecontroleerd gemarkeerd.

Vul een eigen betekenis in en kies **Bewaar mijn eigen betekenis** om die
notitie daadwerkelijk op te slaan. Een ondertitel, opname of gesprek wordt
niet automatisch naar het woordenboek overgenomen.

## Offline oefenen

**🧠 Oefenkaarten** biedt naast gecontroleerde woorden en zelfgemaakte
zinnen ook de zelfgeschreven woordnotities aan. Bij **Antwoord typen**
vergelijkt Tuddy de tekst uitsluitend letterlijk, zonder beoordeling van
uitspraak, grammatica of de betrouwbaarheid van een eigen betekenis.
Optioneel afspelen gebruikt uitsluitend reeds aanwezige door Android als
niet-netwerk geregistreerde stemmen; geen cloudspraakfallback.

## Grenzen en privacy

- Maximaal **120** zelf ingevoerde notities, ieder met één woord (maximaal
  48 tekens) en maximaal 180 tekens voor de eigen betekenis.
- Woordcodes zijn beperkt tot de expliciete ML Kit-teksttalen en
  Kurmanji/Sorani/Pasjtoe als **eigen notitie**, niet als ASR- of
  vertaalondersteuning. Het selecteren van een woordtaal is geen bewijs
  dat het offline spraak- of vertaalmodel is geïnstalleerd.
- Besturingstekens, ongeldige UTF-16-surrogaten en JSON-schemafouten
  worden geweigerd; een beschadigde verzameling wordt niet stilzwijgend
  overschreven.
- App-private Android-opslag; platformbackup is uitgeschakeld. Geen
  automatische overname van de tijdelijke gesprekstijdlijn, geen
  servercommunicatie of automatische export.
- De bestaande **Wis mijn oefengegevens**-actie vermeldt en wist nu
  ook deze verzameling; andere opslagcategorieën worden individueel
  teruggekoppeld bij gedeeltelijke schrijf-/wisfouten.
- De notities zijn **zelf geschreven en niet geverifieerd door een
  moedertaalspreker**, woordenboekredactie of vertaalmodel.

Tuddy blijft **Incubate**: dit is geen claim voor vrije offline
Kurmanji/Sorani/Pasjtoe-ASR/MT, fysieke telefoonacceptatie of een
getekende Android-productierelease.

## Zelf een back-up maken of herstellen

Via **Mijn eigen woorden → Bewaar back-up…** kun je vrijwillig een
begrensd JSON-bestand met alleen je eigen woord, taalcode en zelf
ingevoerde betekenis op een door jou gekozen Android-opslaglocatie bewaren.
Een provider zoals cloudopslag kan het bestand buiten Tuddy synchroniseren.
Er worden geen live gespreksteksten, audiobestanden of spraakmodellen
meegenomen. Als je woordnotities veranderen terwijl de opslagkiezer open
is, annuleert Tuddy de verouderde export.

Via **Herstel back-up…** kiest de gebruiker zelf een bestand. Tuddy
weigert ongeldige UTF-8-gegevens, onbekende versie/schema, dubbele
woordidentiteiten, niet-toegestane talen, ongeldige tekens en bestanden
boven 50.000 bytes. Vervolgens verschijnt een aparte bevestiging met:

- **Voeg alleen nieuwe woorden toe:** behoud huidige lokaal bewerkte
  betekenissen bij dubbele woorden; schrijf alle toevoegingen atomair.
- **Vervang volledige eigen lijst:** vervang bestaande eigen notities
  uitsluitend na deze afzonderlijke, zichtbare bevestiging.

Een mislukt bestand of een te grote samenvoeging verandert niets aan
de huidige lijst. Na export blijft de externe kopie buiten Tuddy's
verwijderfunctie vallen. Androids platformbackup voor de app blijft uit;
dit is uitsluitend een handmatige, expliciete back-up.
