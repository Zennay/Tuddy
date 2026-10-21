# Review private topic backups before restoring

The **Mijn privéthema’s** screen lets users save a separate local `tuddy-privethemas.json` file containing their 12-or-fewer theme names and SHA-256-based mappings of at most 80 personal phrase identities. This file includes no duplicated phrase text. Restore requires that the matching personally authored phrases have already been restored into Tuddy.

A restore previously displayed only counts. Now the confirmation screen shows **every theme** and **every personal phrase that will be mapped to a theme** by that exact backup. The source phrase, language and the person's own meaning appear only in this in-app preview; no additional text is written to the backup or sent to the network. Phrases not listed as mapped will have no theme after a confirmed full replacement. Cancellation leaves everything unchanged.

A restore records an immutable local snapshot of both the current theme backup bytes and the existing personal phrases when the external file is parsed. At confirmation, `PrivatePhraseTopics.replaceImportedIfUnchanged` re-reads them; changes to theme names/assignments or any source phrase/meaning invalidate consent. It rechecks SHA-256 identities against the current phrases and commits the replacement as one preferences operation only if the snapshot still matches. Otherwise the user is instructed to open the backup again.

This is a user-authored, fully offline organization feature. It is not offline ASR, translation, native-speaker validation, device acceptance, signing or production admission; GitHub issues #1 and #2 remain open. No new permissions or automatic external file transfers.
