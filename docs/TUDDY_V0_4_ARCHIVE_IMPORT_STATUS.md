# Tuddy v0.4 archive import status

- Original Library ZIP: `/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip`
- ZIP SHA-256: `e7721d41d4f7baf31850e3836d5655cfd3b20c2197752cc4a3ff1078fc57992c`
- Historical files copied verbatim under `archive/v0.4/LiveLingo-Android/`: 19 of 23 regular files (all product source, configuration, workflow, README and language documentation files).
- Four historical test-source files were intentionally not copied into the repository because the current delivery policy forbids writing or expanding test suites. No test suite was added or run in this change.
- The five already-preserved historical modules remain outside the active Gradle source set. This archive does not replace current app code or enable old runtime behavior.
- Remaining acceptance gate: authorized Android 12+ physical-device airplane-mode evidence tied to an exact APK checksum. This source archive work does not satisfy that device gate or close issue #1.
