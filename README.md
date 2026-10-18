# Tuddy 🎙️

**Tuddy** is the Android-first, privacy-focused realtime translation and language-learning app previously prototyped as **LiveLingo**.

It aims to display speech and its translation together like synchronized lyrics, with a selected target language, automatic source-language detection, scrollable conversation history, and compact word cards. The project prioritizes newcomer and refugee language access.

## Nieuw: privégesprek exporteren als JSON

Naast de bestaande tekstexport heeft het gesprekscherm nu **Bewaar JSON…**.
Je kiest zelf via Android waar de JSON met maximaal 500 afgeronde regels
wordt opgeslagen.
Met **Open JSON-archief…** kun je een zelf gekozen, eerder opgeslagen bestand
begrensd en alleen-lezen bekijken, zonder het lopende gesprek te vervangen.
Het archief heeft nu ook een **tweetalige zoekbalk**, resultaatentelling en
per uitspraak datum/tijd; zoekopdrachten blijven volledig lokaal in RAM.
Ongeldige, gewijzigde of te grote archieven worden geweigerd; de inhoud
is door de gebruiker aangeleverd en geen bewijs van echte spraakopname. Per regel blijven originele taal, doeltaal, tijdstempel,
originele tekst en een eventuele **voltooide** vertaling gekoppeld.
Nog lopende of mislukte vertalingen worden expliciet `null`, niet stilzwijgend
als voltooid beschouwd. Export is opt-in; er wordt niets automatisch
gesynchroniseerd of bewaard. Het gekozen bestandsplatform kan **wel**
privégesprekken buiten Tuddy synchroniseren. Lees
[de JSON-format- en privacyuitleg](docs/PRIVATE_TRANSCRIPT_JSON.md).
Dit is broncode-functionaliteit, geen bewijs van een toestelinstallatie.

## Status — 2026-10-17

**Native Android debug APK available; prototype NOT device-validated or released.**
Source `main@b1a298a907f8ff2f7459b9c934f56fafb462f502` passed hosted
[Android APK CI #39064114978](https://github.com/Zennay/Tuddy/actions/runs/39064114978)
(`testDebugUnitTest` + `assembleDebug`).
Artifact **`Tuddy-debug-apk`**, GitHub ID **12112063195**, contains the debug APK,
its SHA256 receipt and the exact source-commit receipt. **APK SHA256:**
`fdc2dc8d4ec7de7293afca9ce3ed1aad832e21c3d0ba1aedef2210bc3b120bf6`.
This checksum is for `app-debug.apk`, not for the enclosing ZIP.
A successful hosted build does **not** prove Android 12+ airplane-mode behavior,
microphone usability, speech model coverage, device performance or release readiness.

### Implemented and validated in source

- Native Android on-device-only `SpeechRecognizer`, continuous captioning across
  sentences with bounded retries for silence and fail-closed permission/language
  errors. Background, stop and session clearing cancel queued restarts.
- ML Kit **on-device text** translation for 59 supported target languages
  after required model downloads. Offline speech recognition coverage remains
  independent and device-/speech-pack-specific.
- Bilingual live/interim/final captions; 'Volg live' respects the user's pause,
  follows growing interim text and works with a 500-line RAM-only history.
- The chosen spoken source and text target survive process restarts as two
  validated private preference codes (restoring unknown/stale codes falls back
  to reviewed defaults); microphone audio, captions and word lookups remain
  RAM-only and are never restored from storage.
- Small, offline, explicitly **five-phrase** NL/EN → Kurmanji/Sorani/Pashto
  preview. This is NOT arbitrary-sentence translation or offline recognition.
- Tap-to-define bilingual text with accessible manual word entry, Unicode
  selection and a small curated offline NL/EN/DE/FR/ES glossary. Unknown words,
  Arabic/Kurdish/Pashto dictionary coverage and sensitive medical/legal text
  never receive invented explanations. Captions preserve per-line language IDs.
- Source integration PRs [#9](https://github.com/Zennay/Tuddy/pull/9),
  [#10](https://github.com/Zennay/Tuddy/pull/10),
  [#11](https://github.com/Zennay/Tuddy/pull/11),
  [#12](https://github.com/Zennay/Tuddy/pull/12),
  [#13](https://github.com/Zennay/Tuddy/pull/13),
  [#14](https://github.com/Zennay/Tuddy/pull/14),
  [#15](https://github.com/Zennay/Tuddy/pull/15),
  [#16](https://github.com/Zennay/Tuddy/pull/16) and
  [#17](https://github.com/Zennay/Tuddy/pull/17). Current integrated main
  has **29/29** Python quality checks passing in an isolated checkout.
  These complement, not replace, hosted Android unit tests.

### New source feature: on-device speech-pack preparation (P1 #2 partial)

- Android 13+ now has a user-initiated **Controleer spraak** action
  reporting Android's actual installed/pending/downloadable/online-only
  on-device source language packs. Unavailable coverage stays unknown.
- A missing supported pack gets an explicit **Download via wifi** action.
  The device recognizer manages model provisioning; Android 14+ progress
  is visible, Android 13 requests must be checked again. Status is
  invalidated after a language switch or background lifecycle.
- It is still **not** free-form Kurmanji, Sorani or Pashto ASR/translation.
  Those languages remain visibly limited to the five-phrase preview.
  See [offline speech pack guide](docs/OFFLINE_SPEECH_PACKS.md).
- Device execution and end-to-end airplane-mode speech remain **unverified**;
  issue #2 stays open until independently licensed local ASR + MT actually
  works for all three communities.

### Additional offline setup: actual ML Kit text-model inventory

A second operator action, **Controleer tekst**, now queries the on-device
ML Kit downloaded translation models for the selected pair. The screen
shows exactly which non-English models are missing. After **Model via wifi**
Tuddy automatically reads the inventory again; completing a download
request is not by itself a readiness claim. Stale callbacks are canceled
on Stop, backgrounding and language changes. Model inventory and speech
readiness remain independent.

See [offline text readiness](docs/OFFLINE_TEXT_MODEL_READINESS.md).

### Open release and product gates

1. Use [docs/DEVICE_VALIDATION.md](docs/DEVICE_VALIDATION.md) with the exact
   APK checksum above on a **physical Android 12+ device**. Record ten
   airplane-mode utterances, multiple continuous restarts, model setup,
   language/permission lifecycle, TalkBack word card and session privacy;
   do not claim a device result until that evidence exists.
2. Finish full historical Android v0.4 source integration. The original
   ZIP remains in ChatGPT Library at
   `/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip`;
   the imported conversation and phrasebook modules do NOT constitute a
   wholesale migration of its previous UI/viewmodel/speech architecture.
3. Deliver separately licensed and on-device-tested free-form Kurdish
   Kurmanji, Sorani and Pashto **speech recognition AND translation**
   with native-speaker/medical/legal accuracy review. Five beginner phrases
   are not that implementation. Keep [issue #2](https://github.com/Zennay/Tuddy/issues/2)
   open and labels honest.
4. Keep [issue #1](https://github.com/Zennay/Tuddy/issues/1) and zCloud production
   admission/release holds open until an actual device acceptance and owner
   approval; no automatic VPS workload or signed production deployment.

Notion project: https://app.notion.com/p/3f89e19ac955810aa4a5e53871966f8f

Earlier project tracking: https://github.com/Zennay/zCloud/issues/1344 and https://github.com/Zennay/zCloud/issues/1350

## Privacy and accuracy

No cloud transcription fallback is intended. Text/model downloads may require internet once. Accuracy for legal, asylum, and medical contexts is not guaranteed; a qualified interpreter or native-speaker review remains essential.


### Offline woorden bewaren en oefenen (nieuw)

Tik op een woord in een afgeronde oorspronkelijke of vertaalde ondertitel om de
**lokale woordkaart** te openen. Alleen als de kleine, handmatig gecontroleerde
NL/EN/DE/FR/ES woordenlijst de betekenis daadwerkelijk bevat, verschijnt
**Bewaar woord**. Bewaren is uitdrukkelijk opt-in. Via **Mijn woorden** kun je
woorden offline terugvinden, jezelf oefenen door betekenissen te onthullen,
woorden afzonderlijk verwijderen of na bevestiging de hele lijst wissen.

Alleen maximaal 120 unieke `taal|woord`-sleutels worden opgeslagen in private
Android appvoorkeuren; de betekenis komt telkens opnieuw uit de ingebouwde
woordenlijst. Er wordt **geen gesprek, audio, contextzin, providerantwoord,
persoonlijke identiteit of gespreksgeschiedenis** mee opgeslagen, en er is geen
cloud-woordenboek of netwerkverzoek. Android-backup blijft uitgeschakeld.
Gesprekken blijven standaard RAM-only; de bestaande expliciete export is een
afzonderlijke gebruikersactie. Niet bekende woorden en Koerdische/Pasjtoe
uitdrukkingen mogen geen verzonnen definities krijgen. Apparaatacceptatie en
native-speaker review zijn nog niet bewezen.


### Nieuwe offline woordoefening

Via **Mijn woorden → Oefen opgeslagen woorden** kun je nu echt oefenen
met maximaal 20 eerder bewaarde, lokaal gecontroleerde woorden per ronde.
Na **Toon betekenis** beoordeel je zelf **Ik wist het** of **Nog oefenen**;
lastige woorden keren maximaal tweemaal terug. Je ziet je voortgang en
eindoverzicht, kunt een nieuwe ronde doen of de volgende set oefenen.
Oefenresultaten blijven alleen in RAM gedurende het oefenvenster;
gesprekken, audio, scores en leerhistorie gaan nooit naar een server
of het bestaande JSON-back-upbestand. Dit is geen automatische
spaced-repetition-over-dagen of formele taaltoets.
[Uitleg en beperkingen](docs/OFFLINE_WORD_PRACTICE.md).

### Woorden oefenen én back-uppen op eigen verzoek

De bestaande knop **Mijn woorden** biedt nu ook **Bewaar back-up…** en
**Herstel back-up…** voor alleen de zelf opgeslagen taal/woord-sleutels
uit het kleine lokale, handmatig gecontroleerde woordenboek.
Herstel controleert eerst bestandsversie, bekende woorden, dubbelen en
UTF-8-inhoud, toont hoeveel woorden geladen worden en vraagt vervolgens
apart om de bestaande lijst werkelijk te vervangen. Annuleren raakt de
woorden niet. Definities komen altijd uit de lokaal geïnstalleerde
woordenlijst, niet uit het gekozen bestand. Geen gesprekken, audio of
persoonlijke zinnen worden geëxporteerd of geïmporteerd. Android vraagt
zelf om een opslaglocatie; een externe bestandsprovider kan wel
synchroniseren. Zie [woordback-uphandleiding](docs/PRIVATE_WORD_BACKUP.md).
Fysieke toestelacceptatie en release-APK blijven afzonderlijk vereist.

### Voorlezen van afgeronde vertalingen (op eigen verzoek)

Bij een definitieve lokaal vertaalde regel kun je nu op
**Spreek vertaling uit** tikken. Tuddy vraagt pas dan de standaard
Android TextToSpeech-dienst om **reeds geïnstalleerde** stemmen te tonen.
De app accepteert uitsluitend stemmen die die dienst zelf als
`isNetworkConnectionRequired=false` en niet als 'not installed' markeert,
voor de herkende doeltaal. Als geen passende stem beschikbaar is, wordt
er niets afgespeeld; Tuddy start geen stemdownload, server-TTS of
stilzwijgende online fallback. De microfoon moet uitstaan voor de knop
werkt. **Stop stem**, Stop, achtergrond en app-afsluiting ruimen de
spreekdienst op.

Dit is een **Android-gerapporteerde offline-capaciteit**, geen
onafhankelijk bewijs dat alle apparaat-/fabrikantspraakdiensten volledig
zonder netwerk werken. Een fysiek toestel en vliegtuigmodus zijn nog
niet gevalideerd. Niet alle 59 ML Kit-teksttalen hebben lokaal een
geïnstalleerde Android-stem; Kurmanji/Sorani/Pasjtoe krijgen geen
verzonnen tekst- of spraakondersteuning. TTS-uitspraak is ongeschikt als
enige basis voor gevoelige medische, juridische of asielbesluiten.


### Eigen Kurmanji-, Sorani- en Pasjtoe-zinnen offline bewaren

Open onder **Offline basiswoorden** de knop
**Mijn eigen Kurmanji/Sorani/Pasjtoe zinnen**. Je kunt daar maximaal
80 persoonlijke zinnen met je **eigen** Nederlandse of Engelse
betekenis typen, bewust opslaan, terugzoeken, individueel verwijderen
of na bevestiging allemaal wissen. Kurmanji gebruikt Latijns schrift;
Sorani en Pasjtoe krijgen expliciet RTL-tekstweergave. De enige
permanente data zijn door jou bewust ingevoerde zinnen in de
app-private Android-opslag; Android-backup blijft uitgeschakeld.
Uit het gesprek wordt **niets automatisch opgeslagen** of naar deze
lijst overgenomen; de app verstuurt geen van deze zinnen naar een server.

Deze zelfgeschreven en niet-gecontroleerde notities worden **nooit
voorgesteld als automatisch vertaalde resultaten**. De apart gelabelde
vijf ingebouwde beginnerzinnen blijven dezelfde beperkte,
handmatig gereviewde preview. Er worden geen Koerdische/Pasjtoe ASR-
of ML Kit-vertaalmodellen gedownload of voorgewend. Vrije offline
spraak en vertaling voor alle drie talen, native-speaker controle,
modellicenties en toestelacceptatie uit
[issue #2](https://github.com/Zennay/Tuddy/issues/2) blijven open.
Gebruik eigen notities zonder menselijke tolk niet voor gevoelige
medische, juridische of asielbeslissingen.


### Privé back-up en herstel voor eigen zinnen

In **Mijn eigen offline zinnen** kun je nu expliciet **Bewaar back-up…**
kiezen. Android vraagt via de Storage Access Framework-bestandskiezer
welke locatie het JSON-bestand krijgt; de app schrijft nergens anders.
**Herstel back-up…** accepteert alleen het eigen
versie-1-formaat, maximaal 50 KiB / 80 zinnen, strikte UTF-8,
de bekende taal- en betekenisvelden en slechts de originele zelf
ingevoerde teksten. Beschadigde, onbekende en gedupliceerde gegevens
worden geweigerd voordat er opslag verandert. Een geldig bestand laat
eerst het aantal ingelezen zinnen zien en vraagt apart of bestaande
privézinnen werkelijk moeten worden vervangen; annuleren verandert niets.

Geen extra opslag- of internetmachtiging en geen automatische
achtergrondback-up. Een door jou gekozen cloudbestandsprovider kan
de back-up buiten Tuddy synchroniseren. Exporteer geen gevoelige
persoonsgegevens. De zelf ingevulde betekenissen zijn en blijven
niet-gecontroleerde persoonlijke notities, **geen** nieuwe vertaalmodellen.
