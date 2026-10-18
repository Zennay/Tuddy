# Private curated-word backup and restore

The existing **Mijn woorden** list now offers two explicit actions:

- **Bewaar back-up…** creates a JSON file via Android's own document
  picker. You choose the destination; Tuddy never uploads it.
- **Herstel back-up…** opens a file through Android's own document picker,
  verifies the complete backup in temporary RAM and shows the number of
  admitted words. **Vervang woorden** must then be selected separately.
  Cancel, invalid data or a failed write leaves the existing list unchanged.

## Data contract

```json
{
  "format": "tuddy-curated-word-keys-v1",
  "keys": ["en|water", "nl|water"]
}
```

Example keys are illustrative; admission requires an actual entry in the
installed app's curated local glossary. Each key is a canonical
`language|word` identifier. Up to 120 unique keys and 16 KiB of strict
UTF-8 are supported. Unknown schema fields, unsupported versions,
duplicate keys, malformed input, previously removed glossary words and
unsupported languages are rejected before any replacement.

Only the keys are stored; definitions and illustrations are always resolved
from Tuddy's installed curated dictionary at display time. A backup contains
**no recordings, conversation content, transcripts, sentence context,
translation models, provider results or user identity**.

## Privacy and compatibility

Android's file provider may sync a user-selected backup to another device
or a cloud service. Tuddy itself does not send data online, request broad
file permissions, enable Android automatic backup or load a backup in the
background. The original saved words remain app-private and opt-in.

A backup created by a newer glossary version may include a word that an
older Tuddy version does not know. That older version fails closed rather
than introducing an unverified definition. Restore is a full deliberate
replacement, not an automatic merge. Existing custom phrase notes and
live conversations are never affected.

This is **product source functionality**, not a physical device install,
signed release APK, native-speaker validation, legal/medical interpretive
accuracy or proof of offline free-form Kurdish/Pashto translation.
