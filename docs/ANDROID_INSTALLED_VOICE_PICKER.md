# Installeerde offline Android-voorleesstem kiezen

Tuddy kan tekst lokaal voorlezen met `LocalCaptionSpeaker`. Het
spraakherkenningsmodel en de tekstvertaler staan hier volledig los van.

## Van talen naar een expliciete stemkeuze

In **Meer functies** op het Tuddy-hoofdscherm kun je **Stem bron** of **Stem
vertaling** kiezen. Ook in **Vertaal geselecteerde tekst** zijn afzonderlijke
knoppen beschikbaar voor beide kanten. Het stemvenster leest pas na jouw
keuze Androids eigen aanwezige Text-to-Speech-stemmen uit.

De lijst omvat alleen stemmen waarvoor Android zelf rapporteert dat ze:

- de relevante **taal** spreken;
- **geen netwerkverbinding** nodig hebben; en
- niet gemarkeerd zijn als **nog te installeren**.

Tuddy maakt een lijst van maximaal tachtig stemmen met hun Android-naam,
taalvariant en kwaliteitswaarde. Dit zijn door Android aangeleverde
eigenschappen, niet onafhankelijk geverifieerde kwaliteit of vliegtuigmodus.

## Stem kiezen, beluisteren en herstellen

Kies **Kies stem** bij een geschikte stem. Tuddy bewaart uitsluitend de
gekozen stemnaam per taal in de lokale appvoorkeuren; er wordt geen
tekstinhoud, audio of herkenningsresultaat opgeslagen. Voorleesknoppen op
het hoofdscherm en in de snelle tekstvertaler gebruiken daarna dezelfde
instelling via de bestaande `LocalCaptionSpeaker`. Een stem verdwijnt
mogelijk na Android-systeemupdates: de app controleert de gekozen naam
telkens opnieuw tegen de actuele lokaal gemelde stemlijst en kiest anders
alleen een andere veilige lokale stem.

**▶ Voorbeeld** leest alleen een korte, vaste voorbeeldzin voor. Er
wordt geen privévertaling of transcript voor die demonstratie gebruikt.
**Herstel automatisch** verwijdert alleen de keuze voor die taal. De
knop **Ververs geïnstalleerde stemmen** laat Android opnieuw kijken.
Sluiten en naar de achtergrond gaan beëindigt de tijdelijke TTS-engine.
Het venster vraagt nooit zelf een stem- of modeldownload aan.

Een taal zonder geschikte lokale stem blijft tekst-only. Tekstvertaling
en Android-spraakherkenning worden door deze instelling niet geactiveerd.

## Android-audiofocus en bescherming bij wisselende headset

Wanneer je bewust een oorspronkelijke zin, vertaling, stemvoorbeeld of
meerdere ondertitelregels lokaal laat voorlezen, vraagt Tuddy eerst
**tijdelijke Android-audiofocus** aan. Weigert Android dat verzoek of
kan de app de uitvoerapparaten niet inspecteren, dan start er geen
spraakuitvoer. De TTS-engine gebruikt dezelfde Android-muziek-/
spraak-audioattributen als de aangevraagde audiofocus.

Android geeft soms de focus aan een oproep of andere app. Bij verlies
van focus stopt Tuddy de actuele voorleesactie, in plaats van door
andere audio heen te blijven spreken. Tijdens het voorlezen kijkt Tuddy
ook of Android een bekabelde, Bluetooth-, USB- of gehoorapparaat-headset
als aangesloten meldt. Wordt de gebruikte headset losgekoppeld of
meldt Android dat audio mogelijk naar de luidspreker overschakelt,
dan stopt het voorlezen automatisch. Een hoofdtelefoon die pas
*nadat* je op Play hebt gedrukt wordt aangesloten, valt ook onder
die bescherming. Android-notificaties kunnen vertraagd aankomen.

Een bewust op Play gestarte voorleesactie **zonder aangesloten headset**
blijft mogelijk: Tuddy kiest niet zelf stilzwijgend een andere
geluidsuitvoer. De Android-systeemroutering bepaalt nog steeds welk
fysiek apparaat audio afspeelt. Deze bescherming is dus **geen
waterdicht bewijs dat er nooit geluid uit de luidspreker komt**.
Voor privémateriaal blijft zelf controleren van de route belangrijk.

De listeners bestaan uitsluitend zolang er echt een door de gebruiker
gestarte TTS-sessie actief is. **Stop**, scherm verlaten, een andere
voorleesactie, een audiofocusonderbreking of de laatste uitgesproken
zin geven de audiofocus en Android-listeners weer vrij. Deze werkwijze
geldt gedeeld voor het gesprek, de snelle Android-tekstvertaler,
stemvoorbeelden en de bestaande ondertitelreeksweergave, zonder
extra Android-permissies en zonder audio-opname, log of upload.

## Privéstand: alleen bij aangesloten hoofdtelefoon

In het bestaande voorleesmenu van een Tuddy-ondertitel, en in
**Vertaal geselecteerde tekst**, is er nu één expliciete schakelaar:
**Alleen met hoofdtelefoon**. Deze instelling geldt voor alle Tuddy-
voorleesacties, ook het stemvoorbeeld en het voorlezen van meerdere
ondertitelregels. De huidige waarde verschijnt als **✓** (aan) of
**○** (uit), zodat je de keuze ook kunt terugdraaien.

- Bij ingeschakelde privéstand weigert Tuddy **vóór de eerste
  voorleesactie** om audiofocus en TTS-afspelen op te starten als
  Android geen aangesloten bekabelde, Bluetooth-, USB- of gehoorapparaat-
  hoofdtelefoon meldt. Ook na het registreren van de headsetlisteners
  wordt dat opnieuw gecontroleerd.
- Bij een door Android gemelde loskoppeling of uitvoerwisseling stopt
  de spraak meteen, net als bij verlies van Android-audiofocus.
- Het omschakelen van de instelling stopt lopend voorlezen vóór de
  nieuwe keuze wordt opgeslagen. Alleen de aan/uit-keuze staat in de
  lokale Android-appvoorkeuren; er worden geen toestelidentificaties,
  gesproken zinnen, gesprekken of audio opgeslagen.
- De instelling staat op bestaande installaties standaard **uit**:
  bewust voorlezen via de luidspreker blijft mogelijk. Als Android de
  privacyvoorkeur niet kan lezen, werkt de toelating **fail-closed**
  alsof alleen hoofdtelefoon is gekozen.

**Belangrijke beperking:** een door Android als aangesloten gemelde
hoofdtelefoon bewijst niet dat de daadwerkelijke TTS-uitvoer daarlangs
loopt. De systeeminstellingen kunnen nog een andere audioroute hebben,
en systeemmeldingen kunnen vertraagd zijn. De privéstand beperkt
onbedoelde luidsprekeruitvoer, maar garandeert die niet volledig:
controleer vóór het afspelen ook zelf de actuele Android-mediaspeaker-
route. Dit is geen fysieke telefoonproef of release-acceptatie.

## Twee talen in één bewuste lokale voorleesactie

In het gesprek en bij **Vertaal geselecteerde tekst** toont het menu
bij een voltooide vertaling nu **Origineel → vertaling** in normaal en
langzaam tempo. Eén expliciete tik leest eerst de oorspronkelijke zin
en daarna de lokale vertaling voor, met de afzonderlijk gekozen
geïnstalleerde Android-stem voor **elke** taal. Deze functie start
geen microfoon, zet geen vertaalmodel aan en bewaart geen audio.

Vóór de eerste uitspraak controleert Tuddy **beide stemmen** en vraagt
het één tijdelijke Android-audiofocus- en headsetbewaakte sessie aan.
Ontbreekt één veilige lokale stem, dan wordt ook het eerste deel
**niet** uitgesproken. Tussen de twee zinnen wisselt de app alleen
naar de al gecontroleerde tweede stem. Na de tweede zin, Stop,
achtergrond, audiofocusverlies, headsetwissel of TTS-fout stopt de
hele actie en wordt audiofocus vrijgegeven. Verouderde callbacks van
een eerdere uitspraak mogen niet alsnog het andere deel starten.

Iedere zin heeft dezelfde grens van 400 zichtbare tekens als een
bestaande losse voorleesactie. In het hoofdgesprek is de optie alleen
beschikbaar als de vertaling echt als voltooid is gemarkeerd; de
geselecteerde-tekstvertaler toont haar uitsluitend bij een resultaat.
De bestaande **Alleen met hoofdtelefoon**-voorkeur blijft voor
de hele tweetalige sessie gelden. Androids lokale TTS-metadata en
audioroute-indicaties zijn geen onafhankelijke vliegtuigmodusproef.

## Grenzen voor acceptatie

Een door de Android-TTS-engine als lokaal gemelde stem geeft **geen**
betrouwbaar bewijs van werkelijke radiostilte, privacy bij externe
Android-voorzieningen, dialectkwaliteit of native-speaker-acceptatie.
Een echte Android 12+ vliegtuigmodustest, rechten-/runtimeonderzoek voor
Kurmanji/Sorani/Pasjtoe, een ondertekende APK en productie-uitrol blijven
afzonderlijke gates.
