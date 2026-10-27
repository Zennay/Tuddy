# Afbeelding bekijken en draaien vóór offline tekstherkenning

## Eén expliciete, gedeelde Android-flow

Vanaf Tuddy's hoofdscherm, de Android-fotokiezer, een door Android gedeelde
afbeelding of een zelf genomen cameravoorvertoning verschijnt eerst een
**tijdelijke privévoorvertoning**. De gebruiker kan de leesrichting aanpassen
met **↶ 90° links** en **↷ 90° rechts** (ook 180° en 270° via meerdere tikken),
of de afbeelding zonder OCR weggooien.

De knop **Herken deze afbeelding · lokaal** is de afzonderlijke bewuste
start van het ingebouwde Latin-script ML Kit OCR-model. Het gekozen aantal
graden wordt ook doorgegeven aan `InputImage.fromBitmap(bitmap, rotationDegrees)`:
het is dus niet alleen een visueel gedraaide foto. Na herkenning blijft de
bestaande blokselectie, leesvolgorde-correctie, review/undo, lokaal voorlezen,
tekstbestand/export en expliciete vertaalhandoff intact. De gecorrigeerde
tekst wordt niet opnieuw uit de bitmap afgeleid door roteren: eerst de
foto beoordelen, daarna één OCR-poging op precies die oriëntatie.

## Eerst een deel van de foto uitsnijden

Als een screenshot of een foto veel irrelevante tekst bevat, kun je vóór
tekstherkenning bewust **Bovenste helft**, **Onderste helft**, **Linkerhelft**,
**Rechterhelft** of **Middelste helft** kiezen. Dat zijn tijdelijke en
duidelijk benoemde uitsneden, geen automatisch bepaalde OCR-regio's. De
uitsnede wordt op een werkthread gemaakt van de reeds gecontroleerde,
maximaal 1600 × 1600 RAM-preview. Tuddy past eerst de gekozen draaihoek
fysiek toe, zodat boven/links bij de uitsnede overeenkomt met wat je
zojuist op het scherm zag.

**De voorvertoning wordt vervangen door de echte uitgesneden bitmap.**
Controleer de nieuwe uitsnede vóór je afzonderlijk op
**Herken deze afbeelding · lokaal** tikt. De draaihoek staat na een
toegepaste uitsnede weer op nul. Je kunt de uitsnede nogmaals verkleinen;
wil je terug naar de oorspronkelijke hele foto, kies die dan zelf
opnieuw met Android. Te kleine/ongeldige of door geheugendruk geweigerde
uitsneden veranderen de getoonde foto niet. Er is nooit een onzichtbare
fallback waarbij de hele foto alsnog wordt herkend.

De afbeelding blijft alleen tijdelijk in RAM; na geslaagde uitsnede
wordt het eerdere, grotere bitmapobject vrijgegeven. Geen crop-data,
coördinaten, afbeelding, provider-URI of uitgeknipte inhoud worden door
Tuddy opgeslagen of geüpload. Achtergrond, verlaten en een andere foto
ongeldig maken nog lopend previewwerk. De bestaande OCR-werkbudgetten
en de daaropvolgende menselijke tekstcontrole blijven intact.

## Toestel-, privacy- en geheugenbegrenzingen

- De fotokiezer mag een provider met cloudopslag aanbieden; dat bepaalt de
  provider, niet Tuddy. Tuddy verwerkt uitsluitend de ene door de gebruiker
  gekozen `content://`-afbeelding en stuurt die zelf nergens heen.
- Picker- en gedeelde foto's worden via de bestaande `ImageDecoder` op een
  IO-thread gedecodeerd naar een RAM-only voorbeeld van maximaal 1600 × 1600
  pixels en maximaal 12 MB bitmap-geheugen. Een bestandsprovider die niet
  binnen 12 seconden antwoordt, laat de voorvertoning vervallen; het onderliggende
  providerwerk kan later nog eindigen en wordt niet als herkende tekst verwerkt.
- De bestaande systeemgeheugencheck en één-voor-één-ML Kit OCR-werkbudget
  blijven gehandhaafd. Ook een preview start nooit automatisch een zware
  herkenningsaanvraag. OCR heeft nog zijn eigen 20-seconden-zichtbare
  deadline; een late on-device callback mag het scherm niet heropenen.
- De afbeelding, gekozen draaihoek en geselecteerde URI worden niet in een
  Bundle, Intent-extra, bestand, voorkeur, transcript of externe service
  bewaard. De bestaande Activity `onPause` / `onStop` maken deze tijdelijke
  inhoud onzichtbaar en annuleren lopend previewwerk. Bij procesherstart moet
  de gebruiker bewust opnieuw een afbeelding kiezen.
- Een Android-camera met `TakePicturePreview` levert een kleine thumbnail,
  geen nieuwe hoge-resolutiefoto. Het handmatige draaien kan herkenning
  verbeteren als de camera een verkeerd georiënteerd voorbeeld teruggeeft,
  maar herstelt geen onscherpe of te kleine letters.
- ImageDecoder kan EXIF-oriëntatie van gekozen foto's zelf al normaliseren.
  De door de gebruiker gekozen hoek is een **extra** rotatie bovenop de
  zichtbare voorvertoning. Dit geeft geen toestemming voor uploads,
  downloaden van OCR-modellen of andere scripts.

## Acceptatie blijft apart

De bestaande GitHub Android build/source-head-controles bevestigen hooguit
bron-integratie en APK-compilatie. De foto-/camerakiezer, gedeelde
URI-grants, zichtbaar roteren, lokale OCR-nauwkeurigheid en
RAM-only privacy moeten op een fysiek Android 12+-toestel apart worden
waargenomen. Het is geen bewijs voor volledige vliegtuigmodusacceptatie,
rechtengecontroleerde Koerdisch/Pasjtoe-modellen of een getekende
productierelease.
