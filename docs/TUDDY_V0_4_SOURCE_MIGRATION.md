# Tuddy v0.4 source migration map

**Status: partial migration. This document is not a release or device-acceptance claim.**

The historical v0.4 archive used a different source layout from the current app. The current repository has newer implementations of several behaviors, so a matching filename or equivalent feature is not proof that every historical source file was imported byte-for-byte. Keep [issue #1](https://github.com/Zennay/Tuddy/issues/1) open until the remaining source-migration scope and authorized physical-device evidence are resolved.

## Source archive provenance

- Library path: `/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip`
- Size: `42,472 bytes`
- SHA-256: `e7721d41d4f7baf31850e3836d5655cfd3b20c2197752cc4a3ff1078fc57992c`

## Historical modules and current active equivalents

| Historical v0.4 module | Current active implementation | Status / boundary |
|---|---|---|
| `MainActivity.kt`, `LiveLingoScreen.kt`, `MainViewModel.kt` | `app/src/main/java/com/livelingo/android/MainActivity.kt` plus the policy/controller classes below | UI and session behavior evolved into the current activity. The old screen/view-model files are not claimed as verbatim imports. |
| `Languages.kt` | `TargetCatalog.kt`, `LanguageSearch.kt` | Current target catalog and localized search. Catalog discovery does not imply that every model is already installed. |
| `translation/LocalLanguageDetector.kt` | `LocalLanguageDetector.kt` | Session-scoped, bundled on-device identification; merged in [PR #242](https://github.com/Zennay/Tuddy/pull/242). It is advisory text-language identification, not speech recognition or translation-model availability. |
| `translation/OfflineTranslator.kt` | `LocalTranslatorPool.kt`, `OfflineTextModelInventory.kt`, `TypedConversationModelController.kt` | Current finalized-caption path uses a bounded local-client cache; provisional captions retain their short-lived no-download path. Explicit model readiness/download gates remain. Merged in [PR #243](https://github.com/Zennay/Tuddy/pull/243). |
| `speech/OnDeviceSpeech.kt` | `MainActivity.kt`, `OnDeviceSpeechPackController.kt`, `InterimTranslationPolicy.kt` | Current Android speech path has explicit package/device checks and stale-result guards. This does not establish support for every dialect or device. |
| `model/Conversation.kt` | `app/src/main/java/com/livelingo/android/model/Conversation.kt` | Current conversation model is present in the active tree. |
| `model/CommunityPhrasebook.kt` | `app/src/main/java/com/livelingo/android/model/CommunityPhrasebook.kt`, `SafeCommunityPhrasebook.kt` | This remains a five-phrase preview, not free-form MT. |
| Android manifest, Gradle files, resources | Current `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, root `build.gradle.kts`, `app/src/main/res/values/strings.xml` | Current build/configuration is active; it is not a byte-for-byte historical snapshot. |
| `docs/LANGUAGE_SUPPORT.md` | `docs/ARCHIVED_LANGUAGE_SUPPORT_DISCOVERY.md`, `docs/OFFLINE_TEXT_MODEL_READINESS.md` | Current capability and licensing notes are maintained in the newer documentation. |

## Still open

1. **Historical source scope:** the archive's old `LiveLingoScreen.kt`, `MainViewModel.kt`, `Languages.kt`, `OfflineTranslator.kt`, and `OnDeviceSpeech.kt` have not all been imported as verbatim active source files. The current app uses newer equivalents; do not replace them blindly with older implementations that could regress model-download consent or lifecycle safeguards.
2. **Physical-device acceptance:** no authorized Android 12+ airplane-mode acceptance record tied to the exact APK checksum is recorded here. A hosted build is not physical-device evidence.
3. **Full community-language support:** [issue #2](https://github.com/Zennay/Tuddy/issues/2) remains open. Kurmanji, Sorani and Pashto are not full free-form offline ASR+MT targets; the separate five-phrase preview must remain explicitly labeled until appropriate model rights, implementation, native-speaker review and device acceptance exist.

Do not close issue #1 based on this map alone. It records current equivalences and the remaining boundaries so future changes can continue without overwriting newer production code with stale historical modules.


## Archive inspection update — 21 October 2026

The original Library ZIP has now been recovered and extracted for direct inspection; its SHA-256 is unchanged. The exact uncompressed per-file hashes and sizes are recorded in [TUDDY_V0_4_ARCHIVE_INVENTORY.md](TUDDY_V0_4_ARCHIVE_INVENTORY.md). This resolves uncertainty about which files exist in the archive, but does not mean those 23 files have all been copied into the GitHub repository. Current equivalents remain the active implementations; no historical module was blindly overlaid onto the newer privacy/lifecycle architecture. Issue #1 remains open for the remaining source-import work and authorized physical-device acceptance.

## Preserved historical module snapshots — 21 October 2026

The following original v0.4 files are now stored verbatim outside Gradle's active source set, so future comparisons can inspect the old behavior without replacing newer runtime code:

- `archive/v0.4/LiveLingo-Android/app/src/main/java/com/livelingo/android/translation/OfflineTranslator.kt` — source SHA-256 `f1518efb40014395a54518ba71fb0f409bc585a78488db433f732b92699afa5f`.
- `archive/v0.4/LiveLingo-Android/app/src/main/java/com/livelingo/android/speech/OnDeviceSpeech.kt` — source SHA-256 `6105869988bb1453b9b960e13d1d9bfec059ee113f2c6524e759696e165bf896`.

These are archival references only; they are intentionally not part of the active Gradle source set. The archive's `LiveLingoScreen.kt`, `MainViewModel.kt` and `Languages.kt` still require safe source-by-source preservation or equivalent mapping. Issue #1 also remains open for the authorized physical Android 12+ airplane-mode acceptance; this snapshot does not satisfy that gate.
