# LiveLingo — Android offline-AI subtitles

Spotify-Lyrics-style **live bilingual subtitles** for face-to-face conversations. The spoken text and its translation stay paired in a scrollable timeline. Active speech is highlighted; scroll back without auto-follow snapping you to the bottom, then press **Follow live** to resume.

## Android-first MVP

- **Speech:** Android `SpeechRecognizer.createOnDeviceSpeechRecognizer` (API 31+ only; no cloud fallback). Continuously restarts after individual utterances, with interim and final results.
- **Translation:** Google ML Kit Translation 17.0.3, on-device inference. Press **Prepare offline AI models** once while connected to Wi-Fi. Model download needs internet; inference after installation runs locally.
- **Languages:** searchable target picker for all **59 ML Kit on-device translation languages** (including Arabic, Persian, Ukrainian, Russian, Urdu and Dutch); spoken-language detection is best-effort and device-dependent. Unavailable local languages are shown explicitly as planned.
- **History:** in-RAM transcript (up to 500 finalized captions), timestamped, not saved to disk. Clear empties it instantly. No recording, transcript upload, analytics or server.
- **UX:** prominent active phrase, original + translated text, manual scrolling with sticky **Follow live** control.

## Run on a phone

1. Open this directory in Android Studio with Android SDK 35 and JDK 17. Sync Gradle. (Use Gradle 8.11.1+ compatible with AGP 8.9.2.)
2. Deploy `app` to a phone running Android 12 or newer. In Android settings, install/update the **offline speech recognition** component and the language packs you expect to hear (for instance Dutch and Arabic). On-device recognition availability varies by manufacturer.
3. Connect to Wi-Fi and choose **Translate to**, tap **Prepare offline AI models** (up to three common models initially), and grant microphone permission when starting. Other detected source languages will require a one-time model download on Wi-Fi.
4. Tap **Start live subtitles**, then speak naturally. Spoken-language auto-switch requires Android 14+ and a compatible offline recognizer with downloaded speech packs; on Android 12–13 use the device recognition language. Try airplane mode *after* downloading the speech and translation models to verify that inference works offline.
5. Tap **Stop listening** to stop access to the mic; **Clear transcript** removes history from RAM.

## Privacy / limitations

- **Offline-only transcription path:** this code calls only `createOnDeviceSpeechRecognizer`; `EXTRA_PREFER_OFFLINE` is also set. When no on-device recognizer exists it shows an error rather than silently falling back to a cloud engine.
- **Offline translation after downloading:** ML Kit may make network requests for model installation; the app does not call a translation API. The translation SDK's model management and telemetry behaviors are governed by Google's SDK policies; do not represent this as a formally audited zero-network product.
- No per-word timing or phoneme alignment yet: highlighting is **phrase-level** for live speech, not karaoke-style word-level timing. Android recognition often emits an utterance at a time, with gaps and varying partial-result quality. Speaker identification is not implemented.
- This is foreground-only. Pausing/backgrounding the Activity stops the microphone; no hidden listening service.
- A physical-device APK build and end-to-end phone/airplane-mode validation are still required before claiming deployment compatibility.

## Model tests (no Android SDK needed)

```bash
kotlinc app/src/main/java/com/livelingo/android/model/Conversation.kt tests/ConversationTest.kt -include-runtime -d /tmp/livelingo-tests.jar
java -jar /tmp/livelingo-tests.jar
```

## Next milestones

- Test recognition on Samsung/Pixel in airplane mode and verify permission, speech packs and translation behavior.
- Add local speech backend for unsupported Android devices (e.g., whisper.cpp/Vosk) without cloud fallback.
- Add real per-word timestamps or alignment and optional diarization.
- Add downloadable-source-pack management UI, per-language pack status and diagnostics for each Android speech service.
- Package dedicated GitHub repository and connect to zCloud queue after a separate controlled registry integration.

## Hosted build

The package includes `.github/workflows/android.yml`. After moving the files into the **root of a dedicated GitHub repository**, this workflow builds a debug APK and runs JUnit transcript-state tests on an Ubuntu-hosted runner. It does not require access to zCloud's VPS. `gradle-wrapper.jar` is not bundled: the workflow installs Gradle 8.11.1 via `gradle/actions/setup-gradle`; locally, Android Studio can configure Gradle 8.11.1 and generate a wrapper.

## v0.2 — automatic source + explicit target + compact word peek

- **Translate to:** choose English, Deutsch, Nederlands, Français or Español in a dropdown. The chosen target controls which ML Kit source→target model pairs are downloaded over Wi-Fi. Preparing all four pairs may consume significant storage/data; the UI shows download progress. Select a new target and prepare its models; prior captions retain their own translation-language labels and history is not deleted.
- **Spoken language:** automatic source identification uses bundled ML Kit Language ID (`language-id:17.0.6`) on recognized text. **Android 14+** also requests local microphone language detection and switching between installed speech packs for these five languages using `EXTRA_ENABLE_LANGUAGE_SWITCH`; support varies with the installed device speech engine. On Android 12–13, text identification still works but **speech language switching is not automatic**—the on-device recognizer starts in the phone's language, so foreign speech may be missed/misrecognized. No cloud fallback is introduced.
- **Word peek:** press and hold a word in either original or translated captions. A small, non-focusable overlay displays the word, its local translated equivalent when available and a **genuine bundled definition** for a limited curated glossary, with a tiny emoji illustration. For other words it says the definition isn't available (instead of inventing one). Releasing/canceling the press closes the preview immediately. No Wikipedia requests or new screens; encyclopedia-quality images and full dictionary models are deferred.
- **Data safety:** changing target pauses speech but does not delete past lines. Explicit Clear removes the session. Async translation callbacks are tied to a session generation and unique caption IDs so late completions cannot be attached to new captions.
- **Reality check:** automatic speech switching is a best-effort system feature, not a guarantee on every Android 14+ phone. Pure JVM model tests run without Android; actual offline speech/model downloads, popup touch behavior and Android APK compilation still require Android build + device testing.

### Pure Kotlin verification of the new features

```bash
kotlinc app/src/main/java/com/livelingo/android/model/{Conversation,Languages}.kt tests/LanguageAndWordTest.kt -include-runtime -d /tmp/livelingo-new-tests.jar
java -jar /tmp/livelingo-new-tests.jar
```

## v0.3 — multilingual refugee-focused language learning

**Choose the translation language; original speech is still automatically identified whenever the device recognizer supports it.** The target picker is searchable (native scripts + English names + helpful synonyms such as *Syria*, *Palestina*, *Farsi*, *Oekraïens*) and remembers the chosen target on this phone. The transcript/audio **never** gets stored in preferences.

- **59 on-device translation target languages**, matching the published ML Kit catalog. Includes Arabic (`ar`), Persian/Farsi (`fa`), Dutch (`nl`), English (`en`), Ukrainian (`uk`), Russian (`ru`), Turkish (`tr`), Urdu (`ur`), Bengali (`bn`), Swahili (`sw`), Georgian (`ka`) and many more. Names appear in their own writing systems where useful. See [`docs/LANGUAGE_SUPPORT.md`](docs/LANGUAGE_SUPPORT.md) for the full capability matrix and unsupported languages.
- **Important:** 59 local *translation* languages do not imply 59 local *speech recognition* languages. On-device speech models and automatic spoken-language switching are limited by installed Android speech services and language packs. Android 14+ can request automatic switching for a list of common sources, but the service can ignore it or lack packs. Android 12–13 has no equivalent automatic voice switching. Speech recognition and translation are two separate capabilities.
- **Syrian/Palestinian Arabic:** both are `ar` for written translation. Spoken Levantine dialects (`ar-SY` / `ar-PS`) may not be covered by the device's recognition model. Do **not** market dialect-correct recognition or colloquial translation without real device tests and human evaluation.
- **Not supported by this on-device ML Kit translator yet:** Kurdish Kurmanji and Sorani, Pashto, Somali, Tigrinya, Amharic, Oromo, Rohingya and separate Dari dialect coverage. The picker clearly lists these as **planned/unavailable**, not fake working options. An additional fully-local ASR/translation model family is needed before enabling them. Standard Persian (`fa`) is not an appropriate blanket promise for Dari comprehension.
- **Bounded downloads:** after selecting the target, prepare up to **three** relevant source→target model pairs over Wi-Fi. Never try to install 58 translation pairs. When someone speaks an additional supported language, ML Kit automatically installs *only that needed pair* over Wi-Fi on first use. The sentence may appear untranslated until the model is installed; once installed, inference is on-device. This isn't magically offline for an uninstalled pair.
- **Word learning:** tiny hold-to-peek translation with a small illustrated glossary. Full multilingual dictionary definitions, pronunciation, example sentences and picture search still require properly licensed local data/models; don't fabricate explanations.
- **Quality:** ML Kit uses English as an intermediate language for non-English pairs; evaluate translation carefully, particularly for asylum, legal, medical and administrative conversations. Prefer human interpretation for consequential decisions.

### Test offline models and selectors without Android SDK

```bash
kotlinc app/src/main/java/com/livelingo/android/model/{Conversation,Languages}.kt tests/{ConversationTest,LanguageAndWordTest}.kt -include-runtime -d /tmp/livelingo.jar
java -cp /tmp/livelingo.jar ConversationTestKt
java -cp /tmp/livelingo.jar LanguageAndWordTestKt
```

**Status:** Pure Kotlin tests pass; Android APK build, downloaded-model behavior and real-device spoken recognition across languages still require validation. No installable APK is included in this source package.

## v0.4 — Kurdish Kurmanji, Sorani and Pashto offline beginner-word previews

These three languages now appear as **selectable limited beginner-word targets** in the Android picker. They are not ML Kit languages. An independent, tiny, built-in exact-match phrasebook translates five common expressions from recognized Dutch or English: *hello, thank you, yes, no, water*. No model download is required for these exact entries. Unsupported phrases explicitly fail rather than displaying made-up translations; the remaining 59 ML Kit targets are unchanged.

**Crucial limitation:** this is **not yet** free-form Kurdish/Pashto machine translation, and **not** Kurdish/Pashto speech-to-text. It does not support bilingual live conversations in these languages yet. Android still needs offline Dutch/English speech recognition. We must test the curated wording with native speakers before relying on it in education, healthcare, asylum, legal or emergency situations.

A future truly offline full translation engine can use a properly licensed, quantized NLLB-type model with canonical tags `kmr_Latn` (Kurmanji), `ckb_Arab` (Sorani), `pbt_Arab` (Pashto). This remains **not implemented**; the referenced NLLB-distilled-600M checkpoint uses `CC-BY-NC-4.0` and must not be silently shipped in a commercial product. Streaming offline speech ASR for all three remains a separate, unvalidated integration.

Run the pure-Kotlin phrasebook tests as part of `LanguageAndWordTest.kt`. See [`docs/LANGUAGE_SUPPORT.md`](docs/LANGUAGE_SUPPORT.md) for the capability matrix and next steps.
