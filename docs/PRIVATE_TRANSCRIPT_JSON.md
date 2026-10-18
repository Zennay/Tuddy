# Private JSON conversation export

Tuddy's conversation remains in RAM by default. **Bewaar JSON…** is a separate
user action next to the existing text export. It appears only for completed
caption lines when the microphone is stopped. Android's system document picker
asks where to save `Tuddy-gesprek.json`; Tuddy never picks a storage location,
does not upload the conversation, and does not run an automatic backup.

## Format

The file contains a UTF-8 JSON object:

```json
{
  "format": "tuddy-private-transcript",
  "version": 1,
  "privacy": "Explicit user export; a chosen file provider may synchronize this file.",
  "captions": [
    {
      "recorded_at_ms": 1760000000000,
      "source_language": "nl",
      "target_language": "en",
      "source_text": "Goedemorgen",
      "translation_status": "not_finished",
      "translated_text": null
    }
  ]
}
```

The example above is illustrative, not a real transcription. Language codes
are captured per line so a change of languages does not retroactively relabel
earlier speech. Only finalized caption lines are included; live partial
recognition is not written. Text from a pending/failed translator task is
**never** labelled as a completed translation. The original text is retained
so the user can recover what was spoken without relying on a missing translation.

## Copy a single bilingual caption

Each completed caption card also has **Kopieer deze zin**. This is an
explicit, individual Android clipboard action, not an automatic sharing
channel. It copies the original language-labelled sentence and (only when
actually complete) the translated sentence. Pending/missing translations are
clearly marked `[Vertaling niet voltooid]`. Each copied payload is bounded to
16 KiB and is marked **sensitive** in Android's clipboard metadata where
supported, so the operating system can suppress clipboard previews.

**Important:** The clipboard belongs to Android, not Tuddy's RAM-only
conversation history. Other apps, input methods or platform clipboard history
may access its contents. Tuddy does not guarantee automatic removal from the
clipboard. Avoid copying sensitive medical, legal or asylum conversations.

## Read a saved JSON archive privately

Choose **Open JSON-archief…** and select an existing `Tuddy-gesprek.json`
with Android's document picker. Tuddy reads at most 256 KiB, validates the
format and completed/uncompleted translation states, and shows the archived
bilingual captions in an independently labelled **read-only** dialog.
Use **Zoek in dit archief** to filter up to 500 loaded captions by the
original wording, completed translation or language label without sending a
query anywhere. Results show the original per-line date/time and a local
match count; unfinished translations cannot silently match as finished.
Changing search criteria only changes this read-only display, never the
selected file, the source JSON or the live conversation.
Close the dialog to discard its temporary in-memory preview. The selected
document is never copied to app storage, persisted as conversation history,
used as live speech evidence, or sent to a translation model.

Invalid, oversized, malformed or unsupported archives display an error
without changing the current conversation. An archive selected from external
storage is **user-provided text**, not independent evidence that the device
recorded/transcribed/translated the conversation. There is no automatic import,
merge, restore, cloud download, background backup or persistent archive list.

## Boundaries

- At most 500 captions, 4096 UTF-16 units per field and 256 KiB in the
  completed UTF-8 file. Invalid timestamps/fields fail the export.
- No background sync, share-sheet broadcast, browser/cloud connection,
  raw microphone audio, imported source credentials or automatic restore.
- The app clears a prepared snapshot on session clear and validates the
  session generation when Android returns a selected document URI.
- Once the user exports, **the chosen file provider can synchronize or share
  private conversation content outside the app**. Remove files from that
  provider separately if needed. Treat medical/legal/asylum transcripts as
  sensitive and verify machine translations with a qualified interpreter.
- The JSON format is for personal archiving or user-controlled analysis;
  **importing it into Tuddy is not implemented**. Do not claim this is a
  verified device backup, actual device validation, or a source-rights grant.

Release/device acceptance is still required separately; see
[DEVICE_VALIDATION.md](DEVICE_VALIDATION.md). The original `Bewaar gesprek…`
text export remains unchanged.
