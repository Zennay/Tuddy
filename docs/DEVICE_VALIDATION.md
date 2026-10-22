# Tuddy Android 12+ physical-device acceptance — NOT YET VERIFIED

**State:** `HOLD` until the exact APK is installed and tested by an authorized
human on a physical Android 12+ handset. A hosted GitHub build or emulator
cannot authorize a device-ready, offline-speech, or production-release claim.

## 1. Exact build identity and installation receipt

Latest main candidate (22 October 2026):

| Field | Immutable evidence |
|---|---|
| Source Git SHA | `6efc1e645861734942c1be73160b9ad61c87c670` |
| Hosted Android run | [#39717339120](https://github.com/Zennay/Tuddy/actions/runs/39717339120) — COMPLETED SUCCESS |
| Artifact | [Tuddy-debug-apk](https://github.com/Zennay/Tuddy/actions/runs/39717339120/artifacts/12399837589) (ID `12399837589`, expires 5 November 2026) |
| APK filename | `app-debug.apk` |
| **APK SHA256** | `7684327f1b2d403fa26968abebb4bb53169082cc829486d531c852e4336ba318` |
| Artifact ZIP SHA256 (not APK) | `d7e8c5e909a05d88560c09d25370047a06cd94c51e2707af7312c3cbd292e43f` |
| Package | `com.livelingo.android` (legacy ID retained until deliberate migration) |
| Version | `0.4.0` |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

The exact-head workflow computed and verified the APK SHA-256 before uploading the artifact and bound the artifact to the source checkout above. The artifact ZIP digest is recorded separately and must never be substituted for the APK digest. **This proves build provenance only, not physical installation or device acceptance.**

Previous candidate (21 October 2026; superseded by the 22 October main candidate):

| Field | Immutable evidence |
|---|---|
| Source Git SHA | `ecd97aade8ca098e8653925d3dd56a0205fe97ce` |
| Hosted Android run | [#39655468714](https://github.com/Zennay/Tuddy/actions/runs/39655468714) — COMPLETED SUCCESS |
| Artifact | `Tuddy-debug-apk` (ID `12371971869`) |
| APK filename | `app-debug.apk` |
| **APK SHA256** | `620783ab4a47c868c205f8343c478ad34ed136a702c50dd23bb6987b5fb6791c` |
| Artifact ZIP SHA256 (not APK) | `b9051cc2d0f4c6b3b1403ad827ba63b1315d704aee7a1e8f9e5b0377cbd2956e` |
| Package | `com.livelingo.android` (legacy ID retained until deliberate migration) |
| Version | `0.4.0` |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

The 21 October table is historical evidence only; do not use that APK as the current main candidate. For the current candidate, download artifact `12399837589` from run `39717339120` and independently verify `source-commit.txt` plus the APK SHA-256 before installation. If main changes, derive a new exact source/APK pair. Neither successful CI nor a matching checksum substitutes for a physical Android 12+ airplane-mode acceptance record.

Previous candidate (17 October 2026; superseded by the latest main candidate):

| Field | Immutable evidence |
|---|---|
| Source Git SHA | `b1a298a907f8ff2f7459b9c934f56fafb462f502` |
| Hosted Android run | [#39064114978](https://github.com/Zennay/Tuddy/actions/runs/39064114978) — COMPLETED SUCCESS |
| Artifact | `Tuddy-debug-apk` (ID `12112063195`) |
| APK filename | `app-debug.apk` |
| **APK SHA256** | `fdc2dc8d4ec7de7293afca9ce3ed1aad832e21c3d0ba1aedef2210bc3b120bf6` |
| ZIP SHA256 (not APK) | `a376f8d1a402b0aeafceb12fcdfaf2f4b1bc3d76944d746682203cdbb1d351a9` |
| Package | `com.livelingo.android` (legacy ID retained until deliberate migration) |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

The 17 October table below is retained as historical evidence only; do not use that APK as the current main candidate. For the latest candidate, independently download the artifact from run #39655468714 and verify `source-commit.txt` plus the APK SHA256 before installation. If main changes, derive a new exact source/APK pair. Neither successful CI nor a matching checksum substitutes for a physical Android 12+ airplane-mode acceptance record.

Device report to attach to [Tuddy #1](https://github.com/Zennay/Tuddy/issues/1):
handset model/serial-free identifier, Android version/API, recognition provider
and on-device language packs, timezone, fresh-install outcome, APK digest,
source commit, timestamp, measured first-partial/final/translation latency,
memory/battery observations, pass/fail reason. **Do not submit raw private
speech or full transcripts** with reports.

## 2. Steps requiring a real Android device

1. **Permission/stop gate:** launch fresh without mic permission, deny it, then
   grant via system UI. Denied permission must never start recognition.
   Tap Stop while an utterance is ending and while a no-match 850ms retry is
   pending: no restart or new microphone request after Stop.
2. **Model provisioning (new in PR #22 candidate):** while connected to Wi-Fi,
   select the desired source and target and tap **Tekstmodel offline voorbereiden**.
   Confirm visible **Tekstmodel klaar voor offline gebruik** only for that exact
   selection. Try an unsupported Kurdish/Pashto phrasebook choice (must remain
   separate), same-source/target choice (no model requested), switch languages
   while a download is pending and background/Stop the app (old completion must
   not claim the new selection ready). Run a second independent model check after
   relaunch before airplane mode; this status is an in-memory download receipt,
   not proof of the recognizer or persistence. Provision Android's **on-device
   speech packs separately** in system settings. Record both speech and ML Kit
   models; only the latter is downloaded by Tuddy's button. Wi-Fi may be used
   for model downloading, never as a cloud speech recognition fallback.
3. **Network-disabled proof:** switch on airplane mode and independently
   disable/restrict remaining connectivity. Record at least **10** sequential
   NL→EN and EN→NL bilingual utterances; each must yield correct interim/final
   replacement with no duplicate, and on-device text translation after
   provisioning. No cloud STT fallback or silent unverified recognition.
4. **Continuous utterances and optional Android 14+ language switching:**
   wait through a final result, silence and no-match. Recognition must resume
   **only** while explicitly listening in foreground. Permission/unsupported
   language/unknown recognition errors must STOP without busy retries. On
   Android 14+ with downloaded **on-device speech** packs, opt into
   **Automatische spraaktaal** and alternate between two of the six supported
   spoken source tags. Verify only actual accepted language-detection events
   relabel the correct finalized line, never earlier lines; when the device
   reports a missing language-switch pack, the app must show an actionable
   local-model notice instead of cloud fallback. Toggle this off and confirm
   single-source behavior is unchanged. On Android 12–13 the toggle must be
   disabled and manual source selection must still work. Unknown, Kurdish and
   Pashto speech-model coverage must **not** be inferred from a text target
   or five-phrase vocabulary preview. A hosted JUnit build does not establish
   that any phone supports this optional speech API.
5. **Lifecycle race:** during the 350ms final-result and 850ms quiet retry
   windows, press Stop, change source language, lock/background the app, rotate,
   dismiss the Activity, and deny permission. No queued restart may record
   after session invalidation; old callbacks must not resurrect cleared words.
6. **Lyric timeline and privacy:** test the very first growing provisional
   caption, then at least 500 finalized lines. Follow-live must scroll during
   active speech and remain paused if the user scrolled back and disabled
   following. The list must keep at most 500 RAM-only records; old results
   must not reappear after eviction or **Wis gesprek**. Kill the process:
   previous caption history must be absent after relaunch.
7. **Word cards / accessibility:** tap source and successfully translated
   target words in a multi-script conversation. Verify the language captured
   at the time of the line remains correct after changing the picker. Test
   TalkBack/keyboard access to the editable word field. Unknown, unsupported
   and sensitive vocabulary must show a transparent 'no verified definition'
   outcome; the five Kurmanji/Sorani/Pashto beginner entries do **not** imply
   full dictionary or free-form offline translation.
8. **Offline service absent:** on a fresh device lacking speech packs, confirm
   a visible, actionable local-service/pack error and **no cloud fallback**.
   Separately confirm no raw audio/transcript persistence or network STT;
   inspect the app's local storage and network behavior without collecting
   personal speech in logs.
9. **Quality and native review:** measure latency, CPU/battery and accuracy for
   actual native-speaker examples; reserve medical, asylum and legal use for a
   qualified interpreter until accuracy is independently verified. Check
   Arabic RTL rendering and unsupported-language labeling without claims of
   Kurmanji/Sorani/Pashto ASR/model coverage.

10. **Typed local translation when speech is unavailable:** on a device without
   Android on-device speech packs, keep microphone permission denied and select
   two supported ML Kit text languages. While Wi-Fi is available, explicitly
   prepare the selected text pair, then disable network connectivity. Type
   ordinary NL/EN and Arabic Unicode sentences in **Zin typen** and tap
   **Vertaal tekst lokaal**. Confirm local output appears in the **same bounded
   RAM-only bilingual timeline**, without starting microphone recognition or
   granting a permission. Empty, over-1500-unit and binary-control text must
   never enter translation. An unavailable offline model must show an honest
   missing-model status rather than invent an answer. **Wis gesprek** must
   erase both the typed input and conversation. Reopen the app and verify the
   text/audio timeline is absent; only selected language codes may persist.
   No unsupported Kurdish/Pashto free-form model or source license is implied.

11. **Live lyric-style provisional translation:** with source/target text models
    already downloaded, use a slow 10-second spoken sentence. Check the
    second live line appears only after ~650ms of stable interim text (not
    per-word downloads), updates while speech continues and never overwrites
    a newer partial after a delayed old translation finishes. Switch target,
    press Stop, deny permission and clear the conversation while an interim
    request is in-flight; no late result may resurrect a preview. When an
    offline model is missing, raw ML Kit exception details must never be
    rendered and the preview may remain untranslated until models are
    explicitly prepared. Follow-live must scroll for both the growing source
    and the translated provisional subtitle. The source-only and hosted
    synthetic gates cannot replace this actual handset trial.

## 3. Evidence / release record (fill only after running the test)

- Candidate source / APK digest: **NOT YET OBSERVED ON DEVICE**
- Device / Android API / downloaded language packs: **NOT PROVIDED**
- Airplane-mode 10-utterance report, latencies and 500-line test: **NOT RUN**
- Permission/lifecycle/stop + delayed-retry proofs: **NOT RUN**
- TalkBack, word-card, privacy and native speaker review: **NOT RUN**
- Observed blocking defect, logs without speech, remediation PR: **NONE REPORTED**
- Human acceptance / owner sign-off: **NOT GRANTED**
- **Release-ready: NO · zCloud runtime admission: NO**

The repository's P1 [#1](https://github.com/Zennay/Tuddy/issues/1)
and [#2](https://github.com/Zennay/Tuddy/issues/2) remain open. Full
historical v0.4 architecture and licensed, fully on-device Kurdish/Pashto
ASR/MT engines are not completed. Keep all production/release/worker
capabilities disabled until owner-reviewed evidence meets the gates.
