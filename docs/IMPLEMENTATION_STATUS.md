# Implementation status — 1.0.0-rc1

The owner's latest request is implemented as a release-candidate package with explicitly requested identity/policy/signing placeholders. This does not imply Play publication, a connected backend, professional content approval or completed physical-device testing.

| Requested item | Implementation / evidence |
|---|---|
| Local reminders | WorkManager unique daily work; opt-in only; Android 13+ permission prompt and denial fallback; generic private notifications; local 21:00–08:00 quiet hours; one-hour snooze respecting quiet hours; disable; boot/timezone/time-change rescheduling. OS delivery is approximate. |
| Complete translations | English/Sinhala/Tamil resource sets, matching keys and format placeholders, AppCompat/system language selection, all languages bundled offline. Missing/extra translation lint checks enabled; no blanket suppression. Native-speaker approval remains external. |
| Nutrition database | Eight exact food preparations; kcal/protein/carbohydrate/fat/fibre per 100 g; source/version preserved; deterministic scaling; 40 numeric comparisons with pinned source extracts. Primary USDA access was blocked, so provenance explicitly identifies a secondary USDA mirror. No dietitian approval claimed. |
| Offline-first cloud readiness | Repository/gateway interfaces; encrypted transactional outbox; stable mutation IDs; local revisions; compaction; deletion markers; explicit acknowledgements; retained conflicts/retries; Room migration. No connected provider, account, HTTP client or Internet permission. |
| Production build configuration | SDK 36, R8, resource shrinking, all-or-none external signing configuration, configurable production application ID. Defaults remain placeholders. |
| Release artifacts | Release AAB and APK build targets plus an installable test artifact; exact outputs/signing state are listed in the release-package README. |
| Play submission kit | Root `PLAY_STORE_SUBMISSION.md`, plain-text store descriptions, proposed Data Safety and Health Apps answers, privacy template, signing instructions and remaining gates. |

The original phase roadmap still includes optional future products (AI, barcode/OCR, cloud backend, meal-plan pricing, Health Connect and restricted clinical modules). Those are not silently enabled by sync-readiness interfaces.

## Non-code dependencies before production

- Owner-provided application identity, publisher/support details, hosted privacy policy and actual upload signing key. These were explicitly deferred in favor of placeholders.
- Stable-device validation, including API 33/36 notification behavior, language changes, large text, accessibility, persistence/migration and background recovery. See VALIDATION.md for actual test outcomes.
- Native-language, nutrition/content and privacy review appropriate to the intended market. Technical equality checks are not professional signatures.
- Genuine screenshots/store graphics, account-specific Play testing gates, current Console declarations and staged rollout.
