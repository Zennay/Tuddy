# Save the offline readiness report as a local file

From **Offline startcheck**, Tuddy can now offer **Bewaar apparaatrapport als tekstbestand…** in addition to the existing sensitive clipboard action. This is a product feature for keeping a device-local record of language setup, not an automated QA run or physical-device acceptance.

The report contains the app package and version, a SHA-256 computed from the installed base APK file, exact build-source commit / GitHub run identity, Android API and device manufacturer/model, airplane-mode setting, the current microphone-permission grant state, manually selected language tags and **separate** Android on-device ASR/ML Kit text-model states. The installed APK is hashed on `Dispatchers.IO` when the readiness dialog opens; the report is unavailable until that hash finishes, avoiding a main-thread file read. Compare this installed-base-APK digest to the APK-file digest, never to the enclosing artifact ZIP digest. The permission field records Android's permission bit only; it does not prove that a microphone session succeeded. The report explicitly says **no transcript or audio included**.

Nothing is written until the user opens a full scrollable preview, confirms the privacy warning and deliberately chooses an Android Storage Access Framework destination. The report is UTF-8 text and capped at 8 KiB. When the file picker returns, the entire source/target/device/model status is recalculated: any changed bytes refuse writing instead of silently sharing a different report. Tuddy retains no persistent copy and requests no new Android permission. The Activity keeps this one pending export callback alive while the document chooser backgrounds Tuddy. Canceling leaves the device unchanged. An external file provider may sync the report; deleting Tuddy data cannot retract that copy.

**Important:** Android's report of an installed model does not establish real speech quality, network isolation, true airplane-mode end-to-end behavior, or language accuracy. The report contains no invented pass/fail declaration. P1 [issue #1](https://github.com/Zennay/Tuddy/issues/1) needs actual authorized Android 12+ offline phone evidence bound to a specific APK; P1 [issue #2](https://github.com/Zennay/Tuddy/issues/2) still needs rights-cleared/native-speaker-reviewed complete offline Kurdish Kurmanji, Sorani and Pashto ASR+MT. No release signing or production admission is implied.

## Record a manual physical-device session

The Offline startcheck also offers **Leg fysieke offline-waarnemingen vast…**. This is
an optional user-entered evidence record for an authorized physical Android 12+
session; it does not launch speech recognition or run an automated test. Record
only what was actually observed after installing the exact APK whose checksum
appears in the device snapshot.

The form records five explicit confirmations, aggregate successful-observation
counts for NL→EN and EN→NL speech and translation (each 0–10), and the highest
observed latency in milliseconds. It has no free-text notes field and never asks
for sample phrases, transcript content or audio. A partial or failed session can
also be saved; the report labels it incomplete rather than inventing a pass.

Before writing, the full UTF-8 report is previewed. The user must then choose an
Android Storage Access Framework destination. The device/model/build snapshot is
re-read after returning from the picker; if it changed, the write is refused.
No report is uploaded or retained by Tuddy. An external document provider may
synchronize the chosen file.

Every exported session explicitly says **self-reported** and **not independently
verified by Tuddy**. A complete form does not change the status of issue #1,
authorize a signed release, prove absence of network traffic independently, or
satisfy issue #2's licensing and native-speaker gates. Human review of the
report and the exact artifact remains required.

