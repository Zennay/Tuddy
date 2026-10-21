# Selected private phrases → local TSV

Tuddy's **Mijn eigen zinnen** screen now supports deliberately exporting **only** the phrases the user selected by source language and, optionally, the current topic. No model, remote call, sharing intent or new Android permission is involved.

## On-device flow
1. Open **Mijn eigen zinnen**, select the intended Kurdish/Pashto source language and optionally a topic.
2. Tap **Exporteer gekozen zinnen als TSV…**. The preview defaults to that language and current topic, and allows explicitly choosing all three languages or all topics.
3. Review every source phrase and *user-authored* Dutch/English meaning in a scrollable list. The final save action opens Android's Storage Access Framework file picker; canceling leaves the local deck untouched.
4. The app computes the same selection again after the file picker returns. If any selected phrase or meaning changed, or a note moved into/out of the selected topic, saving is refused with a visible message. Otherwise it writes the exact pre-approved UTF-8 bytes to the chosen provider.
5. The generated file uses the existing strict four-column header `language<TAB>phrase<TAB>meaningLanguage<TAB>ownMeaning`. It can be imported through **Importeer eigen zinnenlijst (TSV)…**, where the user chooses merge or replacement after preview.

## Privacy and boundaries
- Stored source notes are limited to 80 entries and the export to 50,000 bytes; a selection of zero rows is refused. The underlying strict parser checks row identities, length limits, language tags, UTF-8 and disallowed control characters.
- Export contains no session transcript, microphone audio, study schedule, favoriting or topic metadata. Topic selection is a *filter* only, not part of the portable file.
- The user decides whether the chosen Android file provider keeps the file locally or syncs it; deleting Tuddy data does not delete external files.
- Kurdish Kurmanji (`kmr`), Sorani (`ckb`) and Pashto (`ps`) meanings are human-authored, **not** verified or automatically translated. The feature does not imply native-speaker validation, offline ASR/MT, physical device evidence or production admission. Open P1 #1 and #2 remain unchanged.
