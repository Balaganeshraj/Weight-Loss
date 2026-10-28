#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/check_locales.py
python3 scripts/verify_nutrition.py
python3 scripts/check_submission.py
./gradlew :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebugAndroidTest "$@"
printf '%s\n' 'Artifacts: app/build/outputs/bundle/release and app/build/outputs/apk. See release/SIGNING.md before Play upload.'
