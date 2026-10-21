# Non-destructive import for one-file personal phrase archives

In **Mijn eigen zinnen → Herstel zinnen én thema’s uit één bestand…**, Tuddy now offers two clearly separate, full-preview import modes.

**Veilig samenvoegen (recommended, default)** adds only source-language + case-insensitive sentence identities not already present on this Android device. Existing personal meanings, theme assignments, theme spellings, favorite pins and review dates always win. Duplicate incoming phrases are ignored without changing their existing notes. Previously unknown incoming theme names are added; topic names matching an existing local name case-insensitively reuse that exact local name. Assignments are brought in **only for newly added notes**. No phrase or topic is partly written: if 80 phrases, 12 topics, 50,000-byte local phrase storage, invalid archive contents, or stale local state prevents safe merging, the entire operation is rejected.

**Hele lijst vervangen (destructive)** preserves the original explicit full-replacement feature. It replaces all personal notes and themes as one preferences commit, after a complete preview and a separate confirmation. It intentionally clears prior favorites and scheduled personal-phrase reviews rather than associating outdated study progress with new content.

Both flows open the user-selected local JSON file at most once, require strict UTF-8/schema/language/hash validation, display each author-written phrase and meaning, and verify the precise local original notes and topic state when the confirmation is pressed. Other word decks and private conversation sessions are unaffected. No network calls, automatic ASR, machine translation, new permissions or background synchronization. User-selected Android file providers can sync files externally; Tuddy cannot revoke those copies.

This is user-authored language learning material, *not* a verified translation model or device acceptance. P1 issues #1 (physical Android airplane mode) and #2 (real rights-cleared Kurdish/Pashto offline ASR+MT/native-speaker assessment) remain open.
