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
**Status: WP1a ✅ (CMP swap, commit 3b08433) · WP1b ✅ (Coil 3, commit d5ed635) ·
WP1d ⏸ deferred to WP7 · WP1c (navigation) ⬅ next**
Replace AndroidX Compose (`compose-bom` + `kotlin.plugin.compose`) with JetBrains Compose
Multiplatform. The app keeps running on Android throughout — CMP targets Android too.
- `org.jetbrains.compose` plugin; `compose.runtime/foundation/material3/ui` from CMP.
- `navigation-compose` → `org.jetbrains.androidx.navigation:navigation-compose` (11 files).
- `lifecycle-viewmodel-compose` → `org.jetbrains.androidx.lifecycle:*`.
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

### WP2 — Resources → Compose Multiplatform Resources  *(L, low-risk, mostly scriptable)*
**The single biggest line item (504 call sites), but far cheaper than it looks:**
CMP Resources reads **the same `values/strings.xml` format**, so the catalogue moves nearly
verbatim into `composeResources/values/strings.xml`. Only one locale, so no fan-out.
- `stringResource(R.string.x)` → `stringResource(Res.string.x)` — scriptable regex.
- 15 plurals → `pluralStringResource(Res.plurals.x, n)`.
- 19 `painterResource(R.drawable.x)` → `Res.drawable.x` (+ move 9 drawables).
- ⚠ **The hard 10%:** 8 `data/rest` repositories build user-facing messages from `R.string`
  through `StringProvider.get(resId: Int)`. CMP has no `Int` resource ids — see **Decision A**.

### WP3 — Platform seams (`expect`/`actual`)  *(M, low-risk)*
Common interfaces + Android actuals; desktop actuals are mostly trivial or no-ops.
| Seam | Android actual | Desktop actual |
|---|---|---|
| `Logger` | `android.util.Log` | stdout |
| `Notifier` (Toast) | `Toast` | Compose snackbar |
| `Clipboard` | `ClipboardManager` | AWT |
| `Sharer` (Intent) | `ACTION_SEND` | open URL / copy |
| `UrlOpener` | `Intent.ACTION_VIEW` | `Desktop.browse` |
| `FilePicker` (SAF/MediaStore) | SAF | AWT `FileDialog` |
| `InstalledApps` | `PackageManager` | returns `false` |
| `LoginFlow` (WebView) | `XLoginActivity` | **unsupported — capability flag** |

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
stake and failures are **compile-time, not silent**. The danger isn't corruption, it's a long red
build. Mitigation: land WP1 first (it's the one that can genuinely block), keep every WP
ending green on Android, and don't move UI until its couplings are gone.

## 5. Status / next step
- ✅ **Decision B decided** (no iOS; Android + Desktop, seams kept iOS-ready).
- ✅ **WP1 icons spike done** — CMP 1.8.2 + icons 1.7.3 verified on both targets.
- ⏳ **Open: Decision A** (data-layer strings) — blocks WP2 + WP5.
- ⏳ **Open: Decision C** (desktop feature parity; recommend reduced v1 + capability flags).

**Next step:** WP1 proper — swap `:app` from AndroidX Compose to CMP, keeping Android green.
