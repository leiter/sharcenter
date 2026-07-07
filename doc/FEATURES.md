# ShareCare — Features & Possibilities

_ShareCare (`cut.the.crap`) is a personal social-content manager for Android. Share URLs
into it to build a curated **Links** library, compose and manage drafts in the **Posts**
editor, publish out to any app on the device, track an **ECI** advocacy campaign, and manage
your data from **Settings**. This document describes what the app does today and where it can
grow next._

_Derived from the 2026-07-04 UX/UI review after its findings were addressed._

---

## App structure

Three primary destinations sit behind the bottom bar, with several secondary screens reached
from them:

| Destination | Route | Purpose |
| --- | --- | --- |
| **Posts** | `home` | Compose, edit, sort and publish draft posts (start destination) |
| **Links** | `search` | Curated library of shared URLs with metadata and tagging |
| **Settings** | `profile` | Appearance, defaults, sharing and data management |
| Import / Export | `import_export` | Move library data in and out |
| Backup management | `backup_management` | Scheduled database backups with retention |
| ECI statistics | `eci_statistics` | Live signature counts per country |
| ECI post composer | `eci_post_composer` | Generate campaign drafts from the statistics |

---

## Posts (composer)

- **Rich draft editor** — compose posts with live **character counting**. Links are counted
  as 23 characters (X/Twitter rules) via `CharCountUtils.calculateCharCount`.
- **In-editor link highlighting** — genuine `http(s)` URLs are validated, trailing
  punctuation is trimmed, and only real links are underlined and made tappable to open.
- **`@handle` and `#tag` coloring** — handles and hashtags are detected and colored inline as
  you type, independent of the URL logic.
- **Drafts list with sorting** — content items render as cards and can be reordered; sort order
  is persisted per item.
- **Native Android share sheet** — publish a draft through `ACTION_SEND`
  (`Intent.createChooser`), which exposes WhatsApp, LinkedIn, Bluesky, Mastodon, Telegram and
  anything else installed — not just X and Facebook.
- **Draft creation from ECI** — the ECI composer can hand generated campaign texts back to the
  Posts view model as new draft posts.

## Links (library)

- **Share-in capture** — URLs shared to the app land in the library; X/Twitter redirects are
  resolved, YouTube metadata (title, channel, thumbnail) is fetched, Bluesky posts are enriched
  with author, post text and a thumbnail via the public AppView API, Mastodon posts are enriched
  the same way from the origin instance's public status API, TikTok posts are enriched (author,
  caption, cover) via the public oEmbed endpoint — with `vm.`/`vt.` short links expanded first — and
  Reddit posts are enriched with title and author via oEmbed (short links `redd.it`/`/s/` expanded
  first). Recognition and processing are pluggable per platform (`SharedLinkHandler`), so new
  networks are additive.
- **Profile shares become handles** — sharing an X, Bluesky, Mastodon, TikTok or Reddit *profile*
  saves the handle to the keyword pool instead of storing the URL as a link, in each network's
  native form (`@user`, `@user@instance`, `u/user`, and `r/subreddit` for a shared subreddit).
- **Thumbnail caching** — loaded thumbnails are cached via Coil for fast re-display.
- **Tagging model** — each link carries **handles**, **hashtags** and **keywords**, surfaced
  as compact per-marker-type summary chips with dropdown menus (`LinkMetadata`).
- **Channel / profile shortcuts** — a link's dropdown can open the source channel or the user
  profile when available.
- **Search across title and description** — text search matches description text as well as
  the title (OR match).
- **Selection & bulk actions** — long-press to enter selection mode, then act on the set:
  - **Favorite / unfavorite selected** — toggles the whole group predictably (favorites all
    unless every item is already a favorite, in which case it clears them).
  - **Tag selected** — bulk-tag the set with handles, hashtags or keywords.
  - **Export selected** and **delete selected**, plus deselect-all.
- **Powerful long-press filters** — long-press gestures can filter by domain, by username, or
  set a date filter.
- **Empty state** — `EmptyStateIndicator` renders guidance when a list has no items.

## ECI advocacy module

- **Live statistics** — signatures-per-country pulled from the ECI portal, shown on a
  dedicated `eci_statistics` screen with filtering and sorting.
- **Config-driven parser layer** — extraction is schema-driven and source-agnostic
  (`ParseSchema` + `SourceParser` for JSON and HTML), so new data sources are configuration,
  not new deserialization code. The ECI campaign is expressed as the first `ParseSchema`.
- **Campaign post composer** — turns the statistics into ready-to-post drafts, showing the
  remaining signatures needed per band, and pushes them into the Posts drafts.
- **Discoverable entry point** — reached as a real navigation destination rather than a single
  unlabeled top-bar icon.

## Settings & data management

- **Appearance** — theme selection and timestamp/display format, plus an **accent color**
  picker (`ColorPicker` / `ColorPickerDialog`): an HSV saturation/value square, hue slider and
  editable hex field. HSV is the internal source of truth so dragging to a grey preserves the
  chosen hue. The row shows the live color as a tinted icon and hex subtitle. Built only from
  multiplatform Compose and pure-Kotlin maths (no `android.graphics`, `toArgb` or `String.format`)
  so it moves to `commonMain` unchanged on the planned KMP migration. Currently a demo — the
  chosen color is not yet persisted or applied to the theme.
- **Per-screen defaults** — default date range, favorite filter and sort order, independently
  for Posts and Links.
- **Import / Export** — move library data in and out of the app.
- **Database backup** — one-tap backup and restore, plus scheduled backups with configurable
  frequency, retention, and a management screen.
- **Sharing configuration** — edit how shared links are handled.
- **X login** — authenticate with X for posting.

## Platform & build notes

- The developer test job that seeded `example.com` links is now gated behind
  `BuildConfig.DEBUG`, so it never runs in release builds.
- **R8 minification, resource shrinking and obfuscation are enabled for release builds**
  (`isMinifyEnabled` / `isShrinkResources`), with ProGuard rules covering the Ktor client
  and serializable models. Debug builds stay un-obfuscated (standard Android behavior for
  debuggable variants).
- **Cleartext HTTP is a debug-only concern.** `network_security_config.xml` and its manifest
  reference live in the `debug` source set / debug manifest overlay, so release APKs never
  declare cleartext traffic. The two dev hosts it permits are only reachable from debug builds.
- Dependency upgrades are broadly gated on a coordinated AGP 8.10 → 9.1 / Gradle 9 /
  Kotlin 2.4 / Hilt 2.60 migration; only a few library bumps are safe before then.
- A Kotlin Multiplatform migration is planned; new shared-candidate code (e.g. `ColorPicker`)
  is written KMP-safe — no `android.*`, `toArgb` or `String.format`.

---

## Possibilities & roadmap

Ideas not yet built, roughly in impact order.

### Composer
1. **Network-aware character counter.** `calculateCharCount` currently models only X (links =
   23 chars) with no limit indicator. Show remaining characters per target network (X 280,
   Bluesky 300, Mastodon 500, Threads 500, LinkedIn) with a color shift when exceeded.
2. **Bridge Links → Posts.** "Compose post from this link" that pre-fills the editor with the
   URL plus its saved handles / hashtags / keywords — the natural save → publish workflow.
3. **Hashtag / handle groups (snippets).** Save a reusable group ("my crypto tags") and insert
   it into the editor in one tap.
4. **A UI to set a post's `category`.** Cards can render `category`, but there is currently no
   affordance to set it.

### Links
5. **Rich previews for the remaining link types.** YouTube, Bluesky, Mastodon, TikTok and Reddit
   now fetch title/author (and a thumbnail where the platform exposes one); extend the same
   treatment to other platforms (e.g. Open Graph image/title for arbitrary URLs) by adding more
   `SharedLinkHandler`s. Note Reddit's oEmbed returns no thumbnail, and its unauthenticated `.json`
   endpoints are now blocked (403), so Reddit cards show title + author over the Reddit badge.
6. **Duplicate-link detection** on share/save so the library doesn't accumulate the same URL.
7. **Discoverability for long-press gestures.** A first-run coach-mark or a "hold to filter"
   hint for the four hidden long-press actions.

### Cross-cutting
8. **Scheduled posting** using the Python `job_queue` backend already in the repo — queue a
   draft and get a reminder/notification when it's time to post.
9. **Personal content dashboard.** Links per domain, saves per week, posting streaks,
    most-used tags — a lightweight analytics screen from metadata that already exists.
10. **Undo on delete for Links.** Posts offer a delete snackbar; standardize Links on
    snackbar-with-Undo too.
11. **Apply the accent color to the theme.** The `ColorPicker` and Settings row exist but the
    chosen color is demo-only — persist it (DataStore) and thread it into `MaterialTheme`.
    Also consider **Material You dynamic color** from wallpaper, and an audit that the
    light/dark toggle covers all `copy(alpha = …)` surfaces.
12. **Onboarding.** `TryMeScreen` exists but is not wired into the nav graph — either surface
    it as onboarding / empty-state guidance or remove it.
13. **Accessibility polish.** Fill remaining empty `contentDescription`s, finish localizing
    stray hardcoded strings, and lift 36dp icon buttons to the 48dp Material touch target.
