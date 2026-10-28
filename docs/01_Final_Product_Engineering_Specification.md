# WELLBEING AI
## Final Product, Android Engineering and Release Specification

**Document version:** 1.1 — implementation handoff (corrected draft)  
**Prepared:** 25 October 2026  
**Product type:** Consumer wellness application; native Android first  
**Initial audience:** Sri Lanka / South India; English, Sinhala and Tamil  
**Status:** Implementation specification, **not** an APK, deployable backend, medical clearance, or Google Play approval.

> **Build rule:** Ship a dependable, non-diagnostic wellness experience first. AI proposes; the user reviews; verified data and deterministic calculations decide the displayed totals. In public production, laboratory interpretation, condition-driven recommendations and therapeutic herbal instructions stay disabled without qualified review and documented authorization.

## 1. Executive decision and scope

Wellbeing AI helps adults keep practical records of food, hydration, body measurements, movement and habits. It combines a Sri Lankan and South Indian food database, low-budget recipes, transparent nutrition estimates and simple progress reports. It must work over weak connections and on everyday Android hardware. The app cannot represent itself as a physician, nutritionist or clinical decision-support service.

The five uploaded source files are the baseline. This specification **expands and corrects** them and replaces the incomplete API draft with a more implementation-oriented contract. It does **not** mean the software has been coded or functionally tested.

### 1.1 Priority and release boundary

| Release | Committed scope | Release condition |
|---|---|---|
| **R0 — Prototype** | Navigation, onboarding, wireframes, curated food/recipe examples, manual log demos | Usability, privacy approach and brand approval |
| **R1 — First Google Play MVP** | 18+ onboarding, weight/height entries, suitable adult BMI display, manual food + hydration + activity, walking/workout timer, mood check-in, local recipes, budget filters, offline data and sync, reminders, export/delete, English/Sinhala/Tamil | Complete functional/security/localization testing; privacy disclosures; Play forms; signed AAB |
| **R1.1 — AI food support** | Consent-based photo recognition, portion ranges, user corrections, barcode and OCR fallback | Measured local-food evaluation, confidence calibration, abuse testing, safe fallback and cost monitoring |
| **R2 — Optional integrations** | Health Connect, selected wearable imports, grocery lists, more cuisines, shopping-price connectors | Explicit per-feature permissions, data minimization, updated disclosures |
| **R3 — Restricted clinical knowledge** | Report organizer/OCR, condition-specific content, supervised clinical rules, practitioner-reviewed traditional medicine knowledge | Medical, legal, privacy and regulator assessment with written sign-off; otherwise remain OFF |

**Correction to original scope:** The original summary informally placed photo AI alongside the first release, while the original PRD roadmap put it in P1.1. This document resolves the conflict: **photo AI is not an R1 launch blocker**, and must not be advertised as launched before R1.1 is enabled and validated.

### 1.2 Who the app serves

- General adult wellness beginners and users with limited time.
- People planning nutritious meals on tight LKR budgets.
- Users wishing to record a clinician-provided plan, **not** receive an unreviewed disease treatment plan.
- Users exploring local cuisine, international recipes and appropriately sourced traditional food knowledge.

The core app is for users **18 years and older**. The present specification restricts **adult BMI category interpretations to ages 20+**, reflecting its source draft's use of CDC adult BMI guidance. For eligible younger adults, the app can record measurements without applying adult BMI classifications pending a domain-reviewed decision. Pregnancy and other situations requiring tailored evaluation are excluded from generic BMI advice.

## 2. Product functions and acceptance criteria

### 2.1 Onboarding and account

**Screens:** Welcome → language → adult eligibility → privacy explanation → sign in / eligible offline mode → optional height/weight → goal/budget/cuisine/allergens → reminder choices → home. Use Google/OIDC or email sign-in only after identity/privacy implementation decisions; avoid unnecessary demographic data. Do not request notifications, camera or Health Connect at first run simply because a future feature exists.

**Acceptance:** The user can skip optional inputs; app language and safety copy change correctly; permissions are requested in context; minors cannot enter unsupported personal-health flows; deleting an account is discoverable in Settings and on a public web page.

### 2.2 Home and daily overview

Show confirmed food energy estimate with uncertainty, hydration total, activity minutes, optional mood check-in and recent progress. Provide one or two neutral prompts. No reward for extreme calorie restriction, no guilt language and no punitive streaks.

**Acceptance:** Aggregates derive from confirmed logs, not pending AI jobs; a date change uses the user's IANA timezone; missing inputs show “not logged,” not zero consumption; stale synced data is labeled.

### 2.3 Food log and AI-assisted food scan

**Manual first:** Search curated foods by Sinhala/Tamil/English labels and aliases. Save recent meals and edit portions. Every entry stores food-source ID/version, unit conversion, timestamp, quantity and source type.

**AI flow (R1.1):** Preview disclosure → choose photo or camera → generate purpose-limited signed upload → validate MIME/size/purpose and content → async vision candidate recognition → review identity, portion, visible components, hidden oil/sugar/coconut-milk questions → deterministic nutrition calculation → explicit **Add to log**.

**UI states:** ready; uploading; queued; processing; review; low-confidence; unsupported; failed; cancelled; consent withdrawn. Photo alone never proves the amount eaten. The user can amend leftovers or eaten fraction. If no plausible candidates exist, offer manual search; never invent foods or weights.

**Acceptance:** No server/client path may create a confirmed FoodLog from model output without the user's affirmative save action. Resubmitting the same mutation never duplicates consumption. Unknown foods return an unsupported path. The model cannot invent source IDs, calorie figures, product labels or provider instructions.

### 2.4 Barcode and packaged food

Order: barcode → matching regional catalog → confirmed label OCR → brand/size search → manual food. Different flavors/pack sizes must not be silently merged. Support per-serving, per-100 g/ml and energy reported in kJ or kcal; convert units deterministically. A scanned purchase is not automatically consumed.

**Acceptance:** Missing catalog entry always has a fallback. OCR uncertainty is shown. If user-reviewed label and old catalog differ, explicitly present the conflict rather than overwriting provenance.

### 2.5 Budget recipes and meal plans

Offer cuisine (Sri Lankan, South Indian, international), dietary pattern, allergens, prep time, available ingredients, equipment and price limits. Each recipe tracks nutrition source, portion, provenance, recipe reviewer, and price source/date. Plan daily and weekly menus with substitutions and a low-cost alternative when known. Costs marked *unknown* rather than fabricated. LKR is initial settlement/display currency; architecture supports currency localization later.

**Acceptance:** Meal planner must respect hard allergies and explicit exclusions; unavailable cost data cannot masquerade as current Sri Lankan retail prices; user can replace a meal and observe total estimated cost changes; food preferences are not medical prescriptions.

### 2.6 Body measurements, movement and hydration

Weight entries include measurement date, original units and editable corrections. BMI is **kg / m²** and presented only as a limited screening number/indicator. No BMI-derived automatic calorie prescription. Exercise sessions use monotonic elapsed timing for the active session, wall-clock timestamps for history, and safe pause/resume under app backgrounding. Store walking or workout time and user-entered perceived intensity; avoid claims of precise energy expenditure.

Hydration includes volume quick-add, custom goal if appropriate, quiet hours and snooze. If the user declares a clinician fluid restriction, suppress generic recommended water targets and direct them to use clinician guidance. Notifications must never reveal private values on a lock screen by default.

**Acceptance:** Timer survives rotation and handles process death using recoverable state. Offline entries sync once. Negative durations, impossible date ranges and nutrient units are rejected. Users can disable reminders in one tap.

### 2.7 Stress and traditional knowledge

Use an optional non-diagnostic mood/stress check-in and gentle habit support. Avoid behavioral surveillance and punitive weight messaging. Any crisis experience needs specialist-authored, region-tested resources, not improvised model responses.

Traditional content must record source author/translator, license, evidence class, review date, botanical identity where relevant and safety cautions. In the public first release only low-risk cultural educational articles and food recipes are allowed. Disease-treatment remedies, dosage schedules, contraindicated substances and unsupported cure claims are outside scope.

## 3. Recommended Android architecture

**Native stack:** Kotlin; Jetpack Compose + Material 3; ViewModel; unidirectional StateFlow; Navigation; Room; DataStore for non-sensitive preferences; WorkManager for approved sync jobs; Android Keystore-backed cryptographic materials; CameraX or Android Photo Picker when suitable; barcode/OCR SDK after license and privacy assessment. Prefer a minimal core with independent feature modules over premature microservices.

**Backend:** FastAPI Python REST / OpenAPI; PostgreSQL; object storage in distinct per-purpose namespaces; durable async task queue; auth via a vetted OIDC/Firebase/Auth provider and backend-validated JWT issuer/audience; strict row-level owner enforcement. Push via FCM only for opt-in reminders. AI key and model routing stay server-side. SDK adoption requires an inventory of actual transfers for Data safety.

### 3.1 Layered data flow

1. Compose screen observes immutable ViewModel state.
2. ViewModel invokes domain use case (log food / hydrate / start workout).
3. Repository writes an optimistic local Room record with UUID `client_mutation_id` and `pending` state.
4. WorkManager syncs eligible changes when connected; server authenticates, authorizes and deduplicates by idempotency key.
5. Server stores authoritative record/revision and returns deterministic calories and server timestamps; repository resolves conflicts and updates UI.
6. Upload jobs are separate from normal log sync and only queued after explicit image-processing consent; offline manual food remains independent.

**Conflict policy:** For append-only records use idempotent create + duplicate suppression; for edits use version/ETag compare-and-swap and preserve a user-visible conflict choice; deletions produce durable tombstones so old devices cannot resurrect records. Never use unbounded last-write-wins on sensitive user corrections.

### 3.2 Module map

| Android module | Responsibility |
|---|---|
| `app` / `core/ui` | App entry, theme, navigation, localization, accessible components |
| `core/domain` / `core/model` | BMI, nutrient math interfaces, domain IDs and constraints |
| `core/database` | Room tables, DAOs, migrations, offline outbox and tombstones |
| `core/network` / `core/security` | API clients, tokens, upload lifecycle, disclosures, permission wrappers |
| `feature/onboarding` / `profile` | Identity, consent, preferences, privacy centre |
| `feature/dashboard` / `insights` | Daily summaries, trends, data/export views |
| `feature/food` / `foodscan` | Manual food, barcode, photo analysis and review |
| `feature/recipes` / `mealplan` | Curated food database, recipe discovery, cost filtering |
| `feature/activity` / `hydration` / `mood` | Workouts, timers, water and non-diagnostic check-ins |
| `feature/reports` | **Excluded from production build or remotely gated OFF** until approved |

### 3.3 Data tables

**Core:** `users`, `consent_records`, `measurements`, `food_sources`, `food_items`, `food_logs`, `food_log_items`, `food_analysis_jobs`, `product_nutrition`, `activity_sessions`, `hydration_logs`, `mood_checkins`, `recipes`, `recipe_ingredients`, `recipe_prices`, `meal_plans`, `reminder_preferences`, `sync_mutations`, `outbox_events`, `deleted_tombstones`, `export_jobs`, `deletion_jobs`, `audit_events`.

Each user-owned table carries `id`, `owner_id`, `created_at`, `updated_at` and a revision/version. Make row authorization explicit and tested; the user ID is taken from authenticated server identity, never trusted from JSON. Partition object storage by purpose; health report blobs (if later approved) must not share routine food-photo or log access paths. Encrypt backups and rehearse restore/deletion procedures.

**Unique/consistency rules:** `(owner_id, client_mutation_id)` unique; nutrition ID/version required for confirmed logs; invariant `low <= likely <= high` when all bounds present; normalized numeric units and source trace required; completed work interval positive and consistent with timestamps; food-image job owned by same caller as associated upload.

## 4. Nutrition and AI engineering

### 4.1 Calculation definitions

- BMI: `weightKg / ((heightCm / 100)²)`; calculate only with valid input, show responsible rounding. An example 70 kg at 170 cm gives about 24.2, a mathematical example not a diagnosis.
- Label calories: normalize kilojoules to kcal using `kcal = kJ / 4.184`, and quantity to source reference serving/100 g, subject to verified labeling data.
- Total nutrients: `sum(confirmed quantity in reference units × curated nutrient record per unit)`; do not generate precision beyond source accuracy.
- Portion uncertainty: derive low/likely/high only when supported by validated amount ranges and nutrition data. Preserve “unknown” rather than inventing a probability distribution.
- Shopping basket and scanner: *purchased quantity* is not *consumed quantity*.
- Activity and hydration: keep measured quantity separate from suggestions; never compute medical targets from BMI alone.

### 4.2 Vision service safety contract

Return a typed result `food-candidates.v1` containing candidate IDs/names, alternatives, visible components, portion interval, confidence, cooking method if observed, clarification questions, image-quality status and model version. Validate shape, values, bounds, Unicode length, and allowed types on the backend before displaying it. Render user-facing candidate labels as untrusted text; never execute embedded instructions from images or captions. Rate limit and control upload size/cost.

**Dataset and evaluation:** consented photos of rice-and-curry, kottu, hoppers, string hoppers, dhal, sambol, roti, biryani, drinks, fruit, fried snacks, packaged foods and mixed servings. Use weighed reference measurements; report food identification accuracy, median portion absolute error, kcal interval coverage, false certainty and failure by cuisine, phone, lighting and demographic proxies where lawful. Publish supported food classes and failure warnings. Thresholds require product/nutrition approval, not a made-up 95% AI accuracy claim.

### 4.3 Model rollout controls

Feature flag by market and app version; job timeout and retry budget; provider fallback only after privacy and price review; model-version pinning; drift monitoring; manual fallback; safety incident kill switch; audits of opt-in data usage. AI analysis must never make medical diagnoses, prescribe supplements or authorize high-risk coaching.

## 5. Privacy, consent, security and law

**Data minimization:** collect no more than required for a selected feature. Allow purpose-specific consent to be recorded and withdrawn (`food_image_processing`, optional model-improvement training, reminders, health integrations, future report-processing). Store an immutable consent audit record with policy version and timestamp. Withdrawing a purpose blocks new processing and triggers the retention/deletion workflow promised to the user.

**Security:** TLS, encryption at rest, Keystore, secret manager, identity provider token validation, role/owner authorization, session expiry, safe password/account recovery, signed uploads/downloads (short TTL), upload virus checks, file content sniffing, EXIF handling, request caps, rate limiting, device attestation as risk signal rather than sole authorization, dependencies/SBOM scans, backup tests and incident escalation. Strip sensitive content from logs, crash reporting, analytics, push previews and third-party ad SDKs.

**Public documentation:** real HTML privacy policy URL, in-app privacy centre, public account-deletion request URL, retention schedule by data type, accessible support email, consent text in each launch language, processor/subprocessor register, breach-response plan, evidence of vendor contracts and cross-border processing assessment. Review Sri Lanka PDPA No. 9 of 2022, Amendment No. 22 of 2025, relevant 2026 notices, and Indian legal requirements with qualified counsel before regional launch; legal conclusions are **TBD**.

**Account export/deletion:** POST export request → owner-only status and expiring download; delete with extra confirmation → revoke tokens and background jobs → delete in primary DB/object store and notify processors → tombstones → declare documented backup expiry and lawful retention exceptions. Google Play requires discoverable **in-app and off-app web** account deletion if in-app accounts are created.

**Retention values remain TBD:** They must be approved by the controller and counsel; do not guess a fixed number of days. Deletion must be tested against photos, cached DB copies, push tokens, AI jobs, backups and processor copies.

## 6. High-risk feature boundary and sign-off

The lab assistant / clinical rule engine is **server-side disabled by default**. Even the `/uploads/presign` operation must reject `purpose=lab_report` until an authorized feature flag is enabled; hiding a screen is insufficient. Approved later reports may do OCR with user verification, preserved units, lab-provided reference intervals and source-page links. It must not autonomously diagnose or provide therapy. Any critical-result recognition requires a qualified clinician's locally applicable validated rules, a regulatory classification opinion, defined escalation pathway, and incident response.

Traditional medicine content may be historically interesting without proven clinical effectiveness. Each entry needs author/source/license, peer or practitioner review, evidence category and interaction/contraindication review before actionable publishing. Never fabricate named manuscripts or describe herbal disease treatment as established evidence.

**Required owners:** product owner; Android lead; backend/security lead; registered nutrition expert; privacy/legal counsel; and, only for R3, appropriately qualified clinical reviewer and regulatory advisor. No single software developer can substitute for clinical authorization.

## 7. API corrections and integration agreement

The replacement file `02_Corrected_OpenAPI_v1_1.yaml` extends the original 13 paths / 15 operations to include missing features and standard response descriptions. It is a **revised contract draft**; it is not a deployed API. It contains owner-scope and idempotency semantics, typed food candidates, purpose-separated upload validation and feature gates. The example `.invalid` server URL is intentionally non-functional.

**New/expanded routes:** measurements, activity/hydration list, mood check-ins, consent, reminder preferences, curated food search, recipes, meal plan creation, upload completion/checksum, food log edit/delete, export status and deletion status. Require authentication and resource authorization for every user-owned route.

**Before client generation:** Engineering must run an OpenAPI validator, validate example payloads against actual FastAPI models, define missing complete `201/204` response bodies and schema/ETag contract where appropriate, test pagination, decide retry and idempotency expiry and document versioning. Do not use this contract to falsely claim working backend functionality.

## 8. UI, accessibility and internationalization

Five primary tabs: Home, Food, Move, Insights, Profile. Minimum 48 dp hit targets; clear TalkBack names; dynamic type; screen-reader-friendly charts with numeric summaries; high contrast; adaptive layouts for Android 16 tablets/foldables; Compose system insets/edge-to-edge; accessible permission-denied states. Avoid hardcoded translations and nutritional units. Support local date, numeric formatting and plurals for Sinhala, Tamil and English; professional native-speaker review is a release gate.

Camera is optional. Prefer the system photo picker for choosing an existing image; camera permissions only when camera-based capture is activated. Android 13+ notification permission requested only when the user enables reminders. Use a default quiet-hours policy, opt-out control and offline notifications that respect timezone changes.

## 9. Build stages, ownership and estimates

These are planning ranges, **not delivery promises**. Times depend on staff, content and security scope.

| Stage | Indicative planning effort | Exit evidence |
|---|---|---|
| Discovery, product/legal and UX architecture | 2–3 weeks | Approved MVP, design flows, risk register, data inventory |
| Android skeleton + offline logging | 4–6 weeks | Buildable app, room migrations, unit tests, offline flows |
| Backend, auth, sync, export/delete | 4–6 weeks | Staging API, penetration/authorization tests, data lifecycle |
| Recipe corpus + budgeting + translations | 3–5 weeks overlapping | Rights review, locale QA, verified nutrition and price records |
| Quality assurance, accessibility, closed beta | 2–4 weeks | Completed checklists, crash/ANR review, testing evidence |
| R1.1 food AI / barcode | Separate 4–8+ week track | Model evaluation evidence, human review, safe fallback |

**Minimum team:** product owner, Android engineer, backend engineer, part-time UI/UX, QA, nutrition content/review, privacy/security consultant and local-language reviewers. Clinical reviewer only if a restricted module is commissioned. Assign an operational owner to restore/backups/security response.

## 10. Test strategy and measurable gates

**Unit tests:** BMI calculations at inputs/bounds; conversions; kilojoule to kcal; recipe scaling; mixed servings; sum/range ordering; date and IANA timezone; timer monotonic logic; prices; allergen filters; minimum and maximum portions.

**Integration/security:** cross-account access to every path and object; SQL/data isolation; expired/replayed JWTs; idempotent POST; offline duplicate prevention; edit conflicts and tombstones; upload purpose/MIME validation; malware handling; signed URL expiry, checksum mismatch, upload abuse and consent revocation; deletion propagation and export ownership; server-side lab-flag denial.

**UI/device:** Android 8+ as the *proposed* `minSdk=26` (confirm device reach before commitment), Android 16 API 36, low-memory phones, airplane mode, incoming phone call during workout, large text, TalkBack, non-Latin fonts, screen rotation, tablets, dark mode, no camera, denied notifications and constrained network.

**AI eval:** local foods; complex curry plates; food occlusion; poor lighting; multiple objects; handwritten/printed labels; prompt injection in photographed text; poisoning/treatment requests; ethnicity/language variance without unsafe profiling. Findings must be logged by food class.

**Hard launch blockers:** verified critical health misinformation, leaked health data, open IDOR/access control issue, unreliable deletion, false automatic food logging, blank/manual fallback failure, missing real privacy policy, unreviewed claims, R3 accessible without gate, target API below required policy or incomplete Play declarations.

## 11. Release readiness and Google Play requirements (verified 25 Oct 2026)

For new standard Android phone apps and app updates, Google Play requires **targetSdk 36+** effective 31 August 2026 (platform-specific exceptions noted by Google). `compileSdk` should also be at least 36; pick a compatible AGP/Kotlin/Compose combination after a reproducible technical spike. Test Android 16 edge-to-edge, predictive back and adaptive large-screen behavior.

The production package must be a **signed Android App Bundle (`.aab`)**. Create a Play Console account (Google currently lists a **US$25 one-time registration fee**), verify account identity, create app listing, enroll in Play App Signing and protect upload keystore securely. App title, short/full description, genuine screenshots, 512×512 store icon and 1024×500 feature graphic are required listing assets. The store listing must include the current non-medical-device disclaimer where applicable, plus guidance to consult a health professional.

Complete **Health apps declaration** (Nutrition/Weight Management and Activity/Fitness as applicable), Data safety and privacy disclosures based on actual SDK/data flows, content rating, target audience, app access/demo account if login blocks review, advertising declaration, and account deletion URL. New personal developer accounts created after 13 Nov 2023 generally need **at least 12 continuously opted-in closed testers over 14 days**, followed by production access application; organization accounts and other cases should follow what Play Console requests. Test on internal and closed tracks, resolve critical issues, then use a staged production rollout and monitor Android vitals.

The separate `03_Installation_and_Google_Play_Guide.md` provides installation and upload steps. The `05_Release_Gate_Checklist.md` is evidence-based and must be signed by owners; it is not pre-completed.

## 12. Unresolved decisions (must be explicit)

| Item | Current safe default | Decision owner |
|---|---|---|
| Trademark / legal app name / domain / package ID | All **TBD**; do not use real production ID in draft | Product / legal |
| Backend region and cross-border AI data processor | Undecided; avoid production health uploads | Privacy / security |
| Identity provider and paid infrastructure | Provider-neutral; no keys in APK | Backend lead |
| Nutrition content licensing/food labels | Curated, licensed, source-traced subset | Nutrition/product |
| Local retail price source and freshness | No live prices without verified integration | Content/commercial |
| Exact retention durations / deletion SLA | TBD pending privacy/legal assessment | Privacy owner |
| R1.1 AI evaluation acceptance thresholds | Set by nutrition and product reviewers from pilot | AI/Nutrition leads |
| BMI 18–19 classification and special populations | Suppress categories pending domain review | Clinical/nutrition |
| Country-specific crisis and health disclaimers | Hold high-risk flows until reviewed | Clinical / legal |
| Release date / commercial model | TBD after functional and Play gate | Product owner |

## 13. Source provenance and policy links

**Original uploaded specifications:** `01_Wellbeing_AI_Product_Requirements.docx`, `02_Developer_Build_Handoff.md`, `03_api_contracts.yaml`, `04_Release_Checklist.md`, `README.md` (user-supplied, 25 Oct 2026). The original proposed modular architecture, release phases, local recipe focus and user-confirmed AI logic remain the conceptual basis. This version's expanded API, sharper safety gating, corrected policy requirements, missing operational detail and release decisions are new engineering recommendations.

**Official verification:**

- Google Play target API: https://support.google.com/googleplay/android-developer/answer/11926878
- Google Play health content & disclaimer: https://support.google.com/googleplay/android-developer/answer/16679511
- Google Play health declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Account deletion: https://support.google.com/googleplay/android-developer/answer/13327111
- New personal developer account closed testing: https://support.google.com/googleplay/android-developer/answer/14151465
- Play Console registration: https://support.google.com/googleplay/android-developer/answer/6112435
- Signing: https://support.google.com/googleplay/android-developer/answer/9842756
- Store assets: https://support.google.com/googleplay/android-developer/answer/9866151
- Android 16 behavior: https://developer.android.com/about/versions/16/behavior-changes-16
- Android architecture: https://developer.android.com/topic/architecture/recommendations
- Sri Lanka DPA documents/gazettes: https://www.dpa.gov.lk/guidelines.php

**Document control:** This is a practical implementation target, not regulatory certification or an assurance that Google will approve the app. Reconfirm policy details at the actual submission date and do not declare any unchecked gate as completed.
