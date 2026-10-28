# Wellbeing AI — Installation, Build, Test and Google Play Publishing Guide

**Date checked:** 25 October 2026. **Important:** This package includes corrected specifications, **not a finished Android Studio project or executable APK/AAB**. The following steps describe how developers create the project, get it running, and submit **after implementation**. UI labels, requirements and fees can change; verify Play Console prompts at submission.

## A. Accounts, tools and prerequisites

1. **Computer:** Windows 11 / recent macOS / Linux, enough free storage for Android Studio, SDK and emulator; use a physical Android device when emulator is slow.
2. **Android Studio:** download the current stable version from https://developer.android.com/studio and install the matching Android SDK components. Use the **bundled JDK** unless your chosen Gradle/AGP version requires otherwise.
3. **Android SDK:** install Android 16 / API 36 or higher platform and suitable build tools; use a Gradle / AGP / Kotlin / Compose version matrix supported by the Android release used. Do not blindly paste latest versions with an incompatible plugin.
4. **Source control:** Git, organization repository with protected `main`, PR checks, secret scan and branch rules.
5. **Backend (when online features are built):** Python 3.12+ if compatible with selected libraries, FastAPI, PostgreSQL, production object storage, queue, secrets manager, secure HTTPS host, domain and alerting. Exact versions and vendor choice require an engineering decision record.
6. **Google Play:** Google account with Play Console developer registration, identity verification and available payment card. Google currently documents a **US$25 one-time developer registration fee** (organization/personal identity steps vary): https://support.google.com/googleplay/android-developer/answer/6112435
7. **App identity:** decide final brand and unique package ID such as `com.yourcompany.wellbeingai`. **This is an example, not a registered or assigned ID.** After publication, applicationId changes create a different Play app.

## B. Create the Android source project

1. Open **Android Studio → New Project → Empty Activity (Jetpack Compose)**.
2. Set Name to the approved product name and Package name to the approved final application ID. Language: **Kotlin**. Proposed `minSdk=26` for Android 8.0+; **confirm reach and dependency compatibility** before locking this decision.
3. Install **API 36 or later** and configure an equivalent app module target (example only; plugin/version lines are intentionally omitted):

```kotlin
android {
    namespace = "com.yourcompany.wellbeingai" // replace before build
    compileSdk = 36
    defaultConfig {
        applicationId = "com.yourcompany.wellbeingai" // choose and lock
        minSdk = 26 // proposed product coverage, subject to verification
        targetSdk = 36 // Google Play minimum for new standard phone apps in Oct 2026
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

4. Gradually add modules described in `01_Final_Product_Engineering_Specification.md`. Start **with Home + manual food/water/workout and offline Room database**. Do not create fake screens for every future feature and label the app finished.
5. Use Android Studio's **SDK Manager** and Gradle Sync to resolve toolchain compatibility. Check generated manifests and final merged manifest for unnecessary permissions. In R1 there should be **no lab-report storage path or dangerous unneeded Health Connect permission**.
6. Add English/Sinhala/Tamil resources under separate locale-qualified `res/values-*` files; ensure font coverage, bidi/rendering, local numeral preferences and native-speaker sign-off.
7. Create an optional `.env.example` for backend development only. Real OAuth secrets/AI keys must live in backend secret storage, never APK, Gradle source or committed files.

## C. Developer build and local installation

After the developers have implemented the Android project and checked in a Gradle wrapper:

**Windows (PowerShell):**

```powershell
cd path\to\wellbeing-ai\android
.\gradlew.bat clean testDebugUnitTest assembleDebug
.\gradlew.bat connectedDebugAndroidTest  # physical/emulator device attached
```

**macOS / Linux:**

```bash
cd /path/to/wellbeing-ai/android
./gradlew clean testDebugUnitTest assembleDebug
./gradlew connectedDebugAndroidTest  # device/emulator attached
```

Connect an Android phone with developer options and USB debugging enabled (or launch an emulator). Click **Run** in Android Studio. Alternatively, after a successful build:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The path shown is Android Studio's conventional **debug** output for an `app` module and only exists **after the source project is built**. An Android App Bundle (`.aab`) is the Play submission artifact, **not** a directly installable APK. Do not ship the debug APK or debug signing key to Play.

**Offline demo:** Manual logs and dashboard must remain functional without backend. During early development, use an explicit **DEVELOPMENT ONLY** backend/mock configuration. Production must not silently fall back to fake calorie prices, fake authentication or demo data.

## D. Backend environment and production prerequisites

The `02_Corrected_OpenAPI_v1_1.yaml` file is a contract, not executable FastAPI code. The backend team must implement routes and generated model validation. Before enabling cloud features:

1. Provision separate **development**, **staging** and **production** resources; use managed PostgreSQL and encrypted object storage.
2. Set identity provider JWT issuer/audience, disable wildcard CORS, reject foreign owner IDs, enforce SQL and object-store authorization, and rate limit uploads.
3. Configure strong credentials in a managed secrets service; set up database migrations, backup encryption, incident logging without health data, automated restore tests and alerting.
4. Add idempotency, consent, file purpose checks, malware scanning, signed URL expiry, deletion propagation and audit metadata. Lab feature flag `report_assistant_enabled=false` in production and staging until approved.
5. Set `API_BASE_URL` using build flavors/build-time configuration **only for a reviewed public endpoint**. No API keys embedded in the app. Replace `https://api.example.invalid/v1` with the approved TLS host.
6. Create a staging seed with synthetic non-identifying food/test data, not copies of real uploaded user records.
7. Run schema validation, contract tests and security tests before linking Android to the staging API.

## E. Mandatory pre-release QA

**User flows:** sign-up/offline path; 18+ eligibility; manual food edit/delete; changes across Sinhala/Tamil/English; broken network; re-connect without duplicates; hydration restrictions; workout timer under app lifecycle; user-requested account export/deletion; navigation, TalkBack, large text and tablet layouts.

**R1.1 food AI extra gate:** purpose-specific consent; file sanitization; photo retake; low-confidence fallback; no auto-save; verify exact label conversions and cost/quality thresholds; confirm server-side AI key isolation. No in-production AI food feature before evaluation sign-off.

**Health safety:** disclaimer and guardrails; no clinical treatment suggestions; lab/report routes remain rejected server-side; no unsupported supplement/medicine dosing. Review all store listing health claims and text with nutrition/legal owners.

**Device and performance:** Play pre-launch report, Android vitals, ANR/crash-free behavior, low-memory device, Android 16 edge-to-edge/predictive back, foldable/tablet layouts. Rebuild and retest after dependency or manifest changes.

## F. Create a release signing key and AAB

1. Back up the final chosen package ID and application name in a secure product record.
2. In Android Studio choose **Build → Generate Signed Bundle / APK → Android App Bundle**.
3. Create a dedicated **upload key** if needed (prefer this over reusing private signing materials), store `.jks` and passwords in controlled secret storage, and keep an offline escrow/backup. **Never commit the keystore or passwords to Git.**
4. Select release variant, enable R8/ProGuard only with reviewed keep rules, and generate the **signed `.aab`**. Typical output is `app/build/outputs/bundle/release/app-release.aab` but actual path follows module/flavor names.
5. Test this precise release artifact through Play internal testing; resolve minification/runtime class issues. Generate and preserve mapping files, source revision, dependency SBOM and checksums.
6. Enroll/confirm **Play App Signing** in Play Console. Distinguish the developer-controlled **upload key** from Google's app signing key: https://support.google.com/googleplay/android-developer/answer/9842756
7. For every new version increment integer `versionCode`; update `versionName` appropriately. Never reuse an uploaded `versionCode`.

A `.aab` is not installed with `adb install`. To test locally, run a debug APK or use `bundletool`/Play test delivery; the signed AAB goes to Play Console.

## G. Create Google Play Console listing

1. Sign in at https://play.google.com/console/ → **Create app**. Set default language, final name, App (not game), free/paid status, developer support email and required declarations.
2. **App identity:** final `applicationId` from AAB must match the Play app. Complete organization/personal account verification as requested.
3. **Store listing:** app name (Google lists max 30 characters), short description (max 80), full description (max 4,000), app category, languages, support contact and genuine screenshots taken from the real functioning app.
4. **Assets:** 512×512 32-bit PNG Play icon (max 1024 KB), **1024×500** JPEG or 24-bit PNG feature graphic, plus screenshots of actual working screens. See https://support.google.com/googleplay/android-developer/answer/9866151
5. **Privacy:** publish a publicly accessible **HTML** privacy policy page and link it both in Play and inside the app. Create a **public account deletion request webpage** that works even after uninstall and clearly references the app/developer. In-app deletion must also be available if accounts are created.
6. **App content:** complete Data safety based on verified actual SDK/API collection, sharing and retention. Complete **Health apps declaration** for relevant Activity/Fitness and Nutrition/Weight Management features; complete content rating, target audience, ads declaration, app access (test credentials if login), and other displayed questionnaires.
7. **Non-medical-device description/disclaimer** where applicable: “This app is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Consult a healthcare professional for medical advice, diagnosis, or treatment.” Ensure actual app function is consistent with this disclaimer; disclaimer alone does not override regulation.
8. **Distribution:** choose countries/regions for which legal/privacy, language, support, food content and price sources are actually ready. Start with a controlled Sri Lankan pilot if approved; do not indiscriminately open all countries.

Google's current Health policy: https://support.google.com/googleplay/android-developer/answer/16679511  
Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291  
Account deletion requirement: https://support.google.com/googleplay/android-developer/answer/13327111

## H. Upload and release the Android App Bundle

1. In Play Console, complete all mandatory dashboard tasks and the store listing; upload from **Testing → Internal testing → Create release** (current UI may vary).
2. Add the signed `.aab`, review app signing certificate, package name, target SDK and Play's warnings; provide release notes; save the test release.
3. Add internal testers, distribute install link and collect QA feedback and crash/ANR reports. Verify account deletion, multilingual rendering, privacy and offline flows in a Play-delivered install.
4. Run a **closed testing** track and log tester feedback/fixes. **For personal accounts created after 13 November 2023, Google currently requires at least 12 closed testers continuously opted in for 14 days before applying for production access.** Testers should genuinely exercise the app; Google may require more testing.
5. After test conditions are met, apply for **production access** if Play Console requires it. Submit truthfully documented testing, feedback and fixes.
6. In **Production → Create new release**, promote the verified AAB/version after sign-off, choose countries, check release review/notes, and choose a small staged rollout if offered (e.g. staged cohorts). Never declare rollout complete until Play confirms publication.
7. Monitor Play reviews, Android vitals, crash-free sessions, ANR, login failures, deleted-account completion, API job costs and safety reports daily during launch. Feature flags should allow rapid **disable of AI features** while leaving the safe offline core usable.

Test requirements: https://support.google.com/googleplay/android-developer/answer/14151465  
Create app: https://support.google.com/googleplay/android-developer/answer/9859152  
Prepare release: https://support.google.com/googleplay/android-developer/answer/9859348

## I. Common upload/rejection errors and recommended fixes

| Symptom / Play warning | Likely reason | Correct response |
|---|---|---|
| Target API level too low | `targetSdk` below Play policy | Move to API 36+ for standard phone new/update apps as of Oct 2026; retest Android 16 behavior |
| Incorrect app bundle/application ID | Package differs or key mismatch | Check merged manifest and Play app signing; do not change published ID casually |
| Duplicate version code | Reused `versionCode` | Increment versionCode and rebuild signed AAB |
| Data safety mismatch | SDK/API transmission not declared | Inventory real data practices and correct app behavior + disclosure before resubmission |
| Health-policy rejection | No declaration, bad claims, missing disclaimer/privacy | Complete health app form; remove misleading claims; add reviewed disclaimer and policy |
| Account deletion rejection | In-app or off-app deletion route missing | Implement both, test as user after uninstall, disclose retained data |
| App access blocked | Reviewer can't sign in | Provide test credentials and exact reviewer instructions in Play Console securely |
| Store screenshot/content misleading | Mock screens or unshipped AI features displayed | Use only actual release screenshots and description of released capabilities |
| Closed-test gate not met | Personal-account testing requirements | Maintain required real opted-in testers and engage them for full period |
| Crash/ANR in production | Release minification, permissions, sync/recovery bugs | Stop rollout/disable feature, examine Play vitals, release a tested fix |

## J. Minimum approval package before pressing Release

- Product owner signed requirements and supported markets.
- Android lead: reproducible signed AAB, SDK/API, permissions, devices and analytics checks.
- Backend/security: auth/IDOR, backup/restore, deletion, upload limits, signed URLs, infra ownership.
- Nutrition reviewer: reviewed local data, allergens, budget-source truthfulness, nutrition calculation test results.
- Privacy/legal reviewer: PDPA/India/legal scope, policy, consent, vendors and Data safety.
- QA lead: functional, regression, localized accessibility and Play internal/closed beta evidence.
- For any R3 module: separate explicit clinical, privacy and regulatory approvals (**not assumed**).

**Critical final reminder:** These instructions do not deploy or upload an app on their own. First implement, integrate, sign and test the actual Android source. The final Play approval decision belongs to Google and applicable regulators, not this document.
