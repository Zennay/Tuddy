# Android-schermlezer: eenmalig en bewust privétekst laten aankondigen

## Waarom

Tuddy heeft live ondertitels, privécorrecties en op het toestel gemaakte
ML Kit-tekstvertalingen. Android TalkBack en andere
toegankelijkheidsdiensten kunnen normale schermtekst zelf verkennen.
Automatisch complete privégesprekken via een Android-toegankelijkheids-
aankondiging sturen is echter een extra gegevensoverdracht aan een mogelijk
extern ontwikkelde dienst. **Dat doet Tuddy niet.**

## Een zin op eigen verzoek aankondigen

- Open op het hoofdscherm **Gesprekstools** en zoek
  **Laatste voltooide gespreksregel · Android-schermlezer**.
  Tijdens microfoonopname, met een actief privacyscherm of zonder
  voltooide regel verschijnt deze actie niet.
- Kies **Lees origineel via TalkBack…** of
  **Lees vertaling via TalkBack…**. Alleen de laatste voltooide
  tekst en de bestaande bron- of doeltaalcode zijn beschikbaar.
- In Androids geselecteerde-tekstvertaler zijn dezelfde twee keuzes
  zichtbaar bij de volledige, voltooide lokale vertaling, niet tijdens
  detectie, modelcontrole, gedeeltelijke vertaling of correctie.
- Een bevestigingsscherm legt uit dat Android de tekst aan een
  ingeschakelde toegankelijkheidsdienst kan aanbieden. De app stuurt
  de tekst **pas na Eenmalig aankondigen**; **Niet delen** maakt niets
  bekend. De zin blijft alleen tijdens dit scherm als tijdelijk concept
  in geheugen.
- Bij bevestiging controleren we opnieuw of het scherm actief en
  zichtbaar is, er werkelijk een schermlezer met aanraakverkenning
  actief is, en exact dezelfde tekst en taalrichting nog getoond
  worden. Een gewijzigde zin moet je opnieuw beoordelen.
- Android ontvangt de aankondiging maximaal één keer voor die tap.
  Tuddy bevestigt alleen het *verzoek* bij Android, nooit of de
  schermlezer daadwerkelijk iets uitsprak, onthield of online verwerkte.

Deze expliciete actie beperkt elke aankondiging tot één regel van
maximaal **600 tekens**. Langere passages worden niet automatisch
ingekort of opgedeeld en verschijnen niet onverwacht in de
toegankelijkheidsaankondiging. De al zichtbare tekst blijft in het
scherm beschikbaar voor eigen TalkBack-navigatie.

## Privacy, stop en grenzen

De functie vraagt **geen extra Android-machtiging**, maakt geen
microfoon- of TTS-sessie aan, stuurt niet naar Tuddy-servers, en
bewaart geen transcript, deelgeschiedenis of aanmeldgegevens.
Bij `onPause`, `onStop`, afsluiten en Compose-dispose wordt een
wachtende bevestiging weggegooid.

Een toegankelijkheidsdienst kan van een derde partij afkomstig zijn,
de privézin zelf verwerken en eventueel opslaan of doorsturen.
Tuddy kan dat niet controleren of ongedaan maken. Gebruik de actie
alleen wanneer je die Android-dienst vertrouwt. De functie is
**geen bewijs van vliegtuigmodusisolatie of fysieke TalkBack-acceptatie**.

De projectgates blijven zelfstandig open: fysieke Android 12+
NL→EN/EN→NL-acceptatie, uitgepakte APK-identiteit op het toestel,
rechten en native-speaker-validatie voor volledige Kurmanji/Sorani/
Pasjtoe ASR+MT, en een getekende productierelease.
