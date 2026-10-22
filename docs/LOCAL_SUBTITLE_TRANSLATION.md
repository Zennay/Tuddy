# Lokale vertaling van een geopend ondertitelbestand

Open **Gesprekstools → Open SRT/WebVTT…** en kies een ondertitelbestand.
In **Eigen ondertitelconcept · lokaal** staat **Vertaal ondertitelconcept lokaal…**.
Deze handmatige actie vertaalt de tijdelijke tekstregels met ML Kit op het toestel
en behoudt de bestaande tijdcodes.

## Werkwijze

1. Kies de werkelijke brontaal en doeltaal uit de ondersteunde ML Kit-teksttalen.
   Tuddy leidt de brontaal niet automatisch af uit de bestandsinhoud.
2. Tik op **Controleer lokale modellen**. Dit leest alleen de actuele lokale
   modelinventaris. Voor een verschillende bron- en doeltaal moeten de benodigde
   modellen al aanwezig zijn; Engels is ingebouwd.
3. Alleen bij een expliciet bevestigde status **READY** kun je **Vertaal N regels**
   kiezen. Tuddy controleert de modelinventaris opnieuw en vertaalt de cues één
   voor één met één tijdelijke lokale vertaler.
4. De voortgang is zichtbaar en **Annuleer** stopt het volgen. Een vertaalfout,
   annulering of overgang naar de achtergrond past geen gedeeltelijke vertaling
   toe. De bronfile op schijf blijft onaangeraakt.
5. Lees de voorbeeldregels terug en kies **Vervang tijdelijke tekst**. Pas dan
   vervangt de vertaling de tekst in het tijdelijke concept. De tijdcodes blijven
   behouden; de bestaande undo/redo-bediening kan de wijziging herstellen.
6. Bewaar desgewenst daarna via de bestaande SRT/VTT-preview een **nieuwe kopie**.
   Tuddy overschrijft het gekozen origineel niet.

## Grenzen en privacy

- De functie gebruikt alleen reeds geïnstalleerde ML Kit-tekstmodellen. Ze
  downloadt of verwijdert geen model, start geen microfoon en gebruikt geen
  cloudspraak- of online vertaalfallback.
- Een ontbrekend of onbekend model blokkeert de vertaling. Sluit de dialoog en
  bereid het gekozen paar bewust voor via **Offline startcheck → Tekstmodel
  offline voorbereiden → wifi**; open het document daarna opnieuw.
- Maximaal 500 cues. Iedere vertaalde regel moet binnen de bestaande
  ondertiteltekstgrenzen blijven. Bij een ongeldige of mislukte regel blijft
  het hele concept ongewijzigd.
- De gekozen taalcode is een expliciete gebruikerskeuze, geen automatische
  taalidentificatie. Controleer alle vertalingen voordat je ze publiceert.
- Dit gebruikt de ML Kit-teksttaalcatalogus; het maakt **Kurmanji, Sorani of
  Pasjtoe** niet tot ondersteunde vrije offlinevertalingen. De afzonderlijke
  licentie-, native-speaker- en fysieke Android-acceptatiegates blijven open.
