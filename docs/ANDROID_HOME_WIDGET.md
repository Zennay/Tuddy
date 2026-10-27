# Tuddy Android-startschermwidget

Deze optionele widget biedt drie veilige, snelle ingangen op Android 12+:

1. **Typ lokaal** opent Tuddy met een lege, expliciete tekstinvoer.
2. **Offline check** opent het bestaande overzicht van lokale spraak- en
   vertaalmodellen en netwerkstatus.
3. **Eén zin klaarzetten** kiest de eenmalige spraakmodus. Opname begint pas
   nadat de gebruiker in de app zelf op **Luister** tikt en de bestaande
   toestel-/privacycontroles dit toestaan.

## Toevoegen

Open in Tuddy **Meer functies · instellingen, oefenen en bestanden → Android-startscherm** en tik op
**Widget op startscherm aanvragen**. Als je launcher directe plaatsing
ondersteunt, verschijnt een Android-bevestiging; een aangenomen verzoek is
nog geen bewijs van daadwerkelijke plaatsing. Tuddy toont bij terugkeer naar
de app hoeveel widgets Android voor deze app registreert.

Werkt dit niet, houd een lege plek op het Android-startscherm ingedrukt, kies **Widgets**,
zoek **Tuddy · snelle acties**, en sleep de widget naar het startscherm.
Beschikbaarheid en plaatsing verschillen per launcher. Een launcher zonder
directe pinondersteuning mag het handmatige Widgets-menu blijven aanbieden.
De aanvraag werkt alleen als expliciete actie in de voorgrond en wordt niet
automatisch herhaald.

## Vergroten of verkleinen

Houd de geplaatste widget ingedrukt om hem via je launcher kleiner of
groter te maken. Android 12+ kiest automatisch de compacte indeling
(korte knoppen, volledige schermlezerlabels) of de normale indeling.
Beide formaten behouden **Typ**, **Offline check** en **Eén zin**.
Er is geen achtergrondverversing of extra netwerkverkeer voor resize.
De kleinste widgetgrootte hangt af van de launcher.

De widget zelf vraagt geen microfoontoestemming, toont geen privétekst en
bewaart geen gesproken audio, vertalingen, geschiedenis of klembordwaarden.
Iedere knop gebruikt dezelfde gecontroleerde ingang als de bestaande
Android-appsnelkoppelingen. Het is geen offline-modelinstallatie of
toestelacceptatie.

## Nog fysiek te beoordelen

Controleer op een geautoriseerd toestel ook compact/normaal
resizen, portret/landschap, alle drie de knoppen en schermlezerlabels.

Controleer op een geautoriseerd toestel ook de zichtbare pin-aanvraag,
het onderscheid tussen aangevraagd en daadwerkelijk geplaatst, het
geplaatste-widgetaantal na terugkeer uit de launcher en de handmatige
fallback bij ontbrekende ondersteuning.

Controleer op een geautoriseerd Android 12+-toestel of alle drie de knoppen
hun eigen scherm openen, ook na koude start en wanneer Tuddy al openstaat;
controleer met vliegtuigmodus dat één zin geen opname begint zonder een
aparte tap. Noteer eventuele afwijkingen zonder issue #1 als afgerond te
markeren. GitHub CI/build is geen bewijs van deze fysieke ervaring.

## Android-snelinstelling · Offline check

Tuddy kan ook één optionele knop aanbieden in **Snelinstellingen** (het paneel
dat je opent door tweemaal vanaf de schermbovenkant omlaag te vegen).
Die tegel opent direct de bestaande **Offline startcheck**, waar je de
aanwezige lokale spraak- en tekstmodellen, permissies en verbinding kunt
bekijken. Dit is een navigatieactie, **geen** aan-/uitknop voor offline
spraak of vliegtuigmodus.

- Op **Android 13+**: Tuddy → **Meer functies** → **Android-startscherm** →
  **Voeg Offline check toe aan Snelinstellingen**. Android vraagt toestemming;
  Tuddy onderscheidt *aanvraag verzonden*, *toegevoegd*, *al aanwezig* en
  *geweigerd*. Alleen de Android-callback bevestigt plaatsing.
- Op **Android 12** en wanneer een fabrikant de automatische tegelplaatsing
  niet ondersteunt: open Snelinstellingen → **Bewerken** en sleep
  **Tuddy · Offline** naar de actieve tegels.
- Tik op de tegel om de offline check te openen. Is het toestel vergrendeld,
  dan laat Tuddy **eerst door Android ontgrendelen**. Annuleren van het
  ontgrendelen opent Tuddy niet.

De tegel heeft geen toegang tot gesproken tekst, het klembord of
modelinhoud. Alleen het tonen van Snelinstellingen doet niets behalve de
statische knop tekenen. Een tik start **geen opname, download,
vertaling, netwerkactie of toestemmingsverzoek**. Alle daadwerkelijke
acties vragen nog steeds om een afzonderlijke gebruikerskeuze in Tuddy.

Bij een geautoriseerde fysieke toestelproef moeten zowel de handmatige
tegelplaatsing (Android 12) als de systeemaanvraag (Android 13+) waar
beschikbaar, een tik na koude/warmestart, vergrendeld toestel,
toegankelijkheidslabel en terugkeer naar de app worden beoordeeld.
Een succesvolle CI-debugbuild bewijst die toestelervaring niet.
