# Wellbeing AI — Evidence-Based Android / Play Store Release Gate

**Rule:** Keep boxes unchecked until a named person signs evidence. Store screenshots, reports, test logs, code commit and Play Console status under each release's sign-off folder.

## 1. Product / rights / design

- [ ] Final app name, legal owner, domain, unique application ID and trademark checks complete.
- [ ] Public release contains **only** R1-approved functioning features; no mock AI diagnosis or lab report claims.
- [ ] Real English, Sinhala and Tamil strings and safety disclosures independently proofread.
- [ ] Accessibility: TalkBack, large fonts, high contrast, keyboard/focus, tablets and reduced motion passed.
- [ ] Authentic app screenshots and owned/licensed visual assets ready.
- [ ] Current recipes, allergens, nutrition data sources/versions and third-party licenses checked.
- [ ] No fabricated price, calorie, product labels, clinical references or historical-text attributions.

## 2. Android / backend / calculations

- [ ] Final AAB targetSdk **36+** for standard Android mobile submission in October 2026, compatible compileSdk/toolchain recorded.
- [ ] Android 16 edge-to-edge, predictive back and adaptive device checks passed.
- [ ] Package name, signing and versionCode verified; release build produces signed AAB.
- [ ] Room migrations upgrade from last supported schema with no data loss.
- [ ] Manual food, hydration, workout, weight and mood features work offline.
- [ ] Offline mutation/retry duplication, edits, tombstones, client clocks/timezones verified.
- [ ] BMI calculation, measurement validation and 20+ adult-category restriction checked.
- [ ] Nutrient unit conversion including kJ/kcal, grams/servings and known-source records unit-tested.
- [ ] Workouts, lifecycle restore, elapsed time, backgrounding and notification states tested.
- [ ] Reminder quiet hours/permission denial and clinician fluid restriction suppression verified.
- [ ] Staging/prod backend TLS, backups, restore, DB migrations, secret rotation and alerting completed.

## 3. Food AI (R1.1 gate — mark N/A if not shipped)

- [ ] Purpose-specific image processing consent before external processing.
- [ ] Photo MIME/size, content sniffing, malware/EXIF policy and signed URL lifetimes tested.
- [ ] Food image job returns structured candidates/portions with uncertainty and retake path.
- [ ] **No** automatic FoodLog persistence before affirmative user confirmation.
- [ ] All AI arithmetic cross-checked against deterministic versioned nutrition records.
- [ ] Local weighed/label ground truth set analyzed by cuisine, image quality and phone tier.
- [ ] Prompt injection, invalid result, unsupported foods, provider outage, cost/rate throttles tested.
- [ ] AI kill switch disables cloud scan while preserving manual logging.

## 4. Safety / privacy / law

- [ ] Lab report screen, `lab_report` upload and report processing all unavailable server-side unless separately signed off.
- [ ] No disease-treatment/medication/herbal dosage advice in the public general wellness release.
- [ ] Health claims and non-medical-device disclaimer reviewed against actual features.
- [ ] Privacy policy live as public, accessible HTML (not merely a PDF).
- [ ] Privacy policy and in-app disclosures match collection, model processing, analytics and processors.
- [ ] Data safety entries checked against real SDK and HTTP data transfers; no sensitive analytics leakage.
- [ ] Age gate, consent version history, revocation and user export tested.
- [ ] In-app account deletion AND public web account deletion request functional where accounts exist.
- [ ] Photo, DB, cloud, cached and processor deletion plus backup-retention policy tested.
- [ ] Cross-account IDOR, JWT expiry, privilege escalation and signed URL abuse tests passed.
- [ ] Sri Lanka and expansion-country privacy/legal review documented with appropriate counsel.
- [ ] Traditional-content rights, evidence status and interaction review signed where applicable.

## 5. Google Play Console

- [ ] Verified developer account, payment/setup and organizational ownership ready.
- [ ] Store name, 80-character short text, 4,000-character full text and real app screenshots ready.
- [ ] 512×512 Play icon, 1024×500 feature graphic, support contacts and locale listing ready.
- [ ] Health apps declaration completed with actual health features.
- [ ] Data safety, account deletion URL, target audience, content rating, app access and ads declarations correct.
- [ ] Play App Signing activated; private upload keystore backed up and access controlled.
- [ ] Internal release installed by real testers through Play.
- [ ] Closed-test requirement met **if** account type/date requires it; feedback and fixes archived.
- [ ] Play pre-launch report, Android vitals and crash/ANR review passed.
- [ ] Countries/regions limited to approved support, legal and content coverage.
- [ ] Production staged rollout, rollback/update and emergency feature-disable process confirmed.

## 6. Sign-off (write names, dates and evidence links)

| Approval | Name / date | Evidence location | Result |
|---|---|---|---|
| Product owner | TBD | TBD | Pending |
| Android tech lead | TBD | TBD | Pending |
| Backend/security lead | TBD | TBD | Pending |
| QA lead | TBD | TBD | Pending |
| Nutrition reviewer | TBD | TBD | Pending |
| Privacy / legal counsel | TBD | TBD | Pending |
| R3 clinical / regulatory (only if applicable) | TBD | TBD | Not approved |

**Status at document creation:** No items have been checked. No Play release or clinical approval is claimed.
