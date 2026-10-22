## Optioneel: APK-checksum uit lokale sidecar overnemen

Download bij de **bewust gekozen, vertrouwde** GitHub Actions debug-APK
ook de `app-debug.apk.sha256` uit hetzelfde `Tuddy-debug-apk`-artifact.
In Tuddy kun je via **Offline startcheck → Leg fysieke offline-waarnemingen
vast → Lees app-debug.apk.sha256 uit zelf gekozen bestand…** dat bestand
handmatig aanwijzen via Android Documenten. De app leest hoogstens
256 bytes op een IO-thread en accepteert alleen één exacte regel in
het formaat `<64 hex>  app-debug.apk`, optioneel met afsluitende
newline. Er worden geen padnamen, extra regels, tekst, spraakbestanden
of checksum-metadata bewaard. Ongeldige invoer laat het bestaande
verwachte hashveld ongemoeid.

De lokale ingestelde verwachte SHA-256 moet exact gelijk zijn aan de
door Tuddy berekende **geïnstalleerde base-APK-hash**. Het gekozen
sidecarbestand is op zichzelf niet cryptografisch geauthenticeerd:
controleer buiten Tuddy de GitHub-run, exacte bron, artifact-herkomst
en ZIP-checksum apart. Een overeenkomst bewijst alleen identiteit
tussen de twee vergeleken waarden, **niet** dat de maker vertrouwd is
of dat offline spraak en alle radio's aantoonbaar geïsoleerd zijn.
Een door de gebruiker gekozen Android Document-provider kan via een
eigen externe dienst synchroniseren. Zonder zelf gekozen bestand is
handmatige invoer van de verwachte hash nog steeds mogelijk.

---

## Fysieke observaties vastleggen — ook wanneer een poging mislukt

De Android-app bevat via **Offline startcheck → Leg fysieke offline-waarnemingen
vast** een tijdelijke, handmatige observatielijst. **Gebruik deze alleen na
waarneming op een fysiek, bevoegd Android 12+-toestel**. De gebruiker kiest
NL→EN of EN→NL en registreert **precies tien werkelijk uitgevoerde pogingen
per richting**, ieder met apart de uitkomst van lokale spraakherkenning en
lokale tekstvertaling. Na een geslaagde vertaling kan de zelf gemeten totale
vertraging in milliseconden worden genoteerd. De laatst ingevoerde poging
kan per richting worden teruggedraaid; geen fictieve of automatisch
afgeleide slagingspercentages worden ingevuld.

Het geëxporteerde rapport vermeldt pogingen, succesvolle herkenningen,
mislukte herkenningen, succesvolle lokale vertalingen en fouten na een
geslaagde herkenning. **Tien pogingen betekent niet tien successen.**
Ook een proef waarin een herkenner faalt is nuttige, eerlijke informatie:
de observatielijst kan volledig zijn zonder dat de app of de fysieke
acceptatie geslaagd is. Voor iedere werkelijk geslaagde lokale vertaling
is apart een handmatig gemeten latency nodig om het registratieformulier
compleet te noemen. Een onvolledig rapport blijft exporteerbaar, met
zichtbare ontbrekende waarnemingen. Het resultaat bewijst niet dat de
operator de stappen correct uitvoerde, noch dat het toestel onafhankelijk
was afgeschermd van alle netwerken.

**Privacy:** alleen tellingen, boolean-uitkomsten en door de gebruiker
ingevoerde numerieke latenties blijven tijdelijk in RAM. Geen audio,
herkende zinnen, persoonlijke teksten, opnamebestanden of automatische
upload. De Android-opslagprovider die de gebruiker daarna zelf kiest
kan synchroniseren; kies voor gevoelige gegevens een vertrouwde lokale
locatie. Het rapport is geen productie-admission of automatische
human sign-off. Blijf de aparte wettelijke, licentie-, toestel- en
native-speakervereisten volgen.

---

# Tuddy Android 12+ physical-device acceptance — NOT YET VERIFIED

**State:** `HOLD` until the exact APK is installed and tested by an authorized
human on a physical Android 12+ handset. A hosted GitHub build or emulator
cannot authorize a device-ready, offline-speech, or production-release claim.

## 1. Exact build identity and installation receipt

Current hosted debug-APK handoff (22 October 2026, after merged product PR #364).
This is the newest known Android code build as of this handoff, **not** a
physical installation, signed release, demonstrated airplane-mode operation or
language-model acceptance. The artifact is bound to GitHub's exact proposed-merge
checkout, which may have a different commit ID from the squash-merged `main`.

| Field | Immutable evidence |
|---|---|
| App-code merge (PR #364) | [`f448dd584cd75f805a2ec480ebb88adf13614d49`](https://github.com/Zennay/Tuddy/commit/f448dd584cd75f805a2ec480ebb88adf13614d49) (`merged=true`) |
| Hosted Android run | [#39756620318](https://github.com/Zennay/Tuddy/actions/runs/39756620318) — source-head and build completed successfully |
| Exact build checkout/source identity | `81046cb702d07fccbbbcf71b78651d2007351b4b` (GitHub build's `EXPECTED_SHA` and `source-commit.txt`) |
| Artifact | [Tuddy-debug-apk #12416246932](https://github.com/Zennay/Tuddy/actions/runs/39756620318/artifacts/12416246932) (expires 5 November 2026 09:39:16 UTC) |
| APK filename | `app-debug.apk` |
| **APK SHA-256** | `3e15949f299e885022bf086294c852ceff83ed8f2ffa93fbb9b5df3e9c64e815` |
| **Artifact ZIP SHA-256 (different file)** | `77ee8642cef211ca26f2e0c4cb48cf6eab415832afd2b08dc29cb0407041b0a5` |
| Package/version | `com.livelingo.android` / `0.4.0` |
| Physical installation and real airplane-mode session | **NOT PROVIDED** |

The hosted workflow verified `app-debug.apk: OK` against its generated
checksum sidecar and printed the APK SHA above before publishing the artifact.
This candidate includes PR #363 original-caption correction and PR #364's
content-free RAM-only speech/translation counters, bounded translation latencies
and manual airplane-mode settings shortcut. It does **not** prove correct speech
recognition, external radios off, accurate offline translation, or production
support for Kurmanji, Sorani or Pashto.

Download the artifact ZIP yourself, and from a Tuddy checkout verify exact
provenance before attempting any authorized handset installation:

```sh
bash scripts/verify-tuddy-apk-artifact.sh \
  artifact.zip \
  77ee8642cef211ca26f2e0c4cb48cf6eab415832afd2b08dc29cb0407041b0a5 \
  3e15949f299e885022bf086294c852ceff83ed8f2ffa93fbb9b5df3e9c64e815 \
  81046cb702d07fccbbbcf71b78651d2007351b4b \
  39756620318
```

The verifier checks artifact integrity and provenance only. The user-controlled
in-app observation form still requires a human to verify airplane mode,
independently disabled remaining connections, NL↔EN speech/translation outcomes,
Stop/lifecycle behavior and privacy after installation. Do not mark accepted
without the signed-off human device evidence.

---

Historical pinned APK acceptance candidate (22 October 2026, built after PR #361). **This is a historical, reproducible handoff, not the latest app build:** PR #363 subsequently merged on main at `1034ddb0fb1d24882e4e3527ccb360249cfae459`, adding original-caption correction from the fullscreen reader. Do not attribute PR #363 behavior to this APK; obtain a new exact-source artifact before accepting the latest app version on a device:

| Field | Immutable evidence |
|---|---|
| Latest app-code merge (PR #361) | `10a213a518405ebbd95f1c5ce04aa1e31787b739` |
| Tree identity | Proposed-merge checkout `4bf1fad19047649c9d9c311c84de395726c08609` and merged main have identical GitHub file blobs for all four PR #361 changed files; unchanged files derive from base `1dd3a6b62d4712bc294df8bdaacf3216905d4468` |
| Hosted Android run | [#39750154761](https://github.com/Zennay/Tuddy/actions/runs/39750154761) — COMPLETED SUCCESS; source-head, build, APK manifest and checksum steps succeeded |
| Build checkout commit | `4bf1fad19047649c9d9c311c84de395726c08609` (proposed-merge checkout; matches the artifact's `source-commit.txt`) |
| Artifact | [Tuddy-debug-apk](https://github.com/Zennay/Tuddy/actions/runs/39750154761/artifacts/12412728475) (ID `12412728475`, expires 5 November 2026 08:49:34 UTC) |
| APK filename | `app-debug.apk` |
| **APK SHA-256** | `0b4ecdd21135febb123bf733ed72258dc7ec307f23988c4efccea73fdb663d76` |
| APK size | See `build-manifest.txt` inside the artifact |
| Artifact ZIP SHA-256 (not APK) | `482fe89d05799205d3aae574f6ebc4f762519b1da75000edaf70ed45f5b01f09` |
| Package | `com.livelingo.android` |
| Version | `0.4.0` |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

This artifact includes PR #361's direct fullscreen-reader entry to the revision-fenced live translation correction, alongside PR #360's consistent Arabic/Persian search folding across private subtitle workflows, PR #359 fullscreen search, PR #358 offline tappable word cards, PR #357 source-row-aware translation errors, PR #355 translation from search matches, and the earlier offline subtitle features. The checksum sidecar was verified by the hosted build (`app-debug.apk: OK`). This is build provenance only: it does not prove physical installation, actual offline speech quality, zero network traffic or device acceptance.

From a Tuddy source checkout, after downloading the artifact ZIP as
`artifact.zip`, run the reusable integrity verifier:

```sh
bash scripts/verify-tuddy-apk-artifact.sh \\
  artifact.zip \\
  482fe89d05799205d3aae574f6ebc4f762519b1da75000edaf70ed45f5b01f09 \\
  0b4ecdd21135febb123bf733ed72258dc7ec307f23988c4efccea73fdb663d76 \\
  4bf1fad19047649c9d9c311c84de395726c08609 \\
  39750154761
```

The helper checks the ZIP digest before extraction, rejects archive path-traversal
entries, verifies the APK and its checksum sidecar, and cross-checks the embedded
build manifest, source commit and workflow run ID. It deliberately reports
integrity only; it does not claim physical installation or offline acceptance.

Previous app-code candidate (22 October 2026, after PR #333 and before PR #335):

| Field | Immutable evidence |
|---|---|
| Default-branch SHA | `fc75935de2a9646ee6f4a0f63f2a752e2288d065` |
| Hosted Android run | [#39730828470](https://github.com/Zennay/Tuddy/actions/runs/39730828470) — COMPLETED SUCCESS |
| Build checkout commit | `51dde85242ed02094f899ff03378f664611859ee` |
| Git tree SHA | `85d28d2fabd301c7ae48ad6dea5917955b1e220d` |
| Artifact | [Tuddy-debug-apk](https://github.com/Zennay/Tuddy/actions/runs/39730828470/artifacts/12405013789) (ID `12405013789`) |
| APK SHA-256 | `ed95c6ec7baa1576c2c12af519f47e0894c6f6c83c7835a9b08c75799665bbe3` |
| APK size | `103413986` bytes |
| Artifact ZIP SHA-256 | `2275f56609a701409d94abb3a32f95caf20cae608f2697871b913b29204e369a` |
| Physical installation / airplane-mode proof | **NOT PROVIDED** |

The PR #333/#335/#342 artifacts are historical; PR #344 is the latest app-code change in the current candidate. The APK and artifact ZIP digests are distinct, and neither this artifact nor any hosted build proves physical acceptance.

Previous app-code candidate (22 October 2026, after PR #318 and before PR #320):



| Field | Immutable evidence |
|---|---|
| Current default-branch SHA | `af3d796e3b8d2d85348d0de69aa8502c86d3a0ef` |
| Default-branch app-code SHA at build | `1d79ca4091386caaff678414808c8f79b87c5924` |
| Hosted Android run | [#39720729363](https://github.com/Zennay/Tuddy/actions/runs/39720729363) — COMPLETED SUCCESS |
| Build checkout commit (PR merge candidate) | `78b28f429cb0c411ff36231461e98951e5cca54b` |
| Git tree SHA for checkout and merged PR #318 main | `36a50c1327cdb813a358f36bde19cba41a1a347a` (identical) |
| Artifact | [Tuddy-debug-apk](https://github.com/Zennay/Tuddy/actions/runs/39720729363/artifacts/12400957098) (ID `12400957098`, expires 5 November 2026 04:12:43 UTC) |
| APK filename | `app-debug.apk` |
| **APK SHA-256** | `b56ba5eb8344b17599759916713bc30f922ba66e76ac46c1342cee16541cd928` |
| Artifact ZIP SHA-256 (not APK) | `5b20b00bcfc796bfb741cb66c5532754535b108e89f361e97b12e9384a3cf8fc` |
| Package | `com.livelingo.android` (legacy ID retained until deliberate migration) |
| Version | `0.4.0` |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

The PR #318 workflow built proposed-merge checkout `78b28f4…`; its tree SHA `36a50c1…` matched merged main `1d79ca4…` at that time. This is now a historical candidate, superseded by the post-#320 build above. The APK and artifact ZIP have separate digests; neither build provenance nor checksum matching proves physical installation or device acceptance.

Previous app-code candidate (22 October 2026, after PR #316 and before PR #318):

| Field | Immutable evidence |
|---|---|
| Default-branch SHA | `d0abed8680c7e50949d20aeeb6af993f42fbe1b9` |
| Hosted Android run | [#39718812370](https://github.com/Zennay/Tuddy/actions/runs/39718812370) — COMPLETED SUCCESS |
| Build checkout commit (PR merge candidate) | `8978be2c0d41c1f0091d1f1a200b2675b509ad53` |
| Git tree SHA for build checkout and main at that time | `9e0eb4dd83b8716a5b98a2fe9c142568caf85f4f` (identical on both commits) |
| Artifact | [Tuddy-debug-apk](https://github.com/Zennay/Tuddy/actions/runs/39718812370/artifacts/12399873450) (ID `12399873450`, expires 5 November 2026) |
| APK filename | `app-debug.apk` |
| **APK SHA256** | `ebc1ae54a36a7d346eb36f4a2ce91e32d998e7ad40844bfd0fa7589b97b2aa98` |
| Artifact ZIP SHA256 (not APK) | `92e5bd43986ba7da380efa0937bf2ca1abfabd5e87033f622f1508d2f34b142b` |
| Package | `com.livelingo.android` (legacy ID retained until deliberate migration) |
| Version | `0.4.0` |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

The PR #316 workflow built proposed-merge checkout (`8978be2…`), whose tree SHA `9e0eb4d…` matched default main at that time. This artifact is historical and is superseded by the post-#318 app-code candidate above. The workflow verified the APK SHA-256 before upload; the artifact ZIP digest is a different digest and must not be substituted for the APK digest.

Previous app-code candidate (22 October 2026, before PR #316):

| Field | Immutable evidence |
|---|---|
| Default-branch SHA | `8f2122efc9ad6cd1ed36787b9ae102fd83489dc6` |
| Hosted Android run | [#39717970966](https://github.com/Zennay/Tuddy/actions/runs/39717970966) — COMPLETED SUCCESS |
| Build checkout commit (PR merge candidate) | `b962a2e26a6b1844b9ddd84c6fc8d6ca1509372f` |
| Git tree SHA | `f59cced2174b61123c5b4e9aca140b8c066cc748` |
| Artifact | [Tuddy-debug-apk 12400280176](https://github.com/Zennay/Tuddy/actions/runs/39717970966/artifacts/12400280176) (expires 5 November 2026) |
| APK SHA256 | `258f7ff9297ae8b163d4c1f8de411b533e2ba31edb0b717502dc395a5036a619` |
| Artifact ZIP SHA256 | `0bd62339f7225b25b17b1064d68822ef803d7144b6553b6820e0f71678f60a8a` |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

The 8f2122 table is historical evidence only; the current app-code candidate is artifact `12401243613` from run `39721897504`. Independently verify the bundled `source-commit.txt` plus APK SHA-256 before installing. If app code changes, derive a new exact source/APK pair. Neither successful CI nor a matching checksum substitutes for a physical Android 12+ airplane-mode acceptance record.

Previous candidate (21 October 2026; superseded by the 22 October main candidate):

| Field | Immutable evidence |
|---|---|
| Source Git SHA | `ecd97aade8ca098e8653925d3dd56a0205fe97ce` |
| Hosted Android run | [#39655468714](https://github.com/Zennay/Tuddy/actions/runs/39655468714) — COMPLETED SUCCESS |
| Artifact | `Tuddy-debug-apk` (ID `12371971869`) |
| APK filename | `app-debug.apk` |
| **APK SHA256** | `620783ab4a47c868c205f8343c478ad34ed136a702c50dd23bb6987b5fb6791c` |
| Artifact ZIP SHA256 (not APK) | `b9051cc2d0f4c6b3b1403ad827ba63b1315d704aee7a1e8f9e5b0377cbd2956e` |
| Package | `com.livelingo.android` (legacy ID retained until deliberate migration) |
| Version | `0.4.0` |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

The 21 October table is historical evidence only; do not use that APK as the current main candidate. For the current candidate, download artifact `12401243613` from run `39721897504` and independently verify `source-commit.txt` plus the APK SHA-256 before installation. If main changes, derive a new exact source/APK pair. Neither successful CI nor a matching checksum substitutes for a physical Android 12+ airplane-mode acceptance record.

Previous candidate (17 October 2026; superseded by the latest main candidate):

| Field | Immutable evidence |
|---|---|
| Source Git SHA | `b1a298a907f8ff2f7459b9c934f56fafb462f502` |
| Hosted Android run | [#39064114978](https://github.com/Zennay/Tuddy/actions/runs/39064114978) — COMPLETED SUCCESS |
| Artifact | `Tuddy-debug-apk` (ID `12112063195`) |
| APK filename | `app-debug.apk` |
| **APK SHA256** | `fdc2dc8d4ec7de7293afca9ce3ed1aad832e21c3d0ba1aedef2210bc3b120bf6` |
| ZIP SHA256 (not APK) | `a376f8d1a402b0aeafceb12fcdfaf2f4b1bc3d76944d746682203cdbb1d351a9` |
| Package | `com.livelingo.android` (legacy ID retained until deliberate migration) |
| Proof of physical installation | **NOT PROVIDED** |
| Proof of airplane-mode session | **NOT PROVIDED** |

The 17 October table below is retained as historical evidence only; do not use that APK as the current main candidate. For the latest candidate, independently download the artifact from run #39655468714 and verify `source-commit.txt` plus the APK SHA256 before installation. If main changes, derive a new exact source/APK pair. Neither successful CI nor a matching checksum substitutes for a physical Android 12+ airplane-mode acceptance record.

Device report to attach to [Tuddy #1](https://github.com/Zennay/Tuddy/issues/1):
handset model/serial-free identifier, Android version/API, recognition provider
and on-device language packs, timezone, fresh-install outcome, APK digest,
source commit, timestamp, measured first-partial/final/translation latency,
memory/battery observations, pass/fail reason. **Do not submit raw private
speech or full transcripts** with reports.

## 2. Steps requiring a real Android device

1. **Permission/stop gate:** launch fresh without mic permission, deny it, then
   grant via system UI. Denied permission must never start recognition.
   Tap Stop while an utterance is ending and while a no-match 850ms retry is
   pending: no restart or new microphone request after Stop.
2. **Model provisioning (new in PR #22 candidate):** while connected to Wi-Fi,
   select the desired source and target and tap **Tekstmodel offline voorbereiden**.
   Confirm visible **Tekstmodel klaar voor offline gebruik** only for that exact
   selection. Try an unsupported Kurdish/Pashto phrasebook choice (must remain
   separate), same-source/target choice (no model requested), switch languages
   while a download is pending and background/Stop the app (old completion must
   not claim the new selection ready). Run a second independent model check after
   relaunch before airplane mode; this status is an in-memory download receipt,
   not proof of the recognizer or persistence. Provision Android's **on-device
   speech packs separately** in system settings. Record both speech and ML Kit
   models; only the latter is downloaded by Tuddy's button. Wi-Fi may be used
   for model downloading, never as a cloud speech recognition fallback.
   For the Android speech-pack request UI (PR #344), separately confirm that
   cellular-only/no-Wi-Fi and Wi-Fi without validated internet (including a
   captive portal) fail closed with an actionable message. Only active Wi-Fi
   with Android-reported internet capability and validation may start an
   explicitly requested Android-managed download. A request receipt is not
   proof that the speech pack installed; verify the pack independently.
3. **Network-disabled proof:** switch on airplane mode and independently
   disable/restrict remaining connectivity. Record at least **10** sequential
   NL→EN and EN→NL bilingual utterances; each must yield correct interim/final
   replacement with no duplicate, and on-device text translation after
   provisioning. No cloud STT fallback or silent unverified recognition.
4. **Continuous utterances and optional Android 14+ language switching:**
   wait through a final result, silence and no-match. Recognition must resume
   **only** while explicitly listening in foreground. Permission/unsupported
   language/unknown recognition errors must STOP without busy retries. On
   Android 14+ with downloaded **on-device speech** packs, opt into
   **Automatische spraaktaal** and alternate between two of the six supported
   spoken source tags. Verify only actual accepted language-detection events
   relabel the correct finalized line, never earlier lines; when the device
   reports a missing language-switch pack, the app must show an actionable
   local-model notice instead of cloud fallback. Toggle this off and confirm
   single-source behavior is unchanged. On Android 12–13 the toggle must be
   disabled and manual source selection must still work. Unknown, Kurdish and
   Pashto speech-model coverage must **not** be inferred from a text target
   or five-phrase vocabulary preview. A hosted JUnit build does not establish
   that any phone supports this optional speech API.
5. **Lifecycle race:** during the 350ms final-result and 850ms quiet retry
   windows, press Stop, change source language, lock/background the app, rotate,
   dismiss the Activity, and deny permission. No queued restart may record
   after session invalidation; old callbacks must not resurrect cleared words.
6. **Lyric timeline and privacy:** test the very first growing provisional
   caption, then at least 500 finalized lines. Follow-live must scroll during
   active speech and remain paused if the user scrolled back and disabled
   following. The list must keep at most 500 RAM-only records; old results
   must not reappear after eviction or **Wis gesprek**. Kill the process:
   previous caption history must be absent after relaunch.
7. **Word cards / accessibility:** tap source and successfully translated
   target words in a multi-script conversation. Verify the language captured
   at the time of the line remains correct after changing the picker. Test
   TalkBack/keyboard access to the editable word field. Unknown, unsupported
   and sensitive vocabulary must show a transparent 'no verified definition'
   outcome; the five Kurmanji/Sorani/Pashto beginner entries do **not** imply
   full dictionary or free-form offline translation.
8. **Offline service absent:** on a fresh device lacking speech packs, confirm
   a visible, actionable local-service/pack error and **no cloud fallback**.
   Separately confirm no raw audio/transcript persistence or network STT;
   inspect the app's local storage and network behavior without collecting
   personal speech in logs.
9. **Quality and native review:** measure latency, CPU/battery and accuracy for
   actual native-speaker examples; reserve medical, asylum and legal use for a
   qualified interpreter until accuracy is independently verified. Check
   Arabic RTL rendering and unsupported-language labeling without claims of
   Kurmanji/Sorani/Pashto ASR/model coverage.

10. **Typed local translation when speech is unavailable:** on a device without
   Android on-device speech packs, keep microphone permission denied and select
   two supported ML Kit text languages. While Wi-Fi is available, explicitly
   prepare the selected text pair, then disable network connectivity. Type
   ordinary NL/EN and Arabic Unicode sentences in **Zin typen** and tap
   **Vertaal tekst lokaal**. Confirm local output appears in the **same bounded
   RAM-only bilingual timeline**, without starting microphone recognition or
   granting a permission. Empty, over-1500-unit and binary-control text must
   never enter translation. An unavailable offline model must show an honest
   missing-model status rather than invent an answer. **Wis gesprek** must
   erase both the typed input and conversation. Reopen the app and verify the
   text/audio timeline is absent; only selected language codes may persist.
   No unsupported Kurdish/Pashto free-form model or source license is implied.

11. **Live lyric-style provisional translation:** with source/target text models
    already downloaded, use a slow 10-second spoken sentence. Check the
    second live line appears only after ~650ms of stable interim text (not
    per-word downloads), updates while speech continues and never overwrites
    a newer partial after a delayed old translation finishes. Switch target,
    press Stop, deny permission and clear the conversation while an interim
    request is in-flight; no late result may resurrect a preview. When an
    offline model is missing, raw ML Kit exception details must never be
    rendered and the preview may remain untranslated until models are
    explicitly prepared. Follow-live must scroll for both the growing source
    and the translated provisional subtitle. The source-only and hosted
    synthetic gates cannot replace this actual handset trial.

## 3. Capture the in-app offline observation report

After installing the exact APK listed in section 1 on an authorized physical
Android 12+ handset, run the real offline session described in section 2.
Keep airplane mode enabled and independently ensure Wi-Fi/mobile data remain
disabled before opening the report form; the form binds its snapshot to the
current device state.

1. Open **Offline startcheck** and choose **Leg fysieke offline-waarnemingen
   vast…**.
2. Copy the **APK SHA-256** from section 1 into the expected APK checksum field.
   Use the digest of `app-debug.apk`, never the enclosing artifact ZIP digest.
   The form compares this value with the installed base APK digest. A mismatch
   means the wrong APK or checksum is being used; do not mark it accepted.
3. Record only observations made on this device: APK identity, disconnected
   state, microphone/Stop lifecycle, absence of cloud fallback, and transcript
   clearing after relaunch.
4. Make ten attempts in each direction for offline speech and local translation
   (NL→EN and EN→NL). Enter how many succeeded (0–10) and the highest observed
   end-to-end latency in milliseconds. Do not enter sample phrases, transcripts,
   audio, names, or free-form notes.
5. Review the full report and choose an Android Storage Access Framework
   destination. Check whether that provider synchronizes files externally.
   The form clears its temporary values after dismissal or returning from the
   file picker. If device/model state changed during the picker, it refuses the
   stale report; reopen the form after restoring the intended offline state.
6. Attach the resulting report only after reviewing it for device identifiers
   and sharing consent. Keep this issue open until a human reviews the evidence.

The report is explicitly **self-reported** and **not independently verified by
Tuddy**. A complete form is not proof of zero network traffic, an independent
speech-quality assessment, a signed release, or production authorization. It
does not close issue #1 automatically. Do not attach raw speech/audio or full
private transcripts.

## 4. Evidence / release record (fill only after running the test)

- Candidate source / APK digest: **NOT YET OBSERVED ON DEVICE**
- Historical v0.4 source archive: original ZIP SHA-256 `e7721d41d4f7baf31850e3836d5655cfd3b20c2197752cc4a3ff1078fc57992c`; all 23 extracted entries have matching Git blob hashes in `archive/v0.4/LiveLingo-Android/` on branch `docs/archive-v04-source-completeness-20261022` (manifest: [`archive/v0.4/IMPORT_MANIFEST.md`](../archive/v0.4/IMPORT_MANIFEST.md)). This archive-integrity record is not evidence of a physical install; historical sources are preserved separately from the active app module.
- Device / Android API / downloaded language packs: **NOT PROVIDED**
- Airplane-mode 10-utterance report, latencies and 500-line test: **NOT RUN**
- Permission/lifecycle/stop + delayed-retry proofs: **NOT RUN**
- TalkBack, word-card, privacy and native speaker review: **NOT RUN**
- Observed blocking defect, logs without speech, remediation PR: **NONE REPORTED**
- Human acceptance / owner sign-off: **NOT GRANTED**
- **Release-ready: NO · zCloud runtime admission: NO**

The repository's P1 [#1](https://github.com/Zennay/Tuddy/issues/1)
and [#2](https://github.com/Zennay/Tuddy/issues/2) remain open. Full
historical v0.4 architecture and licensed, fully on-device Kurdish/Pashto
ASR/MT engines are not completed. Keep all production/release/worker
capabilities disabled until owner-reviewed evidence meets the gates.

## Live Android-verbindingsstatus in Tuddy

De startcheck toont nu expliciet de **vliegtuigmodus**, **wifi-radio** en door
Android zichtbare **netwerktransporten**, met handmatige vernieuwknop en
automatische verversing na terugkomst uit Instellingen. De opzettelijke
apparaatrapport-export bevat dezelfde signalen zonder SSID, IP-adres of
spraak. Zie [lokale verbindingsstatus](OFFLINE_RADIO_STATUS.md). Deze
momentopname is **geen onafhankelijk bewijs** dat alle radios uit zijn of
dat geen enkele Android-dienst netwerkverkeer uitvoert: volg onverkort
het handmatige toestel- en isolatieprotocol hierboven.
