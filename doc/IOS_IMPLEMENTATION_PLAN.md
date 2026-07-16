# iOS Implementation Plan — ShareCenter (`cut.the.crap`)

**Status:** WP-iOS-1 … WP-iOS-6 ✅ **done — the iOS app builds, launches and renders Compose on
the Simulator.** The UI is still a **placeholder**: the real screens need WP7 (the Phase-4C UI
move), which is what will finally exercise the SQLite driver, the full Koin graph and the locale
formatting at runtime. Android stayed green throughout.
**Last updated:** 2026-07-16

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

**New seams added along the way** (neither was in the plan): `AppRestarter` (process restart —
Android only) and `defaultIoDispatcher` (`Dispatchers.IO` is *internal* on Native).

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

1. **WP7 — move the ~57 UI files into `commonMain`** (screens, `My*` components, `NavigationGraph`),
   then flip `MainViewController()` from the placeholder to `App()`. This is the payoff: it makes
   iOS render the real app, proves the driver/graph/formatting at runtime, and unblocks the
   never-built **desktop app (WP8)**.
2. **WP-iOS-7** — CI (macOS runner: assemble the framework), and the deprecation cleanup.

**Estimate accuracy:** the ~1.5–2 week estimate held for the *seam* work (it was as mechanical as
promised). The unbudgeted cost was the toolchain bump + making `commonMain` genuinely native-clean —
roughly a day that the plan priced at zero, because it assumed a fact ("native-clean") that only a
native compile could have established.
