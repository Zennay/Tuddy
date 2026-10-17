# Tuddy 🎙️

**Tuddy** is the Android-first, privacy-focused realtime translation and language-learning app previously prototyped as **LiveLingo**.

It aims to display speech and its translation together like synchronized lyrics, with a selected target language, automatic source-language detection, scrollable conversation history, and compact word cards. The project prioritizes newcomer and refugee language access.

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
