#!/usr/bin/env python3
"""Fetch campaign API responses signed exactly like the ShareCenter (cut.the.crap) app.

Reimplements the app's CTC-Sig Ed25519 request signing (see
shared/src/commonMain/kotlin/cut/the/crap/data/rest/CtcSignature.kt and the
server's utils/ctc_sig.py / utils/ctc_auth.py) so we can see the JSON exactly
as an authenticated client receives it, not just the anonymous/featured view.

Credentials (the Ed25519 seed + registered user_id) are stored locally under
.secrets/campaign_auth/identity.json, which is gitignored and never committed.
Registering hits the identity API for real (POST /api/users) -- by default this
targets the LOCAL dev server, not production. Pass --server to override.

Usage:
    python3 scripts/campaign_auth_fetch.py                       # mine + default campaign
    python3 scripts/campaign_auth_fetch.py --campaign gaza-politik
    python3 scripts/campaign_auth_fetch.py --server https://cutthecrap.link
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import time
import uuid
from pathlib import Path

import requests
from nacl.signing import SigningKey

SECRETS_DIR = Path(__file__).resolve().parent.parent / ".secrets" / "campaign_auth"
IDENTITY_FILE = SECRETS_DIR / "identity.json"
RESPONSES_DIR = SECRETS_DIR / "responses"

DEFAULT_SERVER = "http://127.0.0.1:5099"
DEFAULT_CAMPAIGN = "gaza-politik"


def b64url(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()


def load_or_create_identity() -> dict:
    if IDENTITY_FILE.exists():
        return json.loads(IDENTITY_FILE.read_text())

    seed = os.urandom(32)
    key = SigningKey(seed)
    identity = {
        "seed_hex": seed.hex(),
        "keyid": b64url(bytes(key.verify_key)),
        "user_id": None,
    }
    SECRETS_DIR.mkdir(parents=True, exist_ok=True)
    IDENTITY_FILE.write_text(json.dumps(identity, indent=2))
    IDENTITY_FILE.chmod(0o600)
    return identity


def save_identity(identity: dict) -> None:
    IDENTITY_FILE.write_text(json.dumps(identity, indent=2))


def signed_request(
    server: str,
    method: str,
    path: str,
    key: SigningKey,
    keyid: str,
    body: dict | None = None,
) -> requests.Response:
    raw = json.dumps(body).encode() if body is not None else b""
    ts = int(time.time())
    nonce = str(uuid.uuid4())
    digest = hashlib.sha256(raw).hexdigest()
    message = "\n".join([method.upper(), path, str(ts), nonce, digest])
    sig = b64url(key.sign(message.encode()).signature)
    header = f'CTC-Sig keyid="{keyid}", ts={ts}, nonce="{nonce}", sig="{sig}"'
    headers = {"Authorization": header}
    if body is not None:
        headers["Content-Type"] = "application/json"
    return requests.request(method, server + path, data=raw or None, headers=headers, timeout=15)


def register_if_needed(server: str, key: SigningKey, identity: dict) -> None:
    if identity.get("user_id"):
        return
    resp = signed_request(
        server, "POST", "/api/users", key, identity["keyid"],
        body={"display_name": "campaign-api-script"},
    )
    resp.raise_for_status()
    data = resp.json()
    identity["user_id"] = data["user_id"]
    save_identity(identity)
    print(f"Registered identity user_id={data['user_id']} on {server}")


def fetch_and_save(server: str, path: str, key: SigningKey, keyid: str, out_name: str) -> None:
    resp = signed_request(server, "GET", path, key, keyid)
    RESPONSES_DIR.mkdir(parents=True, exist_ok=True)
    out_file = RESPONSES_DIR / out_name
    out_file.write_text(json.dumps({"status": resp.status_code, "body": _safe_json(resp)}, indent=2, ensure_ascii=False))
    print(f"{path} -> HTTP {resp.status_code}, saved to {out_file}")


def _safe_json(resp: requests.Response):
    try:
        return resp.json()
    except ValueError:
        return resp.text


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", default=os.environ.get("CTC_SERVER", DEFAULT_SERVER))
    parser.add_argument("--campaign", default=DEFAULT_CAMPAIGN)
    args = parser.parse_args()

    identity = load_or_create_identity()
    key = SigningKey(bytes.fromhex(identity["seed_hex"]))
    keyid = identity["keyid"]

    register_if_needed(args.server, key, identity)

    fetch_and_save(args.server, "/api/campaigns/mine", key, keyid, "campaigns_mine.json")
    fetch_and_save(args.server, f"/api/campaigns/{args.campaign}", key, keyid, f"campaign_{args.campaign}.json")


if __name__ == "__main__":
    main()
