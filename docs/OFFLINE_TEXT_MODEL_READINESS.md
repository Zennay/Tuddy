# Offline text-model availability for Tuddy

Tuddy's offline setup now has **two independent user-visible checks**.

## Text translation: ML Kit model inventory

Choose a spoken source language and translation target, then tap
**Controleer tekst**. Tuddy queries the locally installed ML Kit
`TranslateRemoteModel` inventory, without downloading any model, uploading
text, starting a microphone or making a remote translation request.

- For an identical source and target, no model is required.
- ML Kit English is bundled, so it is not treated as a downloadable model.
- For different source/target languages, both non-English model codes must
  exist in the device's inventory. If either is missing, the screen shows
  which model needs Wi-Fi preparation.
- The existing **Model via wifi** action requests exactly the chosen
  translation pair through ML Kit `downloadModelIfNeeded(requireWifi)`.
  After success, Tuddy re-reads the actual local inventory rather than
  declaring readiness based solely on a completed download task.
- On app background, Stop, language change or manual cancellation, stale
  inventory callback results are discarded. Results are kept in RAM, never
  persisted as proof after restarting the app.
- If ML Kit cannot list its installed models, the UI says
  **unknown**, never automatically **ready**.

The inventory indicates model files are installed. It does NOT prove
translation accuracy, runtime performance, offline microphone support,
dialect coverage, or availability of any current transcript. Real Android
12+ airplane-mode device checks remain a separate acceptance gate.

## Source-language identification

Tuddy reuses one bundled ML Kit language-identification client for the current
screen session instead of creating and closing a client for every caption.
Identification stays on device, does not download language packs, and does
not change the installed translation-model inventory. Short text (under eight
characters) falls back to the selected spoken source; detected codes are only
used when they belong to the supported 59-language text catalog. The detector
is closed when the screen is disposed, and callbacks from a disposed detector
are ignored. This is advisory text-language identification—not speech
recognition and not proof that a source/target translation pair is ready.

## Reusing local translation clients

The final-caption pipeline keeps a small access-ordered cache of at most four
source/target translator clients after the app has independently confirmed
that the exact pair's models are installed. This avoids creating and closing
an ML Kit translator for every finalized caption. Provisional captions retain
their short-lived translator client and close it after each attempt; that path
never calls model download. Only idle cached clients can be evicted; clients
with translations in flight close when they become idle or when the screen is
disposed. The pool is invalidated after model download/removal and when the app
leaves the foreground, so cached clients cannot outlive a model inventory
change. Model downloads remain explicit and Wi-Fi-gated, and the pool stores
no transcript or translation text.

## Speech recognition: Android on-device packs

Use **Controleer spraak** for a separate Android on-device language check.
ML Kit text download says nothing about installed speech models. See
[OFFLINE_SPEECH_PACKS.md](OFFLINE_SPEECH_PACKS.md) for Android 13+ API
support, Wi-Fi-only user-initiated requests and on-device lifecycle.

## Kurdish Kurmanji, Sorani and Pashto

The target-language picker recognizes common Kurdish and Pashto names, including supported native-script spellings, and explains that these entries remain phrase-preview only until a fully licensed, native-speaker-reviewed offline engine is available. For a specific dialect search, the empty state also preselects that language in the existing five-phrase preview and clears the query.

The five fixed beginner phrases remain a manual preview. These languages
are not represented as full ML Kit translation targets in the selector;
there is no licensed free-form offline MT or separate ASR engine included.
The app never claims the inventory above makes these dialects available.

## Share a filtered model-research brief

In **Taalondersteuning**, search for a language/model and optionally narrow the
results by ASR vs. text translation and by license/access signal. **Bekijk en
deel onderzoeksresultaten…** opens a scrollable preview of the complete
filtered brief before Android's explicit share chooser is launched. The report
includes the search term, task/access filters, candidate count, public model
card references, declared license, estimated footprint, advertised directions
and unresolved limitations. It explicitly says that catalog classifications
are text heuristics—not legal clearance, model installation or dialect
validation.

Nothing is sent until the user confirms the share action and chooses an
Android share target. No report is saved in Tuddy, no network request is
started by Tuddy, and no user phrases, conversations, audio or device
diagnostics are included. A selected recipient can retain or forward the
metadata; inspect the preview and chosen destination before sharing.

Google model manager docs:
https://developers.google.com/ml-kit/language/translation/android
https://developers.google.com/android/reference/com/google/mlkit/common/model/RemoteModelManager
