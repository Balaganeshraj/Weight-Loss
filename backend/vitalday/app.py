"""Owner-scoped, consent-gated API. Health content stays encrypted at rest.

The Android client remains offline-first; configuring this service does not
silently enable uploads or provision an identity/payment/clinical provider.
"""

import base64
import hashlib
import hmac
import json
import os
import time
import uuid
from contextlib import asynccontextmanager
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Annotated, Literal

import jwt
from cryptography.fernet import Fernet
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict, Field, model_validator
from sqlalchemy import Boolean, Integer, String, Text, UniqueConstraint, create_engine, delete, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column, sessionmaker
from sqlalchemy.pool import StaticPool


@dataclass(frozen=True)
class Settings:
    database_url: str
    encryption_key: str
    environment: str = "production"
    issuer: str = ""
    audience: str = ""
    jwks_url: str = ""
    development_secret: str = ""

    @classmethod
    def from_env(cls):
        return cls(os.environ["VITALDAY_DATABASE_URL"], os.environ["VITALDAY_ENCRYPTION_KEY"],
                   os.getenv("VITALDAY_ENVIRONMENT", "production"), os.getenv("VITALDAY_OIDC_ISSUER", ""),
                   os.getenv("VITALDAY_OIDC_AUDIENCE", ""), os.getenv("VITALDAY_JWKS_URL", ""),
                   os.getenv("VITALDAY_DEVELOPMENT_SECRET", ""))

    def validate(self):
        Fernet(self.encryption_key.encode())
        if self.environment == "production":
            if not self.database_url.startswith("postgresql"):
                raise ValueError("Production requires PostgreSQL and managed encrypted storage")
            if not self.issuer.startswith("https://") or not self.jwks_url.startswith("https://") or not self.audience:
                raise ValueError("Production OIDC issuer, audience and HTTPS JWKS are required")
            if self.development_secret:
                raise ValueError("Development authentication is forbidden in production")
        elif self.environment in {"development", "test"}:
            if len(self.development_secret) < 32:
                raise ValueError("An explicit development signing secret of at least 32 characters is required")
        else:
            raise ValueError("Unknown environment")


class Base(DeclarativeBase):
    pass


class Record(Base):
    __tablename__ = "records"
    __table_args__ = (UniqueConstraint("owner", "client_id"),)
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    owner: Mapped[str] = mapped_column(String(200), index=True)
    client_id: Mapped[str] = mapped_column(String(120))
    version: Mapped[int] = mapped_column(Integer, default=1)
    sealed: Mapped[str] = mapped_column(Text)
    created_digest: Mapped[str] = mapped_column(String(64))
    deleted: Mapped[bool] = mapped_column(Boolean, default=False)


class Consent(Base):
    __tablename__ = "consents"
    owner: Mapped[str] = mapped_column(String(200), primary_key=True)
    granted: Mapped[bool] = mapped_column(Boolean)
    policy_version: Mapped[str] = mapped_column(String(40))
    changed_at: Mapped[int] = mapped_column(Integer)


class Audit(Base):
    __tablename__ = "audit_events"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    owner: Mapped[str] = mapped_column(String(200), index=True)
    action: Mapped[str] = mapped_column(String(40))
    at: Mapped[int] = mapped_column(Integer)


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)


class Payload(StrictModel):
    kind: Literal["FOOD", "WATER", "ACTIVITY", "WEIGHT", "MOOD", "TASK", "SLEEP", "MEDICATION", "MEDICATION_EVENT", "FOCUS_SESSION", "MEAL_PLAN", "SHOPPING", "REPORT"]
    at: int = Field(ge=0, le=4102444800000)
    title: str = Field(max_length=120)
    amount: float | None = Field(default=None, ge=0, le=30000)
    energy: float | None = Field(default=None, ge=0, le=30000)
    note: str = Field(default="", max_length=2000)
    source: str = Field(default="User entered", max_length=200)
    source_version: str = Field(default="user-v1", max_length=100)
    metadata: dict = Field(default_factory=dict)

    @model_validator(mode="after")
    def check_domain(self):
        if len(json.dumps(self.metadata, allow_nan=False)) > 16000:
            raise ValueError("Metadata is too large")
        if self.kind in {"FOOD", "TASK", "MEDICATION", "SHOPPING", "REPORT", "MEAL_PLAN"} and not self.title.strip():
            raise ValueError("A title is required")
        ranges = {"FOOD": (1, 3000), "WATER": (1, 3000), "ACTIVITY": (.016, 1440), "SLEEP": (1, 1440), "TASK": (1, 1440), "MEAL_PLAN": (1, 1440), "WEIGHT": (20, 500), "MOOD": (1, 5), "FOCUS_SESSION": (.016, 1440)}
        if self.kind in ranges:
            low, high = ranges[self.kind]
            if self.amount is None or not low <= self.amount <= high:
                raise ValueError("Amount is outside the supported range")
        if self.kind == "FOOD" and self.energy is None:
            raise ValueError("Confirmed food energy is required; unsupported nutrients remain unknown")
        return self


class CreateRecord(StrictModel):
    client_id: str = Field(min_length=1, max_length=120, pattern=r"^[A-Za-z0-9_-]+$")
    payload: Payload


class ReplaceRecord(StrictModel):
    expected_version: int = Field(ge=1)
    payload: Payload


class ConsentRequest(StrictModel):
    granted: bool
    policy_version: str = Field(min_length=1, max_length=40)


class DeleteAccount(StrictModel):
    confirmation: Literal["DELETE"]


def create_app(settings: Settings | None = None):
    settings = settings or Settings.from_env()
    settings.validate()
    vault = Fernet(settings.encryption_key.encode())
    options = {"pool_pre_ping": True}
    if settings.database_url.startswith("sqlite"):
        options.update(connect_args={"check_same_thread": False}, poolclass=StaticPool)
    engine = create_engine(settings.database_url, **options)
    factory = sessionmaker(engine, expire_on_commit=False)
    # Schema setup is explicit: production uses the separate migrate command.
    if settings.environment in {"development", "test"}:
        Base.metadata.create_all(engine)
    jwks = jwt.PyJWKClient(settings.jwks_url, timeout=5) if settings.environment == "production" else None

    @asynccontextmanager
    async def lifespan(_app):
        yield
        engine.dispose()

    app = FastAPI(title="VitalDay API foundation", version="0.1.0", lifespan=lifespan,
                  description="Offline companion backend. No AI, clinical, pharmacy, payment, upload or staff service is enabled.")
    app.state.engine = engine

    @app.middleware("http")
    async def security_headers(request: Request, call_next):
        # The ingress must also enforce streaming body size/rate limits.
        length = request.headers.get("content-length")
        if length and (not length.isdigit() or int(length) > 65536):
            return JSONResponse({"detail": "Request body too large"}, status_code=413)
        response = await call_next(request)
        response.headers["Cache-Control"] = "no-store"
        response.headers["X-Content-Type-Options"] = "nosniff"
        return response

    def database():
        with factory() as session:
            yield session

    Db = Annotated[Session, Depends(database)]

    def identity(authorization: Annotated[str | None, Header()] = None):
        if not authorization or not authorization.startswith("Bearer "):
            raise HTTPException(401, "Authentication required")
        token = authorization[7:]
        try:
            if jwks:
                claims = jwt.decode(token, jwks.get_signing_key_from_jwt(token).key, algorithms=["RS256"],
                                    audience=settings.audience, issuer=settings.issuer, options={"require": ["sub", "exp", "iat"]})
            else:
                claims = jwt.decode(token, settings.development_secret, algorithms=["HS256"], audience="vitalday-development",
                                    issuer="vitalday-development", options={"require": ["sub", "exp", "iat"]})
            owner = claims["sub"]
            if not isinstance(owner, str) or not 1 <= len(owner) <= 200:
                raise ValueError("Invalid subject")
            if claims.get("role", "member") != "member":
                raise HTTPException(403, "This service accepts member access only")
            return owner
        except HTTPException:
            raise
        except Exception:
            raise HTTPException(401, "Invalid or expired session") from None

    Owner = Annotated[str, Depends(identity)]

    def permit(db, owner):
        consent = db.get(Consent, owner)
        if not consent or not consent.granted:
            raise HTTPException(403, "Explicit cloud-storage consent is required")

    def audit(db, owner, action):
        db.add(Audit(id=str(uuid.uuid4()), owner=owner, action=action, at=int(time.time())))

    def canonical(payload):
        return json.dumps(payload.model_dump(), sort_keys=True, separators=(",", ":"), allow_nan=False).encode()

    def digest(body):
        return hmac.new(base64.urlsafe_b64decode(settings.encryption_key), body, hashlib.sha256).hexdigest()

    def seal(owner, record_id, payload):
        envelope = {"owner": owner, "record_id": record_id, "payload": payload.model_dump()}
        return vault.encrypt(json.dumps(envelope, allow_nan=False).encode()).decode()

    def result(record):
        try:
            envelope = json.loads(vault.decrypt(record.sealed.encode()))
            if envelope["owner"] != record.owner or envelope["record_id"] != record.id:
                raise ValueError("Ciphertext binding mismatch")
        except Exception:
            raise HTTPException(500, "Stored record integrity check failed") from None
        return {"id": record.id, "client_id": record.client_id, "version": record.version,
                "payload": envelope["payload"]}

    def owned(db, owner, record_id):
        record = db.scalar(select(Record).where(Record.id == record_id, Record.owner == owner, Record.deleted.is_(False)))
        if not record:
            raise HTTPException(404, "Record not found")
        return record

    @app.get("/healthz")
    def health():
        return {"status": "ok", "service": "vitalday-foundation"}

    @app.get("/v1/capabilities")
    def capabilities():
        return {"records": True, "cloud_consent_required": True, "ai": False, "clinical_booking": False,
                "payments": False, "pharmacy": False, "report_upload": False, "staff_portal": False}

    @app.put("/v1/consents/cloud-storage")
    def consent(body: ConsentRequest, owner: Owner, db: Db):
        row = db.get(Consent, owner)
        if not row:
            row = Consent(owner=owner, granted=body.granted, policy_version=body.policy_version, changed_at=int(time.time()))
            db.add(row)
        else:
            row.granted, row.policy_version, row.changed_at = body.granted, body.policy_version, int(time.time())
        audit(db, owner, "cloud_consent_granted" if body.granted else "cloud_consent_revoked")
        db.commit()
        return {"granted": row.granted, "changed_at": row.changed_at, "existing_records_deleted": False}

    @app.post("/v1/records")
    def create_record(body: CreateRecord, owner: Owner, db: Db):
        permit(db, owner)
        raw = canonical(body.payload)
        hashed = digest(raw)
        previous = db.scalar(select(Record).where(Record.owner == owner, Record.client_id == body.client_id))
        if previous:
            if previous.deleted or previous.created_digest != hashed:
                raise HTTPException(409, "Client ID already used with different or deleted content")
            return result(previous)
        record_id = str(uuid.uuid4())
        record = Record(id=record_id, owner=owner, client_id=body.client_id, sealed=seal(owner, record_id, body.payload), created_digest=hashed, version=1, deleted=False)
        db.add(record)
        audit(db, owner, "record_created")
        try:
            db.commit()
        except IntegrityError:
            db.rollback()
            previous = db.scalar(select(Record).where(Record.owner == owner, Record.client_id == body.client_id))
            if previous and not previous.deleted and previous.created_digest == hashed:
                return result(previous)
            raise HTTPException(409, "Concurrent client ID conflict") from None
        return result(record)

    @app.get("/v1/records")
    def records(owner: Owner, db: Db, limit: int = 50, after: str = ""):
        permit(db, owner)
        if not 1 <= limit <= 100:
            raise HTTPException(422, "Limit must be between 1 and 100")
        rows = db.scalars(select(Record).where(Record.owner == owner, Record.deleted.is_(False), Record.id > after).order_by(Record.id).limit(limit + 1)).all()
        return {"items": [result(r) for r in rows[:limit]], "next_cursor": rows[limit - 1].id if len(rows) > limit else None}

    @app.get("/v1/records/{record_id}")
    def get_record(record_id: str, owner: Owner, db: Db):
        permit(db, owner)
        return result(owned(db, owner, record_id))

    @app.put("/v1/records/{record_id}")
    def replace_record(record_id: str, body: ReplaceRecord, owner: Owner, db: Db):
        permit(db, owner)
        owned(db, owner, record_id)
        changed = db.execute(update(Record).where(Record.id == record_id, Record.owner == owner, Record.version == body.expected_version, Record.deleted.is_(False)).values(sealed=seal(owner, record_id, body.payload), version=body.expected_version + 1))
        if changed.rowcount != 1:
            db.rollback()
            raise HTTPException(409, "Stale record version; fetch and resolve before retrying")
        audit(db, owner, "record_updated")
        db.commit()
        db.expire_all()
        return result(owned(db, owner, record_id))

    @app.delete("/v1/records/{record_id}")
    def delete_record(record_id: str, expected_version: int, owner: Owner, db: Db):
        permit(db, owner)
        owned(db, owner, record_id)
        changed = db.execute(update(Record).where(Record.id == record_id, Record.owner == owner, Record.version == expected_version, Record.deleted.is_(False)).values(deleted=True, sealed="", version=expected_version + 1))
        if changed.rowcount != 1:
            db.rollback()
            raise HTTPException(409, "Stale record version")
        audit(db, owner, "record_deleted")
        db.commit()
        return {"deleted": True}

    @app.get("/v1/account/export")
    def export(owner: Owner, db: Db):
        # Revocation must not block a data subject from exporting or deleting.
        rows = db.scalars(select(Record).where(Record.owner == owner, Record.deleted.is_(False))).all()
        audit(db, owner, "export_requested")
        db.commit()
        return {"format": "vitalday-foundation-v1", "records": [result(r) for r in rows]}

    @app.post("/v1/account/delete")
    def delete_account(body: DeleteAccount, owner: Owner, db: Db):
        db.execute(delete(Record).where(Record.owner == owner))
        db.execute(delete(Consent).where(Consent.owner == owner))
        db.execute(delete(Audit).where(Audit.owner == owner))
        db.commit()
        return {"local_service_data_deleted": True, "identity_provider_account_deleted": False,
                "note": "Identity-provider deletion and backup expiry require configured production operations."}

    @app.get("/v1/providers")
    def providers(owner: Owner):
        return {"items": [], "available": False, "reason": "No verified provider service configured"}

    @app.get("/v1/pharmacies")
    def pharmacies(owner: Owner):
        return {"items": [], "available": False, "reason": "No verified pharmacy partner configured"}

    return app
