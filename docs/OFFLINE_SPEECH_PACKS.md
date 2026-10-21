# Android offline speech language-pack setup (source delivery)

Voor een doorzoekbaar overzicht van tekstdoelen, spraaklocale-kandidaten en niet-ondersteunde taalverzoeken, zie [de taalondersteuningsgids](LANGUAGE_SUPPORT_GUIDE.md).

**Scope:** On Android 13+ Tuddy can now ask the device's **on-device**
`SpeechRecognizer` about the actual installed, pending, supported and
online-only languages for the selected **spoken source**. It can request a
missing supported speech model **only when the user explicitly taps** the
Wi-Fi button. Android 12 does not expose per-language support introspection;
show `unknown`, never invent installed coverage.

## In-app workflow

1. Pick a spoken source candidate (NL, EN, DE, FR, ES, Arabic, Farsi,
   Ukrainian, Turkish, Russian or Urdu). New choices are locale hints, NOT
   proof that this phone has an installed on-device recognition pack.
2. Tap **Controleer spraak**. The on-device recognition service returns one
   of: installed, download offered, pending, online-only, unsupported or
   unknown. The app does not create the normal network speech recognizer.
3. Only for **download offered**, connect to Wi-Fi and tap **Download via wifi**.
   Before every model request, Tuddy checks that the active network is Wi-Fi.
   This is an admission check, **not** a guarantee about future Android
   transport policy if Wi-Fi drops. Android itself controls the download.
4. Download errors expose **Opnieuw via wifi** rather than trapping the
   user in an error state. During a lengthy status check or OS-managed
   download, **Stop volgen** cancels Tuddy's progress observer, not an
   Android-owned queued download. If Android reports a known missing,
   pending, unsupported or online-only pack, Tuddy blocks the microphone
   start for that selected locale and gives a visible remedy.
5. Android 14+ shows on-device service progress/scheduled/success/failure.
   After success Tuddy **checks installed coverage again**, rather than
   trusting a progress receipt. Android 13 can request download but cannot
   track completion in this API; use **Controleer spraak** again.
6. Android 12 and any service not implementing the API remain
   unknown/unsupported rather than falsely installed. On Android 12 the offline
   startcheck offers **Open Android spraakinstellingen** so users can inspect or
   install the system speech service's offline packs; return to Tuddy and retry
   the check. This action does not prove that a pack is installed. The microphone
   path remains on-device-only and subject to the device's own speech service.
7. On source change, Stop, Activity background, or screen disposal,
   stale callbacks from the manager are invalidated and the manager-owned
   recognizer destroyed. No microphone samples, transcript, model contents,
   URLs, or downloaded status are written to disk.

## Model compatibility and community languages

**Text translation, speech recognition and five community phrases are three
different capabilities.** A downloaded ML Kit text translation pair is never
interpreted as proof of speech coverage. Similarly, seeing `en-US` installed
is no proof that a generic `en` pack, another locale or a switch from Arabic
to Pashto is reliable. The requested tag must appear exactly in Android's
on-device installed list; otherwise users see a partial/unknown status.
The five manually curated preview expressions for Sorani and Pashto use
explicit right-to-left typography in the result panel; Kurmanji remains
left-to-right Latin script.

Kurmanji (`kmr_Latn`), Sorani (`ckb_Arab`, right-to-left) and Pashto
(`pbt_Arab`, right-to-left) **remain a five-phrase manual-only preview**;
they are not offered as spoken input or complete ML Kit translation targets.
Do not download NLLB 600M distilled model weights into a commercial release
without addressing CC-BY-NC-4.0 licensing. A future full feature requires
a license-verified on-device ASR+MT pack, source and target support matrix,
quantized Android runtime, bounded memory/storage and native-speaker review.
Do not label the languages "supported" before real, airplane-mode, native
Android 12+ device and independent human accuracy acceptance.

## User-facing limitations

- On-device Android service may refuse support checks and model downloads.
  Tuddy must show that limitation and must not silently switch to cloud STT.
- Android 13 service-managed download has no progress listener and cannot
  be paused by Tuddy. Android 14 may return `scheduled` or progress. Tuddy
  cancels **its own callbacks**, not Android's queued system download.
- The offline speech model itself can be installed without a speech request
  succeeding or producing an accurate transcript. This code is **not** a
  device acceptance claim.
- Medical, asylum, legal and emergency contexts require a qualified human
  interpreter. No autonomous advice or approval is produced by this app.

Source: Android 13+ `SpeechRecognizer.checkRecognitionSupport()`;
Android 13+ `triggerModelDownload(Intent)`; Android 14+
`triggerModelDownload(Intent,Executor,ModelDownloadListener)`.
Documentation: https://developer.android.com/reference/android/speech/SpeechRecognizer


## Privacy-safe readiness diagnostics

The offline readiness dialog includes an explicit **Kopieer diagnoserapport** action. The report contains the app version, Android API/device model, airplane-mode state, selected language codes, speech-service availability, speech/text-model readiness states, the source SHA, and GitHub Actions run ID (local builds are marked `local`). The CI APK artifact also contains `build-manifest.txt` with source SHA, workflow run ID/attempt, event/ref, APK SHA-256 and byte size. Match the report's source/run ID to that external manifest; the APK checksum cannot be embedded in the APK itself. It never includes transcript text or audio. Copying is user-triggered and marks the clipboard content as sensitive; the user can review it before sharing.


## Alle lokale spreektaalpakketten bekijken

Via **Offline startcheck → Bekijk offline spreektaalpakketten** opent een
alleen-lezen overzicht van de 13 expliciete Android-spraaklocale-kandidaten.
**Controleer alle talen** bevraagt de lokale Android-spraakdienst sequentieel,
één tijdelijke recognizer tegelijk. Per exacte regionale taalcode zie je
*aanwezig*, *download aangeboden*, *in behandeling*, *alleen online*,
*niet ondersteund* of *onbekend*. Er wordt niets automatisch gedownload, en er
wordt geen microfoonopname of cloud-herkenning gestart.

**Kies als spreektaal** stopt eventuele lopende luistersessie, bewaart de
nieuwe bronkeuze indien lokale voorkeuren schrijfbaar zijn, maakt de eerdere
spraak-/tekstmodelstatus ongeldig en keert terug naar de individuele offline
startcheck. Een catalogusstatus van een eerdere taal wordt niet klakkeloos
overgenomen; de gebruiker controleert opnieuw vóór het luisteren.

**Stop controle**, sluiten, schermachtergrond en lifecycle-opruimen vernietigen
de tijdelijke controller en negeren verlate callbacks. Dit is geen externe
telemetrie of permanent modelregister. Android 12 kan geen individuele
spraakpaketten detecteren en toont dus uitsluitend een onbekende status;
een Android-locale-kandidaat bewijst nooit volledige offline taalondersteuning.
Kurmanji, Sorani en Pasjtoe blijven uitsluitend handmatige vijf-zinnenpreview.


## Veilig verder na Android-spraakinstellingen

Na een bewuste tik op **Open Android spraakinstellingen** bewaart Tuddy
uitsluitend in tijdelijk app-geheugen de geselecteerde spreektaal.
Bij terugkeer naar de nog draaiende app opent automatisch opnieuw de
offline startcheck en worden het gekozen spraakpakket en het bestaande
lokale tekstmodelpaar **alleen-lezen** gecontroleerd. Er start geen microfoon,
automatische modeldownload of netwerkspraak. Bij volledig afsluiten van de
app vervalt deze handoff. De instellingenknop is ook bereikbaar wanneer
Android `downloadbaar`, `in behandeling`, `alleen online`, `niet ondersteund`
of een downloadfout meldt. Dat betekent nooit dat de taal offline werkt.

Het overzicht van alle spraakpakketten slaat een onbeantwoorde aanvraag
na **8 seconden** over met status *onbekend*. Daardoor blokkeert één
niet-reagerende Android-spraakdienst niet de rest van de taalkeuzes.
Bij **Stop controle**, sluiten en achtergrond gaan worden de tijdslimieten
en tijdelijke on-device herkenningsdienst opgeruimd. Geen van deze
acties start een download of bewijst offline accuratesse op een toestel.
