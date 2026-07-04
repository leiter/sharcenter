# ShareCare — UX/UI Review & Feature Roadmap

_Project-wide scan, 2026-07-04. ShareCare (`cut.the.crap`) is a personal social-content
manager: share URLs into it (Links library, with X-redirect resolution + YouTube metadata +
tagging), compose drafts (Posts editor → X/Facebook), plus an ECI advocacy module and Settings._

---

## 🐛 Quick wins & things that look broken

1. **Ship-blocking: a dev test job runs on every launch.** `MainActivity.onCreate`
   (lines ~82–92) unconditionally submits a `ShareLinksTask` with `example.com` links to the
   job-queue backend on *every* app start. Gate behind `BuildConfig.DEBUG` / `developerMode`,
   or remove.
2. **Links drag-to-reorder is a no-op.** In `LinksScreen.kt:396` the actual `onMove(...)` is
   commented out — items lift under long-press but never reorder. Either wire it up or remove
   the whole `detectDragGesturesAfterLongPress` block (it also competes with
   long-press-to-select).
3. **Bulk "favorite selected" does nothing.** `ListAction.ToggleFavoritesForSelected` has an
   empty body (see `todo.md`) — the menu item exists but silently fails.
4. **Accessibility gaps.** Empty `contentDescription = ""` on the favorite stars
   (`ContentLinkItem.kt:478, 531`), and hardcoded English strings that escaped localization:
   `"(Empty)"`, `"$characterCount chars"`, `"No keywords"`, `"No account"`,
   `"No domain available"`, `"Show/Hide quick actions"`. The 36dp icon buttons in the
   editor/cards are also below the 48dp Material touch-target minimum.
5. **`TryMeScreen` is dead code** — not referenced anywhere in the nav graph. Either wire it in
   as onboarding or delete it.

---

## 🎨 UX / UI improvements

### Posts / editor (highest-traffic surface)
- **Make the character counter network-aware.** `calculateCharCount` only models X
  (links = 23 chars) and there is *no limit indicator* — the ECI composer hardcodes `280` in one
  place. Show remaining chars per target network (X 280, Bluesky 300, Mastodon 500, Threads 500,
  LinkedIn) with a color shift when exceeded. For a posting app this is the single biggest UX lever.
- **Don't hide the editor to show filters.** `PostsScreen` swaps `ContentEditor` out entirely
  when the filter panel expands (`if (!filterExpanded)`). Losing your draft to glance at filters
  is surprising — overlay/collapse the filters instead.
- **The top bar wastes its center** (`Text("")` + empty `navigationIcon`) while cramming search +
  filter + a label-less ECI BarChart icon into `actions`. Give it a real title and move ECI
  somewhere discoverable (see below).
- **The `category` field renders on cards but has no UI to set it** — dead visual affordance.

### Links
- **Surface the hidden long-press gestures.** Long-press does 4 different powerful things
  (select, filter-by-domain, filter-by-username, set-date-filter) with zero discoverability.
  Add a first-run coach-mark or a small "hold to filter" hint.
- **Rich previews for non-YouTube links.** Only YouTube gets a thumbnail/title; everything else
  is a bare domain icon. `LinkMetadata` already exists — fetch Open Graph image/title for any URL.
- **Fix the snackbar inset hack** (`LinksScreen.kt:469`, the `bottom = 80.dp` TODO) with proper
  `Scaffold`/insets.

### Cross-cutting
- **Real empty states.** Both lists render nothing when empty — add "Share a link to get started"
  with the share instructions (likely what `TryMeScreen` was meant for).
- **Undo on delete.** Posts have a delete-offer snackbar; Links only get a confirm dialog.
  Standardize on snackbar-with-Undo.
- **Surface ECI properly.** A whole module hides behind one unlabeled top-bar icon. Give it a nav
  destination or a labeled entry.

---

## 🚀 New feature ideas (ranked)

1. **Native Android share sheet for outgoing posts.** Posts currently only go to X and Facebook
   via hardcoded web intents. An `ACTION_SEND` chooser (already done in `TranslateIntent`)
   instantly unlocks WhatsApp, LinkedIn, Bluesky, Mastodon, Telegram — with far less maintenance
   than per-network intent classes.
2. **Bridge Links → Posts.** The two halves are siloed. "Compose post from this link" that
   pre-fills the editor with the URL + its saved handles/hashtags/keywords is the natural
   save→publish workflow.
3. **Scheduled posting** using the Python `job_queue` backend that is *already in the repo*.
   Queue a draft, get a reminder/notification when it's time to post. Highest-leverage use of
   infrastructure already built.
4. **Hashtag/handle groups (snippets).** Keywords/handles/tags are already managed — let users
   save a reusable *group* ("my crypto tags") and insert it into the editor in one tap.
5. **Duplicate-link detection** on share/save, so the library doesn't accumulate the same URL twice.
6. **Full bulk actions on links**: bulk favorite (fix the stub), bulk tag, bulk export-selected,
   bulk delete.
7. **Personal content dashboard.** All the metadata exists — links per domain, saves per week,
   posting streaks, most-used tags. A lightweight analytics screen.
8. **Material You dynamic color** from wallpaper (`dynamicColor` in the theme) + verify the
   light/dark toggle covers all the `copy(alpha=…)` surfaces.

---

## Suggested sequencing (impact vs. effort)

1. **Cleanup commit** — remove the dev test job + fill the empty accessibility strings.
2. **First real feature** — network-aware character counter **or** native share sheet.
3. Then work down the ranked feature list, starting with Links → Posts bridging.
