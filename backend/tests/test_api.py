import time
import uuid
import jwt
import pytest
from cryptography.fernet import Fernet
from fastapi.testclient import TestClient
from sqlalchemy import select
from sqlalchemy.orm import Session
from vitalday.app import Settings, create_app, Record


@pytest.fixture
def service():
    settings = Settings("sqlite://", Fernet.generate_key().decode(), "test", development_secret=uuid.uuid4().hex + uuid.uuid4().hex)
    app = create_app(settings)
    def headers(subject="alice", **claims):
        body = {"sub": subject, "iat": int(time.time()), "exp": int(time.time()) + 600,
                "iss": "vitalday-development", "aud": "vitalday-development", **claims}
        return {"Authorization": "Bearer " + jwt.encode(body, settings.development_secret, algorithm="HS256")}
    with TestClient(app) as client:
        yield client, headers, app


def allow(client, headers):
    assert client.put("/v1/consents/cloud-storage", headers=headers, json={"granted": True, "policy_version": "test-v1"}).status_code == 200


def record(client_id="meal-1", **fields):
    return {"client_id": client_id, "payload": {"kind": "FOOD", "at": 1793178000000, "title": "Private test meal", "amount": 100, "energy": 130, **fields}}


def test_expired_missing_and_wrong_audience_authentication(service):
    c, h, _ = service
    assert c.get("/v1/records").status_code == 401
    assert c.get("/v1/records", headers=h(exp=1)).status_code == 401
    assert c.get("/v1/records", headers=h(aud="other")).status_code == 401
    assert c.get("/v1/records", headers=h(role="clinician")).status_code == 403


def test_explicit_consent_required_and_revocation_blocks_next_request(service):
    c, h, _ = service
    token = h()
    assert c.post("/v1/records", headers=token, json=record()).status_code == 403
    allow(c, token)
    assert c.post("/v1/records", headers=token, json=record()).status_code == 200
    c.put("/v1/consents/cloud-storage", headers=token, json={"granted": False, "policy_version": "test-v1"})
    assert c.get("/v1/records", headers=token).status_code == 403
    assert c.get("/v1/account/export", headers=token).status_code == 200


def test_record_ownership_cannot_be_supplied_or_crossed(service):
    c, h, _ = service
    a, b = h(), h("bob")
    allow(c, a); allow(c, b)
    first = c.post("/v1/records", headers=a, json=record()).json()
    assert c.get(f'/v1/records/{first["id"]}', headers=b).status_code == 404
    assert c.put(f'/v1/records/{first["id"]}', headers=b, json={"expected_version": 1, "payload": record()["payload"]}).status_code == 404
    assert c.get("/v1/records", headers=b).json()["items"] == []
    forged = record(); forged["owner"] = "bob"
    assert c.post("/v1/records", headers=a, json=forged).status_code == 422


def test_idempotency_and_changed_payload_conflict(service):
    c, h, _ = service
    token = h(); allow(c, token)
    first = c.post("/v1/records", headers=token, json=record()).json()
    again = c.post("/v1/records", headers=token, json=record()).json()
    assert first == again
    assert c.post("/v1/records", headers=token, json=record(energy=140)).status_code == 409
    assert len(c.get("/v1/records", headers=token).json()["items"]) == 1


def test_optimistic_conflicts_and_deleted_records_do_not_resurrect(service):
    c, h, _ = service
    token = h(); allow(c, token)
    first = c.post("/v1/records", headers=token, json=record()).json()
    path = f'/v1/records/{first["id"]}'
    body = {"expected_version": 1, "payload": record(energy=150)["payload"]}
    assert c.put(path, headers=token, json=body).json()["version"] == 2
    assert c.put(path, headers=token, json=body).status_code == 409
    assert c.delete(path + "?expected_version=1", headers=token).status_code == 409
    assert c.delete(path + "?expected_version=2", headers=token).status_code == 200
    assert c.get(path, headers=token).status_code == 404
    assert c.post("/v1/records", headers=token, json=record()).status_code == 409


def test_ciphertext_export_and_deletion_are_owner_scoped(service):
    c, h, app = service
    a, b = h(), h("bob"); allow(c, a); allow(c, b)
    c.post("/v1/records", headers=a, json=record())
    c.post("/v1/records", headers=b, json=record())
    with Session(app.state.engine) as db:
        rows = db.scalars(select(Record)).all()
        assert all("Private test meal" not in row.sealed for row in rows)
        assert rows[0].sealed != rows[1].sealed
    exported = c.get("/v1/account/export", headers=a).json()
    assert len(exported["records"]) == 1
    assert c.post("/v1/account/delete", headers=a, json={"confirmation": "DELETE"}).status_code == 200
    assert c.get("/v1/account/export", headers=a).json()["records"] == []
    assert len(c.get("/v1/account/export", headers=b).json()["records"]) == 1


def test_unknown_macros_stay_unknown_and_invalid_amount_rejected(service):
    c, h, _ = service
    token = h(); allow(c, token)
    assert c.post("/v1/records", headers=token, json=record(amount=-1)).status_code == 422
    stored = c.post("/v1/records", headers=token, json=record()).json()["payload"]
    assert "protein" not in stored["metadata"]
    assert c.get("/v1/records?limit=1000", headers=token).status_code == 422


def test_unconfigured_services_do_not_fabricate_availability(service):
    c, h, _ = service
    assert c.get("/v1/providers", headers=h()).json()["available"] is False
    assert c.get("/v1/pharmacies", headers=h()).json()["items"] == []
    assert c.get("/v1/capabilities").json()["payments"] is False


def test_production_rejects_development_configuration():
    with pytest.raises(ValueError):
        create_app(Settings("sqlite://", Fernet.generate_key().decode(), "production"))


def test_ciphertext_cannot_be_reassigned_between_owners(service):
    c, h, app = service
    a, b = h(), h("bob"); allow(c, a); allow(c, b)
    one = c.post("/v1/records", headers=a, json=record()).json()
    two = c.post("/v1/records", headers=b, json=record()).json()
    with Session(app.state.engine) as db:
        first, second = db.get(Record, one["id"]), db.get(Record, two["id"])
        first.sealed = second.sealed
        db.commit()
    response = c.get(f'/v1/records/{one["id"]}', headers=a)
    assert response.status_code == 500
    assert "Private test meal" not in response.text
