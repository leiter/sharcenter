# ShareCenter

**Cut the crap. Keep the content that matters.**

ShareCenter is a personal social-content manager for Android. Instead of losing
interesting links in a dozen chat threads and browser tabs, share them straight
into ShareCenter to build one curated, searchable library — then turn what you've
saved into posts you can publish back out to any app on your device.

## What it's for

ShareCenter sits between the content you discover and the content you publish. It's
built for people who collect links across many social networks, tag and revisit
them, and compose their own posts — all in one place, with no account required to
enrich a link and no data leaving the device unless you choose to publish or export.

## Gracefully integrated platforms

When you share a link into ShareCenter, a pluggable per-platform handler recognizes
the source and enriches it automatically — no manual copying, no pasting titles.
Each network is handled in its own native idiom:

| Platform | What ShareCenter does |
| --- | --- |
| **YouTube** | Fetches the video's title, channel name and thumbnail via the public oEmbed endpoint. |
| **X / Twitter** | Resolves shortened/redirect URLs to their canonical form; a shared *profile* becomes an `@handle` in your keyword pool. |
| **Bluesky** | Enriches *posts* with author, post text and a thumbnail via the public AppView API; a shared *profile* becomes an `@handle`. |
| **Mastodon** | Enriches *posts* from the origin instance's public status API; a shared *profile* is stored as a fully-qualified `@user@instance` handle across the fediverse. |
| **TikTok** | Enriches *posts* with author, caption and cover image via oEmbed — expanding `vm.`/`vt.` short links first; a shared *profile* becomes an `@handle`. |
| **Reddit** | Enriches *posts* with title and author via oEmbed — expanding `redd.it` / `/s/` short links first; a shared *user* becomes `u/username` and a *subreddit* becomes `r/subreddit`. |
| **Any other link** | Saved verbatim as a clean fallback, so nothing you share is ever lost. |

New networks are additive by design: each platform is a self-contained
`SharedLinkHandler`, so support grows without touching the rest of the app.

## Features

### Links library
- **Share-in capture** — send URLs from any app; ShareCenter recognizes the platform, resolves redirects and pulls in metadata automatically.
- **Profiles become handles** — sharing a profile saves the account to your reusable keyword pool instead of cluttering the library with a bare URL.
- **Rich tagging** — every link carries handles, hashtags and keywords, surfaced as compact summary chips with quick dropdown menus.
- **Fast search** — match across both title and description text.
- **Thumbnail caching** — previews load once and re-display instantly.
- **Selection & bulk actions** — long-press to multi-select, then favorite, tag or act on the whole set at once.
- **Long-press filters** — filter the library by domain, by username or by date with a single gesture.
- **Channel / profile shortcuts** — jump straight to a link's source channel or author when available.

### Post composer
- **Rich draft editor** with live character counting (links counted as 23 characters, X-style).
- **Inline highlighting** — real `http(s)` links are validated and made tappable; `@handles` and `#hashtags` are detected and colored as you type.
- **Sortable drafts** rendered as reorderable cards, with order persisted per item.
- **Publish anywhere** — the native Android share sheet exposes WhatsApp, LinkedIn, Bluesky, Mastodon, Telegram and anything else installed, not just X and Facebook.

### ECI advocacy module
- **Live statistics** — signatures-per-country pulled from the European Citizens' Initiative portal, with filtering and sorting.
- **Campaign composer** — turn the numbers into ready-to-post campaign drafts, showing how many signatures each country still needs.

### Settings & your data
- **Appearance** — theme, timestamp/display-format selection, and an accent-color picker (HSV square, hue slider and hex field).
- **Per-screen defaults** — independent default date range, favorite filter and sort order for Posts and Links.
- **Import / Export** — move your library in and out as plain text.
- **Backups** — one-tap backup and restore, plus scheduled backups with configurable frequency and retention.

## Privacy

ShareCenter enriches links using each platform's public, unauthenticated endpoints
and stores your library locally. Nothing is uploaded to a ShareCenter server; your
content leaves the device only when *you* publish a post or export your data.
