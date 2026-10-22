# Woorden direct bekijken en bewust bewaren in tweeweg-gesprekken

In het **Tweeweg-tekstgesprek** en de uitklapbare **Gesprek groot lezen**
modus zijn de originele en werkelijk lokaal afgeronde vertaalde regels
nu individueel aantikbaar.

Een tik op één woord opent de al bestaande **Lokale woordkaart** uit
de gewone live-tijdlijn. Daardoor blijft het resultaat consistent:
- Woorden uit het kleine ingebouwde woordenboek (NL/EN/DE/FR/ES)
  kunnen een bestaande lokale basisbetekenis tonen en bewust worden
  bewaard in de beperkte, gecontroleerde basiswoordenlijst.
- Voor andere woorden toont Tuddy expliciet dat er geen gecontroleerde
  lokale definitie beschikbaar is. Er wordt **geen betekenis gegokt**
  of naar een online woordenboek doorgeschakeld.
- De gebruiker kan zélf een privébetekenis typen en met een afzonderlijke
  **Bewaar mijn eigen betekenis**-actie opslaan in het bestaande
  app-private woordenboek. Die notitie is uitdrukkelijk niet
  onafhankelijk geverifieerd.
- Een opgeslagen eigen betekenis wordt bij latere woordkaarten
  opnieuw getoond. Woordnotities vallen onder de bestaande
  eigen-woorden back-up, export en algemene wisactie.
- Alleen daadwerkelijk afgeronde lokale vertalingen hebben een
  klikbare vertaling; ontbrekende/onzekere modeluitvoer wordt niet
  als geslaagde vertaling getoond.

De bestaande `OfflineWordText` en
`PrivateOwnWordDeck` worden hergebruikt, inclusief hun grenzen voor
taalcodes, invoerlengte, privacystatus en handmatige opslag.
We voegen geen kopie van gesprekstekst, impliciete opslag,
extra cloudverkeer, microfoonrechten of nieuwe accountgegevens toe.

De functiewijziging raakt niet de open fysieke Android 12+
vliegtuigmodus-, Koerdische/Pasjtoe ASR/MT-, licentie-,
native-speaker- en production-signing-gates.

## Lokale voorlezing van tijdelijke gespreksregels

Bij de laatste zes tijdelijke regels kun je het origineel of een daadwerkelijk
lokaal afgeronde vertaling laten voorlezen. De bijbehorende zijde wordt
tijdelijk gemarkeerd. Als Android's reeds geïnstalleerde niet-netwerkstem een
tekstbereik terugmeldt, wordt alleen dat bereik gemarkeerd; anders blijft de
actieve zijde zichtbaar zonder een woordtiming te verzinnen. De tekstweergave
blijft ook tijdens het voorlezen tappable voor de bestaande offline woordkaart.

Stoppen, een nieuwe beurt, richtingwissel, het openen van de grotere lees- of
oefenweergave en het verlaten van het scherm beëindigen de spraak en wissen
de tijdelijke markering. Geen audio, bereik of afspeelgeschiedenis wordt
opgeslagen; de gesprekregels blijven uitsluitend sessie-RAM.

### Grote leesweergave

Dezelfde volgmarkering is beschikbaar in de grote leesweergave met maximaal
80 tijdelijke regels. Zoeken/filteren, richting wisselen, sluiten, stoppen of
de app op de achtergrond zetten wist de actieve spraakmarkering.
