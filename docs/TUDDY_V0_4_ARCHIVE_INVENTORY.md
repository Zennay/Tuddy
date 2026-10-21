# Tuddy v0.4 archive inventory

This inventory records the historical source archive that was recovered from the Library and extracted for inspection on 21 October 2026. It is a provenance index, **not** a claim that the archive has been fully backported to the active Android app or that the device-acceptance issue is closed.

- Original Library path: `/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip`
- Archive size: `42,472` bytes
- Archive SHA-256: `e7721d41d4f7baf31850e3836d5655cfd3b20c2197752cc4a3ff1078fc57992c`
- Unpacked regular files: `23`
- Root folder in the archive: `LiveLingo-Android/`

## Per-file SHA-256

Hashes are for each file's original uncompressed bytes, not the enclosing ZIP. Paths are relative to `LiveLingo-Android/`.

| SHA-256 | Bytes | Relative path |
|---|---:|---|
| `a77a9cfc03e4a6c9f3ac1ab8b66edbdf45d679eefd0617dccc7e6041834ddb18` | 327 | `settings.gradle.kts` |
| `f65c2274b7f7c198671bb614da9a037e423967013c65dfcb85f5ab8736445fff` | 59 | `.gitignore` |
| `1df553bc4ca0f8d14c45baebb13c5a3f4c4d949be18799c1f99e8829581c4cee` | 217 | `build.gradle.kts` |
| `f6cc7a75920c7f48ea0cc79b5268215c17845db6f1bf164ec260e66d5a65e0b6` | 921 | `.github/workflows/android.yml` |
| `fa078c27c9e4bde28a029d673875b028884505efa436b68cf60e1ac0be5b679b` | 103 | `gradle.properties` |
| `41f37cdd0dc3e345b982ffcda955c1faad515cd7a5ee69188e1061f7ef8bc42b` | 961 | `app/src/main/AndroidManifest.xml` |
| `b81aff25d7412c0b12a29e8872d643edb6fa23b20155ccd71560f5d913eb6fd2` | 72 | `app/src/main/res/values/strings.xml` |
| `add8d057eb0380d4ac776ad09ace3bf07dc50a29c0eb3d500e0f876d8be2b8bc` | 20,977 | `app/src/main/java/com/livelingo/android/ui/LiveLingoScreen.kt` |
| `0279755adf8b3fcaa4999690c45bf296b5ccd4e530906ce37eec114e5486f550` | 2,337 | `app/src/main/java/com/livelingo/android/MainActivity.kt` |
| `1753d94d7e4154a00224027459ef8654c401fb246d121afe75dc3b1475369d83` | 2,413 | `app/src/main/java/com/livelingo/android/model/Conversation.kt` |
| `65fab2789ba250a58352dafdd6f013b227dee9b0baa157c2907d570754fe3321` | 2,666 | `app/src/main/java/com/livelingo/android/model/CommunityPhrasebook.kt` |
| `ee4803911a2a6543bcd26053bcbfbf59c1740f94bad5060044a087fa0013190d` | 12,260 | `app/src/main/java/com/livelingo/android/model/Languages.kt` |
| `e899257ec4363af14403530c164e82d748152cbc3c4d9d3ca4b451a9933ec12e` | 9,821 | `app/src/main/java/com/livelingo/android/MainViewModel.kt` |
| `ceb4a6339d90917118b8acf63489e9c64305586f402d8b6452491bf48b1e9895` | 745 | `app/src/main/java/com/livelingo/android/translation/LocalLanguageDetector.kt` |
| `f1518efb40014395a54518ba71fb0f409bc585a78488db433f732b92699afa5f` | 5,773 | `app/src/main/java/com/livelingo/android/translation/OfflineTranslator.kt` |
| `6105869988bb1453b9b960e13d1d9bfec059ee113f2c6524e759696e165bf896` | 5,966 | `app/src/main/java/com/livelingo/android/speech/OnDeviceSpeech.kt` |
| `4c8641b1518964e7f3383bb99cbfb704bcef040334ea958b8c44ecd6ee8d7f4c` | 1,747 | `app/src/test/java/com/livelingo/android/model/ConversationUnitTest.kt` |
| `0eb869669aaa1075ceeaedaff179fb84e2f589b4e3ce4e0fb6a88e212c4e1e35` | 2,796 | `app/src/test/java/com/livelingo/android/model/LanguageAndWordUnitTest.kt` |
| `814d17afe2d3798a178bfc6c4600704732cecaf184ec5d4c71ed1c235cf0fc9a` | 1,317 | `app/build.gradle.kts` |
| `0fae8d015cb90e99b8b93e0cf980f75b8def3ee3868c3711202ea27732e3f49f` | 12,459 | `README.md` |
| `932fdc51b119436c751e4c58112dc1f3e0ea9a2bbe66a8f3866809cb790abf4c` | 4,426 | `tests/LanguageAndWordTest.kt` |
| `e771cf1b1128d2272c6782ddca63c6b865b7df228b309fde0b5b98728503bfc3` | 1,473 | `tests/ConversationTest.kt` |
| `31af21a3da12511f52bce24119b84797aff565cc9a3d18f3d75e7f469dd73d6f` | 6,535 | `docs/LANGUAGE_SUPPORT.md` |

## Migration boundary

The current repository intentionally retains newer session-generation, stale-callback, explicit Wi-Fi-consent and speech-pack-readiness protections. Do not copy the historical `MainViewModel.kt`, `OfflineTranslator.kt`, `OnDeviceSpeech.kt` or screen into the active source tree by filename alone; first reconcile their behavior with the current implementations listed in [the migration map](TUDDY_V0_4_SOURCE_MIGRATION.md). This manifest makes the source recoverable and auditable, but **does not itself complete the unpacked source import** or satisfy physical Android 12+ airplane-mode acceptance.
