# Lokale vertaling van een geopend ondertitelbestand

Open **Gesprekstools → Open SRT/WebVTT…** en kies een ondertitelbestand.
In **Eigen ondertitelconcept · lokaal** staat **Vertaal ondertitelconcept lokaal…**.
Deze handmatige actie vertaalt de tijdelijke tekstregels met ML Kit op het toestel
en behoudt de bestaande tijdcodes.

## Werkwijze

1. Optioneel kun je **Stel brontaal voor op basis van geselecteerde regels**
   kiezen. De on-device taalherkenner gebruikt maximaal 12 niet-lege cues uit
   de actuele selectie (tot 1.200 tekens). De uitkomst is een voorstel; controleer
   de brontaal zelf.
2. Kies of bevestig de bron- en doeltaal uit de ondersteunde ML Kit-teksttalen.
   Tuddy start geen vertaling zonder deze expliciete taalkeuze.
3. Kies een expliciete selectie met 1-based regelnummers, bijvoorbeeld
   `1-5,8,11-12`. Een los nummer selecteert één cue; een bereik is inclusief.
   Standaard staat het hele document geselecteerd en **Hele bestand kiezen**
   herstelt dat. Je kunt ook in **Zoek in dit document** zoeken en daarna
   **Vertaal N gevonden regels lokaal…** kiezen om precies de overeenkomende
   cues vooraf te selecteren. De zoekfilter herkent ook Arabische/Perzische
   toetsenbordvarianten, zonder de getoonde geselecteerde cues te herschrijven.
   Ongeldige, dubbele of overlappende nummers blokkeren
   vertalen. De samenvatting toont de genormaliseerde selectie en het exacte aantal
   cues. Alle cues buiten de selectie blijven onaangeraakt.
4. Tik op **Controleer lokale modellen**. Dit leest alleen de actuele lokale
   modelinventaris. Voor een verschillende bron- en doeltaal moeten de benodigde
   modellen al aanwezig zijn; Engels is ingebouwd.
5. Alleen bij een expliciet bevestigde status **READY** kun je **Vertaal N regels**
   kiezen. N is het aantal cues in het geselecteerde bereik. Tuddy controleert de
   modelinventaris opnieuw en vertaalt alleen die cues één voor één met één
   tijdelijke lokale vertaler.
6. De voortgang telt de geselecteerde regels. **Annuleer** stopt het volgen.
   Een vertaalfout, annulering of overgang naar de achtergrond past geen
   gedeeltelijke vertaling toe. Foutmeldingen verwijzen naar de oorspronkelijke
   regelnummers in het bestand, ook als je niet-aaneengesloten cues selecteerde.
   De bronfile op schijf blijft onaangeraakt.
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

## Vastlopende Android-vertaling herstellen

De vertaling van een ondertitelbestand verloopt bewust **alles-of-niets**:
geen enkele geselecteerde cue wordt tussentijds in het bestaande concept
geschreven. Vanaf het begin van de tweede, interne modelcontrole wacht
Tuddy maximaal **30 seconden** op een bevestigde modelstatus. Iedere
daarna afzonderlijk ingediende cue krijgt weer **maximaal 30 seconden**
voor het lokale ML Kit-resultaat. Bij een uitblijvende callback of
andere fout stopt de tijdelijke batch met een duidelijke melding;
het **hele concept en alle tijdcodes blijven ongewijzigd**, ook
wanneer veel eerdere geselecteerde regels wel waren vertaald.

Om het toestel niet te overladen deelt de ondertitelvertaler nu hetzelfde
procesbrede plafond van **drie daadwerkelijk nog actieve ML Kit-taken**
als de live ondertitelvertaler. Na **Annuleer** of een time-out kan
Androids reeds ingediende ML Kit-task fysiek nog bezig zijn: Tuddy
maakt pas weer een werkplek vrij als die taak echt voltooid is.
Als alle plekken bezet zijn, wordt de volgende cue **niet**
aangeboden en verschijnt een veilig foutbericht. Nieuwe vertalingen
worden niet op de achtergrond in een wachtrij geplaatst of
automatisch herhaald.

Bij een fout van een specifieke cue verschijnt **Bekijk alleen mislukte
regel N**. Dit verandert uitsluitend de beoogde selectie naar het
**oorspronkelijke regelnummer** en vraagt opnieuw om bewuste controle
van de geïnstalleerde modellen. Dat is geen automatische herhaling
van de vorige batch. Wil je het hele bereik alsnog opnieuw proberen,
laat dan de vorige selectie staan, controleer de modellen weer en
start expliciet opnieuw. Na een succesvolle batch krijg je nog steeds
de volledige voorbeeld- en bevestigingsstap voordat iets in het
tijdelijke document wordt gewijzigd.

Deze limieten zijn **app-controles**, geen garantie dat een externe
Android/ML Kit-taak binnen 30 seconden daadwerkelijk stopt. Zonder
fysieke Android-toestelmeting vormen de meldingen geen bewijs van
volledig offline functioneren.

## Grenzen en privacy

- De functie gebruikt alleen reeds geïnstalleerde ML Kit-tekstmodellen. Ze
  downloadt of verwijdert geen model, start geen microfoon en gebruikt geen
  cloudspraak- of online vertaalfallback.
- Een ontbrekend of onbekend model blokkeert de vertaling. Sluit de dialoog en
  bereid het gekozen paar bewust voor via **Offline startcheck → Tekstmodel
  offline voorbereiden → wifi**; open het document daarna opnieuw.
- Maximaal 500 cues. De selectie gebruikt 1-based regelnummers en ondersteunt
  losse cues en inclusieve bereiken zoals `1-5,8,11-12`; dubbele of overlappende
  regelnummers worden geweigerd. Cues buiten de selectie behouden hun tekst en
  tijdcodes. Iedere vertaalde regel moet binnen de bestaande ondertiteltekstgrenzen
  blijven. Bij een ongeldige of mislukte geselecteerde regel blijft het hele concept
  ongewijzigd.
- De voorgestelde brontaal komt uit een korte on-device steekproef en kan fout
  zijn. Je kunt de taal handmatig wijzigen; modelcontrole en vertalen vereisen
  nog steeds een aparte expliciete actie. Controleer alle vertalingen voordat je ze publiceert.
- Dit gebruikt de ML Kit-teksttaalcatalogus; het maakt **Kurmanji, Sorani of
  Pasjtoe** niet tot ondersteunde vrije offlinevertalingen. De afzonderlijke
  licentie-, native-speaker- en fysieke Android-acceptatiegates blijven open.
