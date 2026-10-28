# Validation evidence — 28 October 2026

## Passed

- `scripts/build-release.sh --no-daemon`: successful, including debug/release APKs, release AAB, JVM tests, debug/release lint and instrumentation APK compilation.
- 22 JVM tests: 14 domain tests and 8 readiness tests; zero failures or skips. Covers calculations, dates/DST, timers, reminder scheduling and consent/acknowledgement/conflict/retry sync behavior.
- Debug and release lint: no issues found, including zero missing-translation warnings. All 206 resource keys and format placeholders match across English, Sinhala and Tamil.
- Nutrition: eight food preparations, 40 macro/calorie comparisons against preserved source extracts. The pinned secondary USDA mirror is documented in nutrition/PROVENANCE.md. This is technical verification, not dietitian review or current primary-source certification.
- Store descriptions: 70/80 and 2940/4000 characters; submission kit matches the description files.
- Google bundletool 1.18.2 validates the release AAB and generates a universal test APK. AAB has no signing certificate entries: it is intentionally unsigned.
- Test APK verifies with APK signature schemes v2 and v3. Its certificate is for development only.
- APK zip alignment check passes with `zipalign -c -P 16 4`. All eight packaged native libraries have ELF LOAD alignment 0x4000 (16 KB). Bundle config requests PAGE_ALIGNMENT_16K. This does not replace a 16 KB device smoke test.
- Bundle language splits are disabled so all three app locales remain available offline.
- Final release manifest has notification, boot, wake-lock, network-state and AndroidX internal receiver permissions. No INTERNET or foreground-service permission; no debuggable flag.

## Device tests remain unverified

Five instrumentation tests compile: journal flow, encryption/tamper handling, Room deduplication, migration and outbox behavior. No completed passing device-test result was obtained. Earlier API 30/28 software emulators lost Android system services before meaningful app execution. The latest API 28 retry reached boot completion and briefly exposed the package service, but installation/instrumentation did not produce a result before the environment session ended. No emulator process survived into the final packaging session.

A separate check outside the tool sandbox confirmed that /dev/kvm and /dev/dri are absent. Repeating software boots is not a substitute for stable runtime testing. Run `./gradlew :app:connectedDebugAndroidTest` on a dedicated hardware-accelerated emulator or phone. Tests create and delete synthetic journal data. Validate API 33/36 permission denial/revocation, notification delivery/snooze/disable, reboot recovery, encrypted migration, process death, language changes, TalkBack, large fonts and wide layouts before production. No verified screenshots or physical-device approval is claimed.

## Packaging and remaining gates

Version 1.0.0-rc1, code 2, compile/target SDK 36, minimum SDK 26. R8 minification and resource shrinking are enabled. The AAB is unsigned; the universal APK is test-signed. Source archives exclude build caches, local.properties and private keystores. See release/SIGNING.md and PLAY_STORE_SUBMISSION.md for owner signing, identity/policy placeholders, content review, graphics and account-specific submission gates.

The cloud setup draft contains updated start instructions. Saving the draft does not apply or publish it; review/save in environment settings and publish to reuse it. No backend deployment, Play upload or approval occurred.
