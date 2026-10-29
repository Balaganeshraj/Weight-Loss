# VitalDay delivery — 1.1.1-vitalday-rc2

The uploaded specifications describe an 80-item, four-stage product programme. They are requirements, not evidence that accounts, providers, live stock, AI services or clinical review already exist. This delivery implements a native, unrestricted local wellness experience and a separately runnable backend foundation. It is **not the complete Stage A–D commercial/clinical product**.

## Android implementation

- Native Compose Today / Food / Move / Plan / Care navigation, narrow-screen bottom navigation and wide-screen rail; supplied lime/near-black/pastel palette, original vector assets, device illustration, 48 dp controls, system/manual theme and app/system reduced-motion handling.
- Today shows actual journal/planner data, nullable metrics and optional last-confirmed Health Connect data with source timestamps. No fictional steps, sleep, provider availability or nutrition measurements.
- Existing food reference search/manual portions, encrypted food/water/activity/weight/mood journal, recipes and disclaimer remain. Macro totals stay unavailable when any food lacks the nutrient. Device activity never changes food intake.
- Task and meal-plan CRUD, day/week views, priority, project, local-date recurrence, per-occurrence completion, fixed-time conflict checks around the selected date, and an editable shopping list. Planned meals do not count as eaten. This is manual planning; no AI/calendar synchronization or automatic shopping consolidation is claimed.
- Separate persistent focus and workout timers. Workout logs capture user-entered exercise/reps/optional load, preserve completed sets and distinguish early finish. Movement examples are general content, not professionally reviewed exercise prescriptions or a licensed demonstration library.
- Sleep check-ins validate actual elapsed time across midnight/DST. Sleep is self-reported unless explicitly imported; no phone-based sleep measurement or diagnostic score.
- Medication list preserves entered instructions, daily taken/skipped check-ins and optional local reminders. It does not infer doses, dispense medicine, support complex dosing/clinical adherence workflows or recommend changes.
- Task/meal/medication reminders use WorkManager, Android 13+ permission handling, separate generic/private task and medication channels and boot/time changes. Planner quiet hours are 21:00–08:00; medication reminders use the chosen time. Android may delay delivery. In-app lists work without permission. Existing daily wellness reminders retain snooze/disable.
- Breathing text guidance with start/stop, no breath holds and comfort instructions. No licensed audio, music streaming or voice generation is included.
- Health Connect availability/setup, separate steps/sleep permission requests, one active cancellable manual sync, timeout, partial/failure/no-data messages, source timestamps and imported-data deletion. Steps use provider aggregation; overlapping sleep sessions are unioned. Imported snapshots stay out of the cloud outbox. This initial importer rereads current aggregates/sleep sessions; it is not a historical cursor/deletion replication engine, vendor adapter or Wear OS companion.
- Care/report/coach screens remain useful and honest: private report **notes** and medication records work; AI/OCR, secure file vault/upload, clinician sharing, booking, payment, calls and pharmacy checkout are visibly unavailable. Urgent symptoms never require payment or AI; the app directs users to local emergency care without inventing unverified local numbers.
- Existing Android application ID, Room database and Keystore alias are preserved to support an update without an intentional journal reset. New record types fit the existing encrypted payload schema. Production signing remains external.

## Language and accessibility scope

English and Tamil are now the only language choices; Sinhala resources have been removed and previous Sinhala settings fall back to English. Journal forms, navigation, promotion placeholders and assistant screens have matching English/Tamil resources. Other existing VitalDay flows still have English literals, so this is not complete app-wide Tamil localization. Native-language review and broad accessibility/device checks remain release gates.

### Promotion and assistant update
- Three static rounded 16:9 company promotion placeholders on Today. One featured slot and two secondary slots; secondary slots stack on narrow screens and sit side by side on wider screens. No live advertisements, ad SDK, company claims, tracking, placement purchase or backend inventory system.
- Explicitly labeled dummy assistant with text and voice-preview modes. English/Tamil sample responses cover planning, sleep, food and movement. Unknown questions receive a limited-demo message; selected medical/emergency keywords receive appropriate care guidance. This is not a diagnostic classifier or general-purpose AI.
- Voice preview uses only an installed offline Android TTS voice and only scripted replies. Missing voices show a readable fallback. Playback stops when leaving/backgrounding; no microphone permission, recording, recognition or live calls.
- No ElevenLabs integration or claim of universal language support. Live multilingual conversational AI requires a later authenticated backend/provider integration and appropriate disclosures. Demo text stays in screen memory and is cleared when leaving.

## Backend implementation

See `backend/README.md`: FastAPI, SQLAlchemy, PostgreSQL production configuration, explicit OIDC settings, development/test-only isolated auth, owner-scoped encrypted record API, consent/revocation, idempotency, optimistic versions, tombstones, export and service-data deletion, plus automated isolation/integrity tests. Android does not connect to this server yet. API contract is executable at `/openapi.json`.

No production deployment, identity-provider provisioning, Android account/cloud adapter, PostgreSQL load proof, staff portal, AI/OCR model, signed clinical record service, file scanning, care payment state machine or pharmacy integration is supplied. The source handoff backlog remains included so these requirements are not mistaken for completed features.

## Explicit remaining work

- Complete the remaining Stage A items: production identity/biometrics, fully reviewed recipes with ingredient weights/substitutions, dietitian-approved target flow, barcode service, licensed exercise instruction/programmes, fully consolidated meal shopping, multi-dose/snooze medication workflows and notification preference granularity.
- Complete localized new screens if Tamil is to be advertised for the full VitalDay experience.
- Configure and test cloud identity/sync, AI consent/evaluation, report processing, source-change/history handling and any device vendor integrations before enabling Stage B services.
- Obtain provider/pharmacy/payment/call contracts and implement audited staff workflows and transactional commerce before Stage C. Stage D remains future work.
- Complete stable-device, accessibility, content/clinical/security review, production identity and signing, hosted privacy policy and Play declarations before publication. Existing placeholder approvals remain in effect; they do not constitute these reviews.

## Download and update

GitHub Actions builds `VitalDay-Android-download`: installable development-signed APK, unsigned AAB, complete source ZIP, submission/signing documents and this status report. A build success is not Play approval. Development certificates can differ between CI runs; export your journal before any uninstall needed for a signature mismatch. Use an owner-controlled signing key for stable production updates.
