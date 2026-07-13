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

### WP3 — Platform seams  *(M, low-risk)* — **WP3a–d COMPLETE**
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
| `LoginFlow` (WebView) | `XLoginActivity` | **unsupported — capability flag** | WP3e |

**`InstalledApps` did not need to exist.** Its only live caller asked "is X installed?" purely to
decide whether to pin an `ACTION_VIEW` intent to the X package. That is not a question shared code
ever needs answered — it only ever wanted *the URL opened, preferably in the native client*. So it
became `UrlOpener.open(url, preferApp = ExternalApp.X)`, and the `PackageManager` check is now a
private detail of `AndroidUrlOpener`. `TranslateIntent.kt` (the other `PackageManager` user, 230
lines) turned out to be **dead code** and was deleted rather than ported.

**`FilePicker` is not needed either.** Picking is already done by Compose's
`rememberLauncherForActivityResult` in the UI; only *reading* the result crossed the boundary, and
that is `FileAccess` (WP3c).

**Gotcha (WP3d):** `TwitterIntent`/`FacebookIntent` built their URLs with `android.net.Uri.encode`.
Swapping that for a common encoder is invisible to the compiler *and* to a smoke test — a wrong
encoding still yields a valid URL, just with mangled text in it. `Uri.encode`'s safe set is
`[A-Za-z0-9]` + `_-!.~'()*` (note the apostrophe, which a strict RFC 3986 encoder would escape,
changing every tweet containing "don't"). `tools/urlEncode` reproduces it exactly and is pinned by
22 JVM tests — which also prove the share URLs are correct *off* Android, where `Uri` does not exist.

### WP4 — JVM stdlib → multiplatform  *(M, low-risk)*
`java.time`/`SimpleDateFormat`/`Locale`/`NumberFormat` → **kotlinx-datetime** + a small
formatter seam; `java.io.File`/streams → **kotlinx-io/Okio**; `UUID` → Kotlin `Uuid`.
Unblocks `ContentItemManager`/`ItemManager` (the `tools` date helpers) as a side-effect.

### WP5 — Move `data/rest` + `data/preferences` into `:shared`  *(M)*
- Ktor engine → `expect fun httpEngine()` (OkHttp on Android, Java/OkHttp on desktop).
- **Rewrite `UrlResolver` off raw `okhttp3`** onto the Ktor client (1 file, but fiddly:
  it drives redirects manually).
- DataStore: `datastore-preferences-core` is multiplatform; needs a per-platform path factory.
- Depends on WP2's Decision A (the 8 repos referencing `R.string`).

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
- ✅ **WP3a–d complete** — `Log`, `Notifier`, `FileAccess`, `BackupManager`, `Clipboard`,
  `UrlOpener`, `Sharer`. `InstalledApps` and `FilePicker` proved unnecessary; `TranslateIntent`
  was dead code and is gone. The intent builders now live in `commonMain`.
- ⏳ **Open: Decision C** (desktop feature parity; recommend reduced v1 + capability flags).

**UI files importing `android.*`: 14 (WP3 start) → 1.** The one left is `XLoginActivity`
(WebView), which is Android-only by design and is WP3e's capability flag.

**Next step:** WP3e — `LoginFlow` capability flag, so the desktop build can compile without a
WebView and the UI can hide the X-login affordance rather than offer a dead button. Then WP4.
