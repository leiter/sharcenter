# Campaign Schema Spec — Multi-User Campaigns for ShareCenter (`cut.the.crap`)

**Status:** §8 **steps 1–2 implemented** — schema, `GET /api/campaigns/{id}` and
`GET /api/campaigns/mine`, the Abu-Safiya import, and the campaign list and detail screens that
replace the dead country screen. Steps 3–4 still specification only.
**Depends on:** `IDENTITY_SPEC.md` — every `user_id` here is the one that spec defines.
**Scope:** Client (`:shared`, KMP) + the `cut.the.crap` Flask server + a small web authoring UI.
**Last updated:** 2026-07-30

---

## 0. Why this exists

Today the campaign feature serves **one hardcoded campaign to everyone**. `CampaignRepository`
GETs a constant path (`CAMPAIGN_PATH = "/api/abu-safiya"`), the server generates that payload from
a markdown file plus a country registry in `routes/abusafiya.py`, and every install receives the
identical bytes.

Three things should change:

1. **Users author their own campaigns**, and several users join the same campaign.
2. **Work is coordinated** — assignments and completions are tracked, so a group can see coverage
   instead of everyone guessing.
3. **The campaign entry screen becomes useful.** `CampaignCountryScreen` today is an inert list:
   `CampaignCountryItem` has no `onClick` at all, and the payload fields `CampaignCountry.url`,
   `hasParliamentAction` and `Campaign.locateUrl` are parsed and then never used. Once campaigns
   are plural, that screen has an obvious job — it becomes the campaign list.

---

## 1. Design decisions

| # | Decision | Rationale |
|---|---|---|
| C1 | **Invite-only by default. No public directory, no discovery, no browse.** | "Coordinate N people onto a target list" is mechanically the same as brigading. Without discovery it is a tool for a group that already knows each other, not a recruitment surface for strangers. Public campaigns are a separate, later decision (§9). Operator-`featured` campaigns are readable without an identity (§4.1) — that is the app finding its own bundled campaign, and it is still not a directory: nothing lists, searches or surfaces a campaign a user was not invited to. |
| C2 | **Author on the web, act in the app.** | Campaign CRUD is the most screen-heavy, least reusable UI in the system, and would be built three times over Android/desktop/iOS. Authoring is a desk activity; the Flask site already exists and can be iterated without a release. |
| C3 | **Assignment policy differs per item kind.** Amplification overlaps; contact actions are disjoint. | Ten people replying to the same post is the desired outcome. Ten identical letters to the same MP get filtered as spam. One policy cannot serve both. |
| C4 | **Claims expire.** Unacted claims return to the pool. | Static sharding ("user1 gets A–R") silently fails when user1 never opens the app, and nothing detects it. A TTL makes coverage self-healing. |
| C5 | **`claim` is atomic and may grant less than requested.** | Two users opening the app simultaneously must not both be assigned the same contact action. The response states what was actually granted; the client renders that, never its own request. |
| C6 | **Server-side kill switch from day one.** | It will be needed exactly once, at an inconvenient hour, and retrofitting it under pressure is miserable. |
| C7 | **`/api/abu-safiya` stays alive as an alias.** | Its path is a hardcoded constant in shipped clients (`CampaignRepository.kt:32`). Breaking it strands every install that has not updated. |

---

## 2. Data model (server)

```sql
campaign (
  id            TEXT PRIMARY KEY,
  owner_id      TEXT NOT NULL REFERENCES user(id),
  slug          TEXT UNIQUE,            -- optional, for the web authoring URL
  title         TEXT NOT NULL,
  description   TEXT,
  locate_url    TEXT,                   -- geolocating entry point, optional
  site_url_template TEXT,               -- per-country web page, e.g. '…/{country}/abu-safiya' (§2.2)
  featured      INTEGER NOT NULL DEFAULT 0,  -- operator-curated; appears in `mine` (§4.1)
  visibility    TEXT NOT NULL,          -- 'invite' | 'link' | 'public'   (C1: default 'invite')
  state         TEXT NOT NULL,          -- 'draft' | 'active' | 'archived' | 'disabled'
  version       INTEGER NOT NULL,       -- bumped on any item change; drives client cache
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL
)

campaign_member (
  campaign_id   TEXT NOT NULL REFERENCES campaign(id),
  user_id       TEXT NOT NULL REFERENCES user(id),
  role          TEXT NOT NULL,          -- 'owner' | 'editor' | 'member'
  joined_at     INTEGER NOT NULL,
  PRIMARY KEY (campaign_id, user_id)
)

invite (
  code          TEXT PRIMARY KEY,       -- short, human-typeable; see §4.2
  campaign_id   TEXT NOT NULL REFERENCES campaign(id),
  role          TEXT NOT NULL,
  created_by    TEXT NOT NULL REFERENCES user(id),
  expires_at    INTEGER,
  max_uses      INTEGER,
  uses          INTEGER NOT NULL DEFAULT 0
)

campaign_item (
  id            TEXT PRIMARY KEY,
  campaign_id   TEXT NOT NULL REFERENCES campaign(id),
  kind          TEXT NOT NULL,          -- 'post' | 'target' | 'contact'   (§3)
  country       TEXT,                   -- routing code, lower-case; NULL = not country-scoped
  lang          TEXT,
  label         TEXT,                   -- variant label, e.g. "IT-2 (breve)"
  text          TEXT,                   -- outbound copy, for kind='post'
  url           TEXT,                   -- target/contact URL
  author        TEXT,                   -- target's author handle, for kind='target'
  posted_at     INTEGER,                -- target's own timestamp
  note          TEXT,                   -- organizer's note to members
  sort_order    INTEGER NOT NULL DEFAULT 0,
  created_at    INTEGER NOT NULL
)

assignment (
  campaign_id   TEXT NOT NULL REFERENCES campaign(id),
  item_id       TEXT NOT NULL REFERENCES campaign_item(id),
  user_id       TEXT NOT NULL REFERENCES user(id),
  state         TEXT NOT NULL,          -- 'claimed' | 'done' | 'released'
  claimed_at    INTEGER NOT NULL,
  expires_at    INTEGER,                -- NULL once done (C4)
  PRIMARY KEY (campaign_id, item_id, user_id)
)

completion_count (
  campaign_id   TEXT NOT NULL REFERENCES campaign(id),
  item_id       TEXT NOT NULL REFERENCES campaign_item(id),
  count         INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (campaign_id, item_id)
)
```

Indexes: `campaign(owner_id)`, `campaign_member(user_id)`, `campaign_item(campaign_id, kind)`,
`assignment(user_id)`, `assignment(expires_at)` for the sweep.

### 2.1 Why completions are counters

`assignment` rows carry `user_id` and are the working state. `completion_count` is the *durable*
record and holds no user reference. When a claim closes, the counter is incremented and the
assignment row may be pruned on a retention schedule.

This implements the privacy position in `IDENTITY_SPEC.md` §7: a long-lived per-user log of
"who acted on what, where, when" is not a neutral dataset for activism tooling, and the aggregate
is what coverage actually needs.

### 2.2 Countries

Countries are **derived**, not a table: the distinct `campaign_item.country` values plus their
languages. This keeps the existing `CampaignCountryDto` shape (§6.1) computable without a second
registry to keep in sync. `hasParliamentAction` becomes "this country has at least one item with
`kind='contact'`" — which finally gives that flag a behaviour instead of an inert chip.

Implementing step 1 pinned down what "plus their languages" has to mean, and turned up one field
that does not derive:

| Field | Derivation |
|---|---|
| `langs` | the post languages **in order of first appearance**, not sorted — `lu` must stay `["fr","de"]`, and sorting would silently make it `["de","fr"]` and change which language the app defaults to |
| `defaultLang` | the language of the country's first post |
| `parliament` | the country has at least one `kind='contact'` item |
| `name`, `flag` | `utils/countries.py`, the one registry, shared with `routes/abusafiya.py` |
| `url` | **does not derive** |

`url` is the country's page on the campaign *website* — a property of the site, not of the work
items, and nothing in `campaign_item` implies it. It therefore lives on the campaign as
`site_url_template`, with `{country}` substituted. A campaign with no website leaves it null and
its countries report an empty `url`, which is the honest answer rather than a fabricated link.

These rules were checked against all 42 countries of the live payload **before** being written
down, not asserted and hoped for. The check that they hold is the byte comparison in §7.

---

## 3. Item kinds and assignment policy

| `kind` | What it is | Assignment policy (C3) | Client action |
|---|---|---|---|
| `post` | Ready-to-publish outbound copy. What the campaign ships today. | **Unassigned.** Anyone may use any post; overlap is harmless. | Create draft (existing composer), copy. |
| `target` | A specific post worth replying to, quoting or amplifying. | **Overlapping.** Many users may claim the same target — that is the goal. Claims stagger *order*, not exclusivity. | Open in X; copy reply; quote via `x.com/intent/post?…`. |
| `contact` | Write to an MP / office / ministry. | **Exclusive.** At most one active claim per item. | Open the contact URL; mark done. |

Only `contact` requires the atomic single-grant path (C5). `target` claims are recorded for
coverage and ordering but never refused for contention. `post` items bypass assignment entirely,
which keeps the existing composer flow working unchanged.

### 3.1 Staggering targets

For `kind='target'`, the server returns each user a **rotated** ordering of the target list, seeded
by `user_id`. Everyone sees every target, but they start in different places — so a group of
twenty does not produce twenty near-simultaneous replies on the same post, which is both the
signature that gets accounts flagged and a waste of reach.

---

## 4. Endpoints

All signed per `IDENTITY_SPEC.md` §4, except §4.6 and the two read endpoints in §4.1, whose
signature is **optional** — see there.

### 4.1 Campaign lifecycle

```
POST   /api/campaigns                    create (owner = caller)         → 201 {campaign}
GET    /api/campaigns/mine               owned + joined                  → 200 {campaigns:[…]}
GET    /api/campaigns/{id}               full payload (§6)               → 200 {campaign}
PATCH  /api/campaigns/{id}               title/description/state/visibility (owner|editor)
POST   /api/campaigns/{id}/items         add item (owner|editor)
PATCH  /api/campaigns/{id}/items/{itemId}
DELETE /api/campaigns/{id}/items/{itemId}
```

`GET /api/campaigns/mine` returns summaries only — id, title, counts, `version`, the caller's role
— so the campaign list screen is one cheap request.

**As built, `mine` also returns campaigns flagged `featured`.** Owned-and-joined alone would have
given a brand-new install an empty campaign screen, because the campaign the app has always shipped
with is one nobody was ever invited to. `featured` is set by the operator (import or CLI), and C1
still holds: there is no browse, no search, and no endpoint that lists other people's campaigns.
It is how the app finds its own bundled campaign, not a directory. `role` is null for it, and the
client shows that as "Included" rather than "Joined".

`GET /api/campaigns/{id}` requires membership unless `visibility` is `link` or `public`.
A campaign in state `disabled` returns **`451`** to members with a `detail` explaining it was
disabled by the operator (C6), and is invisible in `mine`.

**As built, both reads accept an unsigned request.** `mine` and `{id}` use `signed_user_optional`
(`utils/ctc_auth.py`); with no `Authorization` header the caller is anonymous and sees exactly the
`featured` campaigns — nothing else, in either endpoint. Three things make this narrow rather than
a hole in C1:

- The anonymous view is data the server already hands to anyone unsigned via §4.6. It is the same
  campaign through a general endpoint instead of a hardcoded alias, not a new disclosure.
- A **missing** header is anonymous; a **present but invalid** one is still `401`. A revoked device
  must be told it is revoked, not quietly downgraded to the anonymous view.
- `link` visibility is *not* enough on its own for an anonymous caller — only `featured` is.
  Otherwise a guessed id would be a read key on other people's active campaigns. Widening that is
  the `public` decision in §9, which is the owner's to make.

Why it is this way: step 2 made both endpoints signature-only, and a fresh install creates an
identity only from the Settings screen. So the campaign button — which had worked since the first
release, via the unsigned alias — began answering *"Could not load your campaigns."* Both endpoints
had to change; opening only the list would have produced a list nobody could tap through.

The client needed no change at all: the requests the app already sends are the ones that now work,
and the same calls return more once an identity exists.

### 4.2 Membership

```
POST   /api/campaigns/{id}/invite        {role, expires_at?, max_uses?}  → 201 {code}
POST   /api/campaigns/join               {code}                          → 200 {campaign summary}
DELETE /api/campaigns/{id}/members/me    leave
DELETE /api/campaigns/{id}/members/{userId}   remove (owner only)
```

Invite codes are short and typeable — 8 characters from an unambiguous alphabet (no `0/O`, `1/l/I`).
They are **capability tokens**: possession grants membership, so they are rate limited per IP
(`IDENTITY_SPEC.md` D5), attempt-throttled per code, and expire by default in 7 days.

An owner cannot leave their own campaign; ownership must be transferred first (`PATCH` with a new
`owner_id`, which must already be a member).

### 4.3 Assignment

```
POST   /api/campaigns/{id}/claim    {item_ids:[…]}  → 200 {granted:[…], refused:[{id, reason}]}
POST   /api/campaigns/{id}/release  {item_ids:[…]}  → 200
POST   /api/campaigns/{id}/done     {item_ids:[…]}  → 200 {accepted:[…]}
GET    /api/campaigns/{id}/coverage                 → 200 (§4.4)
```

`claim` semantics (C5):

- `contact` items are granted under a transaction that rejects any item with an active claim by
  another user. Refused items come back with `reason: "claimed"`.
- `target` items are always granted.
- Granted claims get `expires_at = now + 48h` (C4). A sweep sets expired `claimed` rows to
  `released`, returning the work to the pool.
- `done` is idempotent per (item, user): it closes the assignment, clears `expires_at`, and
  increments `completion_count`. Re-sending the same completion does not double-count.

### 4.4 Coverage

```jsonc
{ "campaign_id": "…", "version": 12,
  "items": [ { "item_id": "…", "kind": "contact", "country": "it",
               "claimed": 1, "done": 0, "state": "claimed" } ],
  "by_country": [ { "country": "it", "contact_total": 4, "contact_done": 1, "target_done": 17 } ] }
```

No `user_id` appears in the response (§2.1). This backs both the in-app progress display and the
organizer's web view — the thing that turns sharding into coordination, because it is what makes an
uncovered country visible.

### 4.5 Abuse

```
POST   /api/campaigns/{id}/report   {reason, detail}     → 202     (any authenticated user)
```

Mails the operator. Server-side, an operator flag sets `campaign.state = 'disabled'` (C6); there is
no API for it — it is a database/CLI action, deliberately.

### 4.6 Legacy alias — unsigned

```
GET    /api/abu-safiya      → the Abu-Safiya campaign in the exact current payload shape
```

Unauthenticated, unchanged bytes, indefinite (C7).

---

## 5. Rate limits and abuse controls

Keyed on **IP and install id, never on `user_id` or public key** — keys are free
(`IDENTITY_SPEC.md` §7).

| Action | Limit |
|---|---|
| Campaign creation | 3 / day / IP |
| Invite creation | 20 / day / campaign |
| Invite redemption attempts | 10 / hour / IP, plus per-code throttle |
| `claim` | 200 items / hour / user |

Plus, from C1: no public listing endpoint exists at all in v1 — `visibility: 'public'` is reserved
in the schema but not served. Adding it is §9.

**Terms line**, shown at campaign creation: campaigns must not target private individuals.

---

## 6. Client changes

### 6.1 Payload compatibility

`GET /api/campaigns/{id}` returns the **existing `CampaignDto` shape** — `campaign`, `version`,
`locateUrl`, `countries[]` with `posts[]` — plus new fields. Since the client is configured with
`ignoreUnknownKeys = true`, the additions are invisible to older builds:

```jsonc
{
  "campaign": "…", "version": 12, "locateUrl": "…",
  "id": "…", "title": "…", "description": "…", "role": "member", "state": "active",
  "countries": [ { "code": "it", "name": "…", "flag": "🇮🇹", "defaultLang": "it",
                   "langs": ["it"], "url": "…", "parliament": true,
                   "posts": [ { "id": "IT-2 (breve)", "lang": "it", "text": "…" } ],
                   "targets":  [ { "id": "…", "url": "…", "author": "@…", "text": "…",
                                   "postedAt": …, "note": "…" } ],
                   "contacts": [ { "id": "…", "url": "…", "label": "…" } ] } ]
}
```

`CampaignModels.toCampaign()` therefore needs additions, not a rewrite: two new lists on
`CampaignCountry`, and `Campaign` gains `id`, `title`, `role`.

### 6.2 Repository

```kotlin
interface CampaignRepository {
    suspend fun list(): Result<List<CampaignSummary>>
    suspend fun get(id: String): Result<Campaign>
    suspend fun join(code: String): Result<CampaignSummary>
    suspend fun claim(id: String, itemIds: List<String>): Result<ClaimOutcome>
    suspend fun markDone(id: String, itemIds: List<String>): Result<Unit>
}
```

The existing `getCampaign()` becomes `get(abuSafiyaId)`. All the status-classification logic in
`CampaignRepositoryImpl` (the deliberate 404/5xx/non-2xx branches, since the shared client is built
without `expectSuccess`) is reused verbatim — extract it to a private helper rather than copying it
five times.

Request signing is a Ktor plugin (`IDENTITY_SPEC.md` §4.4), so none of these methods reference auth.

### 6.3 Navigation

| Route | Now | After |
|---|---|---|
| `campaign_countries` | Inert country list, the dead screen | **`campaign_list`** — owned + joined campaigns, plus *Join with code* |
| — | | `campaign_detail/{id}` — the hub: three lanes (Publish / Act / Reach out) + progress |
| `campaign_composer` | Unchanged | Unchanged, reached from the Publish lane, scoped to one campaign |

The top-bar campaign button on `PostsScreen` (`Screen.Home.route`) navigates to `campaign_list`
instead of loading one hardcoded campaign, so `PostsViewModel.loadCampaign()` and
`CampaignUiEvent.NavigateToCountries` are replaced by a plain navigation with the list screen
loading its own data.

The country list survives as a *section of the detail screen* — this time with working
`onClick`s: a country row opens `CampaignCountry.url`, and the parliament chip opens the contact
action, both via the existing `platform/UrlOpener`. `Campaign.locateUrl` becomes a
"find my country" affordance. These three fields are already parsed and currently unused.

**As built:** the filter and sort chips moved across with the country list rather than being
deleted with the screen — forty-two countries is too many to scan, and "parliament action" as a
filter now selects for something a user can act on. `CampaignCountryScreen.kt` is gone;
`CampaignCountryList.kt` (the chips and the subtitle) is kept and reused. `PostsViewModel` loses
`loadCampaign()`, `campaign`, `campaignLoading` and `CampaignUiEvent` entirely: the button is now
plain navigation, and the list screen loads its own data.

Serving the parliament chip needed `contacts` on the country block, so the serializer emits it —
`legacy_payload` strips it again, and the byte comparison from step 1 is what keeps that honest.

### 6.4 Offline

Campaigns and their items are cached locally (SQLDelight, alongside the existing tables), keyed by
`campaign.version` so a refresh is a cheap version check. Completions recorded while offline go to
a local outbox table and are flushed to `POST /done` on next connect — `done` is idempotent per
(item, user) precisely so this replay is safe.

Claims are **not** made offline: an exclusive `contact` claim cannot be granted without the server,
and pretending otherwise produces exactly the duplicate contact actions C3 exists to prevent.

---

## 7. Migrating Abu-Safiya

1. Write an importer that turns `social/abu_safiya_x_posts.md` + the country registry in
   `routes/abusafiya.py` into one `campaign` row, its `campaign_item` rows (`kind='post'`), and
   `kind='contact'` items for the countries currently flagged `parliament: true`.
2. Owner is an operator-held identity.
3. Re-point `/api/abu-safiya` at the generic serializer for that campaign id, and diff the output
   against `shared/src/desktopTest/resources/campaign_abu_safiya_slice.json`.

**As built:** the importer's source is `_campaign_payload()` itself, not the markdown — so the
import cannot disagree with what shipped clients already receive, and the comparison is a real
check instead of two parsers agreeing with each other. The alias falls back to the markdown when
the store is missing or the campaign is not yet imported, so deploying the code and running the
import are independent steps and neither one alone can take the endpoint down. Imported with
`visibility='link'`: the campaign has always been publicly readable, and requiring an invite would
have been a tightening nobody asked for.

That fixture already exists as a verbatim capture from the live endpoint, and
`CampaignRepositoryTest` parses it as a contract test — so **the migration is verified byte-shape
by a test that is already written**. Do not modify the fixture to make the new server pass.

---

## 8. Build order

| # | Step | Verification | User-visible |
|---|---|---|---|
| 1 ✅ | Schema + generic `GET /api/campaigns/{id}`; Abu-Safiya imported; `/api/abu-safiya` re-pointed at it. | **Done.** 26 checks in `test_campaigns.py`, the two that matter being byte comparisons: the store-served payload and the legacy alias are both **character-for-character identical** to what the code at the previous commit produced — checked against a payload rendered from `git archive HEAD`, so the old implementation is the reference rather than a re-description of the new one. Plus access control (invite-only non-member → 404 not 403, disabled → 451; unsigned → 401 at the time, relaxed in step 2a), a repeatable import, and 503 when the store is missing. `CampaignRepositoryTest` untouched and green. | No |
| 2 ✅ | `mine` + `list()` + `campaign_list` screen replacing the dead country screen; detail screen with working country/parliament/locate links. | **Done.** 9 new server checks; 11 client unit tests; **6 headless render tests**, three of which assert that a tap actually opens a URL — the country row, the parliament chip and "find my country", i.e. exactly the three payload fields that were parsed and ignored. `CampaignIntegrationTest` runs the real client against a live server: `mine` from a fresh install, the detail payload, and the unsigned legacy alias side by side with the same campaign fetched by id. | **Yes — this alone fixes the original complaint** |
| 2a ✅ | Optional signature on both campaign reads (§4.1), fixing the fresh-install 401 step 2 introduced. | **Done.** 44 checks in `test_campaigns.py` (was 35): anonymous list and detail both 200 with `role: null` and full counts, a **bad** signature still 401, and a non-featured `link` campaign readable with an identity but 404 without one. Client-side, `CampaignIntegrationTest` gained a no-`register()` case — verified to *fail* against the pre-fix routes and pass after, so it is a regression test and not a decoration. No client code changed. | **Yes — a fresh install can open the campaign again** |
| 3 | Web authoring + invites + `join`. | Second identity joins a campaign authored by the first. | Yes |
| 4 | `target`/`contact` items, claim/done/coverage, offline outbox. | Two clients contend for one `contact` item; exactly one is granted. | Yes |

Step 1 changes nothing observable and de-risks everything after it. Step 2 needs
`IDENTITY_SPEC.md` steps 1–2 (a working signed request) to have landed first.

---

## 9. Deferred — public campaigns

`visibility: 'public'` is reserved in the schema and **not served**. Turning it on means a
discovery surface, which means hosting user-generated content that recruits strangers into
coordinated action against named accounts. That needs a review queue, a notice-and-action process
(DSA obligations apply — the operator is EU-based), and a moderation policy with someone to operate
it.

That is a product decision, not a schema change, and should be taken deliberately rather than
arrived at by adding a `GET /api/campaigns/public`. Invite-only (C1) is the shipping design.
