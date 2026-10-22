# Lokale vertaling van een geopend ondertitelbestand

Open **Gesprekstools → Open SRT/WebVTT…** en kies een ondertitelbestand.
In **Eigen ondertitelconcept · lokaal** staat **Vertaal ondertitelconcept lokaal…**.
Deze handmatige actie vertaalt de tijdelijke tekstregels met ML Kit op het toestel
en behoudt de bestaande tijdcodes.

## Werkwijze

1. Optioneel kun je **Stel brontaal voor op basis van dit bestand** kiezen.
   De on-device taalherkenner gebruikt maximaal de eerste 12 niet-lege cues
   (tot 1.200 tekens). De uitkomst is een voorstel; controleer de brontaal zelf.
2. Kies of bevestig de bron- en doeltaal uit de ondersteunde ML Kit-teksttalen.
   Tuddy start geen vertaling zonder deze expliciete taalkeuze.
3. Kies het bereik met **Eerste regel** en **Laatste regel**. Beide nummers zijn
   inclusief en beginnen bij 1. Standaard staat het hele document geselecteerd;
   **Hele bestand kiezen** herstelt dat bereik. Een ongeldig bereik blokkeert
   vertalen. Cues buiten het gekozen bereik blijven onaangeraakt.
4. Tik op **Controleer lokale modellen**. Dit leest alleen de actuele lokale
   modelinventaris. Voor een verschillende bron- en doeltaal moeten de benodigde
   modellen al aanwezig zijn; Engels is ingebouwd.
5. Alleen bij een expliciet bevestigde status **READY** kun je **Vertaal N regels**
   kiezen. N is het aantal cues in het geselecteerde bereik. Tuddy controleert de
   modelinventaris opnieuw en vertaalt alleen die cues één voor één met één
   tijdelijke lokale vertaler.
6. De voortgang telt de geselecteerde regels. **Annuleer** stopt het volgen.
   Een vertaalfout, annulering of overgang naar de achtergrond past geen
   gedeeltelijke vertaling toe. De bronfile op schijf blijft onaangeraakt.
7. Blader met **Vorige voorbeeldregels** en **Volgende voorbeeldregels** door
   de volledige vertaling (vijf cues per pagina) en kies **Alleen vertaling** of
   **Origineel + vertaling**. De tweetalige modus zet het origineel boven de
   vertaling binnen dezelfde tijdgecodeerde cue. Cues buiten het bereik behouden
   tekst en tijdcodes. Als één gecombineerde cue de bestaande tekstlimieten
   overschrijdt, blijft toepassen geblokkeerd.
8. Kies **Vervang tijdelijke tekst** om uitsluitend de geselecteerde cues in het
   tijdelijke concept te wijzigen. De bestaande undo/redo-bediening kan dit
   herstellen. Met **Ander bereik kiezen** kun je de preview weggooien en een
   ander bereik selecteren zonder het concept te wijzigen.
9. Bewaar desgewenst daarna via de bestaande SRT/VTT-preview een **nieuwe kopie**.
   Tuddy overschrijft het gekozen origineel niet.

## Grenzen en privacy

- De functie gebruikt alleen reeds geïnstalleerde ML Kit-tekstmodellen. Ze
  downloadt of verwijdert geen model, start geen microfoon en gebruikt geen
  cloudspraak- of online vertaalfallback.
- Een ontbrekend of onbekend model blokkeert de vertaling. Sluit de dialoog en
  bereid het gekozen paar bewust voor via **Offline startcheck → Tekstmodel
  offline voorbereiden → wifi**; open het document daarna opnieuw.
- Maximaal 500 cues. Het gekozen bereik gebruikt 1-based, inclusieve
  regelnummers; cues buiten het bereik behouden hun tekst en tijdcodes. Iedere
  vertaalde regel moet binnen de bestaande ondertiteltekstgrenzen blijven. Bij
  een ongeldige of mislukte geselecteerde regel blijft het hele concept ongewijzigd.
- De voorgestelde brontaal komt uit een korte on-device steekproef en kan fout
  zijn. Je kunt de taal handmatig wijzigen; modelcontrole en vertalen vereisen
  nog steeds een aparte expliciete actie. Controleer alle vertalingen voordat je ze publiceert.
- Dit gebruikt de ML Kit-teksttaalcatalogus; het maakt **Kurmanji, Sorani of
  Pasjtoe** niet tot ondersteunde vrije offlinevertalingen. De afzonderlijke
  licentie-, native-speaker- en fysieke Android-acceptatiegates blijven open.
