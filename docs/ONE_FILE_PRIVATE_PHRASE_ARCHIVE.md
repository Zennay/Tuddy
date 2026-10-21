# One-file private phrase + topic archive

The self-authored **Mijn eigen zinnen** screen has a new deliberate one-file transfer path for moving a person's notes and topic assignments to another Android device.

## Saving
Choose **Bewaar zinnen én thema’s in één bestand…**. Tuddy builds a bounded UTF-8 JSON archive (format `tuddy-private-phrase-archive-v1`, maximum 70,000 bytes). An in-app, scrollable preview shows every source phrase, its user-written Dutch/English meaning and its manually assigned topic; only after explicit confirmation does Android's Storage Access Framework ask where to write a new file. The same exact archive bytes are recalculated when the file picker returns. A changed phrase, meaning or topic invalidates that export instead of silently increasing the disclosure.

## Restoring
Choose **Herstel zinnen én thema’s uit één bestand…**. Tuddy reads at most 70,000 UTF-8 bytes and refuses unknown keys, formats, languages, malformed text, duplicate phrase identities, illegal names, dangling/unrecognized SHA-256 identities, over-capacity archives and invalid link records. The preview displays every phrase, meaning and its proposed topic before asking to replace the local set. Both existing notes and theme backup are captured when preview opens and must still match on confirmation. The phrase list, theme names and assignments are written in one SharedPreferences editor transaction; no partial restore.

A confirmed full replacement deliberately **clears this personal deck's favorites and scheduled reviews**, because scores for old meanings must not be inherited by unrelated restored content. Other dictionaries, live transcript and conversation storage remain untouched. Canceling or failing the stale-preview check does not change the local deck.

The archive stores only self-authored phrase text, own meanings, user-created theme names and SHA-256 phrase identity links, not audio, chat transcripts, ASR, generated translations, study grades, analytics or cloud identifiers. A selected Android file provider may synchronize user-created external files; deleting app data does not revoke those external copies. This backup doesn't prove Kurdish/Pashto offline speech, machine translation, native-speaker review, physical Android airplane-mode readiness, signed release or production admission (P1 #1/#2 remain open).
