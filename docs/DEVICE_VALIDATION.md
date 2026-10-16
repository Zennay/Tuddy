# Tuddy Android acceptance — actual device required

This is an execution checklist, **not evidence of device validation**. Track results in [#1](https://github.com/Zennay/Tuddy/issues/1). Do not merge as production-ready or claim an APK verified from GitHub CI alone.

## CI (hosted)

- Use the `Android APK (hosted)` run at the exact commit under test.
- Require both `:app:testDebugUnitTest` and `:app:assembleDebug` to succeed.
- Download `Tuddy-debug-apk` from the successful run, log its SHA-256, artifact URL and Android version.
- Confirm the APK's package is `com.livelingo.android`, and avoid install over an incompatible signed variant.

## Android 12+ device

Record device model, Android version, Android System Intelligence/recognition provider, locale, available offline speech packs, and version/build SHA.

1. Launch without microphone permission; verify explicit request on pressing **Luister**. Deny permission; verify no microphone use or background recording.
2. With Wi-Fi, select Dutch to English and record "Goedemorgen, hoe gaat het?". Confirm translation downloads, then text lines render and the last interim speech is replaced by the final line once.
3. Enable airplane mode **after** speech and translation packs are present. Record 10 bilingual utterances with intermediate captions. Verify microphone and ML Kit function with network disabled. Never allow cloud speech fallback; unsupported locales must fail visibly.
4. Test fast start/stop, denial, interruptions, phone lock/background, rotation, repeated utterances, and language switching. No old callback may reappear after **Wis gesprek**.
5. Scroll back in a long conversation, disable follow mode, and use **Volg live** to return. Verify RTL Arabic output when selected.
6. Kill the app and relaunch. The conversation must not survive process death. Inspect traffic and app data to verify no transcript persistence or cloud STT.
7. Reinstall with a fresh device lacking speech packs; verify an actionable unsupported-offline error, not a silent online fallback.
8. Check native speaker accuracy for Arabic, Farsi/Urdu and other newcomer languages, and tell the user where translation confidence is insufficient for health/legal/asylum contexts.

## Known gaps and release gate

- Current GitHub MVP selects six speech input languages and offers 59 on-device ML Kit text translation targets with a searchable picker; actual offline speech recognition coverage depends on the device. The preserved v0.4 Library source catalog covers 59 ML Kit text targets. Import and reconcile the full v0.4 source before claiming parity.
- Kurdish Kurmanji, Sorani and Pashto are **not** full offline translation / speech recognition. Their archived v0.4 phrasebook has only five simple phrases.
- A screenshot, source-only unit-test pass, or hosted APK build is not a substitute for airplane-mode validation on a physical Android device.
- Keep zCloud project runtime gated: zero AI workers, disabled compute pool and no VPS app workload until these gates and serialized deployment review pass.
