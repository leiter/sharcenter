# Phase 4C Scope — Commonize the UI (Compose Multiplatform)

**Status:** Scoping only — no code written.
**Prerequisite:** ✅ Phase 4A/B done (`:shared` exists, `commonMain` is Android-free).
**Goal:** move the UI into `commonMain` so a desktop/macOS app can actually render.

---

## 1. Measured surface

| Thing | Count |
|---|---|
| UI files / lines / `@Composable` | **57 files · 14,033 lines · 126 composables** |
| `R.string` / `R.plurals` / `R.drawable` refs | **470 / 15 / 19** (504 total) |
| String catalogue | **354 strings + 14 plurals**, in **one locale** (no locale fan-out 🎉) |
| Drawable files | 9 |
| `stringResource` / `painterResource` users | 26 / 5 files |
| `@Preview` users | 21 files |
| `LocalContext` users | 7 files |
| `activity-compose` users | 8 files |
| `AndroidView` users | **1** (the WebView login) |
| ViewModels | 6 |
| `data/rest` files (still in `:app`) | 23 — **8 reference `R.string`** |
| `data/preferences` (DataStore) | 2 |
| Direct `okhttp3` (`UrlResolver`) | 1 |

**Platform seams needed** (`android.*` by capability): Log 10 · Context 14 · Uri 12 · Toast 8 ·
Intent 5 · MediaStore/SAF 5 · Clipboard 4 · PackageManager 2 · WebView 1.

**JVM-only stdlib left in `:app`:** `IOException` 12 · `Locale` 5 · `java.time.format` 4 ·
`Date`/`SimpleDateFormat` 3+3 · `NumberFormat` 2 · `UUID` 2 · `Instant`/`ZoneId` 2+2 · `File`/`OutputStream` 2+2.

---

## 2. Work packages

Ordered so **every package ends with a green Android build**. WP1–WP2 are the backbone;
WP3–WP5 can be parallelised; WP7 is the payoff.

### WP1 — Compose Multiplatform toolchain swap  *(M, high-risk)*
**WP1 COMPLETE.** WP1a ✅ CMP swap (3b08433) · WP1b ✅ Coil 3 (d5ed635) ·
WP1c ✅ CMP navigation · WP1d ⏸ deferred to WP7 (see below).
Replace AndroidX Compose (`compose-bom` + `kotlin.plugin.compose`) with JetBrains Compose
Multiplatform. The app keeps running on Android throughout — CMP targets Android too.
- `org.jetbrains.compose` plugin; `compose.runtime/foundation/material3/ui` from CMP.
- ✅ **`navigation-compose` → `org.jetbrains.androidx.navigation` — turned out to be a PURE
  DEPENDENCY SWAP, zero code changes.** JetBrains publishes it under the *same*
  `androidx.navigation` package names, and on Android it delegates to `androidx.navigation`
  (2.9.0). The app's nav surface is small and conservative — string routes only, no type-safe
  routes, no deep links, no nav-args — so all 11 files compiled untouched.
  ⚠ Caveat: CMP navigation's latest is **2.9.0-beta03** (a beta), vs the stable androidx 2.9.8
  it replaces; this also pins the underlying androidx nav down from 2.9.8 to 2.9.0.
  Also added `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` 2.9.1 (stable).
- **Coil 2 → Coil 3** (`io.coil-kt.coil3`) — API rename, `LocalPlatformContext` (2 files).
- ⏸ **`@Preview` — DEFERRED to WP7 (decided 2026-07-12), not mechanical.** 14 files use
  `Devices` and 13 use `PreviewParameter`/`PreviewParameterProvider`; **CMP's `@Preview`
  supports neither**. Converting now would strip working dev tooling from 21 files for zero
  runtime benefit (previews never ship). Decide per-file at WP7, when each file actually
  moves: simple previews → CMP `@Preview`; `Devices`/`PreviewParameter` ones → either drop,
  or keep an Android-only preview file in `:app` alongside the moved composable.
- ✅ **`material-icons-extended` — SPIKED AND RESOLVED (2026-07-12).** JetBrains stopped
  publishing the icons artifact after **1.7.3** (latest CMP is 1.8.2), and the app uses **87
  distinct icons**. Verified fix: **use CMP 1.8.2 and pin the icons artifact at 1.7.3.** They
  coexist — Gradle upgrades the icons' transitive `compose.ui:ui:1.7.3 -> 1.8.2`, and a
  `commonMain` composable using `Icons.Default.*` / `Icons.AutoMirrored.*` **compiled for both
  the Android and desktop targets**. Catalog entries are already in place
  (`composeMultiplatform = "1.8.2"`, `composeIcons = "1.7.3"`).
  *The biggest WP1 unknown is now closed; no icon vendoring needed.*

### WP2 — Resources → Compose Multiplatform Resources  ✅ **COMPLETE**
The catalogue now lives in **`:shared/commonMain/composeResources/`** (not `:app`) — that is
where it must end up for WP7 anyway, and `:shared` is a real KMP module so `Res` generation is
guaranteed there. `compose.resources { publicResClass = true }` lets `:app` consume it across
the module boundary. Generated and verified: **354 strings · 14 plurals · 9 drawables**, an
exact match with the old catalogue.

`app/src/main/res/values/strings.xml` survives with **exactly one entry, `app_name`** — it is
the manifest's `android:label`, resolved by AAPT, so it cannot come from a Compose resource.

**Four things that were *not* the mechanical find-and-replace the estimate assumed:**

1. **Accessors are extension properties** (`val Res.string.foo`), so every single one needs its
   own `import` — you cannot just swap `R.` for `Res.`.
2. **Deferred `@StringRes Int` fields had to be retyped.** `Screen`, `SettingsModels` (7 enums),
   `MyPopupMenu.MenuItem`, `StateIndicators` and the ECI enums all *stored* resource ids as
   `Int`. Those became `StringResource` / `DrawableResource` — strictly better typing, and it is
   what makes them movable.
3. **`getString` is `suspend` outside composition.** 47 call sites in 8 files were not
   composable. Two patterns, no `runBlocking` anywhere: **hoist** the string into composition and
   capture the `String` in the callback (fixed text), or resolve it in a **coroutine** (when the
   value — e.g. a plural quantity — only exists at click time). In `ShareActivity`, `finish()`
   had to move *inside* the coroutine: finishing first cancels `lifecycleScope` before the
   Toast is ever created.
4. **⚠ CMP only substitutes *positional* placeholders.** Android's `getQuantityString` accepts
   bare `%d`; **CMP does not** — it renders the literal text. All 28 plural items and 5 strings
   used bare `%d`/`%s`, so **every plural in the app was silently broken**: it compiled, all 212
   tests passed, and nothing was logged. Caught only by *looking at the running app* (the editor
   header read `%d chars`). Fixed by converting 33 placeholders to `%1$d`/`%1$s`.
   **Lesson: a green build proves nothing about resources — they resolve at runtime.**

**Decision A2 got extended one layer up.** WP2 exposed that the *ViewModel* layer still built
user-facing prose (`LinksActionHandlers` / `LinksImportExport`), which broke 4 JVM unit tests —
CMP's `getPluralString` needs `android.content.res.Resources`, absent in plain JVM tests. Rather
than paper over it with Robolectric, the same A2 split was applied: the ViewModel now emits a
typed **`LinksSnackbar`** event and the UI phrases it (`LinksSnackbarMessages.kt`, mirroring
`AppErrorMessages.kt`). The ViewModel layer is now **100 % resource-free**, the tests are pure
JVM again with no Android stubs, and `LinksViewModel` is that much closer to `commonMain` (WP6).

*Note: the old "hard 10%" (8 `data/rest` repos using `StringProvider`) was already dissolved by
the A2 work in `63c9418`; `data/rest` never touched a resource in WP2.*

### WP3 — Platform seams  *(M, low-risk)* — **WP3a–e COMPLETE**
Common interfaces + Android actuals; desktop actuals are mostly trivial or no-ops.

Only `Log` ended up as `expect`/`actual`. Everything else is a plain **interface + DI binding**,
because every other seam needs a `Context` on Android and an `expect object` cannot carry state.

| Seam | Android | Desktop | Status |
|---|---|---|---|
| `Log` | `android.util.Log` | stdout/stderr | ✅ WP3a |
| `Notifier` | `Toast` | `SharedFlow` for a snackbar | ✅ WP3b |
| `FileAccess` | `ContentResolver` | `java.io.File` | ✅ WP3c |
| `BackupManager` | MediaStore/SAF | *(WP8)* | ✅ WP3c |
| `Clipboard` | `ClipboardManager` | AWT | ✅ WP3d |
| `UrlOpener` | `Intent.ACTION_VIEW` | `Desktop.browse` | ✅ WP3d |
| `Sharer` | `ACTION_SEND` chooser | `isSupported = false` | ✅ WP3d |
| ~~`InstalledApps`~~ | — | — | ✅ **dissolved** (see below) |
| `LoginFlow` (WebView) | `XLoginActivity` | `isSupported = false` | ✅ WP3e |
| `FilePicker` | SAF `OpenDocument` | AWT `FileDialog` | ✅ WP3f |

**`InstalledApps` did not need to exist.** Its only live caller asked "is X installed?" purely to
decide whether to pin an `ACTION_VIEW` intent to the X package. That is not a question shared code
ever needs answered — it only ever wanted *the URL opened, preferably in the native client*. So it
became `UrlOpener.open(url, preferApp = ExternalApp.X)`, and the `PackageManager` check is now a
private detail of `AndroidUrlOpener`. `TranslateIntent.kt` (the other `PackageManager` user, 230
lines) turned out to be **dead code** and was deleted rather than ported.

> ⚠️ **Correction (WP3e).** WP3d claimed "`FilePicker` is not needed — picking is already done by
> Compose's `rememberLauncherForActivityResult`". **That was wrong**, and it was wrong because the
> metric was wrong: I was counting `import android.*` lines, which misses both fully-qualified
> usages *and* `androidx.activity.*`. `rememberLauncherForActivityResult` /
> `ActivityResultContracts` are **`androidx.activity.compose`, Android-only** — they do not exist
> in CMP common. Three screens use them (`LinksScreen`, `PostsScreen`, `SettingsScreen`), so the
> seam is real and is now **WP3f**. Lesson: *count the coupling you have, not the coupling your
> grep can see.*
>
> `BackHandler` (`PostsScreen`, `MySearchBar`) is **not** a seam — CMP ships
> `org.jetbrains.compose.ui:ui-backhandler` for every target, so it is a one-line import swap
> (plus `@OptIn(ExperimentalComposeUiApi::class)`; it is still experimental). Verified on device:
> Back still exits selection mode rather than the app.

**`FilePicker` is the one seam that had to be a composable (WP3f).** Every other seam is an
injected interface, but registering an activity-result contract *must* happen during composition,
before anything is launched — an injected object cannot do it. So the seam takes the shape of the
thing it hides: `@Composable expect fun rememberFilePicker(...): FilePickerLauncher`. Both Android
contracts (single + multiple) are registered unconditionally, because
`rememberLauncherForActivityResult` cannot be called behind an `if`; which one launches is decided
at click time. Cancelling yields an empty list.

*Behaviour change, deliberate:* `LinksScreen` used `GetContent` (`ACTION_GET_CONTENT`) while the
other two used `OpenDocument` (`ACTION_OPEN_DOCUMENT`). The seam unifies on `OpenDocument` — the
SAF picker, which also grants persistable permission. **Not exercised on device**, because the
Links import entry point is commented out (see below); the Settings restore path, which uses the
same code, was.

*Dead UI found:* `LinksScreen`'s import/export speed-dial items are commented out, so
`FileAction.Import` is unreachable from the Links UI. The commented code still passes `Uri.EMPTY`
and would not compile today. Left alone — pre-existing, and not this migration's call to make.

**`LoginFlow` is the one seam that stays unsupported (WP3e).** Sign-in scrapes session cookies out
of a WebView; a desktop JVM has none, and a fake login is worse than no login. So the capability is
declared, not faked: the settings screen renders the "X Login" row only `if (loginFlow.isSupported)`
and **Manual X Credentials** — which works on every platform — stays as the way in. There is no
result to await: the flow persists credentials itself and the screen already renders from that
state, which is what keeps Android's activity-result plumbing out of common code.

**Gotcha (WP3d):** `TwitterIntent`/`FacebookIntent` built their URLs with `android.net.Uri.encode`.
Swapping that for a common encoder is invisible to the compiler *and* to a smoke test — a wrong
encoding still yields a valid URL, just with mangled text in it. `Uri.encode`'s safe set is
`[A-Za-z0-9]` + `_-!.~'()*` (note the apostrophe, which a strict RFC 3986 encoder would escape,
changing every tweet containing "don't"). `tools/urlEncode` reproduces it exactly and is pinned by
22 JVM tests — which also prove the share URLs are correct *off* Android, where `Uri` does not exist.

### WP4 — JVM stdlib → multiplatform  *(M)* — **a/b DONE**

**The hard part was never `java.time`. It was that locale-aware formatting has no multiplatform
equivalent at all** — and getting it wrong is invisible: it compiles, passes every test, and is
only wrong on screen.

So the work split by *whether locale actually matters*:

- **Locale-independent** — all-numeric patterns (`dd.MM.yy`, `yyyy-MM-dd_HHmmss`, …) → kotlinx-datetime
  in `commonMain`. These render identically everywhere, which means the `Locale` argument they used
  to take was doing nothing at all.
- **Locale-sensitive** — thousands separators, percentages, file sizes, month names → an
  `expect`/`actual` `LocaleFormat` seam. Both actuals are JVM and identical, but they stay separate
  actuals rather than sharing an intermediate JVM source set, because that is what keeps iOS
  addable (it would use `NSNumberFormatter`).
- **Locale-sensitive *for a named language*, not the device** — the ECI post templates compose text
  *in* a language, so `formatIntegerForLanguage(value, "lt")` is a different question from "how does
  this phone format numbers". Verified on device: a Lithuanian post renders 6476 as `6 476` (space
  separator), while the German UI renders the same magnitude as `5.155`.

**Hidden JVM dependencies an import scan cannot see** — worth knowing for WP6/WP7:
- `Character.toChars` (used for flag emoji) is `java.lang`, so it is **auto-imported and invisible
  to `grep '^import java.'`**. Replaced with an explicit surrogate-pair encoder.
- `String.format` is JVM-only and locale-sensitive (`1,5 MB` vs `1.5 MB`).
- `normalizeToStartOfDay` and `toStartOfDay` were the same function by two routes (`Calendar` vs
  `java.time`).

`UUID` → Kotlin's multiplatform `Uuid`; no seam needed, just an opt-in.

**Still in `:app` (file IO, deliberately deferred):** `FileHelper`, `DatabaseBackupManager` (both
Android-only anyway — MediaStore/SAF), `LinksImportExport` (`OutputStream`), and
`StringExtension`'s `java.net.URL` (→ Ktor `Url`).

### WP5 — Move `data/rest` + `data/preferences` into `:shared`  *(M)* — **a/b/c DONE**

- ✅ **WP5a** — `SettingsRepository` + `ColorHistoryRepository` → `commonMain`, on the multiplatform
  DataStore. The stores are now named Koin **singles** (the Android `Context` delegate had been
  making them singletons *by accident*; the repos are factories, so a naive port would have built
  one DataStore per injection and crashed).
- ✅ **WP5b** — `UrlResolver` → `commonMain`, off two raw OkHttp clients and off `org.json`.
- ✅ **WP5c** — `expect fun httpClientEngine()` seam + the six clean repos (Message, YouTube,
  Bluesky, Mastodon, TikTok, Reddit) + `SocialMediaParser` → `commonMain`. `java.io.IOException` →
  `okio.IOException` throughout.

**Deliberately left in `:app`, with reasons:**

| File | Why it stays |
|---|---|
| `eci/EciPostTemplates`, `eci/EciReferenceData` | `java.time`, `NumberFormat`, `Locale` — **WP4's job**, not WP5's. Moving them would drag WP4 forward. |
| `eci/EciStatisticsRepository`, `eci/EciModels` | Kept with their `Eci` siblings rather than splitting the cluster across modules. |
| `task/JobQueueRepository` | `java.util.UUID`. Small swap, but it belongs with WP4's stdlib work. |
| `YouTubeMetadataBackfiller` | Still uses the Context-bound DataStore delegate (the third store). Same fix as WP5a. |
| `NetworkModule`, `RepositoryModule` | DI wiring, and `NetworkModule` reads `BuildConfig`. Modules are the composition root's job — they can legitimately stay. |
| `YouTubePreviewViewModel` | A ViewModel — that is **WP6**. |
- Ktor engine → `expect fun httpEngine()` (OkHttp on Android, Java/OkHttp on desktop).
- **Rewrite `UrlResolver` off raw `okhttp3`** onto the Ktor client (1 file, but fiddly:
  it drives redirects manually).
- DataStore: `datastore-preferences-core` is multiplatform; needs a per-platform path factory.
- Depends on WP2's Decision A (the 8 repos referencing `R.string`).

#### Spike results — both unknowns clear, no plan change

The two questions that could have invalidated the plan are answered, each by a test that runs on
the **JVM**, i.e. off Android (11 new tests, all green).

**1. DataStore multiplatform — ✅ works.** `preferencesDataStore(name = …)` is a Context-bound
delegate and Android-only, but `datastore-preferences-core` exposes the *same* Preferences API and
just asks the caller for an `okio.Path`. All the keys (`stringPreferencesKey` and friends) are
already multiplatform, so **the repositories on top need almost no change** — only
`java.io.IOException` → `okio.IOException`. `createPreferencesStore(name)` + an `expect fun
preferencesPath(name)` is the whole seam. Proven by writing to a real file and reading it back
through a *second* store instance.

- *Constraint found:* DataStore **throws if two live instances share one file**. It must be a DI
  singleton — which it already is. Pinned by a test so nobody "helpfully" makes it a factory.
- *There are three stores, not one:* `app_settings`, `backup_preferences`,
  `youtube_backfill_preferences`.
- The Android path reproduces the delegate's own layout (`filesDir/datastore/<name>
  .preferences_pb`), so the existing store is opened rather than orphaned. Back-compat is not
  actually required (the app is unreleased) — but matching it costs nothing and keeps the dev
  device's real X credentials.

**2. Ktor for `UrlResolver` — ✅ has both capabilities.** The risk was that an engine abstraction
would hide what `UrlResolver` needs from its two OkHttp clients. It does not:

| Needed | OkHttp today | Ktor | Test |
|---|---|---|---|
| Final URL after redirects | `response.request.url` | `response.request.url` (identical) | ✅ incl. multi-hop chains |
| `Location` header *without* following | second client, `followRedirects(false)` | `HttpClient { followRedirects = false }` | ✅ 3xx handed back intact |

**3. Unknown the plan missed: `UrlResolver` also uses `org.json.JSONObject`** — an *Android* class,
not JVM — to navigate the X GraphQL response, plus `java.net.URLEncoder`. Neither is a risk
(`kotlinx-serialization-json` is already a dependency and does the same optional-navigation, and
`tools/urlEncode` from WP3d replaces `URLEncoder`), but it is **~150 lines of real rewriting** that
was not costed. `UrlResolver` is 558 lines. Budget WP5 nearer the high end.

### WP6 — ViewModels + Koin modules → `commonMain`  *(S)*
`androidx.lifecycle` ViewModel is already multiplatform, and Koin is pure Kotlin — this is
small once WP3–WP5 land. `LinksViewModel` needs its `Context` dependency replaced by seams.

### WP7 — Move the 57 UI files → `commonMain`  *(M, mechanical once WP1–WP6 land)*
The payoff. Mostly relocation; the couplings were already removed upstream.

### WP8 — Desktop app module  *(S–M)*
`desktopApp` with Compose `application { Window { App() } }`; bind the JDBC driver (already
proven in `:shared` desktopTest), Java/OkHttp engine, AWT actuals. **First runnable desktop build.**

---

## 3. Decisions needed before starting

### Decision A — how do data-layer strings work in `commonMain`? *(blocks WP2 + WP5)*
The 8 `data/rest` repos localise messages via `StringProvider.get(resId: Int)`. CMP resources
aren't `Int`s.
- **A1 — Pass CMP `StringResource` objects.** Smallest diff; keeps message-building in the data
  layer. But it drags a UI-resource concept into repositories.
- **A2 — Push message construction into the UI (recommended).** Repos return typed
  errors/results; the UI maps them to strings. Architecturally correct, and it deletes
  `StringProvider` entirely — but it touches all 8 repos and their call sites.

### Decision B — iOS scope  ✅ **DECIDED (2026-07-12): NO iOS.**
**Targets are Android + Desktop (JVM).** macOS is served by the Compose Desktop app (packaged
as a `.app`) — no Kotlin/Native, no `NativeSqliteDriver`, no Darwin engine, no Xcode project.
The JDBC driver already works (proven by the `[desktop]` tests).

**However — seams are still built properly.** Platform code goes behind `expect`/`actual` and
interfaces rather than `if (isAndroid)` branching, so iOS remains *addable* later (it would mean
adding actuals, not re-architecting). Estimate: **~2 weeks**, not 3.5.

### Decision C — desktop feature parity?
Recommend **reduced desktop v1**: no WebView login, no Android share-intents, no MediaStore
import — expose these as capability flags rather than blocking the build.

---

## 4. Estimate

| | Desktop/macOS only (Decision B = no iOS) | + iOS |
|---|---|---|
| WP1 toolchain | 1–2 d | 1–2 d |
| WP2 resources | 2–3 d | 2–3 d |
| WP3 seams | 2 d | 4 d |
| WP4 stdlib | 1–2 d | 1–2 d |
| WP5 data/rest | 2 d | 3 d |
| WP6 VMs + Koin | 1 d | 1 d |
| WP7 UI move | 2–3 d | 2–3 d |
| WP8 desktop app | 2 d | +4–5 d (iOS app) |
| **Total** | **~2 weeks** | **~3–3.5 weeks** |

**Risk profile — the inverse of Phases 2–3:** *low consequence, high churn.* No user data is at
stake. The danger isn't corruption, it's a long red build. Mitigation: land WP1 first (it's the
one that can genuinely block), keep every WP ending green on Android, and don't move UI until its
couplings are gone.

> ⚠️ **The original claim here — "failures are compile-time, not silent" — is wrong, and it has
> now been falsified twice.** WP2's placeholder bug (CMP substitutes only `%1$d`, never a bare
> `%d`) compiled, passed 212 tests, logged nothing, and broke *every plural in the app*; it was
> caught only by looking at the running screen. WP3d's `Uri.encode` swap had the same shape: a
> wrong encoder still produces a *valid* URL, just with the wrong text in it.
> **Anything that resolves at runtime — resources, URLs, DI, `startActivity` flags — must be
> proven at runtime.** A green build is not evidence about any of them.

## 5. Status / next step
- ✅ **Decision A decided + done** (A2: typed errors; the UI localises). Extended to the
  ViewModel layer in WP2 via `LinksSnackbar`.
- ✅ **Decision B decided** (no iOS; Android + Desktop, seams kept iOS-ready).
- ✅ **WP1 complete** — CMP toolchain, Coil 3, CMP navigation. (`@Preview` deferred to WP7.)
- ✅ **WP2 complete** — resources in `:shared/commonMain/composeResources`; `:app` has no
  `R.string`/`R.plurals`/`R.drawable` left (only 3 `R.mipmap` in an Android-only `@Preview`).
- ✅ **WP3a–e complete** — `Log`, `Notifier`, `FileAccess`, `BackupManager`, `Clipboard`,
  `UrlOpener`, `Sharer`, `LoginFlow`. `InstalledApps` proved unnecessary; `TranslateIntent` was
  dead code and is gone. The intent builders now live in `commonMain`.
- ⏳ **Open: Decision C** (desktop feature parity; recommend reduced v1 + capability flags).
  `LoginFlow` is the first concrete instance of the capability-flag pattern this decision needs.

**Android coupling in the UI layer (`android.*` imports *and* fully-qualified usages): 14 → 0**,
apart from `XLoginActivity` itself, which is a WebView and Android-only by design — it is now
behind the `LoginFlow` seam, so no other screen names it.

**`androidx.activity.*` in the UI layer: 4 files → 0.** `XLoginActivity` still uses it, but it *is*
an Activity, and nothing else names it.

**WP3 is closed.** Every platform seam exists, has an Android and a desktop implementation, and is
verified on a real device.

### The compile gate — ✅ DONE (and it should have come sooner)

Everything up to WP3f was verified by an *Android* build, which by construction **cannot tell you
whether code would compile in `commonMain`**: `:app` resolves `android.*` and `androidx.activity.*`
perfectly well. That left a grep as the only check, and the grep was wrong three times:

1. WP3c: a fully-qualified `android.net.Uri` in `SettingsScreen` (invisible to `^import android.`)
2. WP3d: the "`FilePicker` is not needed" claim (invisible to a metric that ignored `androidx.*`)
3. The gate's own first run: `SettingsModels` referencing a **fully-qualified**
   `cut.the.crap.ui.components.ActiveState` — same blind spot, third time.

So a canary set of already-clean UI now lives in `shared/commonMain` and is compiled for desktop on
every build: the four `theme/` files, `ColorPicker`, `StateIndicators`, `SettingsModels`,
`CharCountUtils`, and `ActiveState` (extracted from `MyChip`, whose `@Preview`s keep it in `:app`).
**The compiler now enforces the boundary instead of me.**

Three real problems surfaced the moment it ran — none of which a single-module build can produce:

- **`internal` is *module*-scoped.** `ModifierExt`'s `textDependentHeight`/`textDependentSize` were
  `internal`; moving them to `:shared` hid them from `:app`. Made public.
- **Smart casts stop at the module boundary.** `if (settings.xAuthToken != null)` no longer
  narrows, because Kotlin cannot prove a getter in another module is stable. Bind to a local first.
  Expect more of these as `AppSettings`-shaped types move.
- **A missing `getValue` import** in `FilePicker.desktop.kt` — green on Android, red on desktop.

Every one of these is a fact about crossing a module boundary that WP6/WP7 would have hit *en
masse*. They now arrive one file at a time.

### WP5 spike — ✅ DONE. Both unknowns clear; the plan stands.

DataStore works multiplatform, and Ktor exposes both things `UrlResolver` needs from OkHttp. See
the WP5 section above for detail. One cost the plan had missed: `UrlResolver` also uses
`org.json.JSONObject` (Android-only), so budget ~150 lines more rewriting than estimated.

**Nothing left can invalidate the plan.** The remaining work is laborious but known:

- **WP5** — move `data/rest` + `data/preferences` into `:shared`; `expect fun httpEngine()`;
  rewrite `UrlResolver` (okhttp3 + org.json → Ktor + kotlinx-serialization).
- **WP4** — `java.time` → kotlinx-datetime, `java.io` → okio. Grunt work, no unknowns; good filler.
- **WP6** — ViewModels + Koin modules → `commonMain`.
- **WP7** — the 57 UI files → `commonMain`; decide `@Preview` per file (WP1d lands here).
- **WP8** — the desktop app module. Decision C (feature parity) must be settled by then;
  `LoginFlow` and `Sharer` already show the capability-flag shape it should take.
