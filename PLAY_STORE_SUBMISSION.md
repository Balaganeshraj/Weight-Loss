# Google Play submission kit — VitalDay 1.1.0-vitalday-rc1

Prepared 28 October 2026. This describes the Android release, not a connected production backend. Identity, support, privacy-policy URL and owner signing remain placeholders as requested. This is a release candidate; no Play, clinical, translation or security approval is claimed.

## Listing identity

- App title: VitalDay (working name; verify trademark/store availability).
- Category: Health & Fitness; audience adults 18+.
- Application ID: `[PRODUCTION_APPLICATION_ID]`; current upgrade-compatible default `com.example.wellbeing.offline`.
- Publisher: `[PUBLISHER_LEGAL_NAME]`.
- Support: `[SUPPORT_EMAIL]`.
- Public policy: `[PUBLIC_HTTPS_PRIVACY_POLICY_URL]`.
- App access: no login/subscription required for enabled local features; no reviewer credentials.
- Monetization: no ads, purchases, paid consultations or pharmacy sales implemented.
- Version: code 3; name 1.1.0-vitalday-rc1; minimum Android 8 (API 26); target API 36. Health Connect additionally requires a supported device/provider and permissions.
- Language: new VitalDay shell/flows are English, as specified in the new handoff. Existing journal forms retain English/Sinhala/Tamil resources. Do not advertise complete new-screen localization.

## Short description (76 characters; maximum 80)

Plan your day, log meals and movement, and make room for rest with VitalDay.

## Full description (3050 characters; maximum 4,000)

Your day, in balance.

VitalDay brings everyday wellness and personal planning into a calm Android experience. Use the core local features without an account, wearable or subscription.

TODAY
See your saved meals, water, sleep check-ins and upcoming plans. Missing information stays unavailable rather than appearing as a made-up zero. Hide calorie and weight displays when you prefer.

FOOD
Search eight source-checked food preparations or enter information from a package label. Confirm portions before saving. Track available nutrients; incomplete nutrient totals remain unavailable. Keep meal plans separate from food actually eaten, and maintain an editable shopping list. Recipe ideas are general examples, not professionally approved treatment plans.

MOVE AND REST
Log activity, run a pauseable movement timer, and record your own workout sets, repetitions and optional load. Save early finishes honestly. Record sleep across midnight and use gentle text-based breathing guidance. The app does not diagnose sleep conditions or prescribe exercise intensity.

PLAN
Create tasks with dates, times, duration, priority, projects and recurrence. Review day/week plans, mark individual occurrences complete, protect fixed-time plans and use a persistent focus timer. All changes remain under your control.

REMINDERS
Opt into local wellness, planner or medication reminders. Notification permission is optional; your in-app lists still work without it. Android and battery settings can delay delivery. Medication records preserve the instructions you enter; they never recommend a dose change or doubling a missed dose.

OPTIONAL HEALTH CONNECT
On supported devices, request separate read permissions for steps and sleep. Start or cancel a sync yourself and inspect source timestamps and partial results. Imported data stays on your phone. Food intake is never increased by wearable activity. VitalDay does not pair universally with watches or provide emergency monitoring.

PRIVATE BY DEFAULT
Journal payloads are encrypted using Android Keystore-backed protection. Export or delete local data in settings. Imported Health Connect copies can also be deleted separately. The Android app has no Internet permission, advertising or analytics. A separate backend foundation is included in the source project but is not connected to this app.

CLEAR LIMITS
The new VitalDay screens use English. Existing journal forms also retain English, Sinhala and Tamil resources. AI coaching, automated photo/barcode recognition, report-file processing, cloud accounts/sync, clinical bookings, paid calls and pharmacy checkout are not enabled. No provider availability or medicine stock is fabricated.

Nutrition values were technically checked against a pinned secondary USDA-derived source, not independently approved by a dietitian. VitalDay supports personal wellness organization; it is not a medical device, diagnosis, prescription or substitute for professional advice. Seek local emergency care for severe or urgent symptoms instead of waiting for this app.

## Data Safety — proposed answers for this Android build

Recheck the exact Console questionnaire, dependency behavior and any later integrations before submission. These answers apply to the offline Android artifact, not the separately supplied backend code.

| Question | Proposed answer / scope |
|---|---|
| Does the app collect data off-device? | No developer-controlled collection in this build. No Internet permission, analytics, ads, crash-upload SDK, cloud login or sync client. |
| Does the app share data with third parties? | No automatic sharing. A user can deliberately export a file through Android; classify that user-initiated transfer under current Console definitions. |
| Health/fitness data used locally | Meals, water, movement, optional weight/mood, sleep, prescribed-instruction notes, report notes and optional Health Connect steps/sleep. None is sent to this backend. |
| Personal planner data | User-entered tasks, plans, shopping items and focus records remain local. |
| Encryption | Journal and imported payloads use Android Keystore-backed AES-GCM. Opaque identifiers/revisions/queue metadata and ordinary UI/reminder settings are not all encrypted. Backups/device transfer are disabled. Exports are plain JSON chosen by the user and must be protected separately. |
| Deletion | Delete the local journal in Profile and privacy; delete imported Health Connect copies separately in Devices and data. Neither deletes Health Connect source records or already exported files. |
| Account creation | Not implemented in this Android release; therefore no app account deletion URL is supplied. Reassess when OIDC/account features are enabled. |
| Independent security review | Not completed; do not claim certification. |

App permission review:

- `POST_NOTIFICATIONS`: requested only when enabling local reminders; denial keeps manual features working.
- `RECEIVE_BOOT_COMPLETED` / wake lock: reschedule WorkManager reminders; approximate delivery.
- Health Connect `READ_STEPS` and `READ_SLEEP`: separate, optional read scopes. No heart-rate, location, write-health, background-health-history or universal wearable permissions.
- No camera, microphone, contacts, calendar, broad storage, location or Internet permission requested. The Health Connect setup link opens an external service only after a user action.

## Health Apps declaration — proposed responses

- Declare health/fitness functionality for nutrition/weight-management journaling, physical activity, sleep/recovery and medication reminder organization as applicable in the current form. These are wellness/self-recording functions.
- Declare Health Connect steps and sleep reads accurately. Explain that the user chooses scopes, invokes sync and reviews dates/source freshness. No records are written back to Health Connect or uploaded to a server.
- Data use is directly visible in personal wellbeing displays. No advertising, sale, insurance/employment decisions, surveillance, diagnosis or population profiling.
- The medication feature repeats user-entered prescribed instructions; it does not recommend, prescribe, adjust or dispense medicine. Reminders cannot guarantee adherence or delivery.
- No regulated medical-device, diagnostic, treatment, clinician-monitoring or emergency-service claim. Do not claim clinical validation.
- Professional booking, payment/calls, pharmacy checkout, report-file/OCR interpretation, AI, staff portals and cloud account features are unavailable. Empty states must remain explicit.
- Publish the privacy policy and Health Connect data-use disclosures before production. The permission-rationale and permission-usage intents open a dedicated disclosure that works without onboarding or database access.

## Privacy policy template

**VitalDay Privacy Policy**

Effective date: `[EFFECTIVE_DATE]`

Controller/publisher: `[PUBLISHER_LEGAL_NAME]`

Contact: `[SUPPORT_EMAIL]`

Public location: `[PUBLIC_HTTPS_PRIVACY_POLICY_URL]`

### Purpose and audience
VitalDay helps adults organize personal wellbeing and daily plans. It is not a diagnosis, prescription or emergency-monitoring service. Contact a qualified professional for medical questions and local emergency services for urgent symptoms.

### Information kept on your phone
You may enter age eligibility, optional profile information, meals, water, activity, weight/mood, sleep check-ins, tasks, shopping items, medication instructions, report notes and workout/focus logs. Health Connect access is optional and separately requests steps and sleep. Records are read on your phone and stored locally; this Android version does not upload them to the supplied backend or an AI provider.

### Health Connect choice and deletion
Your fitness source must first write to Health Connect. VitalDay imports only granted data types when you request a sync. You may revoke access in Health Connect settings. Revocation prevents further reads; existing local copies can be deleted separately. A sync rechecks permissions and removes that day's displayed snapshot fields when permission is absent. Deleting VitalDay copies does not alter source data in Health Connect.

### Security and retention
Journal payloads are encrypted with an Android Keystore key. Some technical metadata and ordinary UI/reminder preferences remain outside encrypted payloads. Android backup and device transfer are disabled. Data stays until you delete it or clear/uninstall the app. There is no developer cloud retention for this Android build. Exported JSON files are not encrypted by the app and remain at your selected destination until you remove them.

### Notifications and external actions
Notifications contain a generic check-in message, not medicine names or report values. You control Android notification permissions. Delivery can be delayed by the OS or battery restrictions. A Health Connect setup link can open the external store after your action; its own terms apply. The app does not send contacts, records, payment details or report contents in that link.

### Sharing, advertising and AI
This build has no advertising, tracking analytics, developer-controlled collection, remote AI processing or automatic third-party sharing. File export is initiated by you. Clinical providers, pharmacies, payments and cloud accounts are not connected. Any future processing requires updated disclosures, technical authorization and explicit purpose-specific choices before activation.

### Your controls
Review/edit your entries, export them, delete imported snapshots or delete the entire local journal in settings. These actions do not erase your copies exported to other locations. Contact `[SUPPORT_EMAIL]` for support after the publisher supplies a functioning address.

### Changes and jurisdiction
The publisher must supply its legal identity, operative support and hosted policy, intended markets, applicable rights and legal review. Reassess Sri Lankan and other applicable requirements before real production distribution. This template is not legal advice or a compliance certification.

## Submission gates

1. Set an owned production application ID before first publication and supply owner signing. The AAB is intentionally unsigned without those inputs; the test APK must not be used as a production upload identity.
2. Replace all identity/support/policy placeholders and host the policy. Verify brand availability and markets.
3. Complete Health Connect/Health Apps declarations and any policy approval for the exact enabled scopes. Recheck current target-SDK and account-specific Play testing requirements.
4. Complete physical-device tests for API 33/36 permission changes, background reminders, Health Connect denial/revocation/partial imports, accessibility at 200% text and lifecycle recovery. A software emulator is not a full device matrix.
5. Obtain content, native-language, privacy and security reviews. Eight-food source comparison and synthetic tests are not dietitian/clinical signatures.
6. Supply genuine final-build screenshots, store icon/feature graphic, support operations and staged rollout/rollback plan. Original handoff mockups are not runtime evidence.
7. Follow `docs/vitalday/DELIVERY_STATUS.md` for features still unimplemented. Do not advertise unavailable care, AI or cloud services.
