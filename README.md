# VitalDay Android

A native Compose wellness and daily-planning app based on the supplied VitalDay handoff. Version **1.1.0-vitalday-rc1**, application ID `com.example.wellbeing.offline` (preserved for updates), minimum API 26, target API 36.

## Download

Open the repository **Actions → Android download → latest successful run → Artifacts → VitalDay-Android-download** while signed into GitHub. Extract the ZIP and install `VitalDay-installable-test.apk`. The ZIP also contains the unsigned AAB, source ZIP and submission/status documents. Internal cloud-workspace paths are not public download links.

The APK is development-signed. CI signing certificates can change; export your journal before uninstalling an older test build if Android reports a signing mismatch. Use an owner-controlled release signing key for production upgrades.

## Included

- Today, Food, Move, Plan and Care with responsive native navigation, the supplied visual palette/assets, light/dark/system appearance and reduced motion.
- Encrypted manual journals, source-checked nutrition, nutrient completeness handling and recipe examples.
- Planner with recurrence, projects, priority, per-day completion and fixed-plan checks; separate focus/workout timers, workout sets, shopping list, sleep diary, medication check-ins and local reminders.
- Optional Health Connect steps/sleep reads with granular permission requests, cancellable manual sync, freshness/partial result states and imported-copy deletion. No cloud upload.
- Clear unavailable states for AI, OCR/file reports, clinical booking/payment/calls and pharmacy partners. Private report notes remain usable.
- Separately runnable backend foundation with authenticated encrypted record APIs, consent, ownership/version/idempotency checks and automated tests. It is not connected to the Android app.

**Read [delivery status](docs/vitalday/DELIVERY_STATUS.md)** for precise coverage and missing Stage A–D items. This is not the complete clinical-commerce product or a production-approved release. New VitalDay screens use English per the new handoff; earlier journal forms retain Sinhala/Tamil translations. No professional content/translation approval is claimed.

## Build and verify

```sh
./scripts/bootstrap-android.sh
source /workspace/toolchains/android-env.sh
./scripts/build-release.sh --no-daemon
```

Android Studio users can supply JDK 17 and Android SDK 36 instead. Do not commit `local.properties`, caches, credentials or signing keys. The production application ID is configurable via `-Pwellbeing.applicationId=YOUR.OWNED.ID`; signing instructions are in [release/SIGNING.md](release/SIGNING.md).

```sh
# Dedicated test device only: tests create and delete synthetic journal data.
./gradlew :app:connectedDebugAndroidTest

# Backend tests, isolated from Android and live services:
cd backend
python3 -m venv .venv
.venv/bin/pip install -r requirements.lock
.venv/bin/python -m pytest -q
```

See [backend setup](backend/README.md), [OpenAPI](backend/openapi.json), [validation evidence](docs/VALIDATION.md), and [Play submission kit](PLAY_STORE_SUBMISSION.md). The Android app has no Internet permission and requires no backend credentials for its local features.
