# VitalDay backend foundation

This is a runnable FastAPI/SQLAlchemy service, **not a deployed clinical backend**. The Android release does not authenticate to or upload to it. It has no Internet permission and works without this service. A future Android OIDC and explicit-consent sync adapter must map the existing encrypted outbox to these server-assigned IDs and versions; no automatic adapter is claimed.

Implemented: JWT verification, owner-derived access, explicit cloud-storage consent and revocation, deterministic idempotent create, optimistic version conflicts, tombstones, encrypted record payloads bound to owner and record ID, audit events without health contents, pagination, export and service-data deletion. Unknown nutrients remain absent. Authenticated provider/pharmacy directories explicitly return unavailable with no fabricated listings. Executable OpenAPI is available at `/openapi.json` after startup.

## Local verification

```sh
cd backend
python3 -m venv .venv
.venv/bin/pip install -r requirements.lock
.venv/bin/python -m pytest -q
```

Tests generate random, disposable secrets and synthetic records in memory. They never require cloud credentials. The SQLite adapter is for development/tests only. Production requires PostgreSQL.

## Configuration and startup

Set securely outside source:

- `VITALDAY_DATABASE_URL`: `postgresql+psycopg://...` with least-privilege credentials and TLS configured by the deployment.
- `VITALDAY_ENCRYPTION_KEY`: a Fernet key generated using `Fernet.generate_key()`; manage it in a secrets service. Losing it loses payload access. Rotation/backup procedures are a release gate.
- `VITALDAY_ENVIRONMENT`: `production` (default), `development`, or `test`.
- Production: `VITALDAY_OIDC_ISSUER`, `VITALDAY_OIDC_AUDIENCE`, `VITALDAY_JWKS_URL` (HTTPS). Only RS256, required sub/exp/iat and matching issuer/audience are accepted.
- Development/test only: `VITALDAY_DEVELOPMENT_SECRET`, at least 32 random characters. HS256 tokens require issuer/audience `vitalday-development`. Production explicitly rejects this mode and secret.

```sh
python -m vitalday.migrate  # explicit initial schema creation; never automatic in production
uvicorn vitalday.app:create_app --factory --host 127.0.0.1 --port 8000 --no-access-log
```

No sample live secret or bypass token is included. For development use an isolated SQLite URL and random development secret; do not use this mode to serve real health data. The Dockerfile uses a non-root account and disables access logs. It has not been deployed or independently security-certified.

## Boundaries and production gates

- The generic record API covers user-entered wellness records, tasks, medication instructions and report **notes**, not signed clinician notes, report files, device uploads, diagnoses or prescriptions.
- Cloud consent blocks the next authenticated records request after revocation. Export and deletion remain available so revocation never removes user rights. Identity-provider account deletion and backup expiry are not performed by service-data deletion; the response states this explicitly.
- `expected_version` is the **server** version. Android `localRevision` must not be used as a cross-device server version. Build an initial upload/pull adapter and conflict UI before enabling cloud sync.
- Add versioned schema migrations before changing this initial schema. Run PostgreSQL concurrency/load tests; current tests use SQLite and do not prove PostgreSQL production behavior.
- Configure TLS termination, bounded streaming request bodies and distributed rate limiting at ingress, managed database/object/backups encryption, secret rotation, monitoring, recovery drills and legal retention before deployment. The app rejects oversized declared requests; ingress must reject chunked/undeclared over-limit bodies too.
- OAuth account creation/recovery, biometrics, multi-factor staff access, staff portals, approved AI/OCR, secure quarantine uploads, report sharing, appointments, payment reconciliation, calls, pharmacy orders and moderation are **not implemented**. These require the contracts and reviews described in the handoff, not fake demo endpoints.
- Deployment, content/clinical review, jurisdiction review, production signing and Play approval are external gates. No production-ready or compliance claim is made.
