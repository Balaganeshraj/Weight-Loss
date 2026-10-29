# VELNUMI validation

## Current release — 1.2.0-velnumi-rc1 (version code 5)

- Full release build passed: debug/release APK, unsigned AAB, 33 JVM tests, debug/release lint and instrumentation APK compilation.
- 258 English/Tamil string keys and placeholders match; no missing-translation lint warnings. Some existing feature screens still contain English literals.
- Bundletool validates the AAB; development-signed optimized APK passes signature and 16 KB ZIP alignment checks.
- APK metadata identifies VELNUMI version 1.2.0, retaining the prior application ID for data/update compatibility.
- API 28 brand and assistant flow checks passed: VELNUMI header/tagline, Nutrition navigation, Sleep-to-Care access, light/dark theme, typed English/Tamil demo responses and transcript clearing. Visual review identified and corrected narrow-label wrapping and selected-icon contrast; the corrected brand flow passed again and light/dark captures were visually checked.
- Branding includes native vector V/leaf reconstruction, adaptive and monochrome launcher assets, notification mark, warm/red/plum palettes, onboarding, header, disclosures and release names. Original vector artwork approval and API 31/33 launcher/splash behavior still need physical-device review.

## Previous update — 1.1.1-vitalday-rc2 (version code 4)

- Clean full release build passed: debug/release APK, unsigned AAB, 33 JVM tests, debug/release lint, and instrumentation APK compilation.
- 253 resource keys and placeholders match across English and Tamil. Sinhala removed from settings, locale configuration and resource packaging. Some earlier VitalDay flows remain English-only.
- Bundletool validates the AAB. Packaged APK configurations include Tamil and no Sinhala.
- New API 28 emulator flow test passed: all three promotion slots, typed medical-boundary reply, Tamil sample reply, voice preview UI and transcript clearing on exit. Previous release device/backend results below were not rerun for this UI-only change.
- New unit coverage checks English/Tamil scripted assistant topics, unknown questions and medical/urgent-keyword precedence. This is a limited demo, not a clinical classification system.
- No live advertisements, microphone recording, live AI or ElevenLabs service is enabled. Offline TTS availability depends on installed voices; spoken audio on physical devices remains to be checked.

## Previous release evidence — 1.1.0 (28 October 2026)

Version 1.1.0-vitalday-rc1, version code 3; compile/target SDK 36, minimum API 26. Production upload signing and identity/policy placeholders remain external.

## Passed

- Complete `scripts/build-release.sh --no-daemon --max-workers=2 --console=plain`: debug/release APKs, optimized release AAB, JVM tests, debug/release lint and instrumentation compilation.
- **31 JVM tests**, zero failures/skips: calculations, portions, date/DST handling, reminder scheduling, cloud-interface consent/conflict/retry behavior, recurrence/overlap, cross-midnight/DST sleep, overlapping sleep-source union and new record validation.
- **7 Android instrumented tests passed** on the API 28 default x86 software emulator: adult onboarding, food/water saving, timer recreation/pause/save, local deletion, encrypted migration, outbox compaction/tombstones, AES-GCM tamper rejection, Room deduplication, planner per-occurrence completion, focus recovery and quiet-hours behavior that does not shift medication instructions. Full run completed with `OK (7 tests)`. Earlier failed test attempts used unsuitable accessibility/asynchronous-save queries; the corrected suite passed in full.
- **10 backend tests passed** with isolated SQLite and random test-only credentials: JWT/role rejection, consent/revocation, cross-user isolation, idempotency, version conflicts, tombstones, ciphertext/owner binding, export/deletion, input validation and unavailable provider contracts. SQLite tests do not prove PostgreSQL concurrency or deployment readiness.
- Debug/release lint: no blocking issues. Online dependency-upgrade notices remain informational so the pinned toolchain does not fail when new versions are published. Correctness/security/translation warnings remain blocking.
- At that release, 209 resource keys and format placeholders matched across English, Sinhala and Tamil. Sinhala was removed in 1.1.1 at the owner’s request. New VitalDay screens use English literals per the handoff's initial language assumption; this check does **not** claim complete new-screen translations.
- Eight-food technical nutrition verification: 40 comparisons against preserved source extracts. No professional nutrition review claimed.
- Store descriptions: 76/80 and 3050/4000 characters; file contents match the submission kit.
- Bundletool validates the final AAB. Release APK is R8-minified/resource-shrunk and separately development-signed; signature verification and 16 KB APK alignment checks pass. AAB remains unsigned. The optimized development-signed APK also installed successfully on API 28. Private signing keys are excluded from source and delivery archives.
- GitHub workflow YAML and embedded Bash/Python syntax checked. Backend gets its own validation workflow.

## Runtime evidence and limits

Real light-theme Today and Plan Compose captures from the synthetic-data device tests are included under `release/screenshots/`. The emulator previously displayed Android System UI ANR dialogs and earlier sessions could not finish tests. The final seven-test run completed; this improves the evidence but does not establish broad device stability.

Still required: API 33/36 runtime notification permission denial/revocation and delayed delivery; actual Health Connect provider reads/revocation/partial imports on supported hardware; dark/wide layouts, TalkBack, 200%-font and reduced-motion physical-device checks; low-memory/long-running timers and timezone travel; independent security, professional content and language review. No 99.5% crash-free rate, response-time SLO, clinical approval, production restore drill or Play approval is claimed.

See `docs/vitalday/DELIVERY_STATUS.md` for the unimplemented portions of the Stage A–D requirements and `backend/README.md` for backend boundaries. The Android app has no Internet permission; no backend or clinical service was deployed.
