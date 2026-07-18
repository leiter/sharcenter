# iOS Implementation Plan — ShareCenter (`cut.the.crap`)

**Status:** WP-iOS-1 … WP-iOS-6 **and WP7** ✅ **done — the iOS app runs the real UI on the
Simulator, runtime-verified.** Rendering the real screens (not the old placeholder) confirmed the
SQLite driver, the full Koin graph and the locale formatting on-device. Android/desktop stayed green
throughout (`desktopTest` 201/0, app unit tests incl. Koin verify). Remaining work is all deferred
items (§4) + the desktop app (WP8, now unblocked).
**Last updated:** 2026-07-18

---

## Context

The KMP migration deliberately deferred iOS (Phase 4C Decision B: "Android + Desktop only"), **but
built every platform edge behind a seam so iOS stays addable — adding actuals, not
re-architecting.** This work cashed that in. The bet largely paid off: every seam did just need an
actual. What it did *not* predict was a toolchain/dependency problem (see §3).

**Scope decisions:**
- **Reduced v1** — core CRUD/lists/networking/posting/settings. Android-only features (WebView
  X-login, MediaStore backup) stay behind capability flags, as desktop already does.
- **Share Extension deferred** — a separate Xcode target + app-group DB sharing; a later phase.
- **Targets:** `iosX64`, `iosArm64`, `iosSimulatorArm64`; dynamic `Shared.framework`; no CocoaPods.

---

## 1. What shipped

| WP | Outcome |
|---|---|
| **iOS-1** | `handleAction` + the `Screen` destinations → `commonMain`, behind a new **`AppRestarter`** seam plus the existing `Notifier`/`BackupManager`/`CoroutineScope`. `NavigationGraph` stayed in `:app`. |
| **iOS-2** | iOS targets + framework; **Kotlin 2.1.21→2.2.20, CMP 1.8.2→1.10.3** (forced, see §3); `commonMain` made genuinely Native-clean. |
| **iOS-3** | `iosMain` actuals: `Log`→println, `httpClientEngine`→**Darwin**, `preferencesPath`→`NSDocumentDirectory`, `LocaleFormat`→`NSNumberFormatter`/`NSDateFormatter`. `rememberFilePicker` **stubbed**. |
| **iOS-4** | `Clipboard`→UIPasteboard, `UrlOpener`→`openURL`, `Sharer`→`UIActivityViewController`, `Notifier`→SharedFlow, `FileAccess`→**okio** (no cinterop), `LoginFlow`/`AppRestarter`/`BackupManager`→capability-flagged no-ops, `createDriver`→**NativeSqliteDriver** (foreign keys on), `iosPlatformModule` + `iosDatabaseModule`. |
| **iOS-5** | **`AppConfig`** seam replaces `BuildConfig`; `networkModule` + `repositoryModule` → `commonMain`; `androidAppModule` (AppConfig from BuildConfig + `YouTubeMetadataBackfiller`); iOS `setupKoin()` + `MainViewController()`. |
| **iOS-6** | `iosApp/` — SwiftUI host, **XcodeGen** `project.yml` (no hand-written `.pbxproj`), Gradle `embedAndSignAppleFrameworkForXcode` pre-build script. **Launches on the Simulator.** |
| **WP7** | The ~33 UI files (screens, `My*` components, `NavigationGraph`, `DomainIcons`) → `commonMain`; root **`App()`** extracted from `MainActivity.setContent{}`. Both launchers are now one-liners over the same `App()`. `@Preview`s stay Android-only in sibling `*Previews.kt` (WP1d). |

**New seams added along the way** (neither was in the plan): `AppRestarter` (process restart —
Android only) and `defaultIoDispatcher` (`Dispatchers.IO` is *internal* on Native).

**Runtime verification (Simulator, the real UI):** app launches, no crash; the Links list renders
from `NativeSqliteDriver` (confirmed the driver *created* `app_database` with the full schema —
`tweets_table`, `handle_tag_table`, cross-ref tables — then queried it); seeded rows render with
**correct dates**, closing the `LocaleFormat` "invisible bug" risk. So the SQLite driver, the full
Koin graph, the ViewModels, `LocaleFormat`, and Compose-on-iOS are all confirmed at runtime, not
just at compile.

---

## 2. Verification — what is actually proven

**Proven at runtime (Simulator):** framework builds + embeds + signs, Swift↔Kotlin bridge
(`setupKoin()`, `MainViewController()`), Koin `startKoin` without throwing, **Compose Multiplatform
rendering on iOS**.

**Proven at build level only:** `:shared:linkDebugFrameworkIosSimulatorArm64` links with no
unresolved symbols (stronger than compiling — it exercises every actual).

**NOT yet proven — needs the real UI (WP7):**
- `NativeSqliteDriver` actually opening/migrating the DB on iOS
- Full Koin graph resolution (VM → repos → HttpClient). *There is no iOS-side `verify()`* — the
  Android `KoinGraphTest` covers the Android module set only.
- **`LocaleFormat`** — the known "invisible bug" class: it compiles and is wrong only on screen
  (German `5.154` / `38,48 %`, Slovenian `14. jul. 2026`). **Check this on a running screen.**
- Darwin HTTP engine, clipboard, share sheet.

**Android/desktop regression status:** `:shared:desktopTest` 198/0 · `:app` unit tests incl.
`KoinGraphTest` green · `:app` compiles.

---

## 3. ⚠️ What the plan got wrong

Worth reading before the next platform bring-up — the pattern repeats.

**1. The predicted #1 risk was a non-issue; the real blocker was invisible.**
The plan's headline spike was `material-icons-extended` 1.7.3 vs CMP 1.8.2 on iOS. It **resolved
fine, first try**. What actually blocked everything was never mentioned: **`datastore-preferences-core`
1.2.1's iOS klib was built with Kotlin 2.2.20 and is unconsumable by Kotlin 2.1.21** (klib ABI is
hard-locked to the compiler; JVM bytecode is not). That forced the Kotlin/AGP bump the migration
plan had explicitly **decoupled and deferred** — Kotlin 2.1.21→2.2.20 + CMP 1.8.2→1.10.3, plus
migrating `:shared` off the now-hard-error `kotlinOptions` DSL. *Lesson: on Native, a dependency's
build-time Kotlin version is a hard compatibility constraint, and it is not visible from the JVM.*

**2. "`commonMain` is native-clean" was wrong — and the desktop compile gate could never say
otherwise.**
The readiness check ("0 `java.*` imports") was true and irrelevant. Enabling iOS immediately found:
- **`System.currentTimeMillis()` in 13 files / 29 sites.** `java.lang` is auto-imported, so *no
  import line exists to grep for* — this is blind spot #4 from `PHASE_4C_SCOPE`, hit again.
- **`Dispatchers.IO`** — public on JVM, **`internal` on Native** (5 sites).
- **`javaClass`**, and **`Map.getOrDefault`** (a JVM-only extension).

None of this could be caught by the `[desktop]` compile gate, **because desktop is a JVM target**.
A gate across two JVM targets proves portability *between JVMs*, not portability. *Only a native
target is a real portability gate — which is exactly what the plan predicted, and the single
biggest reason to add iOS even if you never ship it.*

**3. A green compile still proved nothing — again.**
After everything compiled on all three targets, `:shared:desktopTest` failed **39 tests**:
`commonMain` compiled against **kotlinx-datetime 0.6.2** while a transitive constraint forced
**0.7.1 at runtime**, where `kotlinx.datetime.Instant` became a typealias to `kotlin.time.Instant`
and the class the bytecode referenced no longer exists → `NoClassDefFoundError`, surfacing as
`UncaughtExceptionsBeforeTest` inside coroutines. Fixed by pinning 0.7.1 (align compile with the
winning runtime). **Caught only by running the tests.** The doc's own rule — *"anything that
resolves at runtime must be proven at runtime"* — held for a case nobody listed: dependency
resolution itself.

**4. WP-iOS-1 was mis-sequenced.**
It assumed a common `App()` could be extracted. It cannot: `NavigationGraph` references the screen
composables, which are **still in `:app`** (WP7 isn't done). Only `handleAction` was separable —
which was the valuable half anyway. The `App()`/NavHost shell is a ~15-line relocation that lands
naturally with WP7.

**5. Small things the plan didn't know:**
- Kotlin/Native does **not** expose ObjC class factory methods (`NSNumber.numberWithLong`,
  `NSDate.dateWithTimeIntervalSince1970`, `NSLocale.currentLocale`) — use **constructors**
  (`NSNumber(long=)`, `NSDate(timeIntervalSinceReferenceDate=)`) and let the formatters default to
  the current locale. `NSCalendar.currentCalendar`, inconsistently, *does* resolve.
- Kotlin/Native exports `init*` functions to Swift as `doInit*` → the entry point is `setupKoin()`.
- Kotlin **default arguments don't bridge to Swift**, so Swift-facing functions take no defaults.
- **Compose hard-crashes at launch** (`PlistSanityCheck`) unless `CADisableMinimumFrameDurationOnPhone=true`
  is in `Info.plist`; we set it *and* disable the strict check via
  `ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false })`.
- A **dynamic** framework (`isStatic = false`) avoids making the Xcode app link the Kotlin runtime's
  transitive system libs (sqlite3, …) by hand.

**6. WP7 was mechanical — until the *link* step, a whole failure class compile could not show.**
Moving the UI into `commonMain` was as smooth as promised (a handful of dead-code couplings the
compiler flagged one file at a time — see the WP7 commit). Both compile and iOS *compile* were
green on all targets. Then `linkDebugFrameworkIosSimulatorArm64` failed with
`Undefined symbols: androidx.lifecycle.viewmodel.compose#LocalViewModelStoreOwner$stableprop_getter$artificial`,
referenced from the three functions that call `koinViewModel()` (`App`, `SettingsScreen`,
`BackupManagementScreen`). Two dead ends before the real cause:
- It *looks* like a lifecycle-version problem, so I chased the JetBrains lifecycle fork: 2.9.6/2.10.0
  resolve but still miss the symbol, 2.11.0 has no iOS artifacts, androidx's own
  `lifecycle-viewmodel-compose` is Android/JVM-only even at 2.10.0. All wrong.
- The actual cause is **Koin issue #2175**: `koinViewModel()` didn't link on iOS with Koin 4.0.x
  (fixed in the 4.1.0 milestone). But Koin **4.2.x** fails for the *opposite* reason — its klibs are
  built by **Kotlin 2.3.20** (ABI 2.3.0), unconsumable by our 2.2.20 (the datastore ABI-lock,
  inverted). **4.1.1** threads the needle: has the fix, predates the 2.3 bump.

*Lessons:* (a) linking is a distinct gate — several deps that *compile* fine only fail at link,
because Kotlin/Native resolves inline bodies and Compose-generated symbols then. (b) For any KMP dep
there is now a **two-sided version window**: new enough to have the fix/feature, old enough that its
klib was built with a Kotlin ≤ ours. Both edges bit here.

---

## 4. Deliberately deferred

| Item | Why |
|---|---|
| **Real `rememberFilePicker`** | `UIDocumentPickerViewController` + delegate + **security-scoped URLs** (which the iOS `FileAccess` must cooperate with). Needs *presentation* testing, not just compilation. Import/restore is capability-flagged in v1, so the stub yields nothing. |
| **Backup/restore, WebView X-login** | `IosBackupManager` / `IosLoginFlow.isSupported = false`. Android impls are MediaStore/SAF + WebView specific. |
| **iOS Share Extension** | Separate target + app groups. |
| **iOS test suite / CI** | The suite is JVM-only (JUnit/MockK/Truth). Running it on `iosSimulatorArm64` means porting the test libs — real work, not a source-set add. **WP-iOS-7.** |
| **kotlinx-datetime 0.7 deprecations** | `dayOfMonth`→`day`, `monthNumber`→`month`, `Instant` typealias. Warnings only. |

---

## 5. Next

WP7 is done and runtime-verified. Remaining, in rough priority:

1. **Android regression run** — `MainActivity` now resolves ViewModels via `koinViewModel()` inside
   the shared `App()` (was `by viewModel()`), and Koin went 4.0.0→4.1.1. Behaviour should be
   identical; not yet re-confirmed on a device.
2. **Desktop app (WP8)** — now unblocked: `App()` is common. A `desktopApp` with
   `application { Window { App() } }`, the JDBC driver, an `AppConfig`, and a desktop Koin module.
3. **Real `rememberFilePicker`**, then the iOS **Share Extension** — the two things that make iOS a
   first-class client rather than a viewer.
4. **WP-iOS-7** — CI (macOS runner: assemble/link the framework), and the kotlinx-datetime 0.7
   deprecation cleanup.

**Estimate accuracy:** the ~1.5–2 week estimate held for the *seam* work — it was as mechanical as
promised, WP7 included. The unbudgeted cost was entirely **dependency/toolchain alignment**: the
forced Kotlin bump (§3.1), making `commonMain` genuinely native-clean (§3.2), the kotlinx-datetime
skew (§3.3), and the Koin/lifecycle link saga (§3.6). Every one was a version-compatibility problem
invisible to the JVM and, in two cases, invisible to compilation itself. That — not the seam
plumbing — is where an "add iOS to an existing KMP app" estimate should put its contingency.
