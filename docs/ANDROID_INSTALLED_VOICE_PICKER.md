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

## Grenzen voor acceptatie

Een door de Android-TTS-engine als lokaal gemelde stem geeft **geen**
betrouwbaar bewijs van werkelijke radiostilte, privacy bij externe
Android-voorzieningen, dialectkwaliteit of native-speaker-acceptatie.
Een echte Android 12+ vliegtuigmodustest, rechten-/runtimeonderzoek voor
Kurmanji/Sorani/Pasjtoe, een ondertekende APK en productie-uitrol blijven
afzonderlijke gates.
