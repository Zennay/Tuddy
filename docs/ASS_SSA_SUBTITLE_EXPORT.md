# Offline ASS / SSA export — Tuddy

Tuddy already accepts user-supplied ASS/SSA Dialogue cues locally, stripping
formatting and refusing drawing/animation instructions. This feature adds a
**new .ass subtitle copy** from an existing, manually reviewed subtitle draft.

1. Open your own timed subtitles, edit them locally, then tap
   **Bewaar onopgemaakte ASS/SSA-kopie…**.
2. Tuddy shows the entire prospective ASS text in a paged preview and makes
   the exact byte size visible. Nothing is written before confirmation.
3. Tap **Kies opslaglocatie…** and choose a new file through Android's
   document picker. A third-party document provider may synchronize it.
4. Cancel returns to the in-memory draft without modifying the source.

ASS/SSA times use **centiseconds** (10 ms). Unlike SRT, WebVTT and TTML,
ASS cannot precisely store timestamps such as 00:01.234. Tuddy refuses that
export instead of rounding, even when only one cue is affected. Choose SRT
or WebVTT to preserve millisecond timestamps.

The exporter retains every author-written cue, timestamp, text line, comma,
and literal brace. It includes a minimal default style for compatibility,
not the styles of the imported source. It validates size, UTF-8, and
re-imports the generated text using the existing constrained ASS reader;
if any text or timing would change (including ambiguous backslash escapes)
the export is refused. At most 500 cues or 256 KiB are supported.

No subtitle is auto-translated, no URL is fetched, no media or microphone is
opened, and no live transcript is persisted by this export. It is **not**
offline Kurdish/Pashto speech recognition, signed release or proof of an
Android 12+ airplane-mode installation. See [import restrictions](ASS_SSA_SUBTITLE_IMPORT.md).
