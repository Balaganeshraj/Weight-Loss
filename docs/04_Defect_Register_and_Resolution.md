# Wellbeing AI — Findings, Defects, Corrections and Remaining Risks

**Review type:** Documentation + draft API consistency review of five user-supplied files (25 Oct 2026). This is **not** runtime bug hunting: no Android source, build scripts, backend or app bundle was supplied. The initial YAML is syntactically parseable; missing feature contracts are design gaps rather than syntax errors.

| ID | Severity | Identified issue in uploaded files | Change in final handoff | Status |
|---|---|---|---|---|
| D01 | High | README/summary implies AI photo tracking in initial MVP, while main PRD puts the feature in P1.1 | Defined R1 manual-first; R1.1 AI release gated by measured evaluation | **Spec clarified; not implemented** |
| D02 | High | Google Play target API listed as re-check action, no concrete Oct 2026 target | Explicit **targetSdk 36+** for new standard phone apps and updates as of 31 Aug 2026; links to official policy | **Documentation corrected** |
| D03 | High | Draft API 13 paths/15 operations cannot support all P1 tracker screens | Expanded to 24 paths/33 operations: measurement, consent, mood, recipes, reminders, mealplans, lists, export status, edits and deletion status | **Contract expanded; backend not coded** |
| D04 | Critical | Original `/uploads/presign` accepted purpose `lab_report` despite report analysis feature gate | Purpose-specific request schema plus explicit backend-deny lab uploads and `/reports` by default | **Contract tightened; server flag not implemented** |
| D05 | High | Original presign accepted `application/pdf` for food photos, a purpose/MIME mismatch | Food photo schema JPEG/PNG max 10 MB; reports PDF/JPEG/PNG max 25 MB but blocked | **Contract corrected; content inspection pending** |
| D06 | High | Original consent version not uniformly required; no consent read/withdraw workflow | Consent records, purpose-scoped update/read, denial/withdraw semantics and upload consent requirement | **Contract expanded; UI/storage pending** |
| D07 | High | Model results were `result: any object`, leaving important fields underdefined | Typed `FoodCandidate` and `FoodAnalysisResult` with uncertainty and confirmation requirement | **Partially specified; source reconciliation pending** |
| D08 | High | Food log POST had idempotency header but no full offline mutation/duplicate model | Stable UUIDs, server deduplication rules, append/edit/tombstones and conditional edit concept | **Architecture corrected; sync code pending** |
| D09 | High | Missing edit/delete FoodLog and missing GET activity/water logs | Added routes and ownership/revision requirements | **Contract expanded; implementation pending** |
| D10 | High | Upload workflow had presign but not upload checksum/ready verification step | Added `/uploads/complete` with SHA-256 and verified/denied states | **Contract expanded; malware scanning pending** |
| D11 | High | Error response standards, auth failure and ownership messages inconsistent | Shared JSON error type; 401/403/404/422/429/500 responses; documented owner authorization | **Contract revised; test pending** |
| D12 | Medium | Food list lacked pagination; no next cursor | Cursor/limit and `next_cursor` for food logs | **Contract corrected** |
| D13 | High | Nutrition data units and uncertainty rules not explicit enough to prevent incorrect arithmetic | Added kcal/kJ conversion rule, source-version trace, per-serving/100 g checks, range invariant and review workflow | **Spec corrected; numeric tests pending** |
| D14 | High | BMI age language used “adult” in several places but main source specifies 20+ for adult BMI categories | R1 eligible 18+ for basic app, only show adult BMI categories 20+ pending domain review | **Decision provisional; nutrition/clinical approval needed** |
| D15 | High | Data security described generally but processing vendor/data-retention architecture open | Added per-purpose consent, encryption, processor inventory, deletion propagation, breach/backup and retention decision matrix | **Design expanded; legal and deployment review pending** |
| D16 | High | Account deletion described but no external web request route for Play policy | Spec and Play guide require accessible in-app option **and public deletion webpage** | **Requirement corrected; website pending** |
| D17 | Medium | No installation/build/release keystore steps in the source pack | Added Android Studio, SDK, debug build, signed AAB, Play Signing, test tracks and upload guide | **Guide supplied; no build executed** |
| D18 | Medium | Release assets and personal developer closed-test requirement not explicit | Added icon 512×512, graphic 1024×500, 12/14-day rule where applicable, and Play questionnaire gates | **Guide supplied** |
| D19 | High | Traditional remedies could be misinterpreted as authorized treatment | Clear historical-use/evidence/provenance separation and professional reviews | **Spec clarified; content corpus pending** |
| D20 | Medium | Source references include health/regulatory links but were not reverified for current Play policies | Official 2026 links included; submission-time verification required | **Reviewed 25 Oct 2026** |
| D21 | Medium | API uses example.invalid host and has no deployment setup | Explicit placeholder preserved; production DNS/TLS, secrets, queue and database steps defined | **Remains TBD** |
| D22 | High | No supplied executable Android source or backend, so app behavior cannot actually be tested | Clear no-APK/no-live-server status and compilation/test gates; developer implementation needed | **Open / not an error that docs can fix** |
| D23 | Medium | `FoodLog.items` and `MealPlan.days` remain permissive/generic; response bodies incomplete on some original routes | Added ownership and structure guidance; flagged need for stronger type contracts and examples before code generation | **Partially resolved; developer must complete** |
| D24 | Medium | No formal privacy and nutrition review sign-off authority shown for each public feature | Required owners, sign-off and immutable release evidence list added | **Process defined, signatures pending** |

## Verified versus unverified

**Verified in this documentation review:** Original `03_api_contracts.yaml` is valid YAML; updated YAML parses; local `$ref` links exist; route/operation counts are 24/33; official Google Play 2026 target API, health declaration, deletion and personal-account testing rules were checked against Google documents.

**NOT verified:** OpenAPI 3.1 spec conformance by a full independent OpenAPI validator, code generation against selected Kotlin/FastAPI versions, installed Gradle/Android SDK, application compilation, database migrations, runtime security, device tests, AI accuracy, legality of collecting/processing real health data, trademark, Play listing approval, Play publication or medical/regulatory clearance.

## Prioritized action queue

**P0 before development:** Decide app name/owner, target markets, actual user account strategy, data processing region and processor, storage policy, nutrition licensing, R1 feature boundaries, privacy/legal contacts, initial package ID.

**P0 before public testing:** Implement core offline app and backend; run tests; translate and review languages; confirm data flows and privacy policy; build public deletion webpage; gather content licenses and user consent copies.

**P0 before Google Play production:** Signed AAB; relevant Play declarations, health disclaimer and real screenshots; tester evidence; no unresolved critical security or health-safety issue; written sign-off.

**P1 separate release:** AI calibration on local cuisine and consumer phone photos; data-source/version accuracy, provider DPA/data agreements, monitoring and kill switch.

**Hold indefinitely without approvals:** autonomous lab interpretation, medication/insulin guidance, medical treatment plans, therapeutic herbal dosing and disease-specific clinical recommendations.
