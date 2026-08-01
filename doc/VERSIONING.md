# Client/server versioning

> Status: **findings and a recommendation, nothing built yet.** Written 2026-08-01, after the
> first production deploy of the identity + campaign stack.
>
> Companion to `IDENTITY_SPEC.md` (the wire protocol) and `CAMPAIGN_SCHEMA_SPEC.md` (the payloads).
> Neither of those says what happens when the two sides disagree about the shape of things. This
> does.

## 0. The situation this has to survive

One server, many client versions, and **clients that may never update**. That asymmetry decides
almost everything below:

- The direction that matters is **old client against new server**. New-client-against-old-server
  barely exists — there is a single host and we control when it changes.
- `CAMPAIGN_SCHEMA_SPEC.md` C7 (`/api/abu-safiya` stays alive forever) is not sentiment. Its path
  is a compiled-in constant in builds already on people's phones. Nothing we do later reaches them.

So the goal is not "version the API". It is: **make the server able to change without asking
anyone's permission, and be able to prove when it is safe to drop something.**

## 1. What we already have (three facts, two of them load-bearing)

### 1.1 Additive payload changes are already free

The shared Ktor client parses with `ignoreUnknownKeys = true` (`NetworkModule.kt:46`), and every
field on `CampaignDto` / `CampaignSummaryDto` has a default. That is *why* step 1 of the campaign
work could add `id`, `title`, `description`, `state` and `role` to a payload that shipped clients
were already parsing, without touching a single one of them.

**Consequence: adding a field needs no versioning at all.** Versioning is only needed for changing
a field's meaning, or removing it. Which leads to the one rule this whole document reduces to —
see §5.

### 1.2 The signing header is accidentally forward-compatible

`utils/ctc_sig.py:parse_authorization()` collects *every* `k=v` pair it finds into a dict, and then
only checks that `keyid`, `ts`, `nonce` and `sig` are present. An unknown parameter is parsed and
ignored, never rejected.

**This means the currently deployed server already tolerates a `v=` parameter appearing later** —
which is normally the hard part of retrofitting a protocol version. We got it for free.

The scheme token, however, is compared exactly:

```python
scheme, _, rest = header.partition(" ")
if scheme != _SCHEME:          # "CTC-Sig"
    raise SignatureError("unsupported_scheme", …)
```

So `CTC-Sig-2` would be a hard 401 from every server ever deployed.
**Version inside the parameters. Never in the scheme name.**

### 1.3 `version` in the payload is *content*, not schema

`campaign.version` bumps when a campaign's contents change; it is the cache key for the durable
SQLDelight cache in `CAMPAIGN_SCHEMA_SPEC.md` §6.4. Do not overload it with shape changes — a
client would then have to re-download 47 kB because a field was added, and could not tell "new
posts" from "new format". A schema version, if we ever need one, is a **separate** number.

## 2. The three things that break, and why each needs a different answer

| What changes | Breaks old clients? | Mechanism |
|---|---|---|
| A new field in a payload | No — §1.1 | Nothing. Never remove or repurpose one. |
| A new endpoint, or new optional behaviour | No, if additive | New path. Never change what an existing path returns. |
| The signing string, `CTC-Sig` params, auth rules | **Yes, totally** | Explicit protocol version; server accepts both during the overlap |

The third row is the only one needing real machinery, for a nasty reason: **a client that cannot
authenticate cannot be told that it is outdated.** Every other failure can carry an explanation.

## 3. Recommended now: send a client version, and log it

Small, changes no behaviour, and everything else on this page depends on it.

Today the server has **no idea what is in the field**. So questions like "can we retire the alias?"
or "can we stop accepting protocol v1?" are not answerable — only guessable. One header fixes that:

```kotlin
// AppConfig gains: val clientVersion: String, val schemaVersion: Int
request.headers.append(
    "X-CTC-Client",
    "android/${config.clientVersion} schema=${config.schemaVersion}",
)
```

Server side: stamp it alongside the existing `identity_store.touch_user()`, plus a column for
anonymous callers so the unsigned campaign reads are counted too.

Two deliberate choices:

- **A header, not a signed field.** It must work on the unsigned campaign reads
  (`CAMPAIGN_SCHEMA_SPEC.md` §4.1), which have no identity to attach it to.
- **Not authenticated, so not trusted.** It is telemetry for *our* decisions, never an input to an
  authorisation check. A client can lie about it; nothing may depend on it not lying.

Do it **before** it is needed. It only starts producing data as installs update, so the lag between
shipping it and being able to use it is the entire point.

## 4. What to add when it actually bites

### 4.1 A protocol version parameter, for CTC-Sig only

Add `v=1` to the `Authorization` params now — per §1.2 the deployed server ignores it. When the
signing string must change, ship `v=2` clients while the server accepts both, dispatching on that
parameter. Retire v1 when §3's telemetry says the tail is gone.

Without the parameter, a server meeting a v2 client sees only "the signature does not verify",
which is indistinguishable from an attack and unrecoverable.

### 4.2 `minClientVersion` + `notice` in responses

So the server can say *"this build is too old, update"* once, rather than each screen failing
separately with its own unhelpful message.

This is not hypothetical: on 2026-08-01 a misconfigured server surfaced in the app as
*"Could not load your campaigns."* — a message that pointed at the campaign feature when the actual
fault was Apache stripping a header. A server-supplied notice is how that becomes one accurate
sentence instead of a debugging session.

### 4.3 A written deprecation record

C7 says the alias is forever. That is the right default for a path compiled into shipped builds,
but "forever" should be a decision backed by evidence rather than an absence of one. Once client
versions are visible, "forever" can become *"until the last install predating X is gone"* — and
that is a fact, checkable, not a promise nobody dares revisit.

## 5. What to skip, and the rule that replaces it

**Skip `/api/v1/` URL prefixes.** They mostly help new-client-against-old-server, which per §0 we
do not have. They would double every path for a case that does not arise.

The load-bearing discipline is not machinery at all:

> **Fields are append-only, and a field's meaning never changes.**

That is already how this codebase behaves, and it is already *enforced*, not merely asserted:

- `utils/campaign_payload.py:legacy_payload()` strips new keys explicitly rather than letting them
  leak into the alias — including `contacts` inside each country.
- `test_campaigns.py` compares the alias **byte-for-byte** against a payload rendered from the
  pre-migration implementation via `git archive`, so the reference is the old code rather than a
  re-description of the new.

**The cheapest extension of this idea is worth more than any version number:** keep a frozen
fixture per shipped client version and byte-compare against it in CI. A breaking change then fails
at test time, where it is free, instead of on a phone that will never be updated.

## 6. Checklist, in order

- [ ] `X-CTC-Client` header + server-side logging (§3) — do this first; everything else needs it
- [ ] `v=1` in the `CTC-Sig` params (§4.1) — inert today, unrecoverable to retrofit later
- [ ] Frozen per-version payload fixtures byte-compared in CI (§5)
- [ ] `minClientVersion` / `notice` (§4.2) — when there is a first real breaking change
- [ ] Revisit C7 with actual numbers (§4.3) — only once §3 has been shipping for a while

## 7. Related

- `IDENTITY_SPEC.md` §4 — the wire protocol these versions would describe; §4.5 for the
  optional-signature rules the unsigned reads depend on
- `CAMPAIGN_SCHEMA_SPEC.md` §4.1, C7, §6.4 — payload shape, the alias contract, the content-version
  cache
- Server repo `doc/MIGRATION.md` §8.0–8.1 — the deployment traps found the same day
  (`WSGIPassAuthorization`, root-owned data directories, venv vs system interpreter). Recorded
  there rather than duplicated here.
