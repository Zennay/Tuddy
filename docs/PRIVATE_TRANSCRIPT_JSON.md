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
