# Tuddy 🎙️

**Tuddy** is the Android-first, privacy-focused realtime translation and language-learning app previously prototyped as **LiveLingo**.

It aims to display speech and its translation together like synchronized lyrics, with a selected target language, automatic source-language detection, scrollable conversation history, and compact word cards. The project prioritizes newcomer and refugee language access.

## Status — 2026-10-13

**Prototype, not a released APK.** Existing Kotlin/Jetpack Compose v0.4 Android source is tracked in ChatGPT Library at `/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip`, and is being imported into this repo. Android device/build validation has not yet been completed.

The existing prototype implements on-device Android speech recognition when supported by the device, ML Kit offline text translation (downloaded models) for 59 supported target languages, searchable target-language selection, and a scrolling bilingual timeline. Offline speech recognition is *not* guaranteed for every language/device.

**Kurdish Kurmanji, Kurdish Sorani and Pashto** are separate selectable **limited phrasebook** languages (five beginner phrases from Dutch or English) in v0.4. They are **not** full speech-recognition or arbitrary-text translation languages yet. This requires licensed offline MT models, appropriately supported offline ASR, native-speaker evaluation and Android device tests.

## Next milestones

1. Import the Android v0.4 sources and Gradle files into the repository.
2. Make the GitHub Actions Android debug build reproducible; upload installable debug APK.
3. Test airplane-mode speech, translation, language selection, permissions, transcript clearing and scrolling on physical Android devices.
4. Add fully offline Kurdish Kurmanji/Sorani and Pashto speech/translation, with honest language-availability labels.

Notion project: https://app.notion.com/p/3f89e19ac955810aa4a5e53871966f8f

Earlier project tracking: https://github.com/Zennay/zCloud/issues/1344 and https://github.com/Zennay/zCloud/issues/1350

## Privacy and accuracy

No cloud transcription fallback is intended. Text/model downloads may require internet once. Accuracy for legal, asylum, and medical contexts is not guaranteed; a qualified interpreter or native-speaker review remains essential.
