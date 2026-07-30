# Identity Spec — Keypair Identity for ShareCenter (`cut.the.crap`)

**Status:** Specification only — no code written. Library choice is an **open decision** (§3.2).
**Scope:** Client (`:shared`, KMP) + the `cut.the.crap` Flask server.
**Motivates:** per-user campaigns, campaign ownership, membership, and per-user work assignment.
**Last updated:** 2026-07-30

---

## 0. Why this exists

The app today has **no identity of any kind**. `AppConfig` carries two base URLs and a debug flag;
there is no `Authorization` header anywhere in `data/rest/`. The campaign feature serves one
hardcoded campaign to everyone (`CampaignRepository.CAMPAIGN_PATH = "/api/abu-safiya"`).

The target feature set needs identity:

- users **own** campaigns they authored,
- several users **join** the same campaign,
- work items are **assigned/claimed per user**, so two people don't duplicate a contact action.

This spec defines that identity as a **client-generated Ed25519 keypair**: the private key never
leaves the device, the public key registers a pseudonymous user object on the server, and requests
are authenticated by signature.

### 0.1 Explicit non-goal: X credentials

An earlier idea was to authenticate against the server using the stored `xAuthToken` / `xCt0Token`.
**Rejected.** Those are full X *session cookies* — bearer-of-account, unscoped, no expiry we
control. Sending them to the server would make our host a custody point for account-takeover
credentials, and replaying them from a datacenter IP is the classic anti-abuse signature that gets
*users'* accounts locked. They stay on-device, used only by `UrlResolver` for their existing
purpose (`XSharedLinkHandler.kt:37`).

---

## 1. Design decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | **The user is not the key.** `user` and `user_key` are separate tables. | A public key as primary key makes rotation and multi-device impossible. Expensive to retrofit. |
| D2 | **Ed25519**, not P-256. | 32-byte keys, deterministic signatures, no curve/parameter footguns. |
| D3 | **Software key, portable** — not hardware-backed (Keystore/Secure Enclave). | Hardware keys are P-256-only and non-extractable, which kills backup and multi-device. Losing campaign ownership to a dead phone is a worse failure than key extractability, given the threat model (§8). |
| D4 | **Trust on first use.** First signed request from an unknown key creates the user. | No email, no password, no PII, no recovery flow to operate. |
| D5 | **Rate limits key off IP / install ID, never off the public key.** | Keys are free; the scheme has no sybil resistance (§7). |
| D6 | **Private key is stored outside the app database.** | `DatabaseBackupManager` writes backups to Downloads — a key in the DB would be copied into a world-readable folder daily (§6.2). |

---

## 2. Data model (server)

```sql
user (
  id            TEXT PRIMARY KEY,      -- server-generated opaque id
  display_name  TEXT,                  -- optional, user-chosen, not unique
  created_at    INTEGER NOT NULL,
  last_seen_at  INTEGER
)

user_key (
  pubkey        TEXT PRIMARY KEY,      -- base64url of the raw 32-byte Ed25519 public key
  user_id       TEXT NOT NULL REFERENCES user(id),
  label         TEXT,                  -- device label, e.g. "Pixel 8"
  added_at      INTEGER NOT NULL,
  revoked_at    INTEGER                -- NULL = active
)

seen_nonce (
  nonce         TEXT PRIMARY KEY,
  expires_at    INTEGER NOT NULL       -- swept periodically; see §4.3
)
```

Indexes: `user_key(user_id)`, `seen_nonce(expires_at)`.

**Everything else in the system references `user_id`** — never `pubkey`. That indirection is the
whole point of D1: `campaign.owner_id`, `campaign_member.user_id` and `assignment.user_id` survive
every key rotation and device addition.

### 2.1 Relationship to the campaign schema

Out of scope here, specified separately, but the join points are fixed by this document:

```
campaign(id, owner_id → user.id, …)
campaign_member(campaign_id, user_id → user.id, role, joined_at)
assignment(campaign_id, item_id, user_id → user.id, state, claimed_at, expires_at)
```

---

## 3. Client-side key material

### 3.1 Storage seam

A new `platform/` binding, following the existing interface + Koin idiom used by `Clipboard`,
`Notifier` and `UrlOpener` (a plain interface in `commonMain`, one implementation per target bound
in the platform Koin module) — **not** `expect`/`actual`.

```kotlin
package cut.the.crap.platform

/**
 * Secure storage for the identity private key. Deliberately separate from the app database:
 * DatabaseBackupManager copies the database into Downloads, which must never contain a
 * signing key.
 */
interface KeyStore {
    /** The stored private key seed (32 bytes), or null if this install has no identity yet. */
    suspend fun loadSeed(): ByteArray?

    /** Persists the seed, replacing any existing one. */
    suspend fun storeSeed(seed: ByteArray)

    /** Erases the stored seed — backs the "reset identity" action in Settings. */
    suspend fun clear()
}
```

| Target | Implementation |
|---|---|
| Android | `EncryptedSharedPreferences` (or Keystore-wrapped AES over a DataStore blob). `minSdk = 26` is fine for both. |
| iOS | Keychain, `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`. |
| Desktop | A file with owner-only permissions under the app config dir. No OS keyring dependency in v1; documented as weaker. |

The *public* key and `display_name` are ordinary settings and may live in the existing
`SettingsRepository` DataStore.

### 3.2 OPEN DECISION — crypto library

`minSdk = 26` rules out the platform's own Ed25519 on Android (`Signature.getInstance("Ed25519")`
is not reliably available until far newer APIs), so an implementation must be bundled. With three
targets (`androidTarget`, `jvm("desktop")`, `iosX64`/`iosArm64`/`iosSimulatorArm64`) there are two
routes:

| Option | Assessment |
|---|---|
| **One multiplatform library** *(recommended)* | Single key encoding, single API. Candidates: `com.ionspin.kotlin:multiplatform-crypto-libsodium-bindings` (Ed25519 is core libsodium; covers all targets) and `dev.whyoleg.cryptography` (nicer API, provider-per-platform — **verify Ed25519 support before committing**). Default recommendation: the libsodium bindings. |
| Three `actual`s | BouncyCastle on Android, JDK on desktop, CryptoKit `Curve25519.Signing` on iOS. Three key encodings to reconcile; encoding mismatch is where this work historically burns days. Only if the library route fails. |

**Must be verified before implementation:** compatibility with the current toolchain
(Kotlin 2.2.20 / CMP 1.10.3) and that it introduces no transitive constraint that collides with the
AGP-8.10→9.1 upgrade currently gating dependency bumps.

Server side: `pynacl`.

> ⚠️ `deploy.sh` is an **allowlist** — a new Python dependency must actually be installed on the
> host, and an unshipped module 500s the entire site. This is exactly how the missing `social`
> package took down cutthecrap.link on 2026-07-21.

---

## 4. Wire protocol

Modelled on RFC 9421 (HTTP Message Signatures) semantics without the full machinery.

### 4.1 Header

```
Authorization: CTC-Sig keyid="<b64url pubkey>", ts=<unix-seconds>, nonce="<uuid>", sig="<b64url>"
```

### 4.2 Signing string

Exactly these five lines, joined by `\n`, no trailing newline:

```
<METHOD>
<path-and-query>
<ts>
<nonce>
<sha256hex(body)>
```

- `METHOD` uppercase (`GET`, `POST`).
- `path-and-query` as sent, including the leading `/` and any `?query`; no scheme or host.
- `sha256hex` of the raw request body bytes; for an empty body, the SHA-256 of the empty string.
- `nonce` from the existing `tools/Uuid.kt` `randomUuid()`.

### 4.3 Server verification order

1. `ts` within **±300 s** of server time → else `401 clock_skew`.
2. `nonce` not in `seen_nonce` → else `401 replay`. On success, insert with
   `expires_at = ts + 600`. **Without the nonce check the scheme is trivially replayable.**
3. `keyid` exists in `user_key` and `revoked_at IS NULL` → else `401 unknown_key` /
   `401 key_revoked`.
4. Signature verifies over the §4.2 string → else `401 bad_signature`.
5. Resolve `user_id`, touch `user.last_seen_at`, proceed.

`seen_nonce` is swept on a schedule (cron or a probabilistic sweep per request); rows older than
`expires_at` are deleted.

### 4.4 Client integration

A Ktor **client plugin** installed on the shared `HttpClient` in `networkModule`. It signs outgoing
requests to the campaign host only (not the job-queue backend). This keeps `CampaignRepository` and
every future repository entirely unaware that auth exists — no changes to
`CampaignRepository.getCampaign()` beyond the URL it already builds.

The plugin no-ops when no identity exists yet, so unauthenticated endpoints (including the legacy
`/api/abu-safiya`) keep working.

---

## 5. Endpoints

All are signed per §4 unless noted.

### 5.1 `POST /api/users` — register (TOFU)

Signed by the **new** key, which is not yet known to the server. This is the one endpoint where an
unknown `keyid` is not an error.

```jsonc
// request
{ "display_name": "…", "key_label": "Pixel 8" }
// 201
{ "user_id": "…", "display_name": "…", "created_at": 1753… }
```

Idempotent: if `keyid` already exists, returns `200` with the existing user instead of creating a
second one.

Rate limited **per IP** (D5).

### 5.2 `GET /api/users/me`

```jsonc
{ "user_id": "…", "display_name": "…",
  "keys": [ { "pubkey": "…", "label": "Pixel 8", "added_at": …, "revoked_at": null } ] }
```

### 5.3 `PATCH /api/users/me`

`{ "display_name": "…" }`

### 5.4 `POST /api/users/keys` — add a device

Signed by an **existing, non-revoked** key. The new device's key is vouched for by the old one.

```jsonc
{ "pubkey": "<b64url>", "label": "Desktop", "proof": "<b64url>" }
```

`proof` is the new key's own signature over the ASCII string
`add-key:<user_id>:<new-pubkey-b64url>`, proving the requester actually holds the new private key
rather than binding an arbitrary third-party key.

### 5.5 `DELETE /api/users/keys/{pubkey}` — revoke

Signed by any non-revoked key of the same user. Sets `revoked_at`. **A user may not revoke their
last active key** — that would orphan the identity and every campaign it owns; the client must use
"reset identity" (§6.3) instead.

### 5.6 `GET /api/ping` — signed no-op

Returns `{ "user_id": … }`. Exists purely so the protocol can be verified end-to-end before any
campaign work depends on it (§9, step 2).

### 5.7 Error shape

```jsonc
{ "error": "replay", "detail": "nonce already used" }
```

`401` for every authentication failure listed in §4.3 — the client maps these to the existing
`AppError` types and must **not** retry a `401` automatically, except once after a clock resync for
`clock_skew`.

---

## 6. Recovery, backup and reset

### 6.1 Recovery phrase

The 32-byte seed is rendered as a **BIP39-style 24-word mnemonic**, shown once at identity
creation with an explicit "write this down" step, and re-viewable from Settings behind a
confirmation. Entering the phrase on another device re-derives the same key — this is also the
fallback path for adding a device when §5.4 is impossible (original device lost).

Lost phrase + lost device = lost identity and every campaign owned by it. This is stated plainly in
the UI at creation time; there is no server-side recovery, by design (D4).

### 6.2 Interaction with database backup — **required**

`DatabaseBackupManager` writes one `.db` per day into **Downloads**. If the private key were stored
in the app database, every daily backup would drop a signing key into a world-readable location.
Hence D6: the key lives in `KeyStore` (§3.1), never in SQLDelight, and is **excluded from backup
and from Import/Export**.

The iOS backup path is being implemented now (`IOS_IMPLEMENTATION_PLAN.md`) — it must be written
with this exclusion already in place rather than retrofitted.

### 6.3 Reset identity

A Settings action that calls `KeyStore.clear()`, drops the cached `user_id`, and generates a fresh
keypair on next use. The old server-side user is **not** deleted (its campaigns and completions
still exist); it simply becomes unreachable. The UI must state that owned campaigns become
permanently unmanageable.

This doubles as the privacy control described in §7.

---

## 7. Threat model

**Protected against**

- Server compromise leaking passwords or PII — there are none; the server holds public keys, an
  opaque id, and an optional self-chosen display name.
- Impersonation by the server, or by another user — only the private-key holder can produce a valid
  signature.
- Replay of an intercepted request — nonce + timestamp window (§4.3).
- Losing a device — revoke that key (§5.5), identity and campaigns survive (D1).

**Not protected against**

- **Sybil.** Keys cost nothing; one person can mint unlimited identities. Every rate limit —
  registration, campaign creation, invite redemption, claim churn — must therefore key off IP or
  install ID (D5). Likewise the abuse kill switch disables a *campaign*, not a key.
- **Device compromise.** A software key (D3) is extractable by malware or on a rooted device. This
  was accepted in exchange for portability.
- **A malicious server serving different content to different users.** Out of scope; see §10.

### Privacy note

A pseudonymous identity plus reported campaign completions is a behavioural record: *"user 7f3a
acted on this campaign in Italy on these dates."* For activism tooling that is not a neutral
dataset. Accordingly:

- no IP logging on signed routes;
- once a claim closes, completions are aggregated to per-item counters rather than kept as
  per-user rows;
- "reset identity" (§6.3) is offered as a first-class Settings action, not buried.

---

## 8. What this does *not* decide

Specified elsewhere: the campaign schema itself, invite codes and membership roles, the
claim/done/coverage assignment protocol, web-based campaign authoring, and the abuse kill switch.
This document fixes only the identity primitive and the `user_id` those systems reference.

---

## 9. Build order

Each step is independently shippable; nothing user-visible changes before step 4.

| # | Step | Verification |
|---|---|---|
| 1 | `KeyStore` interface + three implementations; key generation; mnemonic derivation. Client-only, wired to nothing. | Unit tests in `desktopTest`: generate → sign → verify; seed → mnemonic → seed round-trip. |
| 2 | Ktor signing plugin + server-side `user` / `user_key` / `seen_nonce` tables, `POST /api/users`, `GET /api/ping`. | `/api/ping` returns the right `user_id` from all three clients. Replay and skew both rejected. |
| 3 | `GET`/`PATCH /api/users/me`, device add (§5.4) and revoke (§5.5), recovery-phrase entry. | Two installs resolve to one `user_id`. |
| 4 | Point campaign ownership and membership at `user_id`; identity section in Settings incl. reset. | Separate spec. |

Step 1 lands entirely inside `:shared` and is testable without touching the server.

---

## 10. Possible follow-on — signed campaign content

Once both sides hold the primitives, a campaign owner can sign the canonical JSON of their campaign
items, and members can verify that signature against the owner's public key. The server then
becomes untrusted for *integrity*: it can withhold a campaign but not silently alter a target list.

That is a genuinely valuable property for this domain and nearly free once §1–§5 exist. It is
explicitly **not** in v1 — it requires a canonical JSON serialization agreed by both sides, which
is its own small specification.
