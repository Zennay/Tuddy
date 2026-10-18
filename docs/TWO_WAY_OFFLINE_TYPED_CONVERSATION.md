# Tweeweg-tekstgesprek — zonder microfoon

Via het hoofdscherm **⇄ Tweeweg-tekstgesprek · zonder microfoon** kunnen twee
mensen om de beurt een zin typen. Er wordt **geen microfoon gestart**, ook
niet wanneer Android geen lokale spraakherkenner heeft. De getypte zinnen
verschijnen in dezelfde tijdelijke tweetalige tijdlijn als de andere
ondertitels, inclusief lokaal resultaat, status en gekozen taalrichting.

## Talen en richting

- Kies afzonderlijk **Van** en **Naar** uit de 59 ML Kit-teksttaalcodes.
  Dit is nadrukkelijk een **tekstkeuze** en geen claim dat Android ook
  lokaal gesproken woorden in deze talen kan herkennen.
- Tik op **Wissel wie typt** om beide teksttalen om te keren. Standaard
  wisselt de invoerrichting ook na het toevoegen van een beurt; dat kan
  worden uitgezet voor meerdere zinnen achter elkaar.
- De door de gebruiker gekozen **brontaal** wordt bij getypte berichten
  niet door automatische korte-tekst-taaldetectie overschreven. Elke
  tijdlijnregel houdt zijn eigen bron- en doeltaal.
- Kurmanji (`kmr`), Sorani (`ckb`) en Pasjtoe (`ps`) komen **niet**
  in de vrije ML Kit-teksttaalcatalogus voor. De bestaande beperkt
  gecontroleerde vijf beginnerswoorden blijven daar los van staan.

## Offline modellen

Het gespreksvenster controleert of de twee geselecteerde lokale
**tekstmodellen werkelijk op het apparaat** aanwezig zijn. Gebruikers
kunnen na een aparte tik **Modellen via wifi…** kiezen. Alleen deze
knop mag via de bestaande ML Kit-vertaler ontbrekende taalmodellen
opvragen; alleen het typen of omdraaien van een beurt start **nooit**
een netwerkdownload. De gebruiker krijgt een echte lokale
voorraadcontrole na een geslaagde downloadmelding.

Ontbreekt een model, dan bewaart de huidige RAM-tijdlijn de oorspronkelijke
zin met een onvoltooide status; Tuddy verzint geen betekenis of
cloudfallback. Tekstkwaliteit, dialectherkenning en geschiktheid voor
gevoelige gesprekken worden niet gegarandeerd.

## Privacy en grenzen

- Invoer maximaal 1.500 UTF-16-tekeneenheden per beurt.
- Hoogstens de bestaande 500 RAM-gespreksregels. Het venster toont de zes
  recentste regels en alleen echt lokaal voltooide vertalingen als resultaat.
- Conceptinvoer wordt niet opgeslagen en verdwijnt bij annuleren,
  achtergrond of verlaten van het venster. De tijdlijn blijft in RAM totdat
  de gebruiker zijn bestaande wisactie uitvoert of de app wordt gesloten.
- Geen automatische export of kopie; de bestaande expliciete Android
  opslag-/deeltoestemmingen blijven vereist.
- Aparte, optionele opslag van taalkeuzes voor **spraak** blijft ongewijzigd;
  de tekstgespreksrichting kan spraakmodellen niet stilzwijgend wijzigen.

Dit is geen fysieke Android-toestelacceptatie, native-speakerreview,
vrij Koerdisch/Pasjtoe spraak-/tekstmodel of release-signing.
