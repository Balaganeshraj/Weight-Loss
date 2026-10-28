# Wellbeing — Android release candidate

Native Kotlin / Jetpack Compose wellness journal for adults, based on the supplied v1.1 handoff. Version **1.0.0-rc1**, minSdk 26, compile/targetSdk 36. The default package `com.example.wellbeing.offline` is a placeholder requested by the owner.

## Implemented

- Adult onboarding; Home, Food, Move, Insights and Profile; phone bottom navigation and wide-screen rail; light/dark appearance.
- Complete English, Sinhala and Tamil app string resources and in-app language switching. All languages are included together in the App Bundle for offline switching. Native-speaker review is not claimed.
- Confirmed manual food, water, activity, mood and weight records; editing, deletion, historical dates, neutral summaries and an optional limited BMI number. No calorie prescriptions.
- Eight source-checked USDA-derived reference foods with kcal, protein, carbohydrate, fat, fibre, exact preparation descriptions, source IDs and versions. Source comparison and provenance are documented in `docs/nutrition/PROVENANCE.md`. This is not a dietitian/clinical approval or a full local-food corpus.
- Optional daily WorkManager reminders, contextual Android 13+ notification permission, notification settings fallback, quiet hours, snooze, disable controls and timezone/reboot rescheduling. Delivery is inexact.
- Monotonic movement timer with persisted state and atomic completion. A device reboot retains the previously saved accumulated duration rather than inventing time.
- Room schema 2 with AES-GCM encrypted record/outbox payloads and migration from schema 1; DataStore settings; disabled backup/transfer; JSON export and confirmed local deletion.
- Clean `JournalRepository`, `SyncQueue` and `CloudJournalGateway` interfaces, stable mutation IDs, local revisions, queue compaction, tombstones and acknowledgement/conflict handling. **No cloud provider or network permission is active.** See `docs/SYNC_READINESS.md`.
- Three separately labelled recipe ideas with cuisine and declared-allergen filters. No fabricated current prices or recipe nutrition.

## Build

Open in Android Studio with JDK 17, SDK platform 36 and build tools 35.0.0. Configure your own ignored `local.properties` and accept the SDK licenses.

```sh
./scripts/build-release.sh
# Stable dedicated device/emulator required; tests create and delete test records:
./gradlew :app:connectedDebugAndroidTest
```

In the prepared cloud workspace:

```sh
./scripts/bootstrap-android.sh
source /workspace/toolchains/android-env.sh
./scripts/build-release.sh --no-daemon
```

The cloud bootstrap uses verified tool distributions and platform proxy trust without disabling TLS/checksum checks. Existing checkouts are used; do not create worktrees unless explicitly requested.

Outputs: `app/build/outputs/bundle/release/app-release.aab`, `app/build/outputs/apk/release/`, and `app/build/outputs/apk/debug/app-debug.apk`. R8/resource shrinking are enabled for release. Read `release/SIGNING.md`: without owner-supplied signing variables the release artifacts are **unsigned**, never silently debug-signed. A separately labelled development-certificate APK may be included to test the optimized bundle.

## Submission and evidence

- `PLAY_STORE_SUBMISSION.md`: listing descriptions with verified length limits, proposed Data Safety/Health Apps answers, privacy-policy template and remaining submission actions.
- `docs/VALIDATION.md`: checks actually executed and their limits.
- `scripts/check_locales.py`: resource completeness and formatting checks.
- `scripts/verify_nutrition.py`: 40 source-value checks across eight foods.
- `docs/ICON_ATTRIBUTION.md`: attribution for the small set of vendored Material vector icons.

This is a release-candidate package, not an approved or published Play release. Replace publisher/support/policy/application-ID placeholders and provide the real upload signing identity before submission. Device testing, native-speaker and content/privacy review remain evidence-based gates. A local privacy template is not a hosted policy.

No accounts, deployed backend, actual cloud synchronization, photo AI, barcode/OCR, Health Connect, clinical report processing or treatment advice are advertised as active. The original handoff/OpenAPI documents remain in `docs/` for future phases.
