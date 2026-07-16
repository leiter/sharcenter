# iOS Implementation Plan — ShareCenter (`cut.the.crap`)

## Context

The KMP migration (`kmp-migration` branch) deliberately deferred iOS (Phase 4C
Decision B: "Android + Desktop only"), **but built every platform edge behind a seam
so iOS stays addable — adding actuals, not re-architecting.** This plan cashes that in:
it adds the iOS target, fills in the iOS actuals, and stands up an Xcode app that renders
the existing Compose Multiplatform UI.

**Deliverable of this task:** write this plan to `doc/IOS_IMPLEMENTATION_PLAN.md`
(mirroring the existing `doc/KMP_MIGRATION_PLAN.md` / `doc/PHASE_4C_SCOPE.md`).

**Scope decisions (confirmed with user):**
- **Reduced v1** — core CRUD/lists/networking/posting/settings. Android-only features
  (WebView X-login, MediaStore backup) stay behind capability flags / `isSupported = false`,
  exactly the pattern already used for desktop (`DesktopLoginFlow`, `DesktopSharer`).
- **Share Extension deferred** — receiving `ACTION_SEND`-style shared URLs (the Android
  `ShareActivity` equivalent) is a separate Xcode target + app-group DB sharing; it is a
  named follow-up phase, not v1.
- **Common `App()` extraction is in scope** as the first step — it also unblocks the
  never-built desktop app (WP8).

---

## Readiness assessment (verified against the code)

| Area | State | iOS implication |
|---|---|---|
| `shared` targets | `androidTarget()` + `jvm("desktop")` only | **Add `iosX64/iosArm64/iosSimulatorArm64`** |
| `commonMain` purity | **0 real `java.*`/`javax.*` imports** (only in comments) | Native-clean today — but both current targets are JVM, so a lurking `java.*` would have compiled. Enabling iOS is itself the ultimate compile gate. |
| `expect`/`actual` seams | `Log`, `preferencesPath`, `rememberFilePicker`, `LocaleFormat`×6, `httpClientEngine()` — Android+desktop actuals only | **Add `iosMain` actuals** |
| Interface+DI seams | `Notifier`, `Sharer`, `Clipboard`, `FileAccess`, `UrlOpener`, `LoginFlow`, `BackupManager` — Android+desktop impls only | **Add iOS impls + an iOS Koin module** |
| DB driver | `createDriver(Context)` is a plain per-source-set fn in `androidMain` (not `expect`); `createDatabase(driver)` is common | **Add `createDriver()` in `iosMain`** using `NativeSqliteDriver` |
| Root UI | `App()`/NavHost/`handleAction` still inside `app/.../MainActivity.kt` `setContent{}` | **Extract a common `App()`** into `commonMain` |
| Composition root | `databaseModule`/`platformModule`/`networkModule`/`repositoryModule` are Android-only in `:app`; only `viewModelModule` is common | **iOS `initKoin()` with iOS platform/db/network modules**; move platform-agnostic module bodies to `commonMain` where possible |
| Config | `NetworkModule` reads `BuildConfig.API_BASE_URL` / `BuildConfig.DEBUG` | **Replace `BuildConfig` with an injected `AppConfig`** (no `BuildConfig` on iOS) |

**Toolchain (all iOS-capable at current versions):** Kotlin 2.1.21, CMP 1.8.2 (iOS ✅),
SQLDelight 2.0.2 (`native-driver` ✅), Ktor 2.3.5 (`ktor-client-darwin` ✅), Koin 4.0.0
(native ✅), Coil 3.2.0 (iOS ✅), datastore 1.2.1 (native ✅), okio 3.9.1, kotlinx-datetime
0.6.2, lifecycle-viewmodel (JetBrains fork, iOS ✅).

---

## Spikes to run first (each can invalidate an estimate, none the plan)

1. **`material-icons-extended` pinned at `1.7.3` while CMP is `1.8.2`** — this coexistence
   was proven for Android+desktop; confirm the `1.7.3` icons artifact **publishes iOS
   klibs** and links. The app uses 87 icons. If it doesn't resolve for native, either bump
   the pin or vendor the ~87 icons. *(Highest-risk unknown, mirrors the WP1 icons spike.)*
2. **Full `commonMain` native compile** — `./gradlew compileKotlinIosSimulatorArm64` the
   moment the target is added, before writing any actual. This surfaces any JVM API that
   slipped into common under cover of the two JVM targets.
3. **Coil 3 image loading on iOS** — confirm the Coil setup used by the 2 image screens
   needs no extra iOS platform context wiring.

---

## Work packages

Ordered so Android stays green throughout (adding a target + `iosMain` never touches
`androidMain`). WP-iOS-1 is a shared refactor that also benefits desktop.

### WP-iOS-1 — Extract a common `App()` + platform-agnostic action handling *(M, shared)*
The blocker: the NavHost, theme wrapper and `handleAction()` live in `MainActivity`, and
`handleAction` is tangled with `activity`, `lifecycleScope`, `Toast`, `DatabaseBackupManager`
and `restartApp`.
- Move `NavigationGraph` + the `MyAppTheme { Surface { … } }` wrapper into a
  `commonMain` `@Composable fun App()`.
- Rework `handleAction` to depend only on the existing seams already in `commonMain`
  (`Clipboard`, `UrlOpener`, `Sharer`, `Notifier`, `BackupManager`) instead of `activity`
  /`Toast`/`lifecycleScope`. The backup/restore branches route through `BackupManager` +
  `Notifier`; the process-restart after restore becomes an `expect fun restartApp()` (or a
  capability that iOS/desktop no-op with a "relaunch to finish" notice).
- `MainActivity` shrinks to `setContent { App() }`; **Android stays green** and gains
  nothing platform-specific it didn't have.
- *Payoff beyond iOS:* this is exactly what the desktop app (WP8) needs too.

### WP-iOS-2 — Enable iOS targets + framework packaging *(S)*
In `shared/build.gradle.kts`:
- Add `iosX64()`, `iosArm64()`, `iosSimulatorArm64()` with a
  `binaries.framework { baseName = "Shared"; isStatic = true }` (direct framework, **no
  CocoaPods** — there are no native pod deps). Consider an `XCFramework` assembly task.
- Add `iosMain` deps: `ktor-client-darwin`, `sqldelight-native-driver`.
- Rely on the default hierarchical source-set template (`iosMain` shared across the three
  iOS targets; add `appleMain` later only if macOS-native joins).
- Add an `iosTest` source set so the existing suite can also run on
  `iosSimulatorArm64` (the native counterpart to the current `[desktop]` compile gate).

### WP-iOS-3 — `iosMain` actuals for the `expect` declarations *(M)*
| `expect` | iOS `actual` |
|---|---|
| `Log` (`platform/Log.kt`) | `NSLog` / `println` |
| `preferencesPath(name)` (`data/preferences/PreferencesStore.kt`) | path under `NSDocumentDirectory` via `NSFileManager` (mirrors the Android `filesDir/datastore/<name>.preferences_pb` layout convention) |
| `httpClientEngine()` (`data/rest/HttpClientEngine.kt`) | `Darwin` engine factory |
| `LocaleFormat`×6 (`tools/LocaleFormat.kt`) | `NSNumberFormatter` / `NSDateFormatter`; the per-language variants (`formatIntegerForLanguage`, `formatMediumDateForLanguage`) build an `NSLocale(localeIdentifier = languageTag)`. **Verify against the WP4 device evidence** (`lt` → `6 476`, `de` → `5.155`) — formatting bugs are invisible to the compiler. |
| `rememberFilePicker(...)` (`platform/FilePicker.kt`) | `UIDocumentPickerViewController` presented via the current `UIViewController`; cancel → empty list, matching the Android/desktop contract |
| `createDriver(name)` (new `iosMain` fn, sibling of `androidMain`'s) | `NativeSqliteDriver(ShareDatabase.Schema, name)` with `onConfiguration` enabling `PRAGMA foreign_keys=ON` (the cross-ref CASCADE requirement) |

### WP-iOS-4 — iOS implementations of the interface seams + iOS Koin module *(M)*
Add iOS impls (in `iosMain`) and an `iosPlatformModule` mirroring `PlatformModule.kt`:
| Seam | iOS impl |
|---|---|
| `Notifier` | in-app snackbar via a `SharedFlow` (same shape as `DesktopNotifier` — iOS has no Toast) |
| `Clipboard` | `UIPasteboard.general` |
| `UrlOpener` | `UIApplication.sharedApplication.openURL`; `preferApp` hint ignored on iOS |
| `Sharer` | `UIActivityViewController` |
| `FileAccess` / `saveToDownloads` | write to `NSDocumentDirectory` (or present a share sheet); files surface in the Files app |
| `LoginFlow` | `isSupported = false` (capability flag; WKWebView login is a later phase) |
| `BackupManager` | `DatabaseBackupManager` is Android MediaStore-only and lives in `:app`. For iOS, bind a minimal file-based backup or a capability-flagged no-op — decide in WP-iOS-4. |

### WP-iOS-5 — Config seam (replace `BuildConfig`) + iOS composition root *(S–M)*
- Introduce an injected `AppConfig(apiBaseUrl, isDebug)` and change `networkModule` to read
  it instead of `BuildConfig`. Move the platform-agnostic `networkModule`/`repositoryModule`
  bodies into `commonMain`, leaving each platform to provide only its `AppConfig`, DB
  driver and platform module. (Android provides `AppConfig` from `BuildConfig`; iOS from a
  constant / plist.)
- Add `iosMain` `fun MainViewController(): UIViewController = ComposeUIViewController { App() }`
  and `fun initKoin()` that `startKoin { modules(iosDatabaseModule, iosPlatformModule,
  networkModule, repositoryModule, viewModelModule) }` — the iOS analogue of
  `MainActivity`'s `startKoin` block.

### WP-iOS-6 — Xcode `iosApp` project *(S–M)*
- New `iosApp/` Xcode project (SwiftUI `App` or UIKit) whose root hosts
  `MainViewController()` from the framework; call `initKoin()` at launch.
- Link the `Shared` framework (Xcode "Run Script" invoking the Gradle
  `embedAndSignAppleFrameworkForXcode` task, or an `XCFramework`).
- `Info.plist`: app identity, any custom URL scheme, portrait/orientation. **Not** in v1:
  Share Extension entitlements/app groups.
- Keep `iosApp/` out of the Gradle `settings.gradle` include set (it's an Xcode project,
  like the plan's `iosApp/` sketch in `KMP_MIGRATION_PLAN.md` §2).

### WP-iOS-7 — Verification, CI, docs *(S)*
- Run the migrated unit suite on `iosSimulatorArm64` (proves common logic on native, not
  just JVM).
- Add a macOS CI job: assemble the framework + run `iosSimulatorArm64Test`.
- Update `doc/DESCRIPTION.md` / `KMP_MIGRATION_PLAN.md` status to reflect iOS landing.

---

## Deferred (explicitly out of v1)
- **iOS Share Extension** (receive shared URLs) — separate Xcode target, app-group-shared
  DB, own process. Named follow-up phase.
- **WKWebView X-login** — `LoginFlow.isSupported = false` on iOS; Manual X Credentials
  (works everywhere) is the way in, same as desktop.
- **macOS-native target** — the Compose Desktop `.app` already serves macOS. iOS seams are
  built `appleMain`-ready so macOS-native remains addable.
- **Full MediaStore-parity backup UI** — iOS uses a reduced file/Files-app backup.

---

## Verification (runtime, not just green build)

The project's hard-won lesson — *"a green build proves nothing about resources, URLs, DI,
or formatting; prove them at runtime"* (WP2 plural bug, WP3d `Uri.encode` bug) — applies
doubly on a brand-new platform:

1. **Compile gate:** `./gradlew compileKotlinIosSimulatorArm64` green (WP-iOS-2).
2. **Unit suite on native:** `iosSimulatorArm64Test` green — the ~198 migrated tests now
   also run off-JVM.
3. **Boot the app in the iOS Simulator** and confirm end-to-end:
   - Links list loads real rows (DB driver + `foreign_keys` pragma + adapters).
   - **Plurals & number/date formatting render correctly** (the `NSNumberFormatter`
     actuals — check a plural count and the `lt`/`de` locale cases from WP4).
   - Networking: a post/preview round-trips through the Darwin engine.
   - Clipboard copy, external URL open, and the iOS share sheet work.
   - Settings shows **no** X-Login row (`isSupported=false`) but keeps Manual Credentials.
4. Android app still builds and runs after WP-iOS-1's `App()` extraction.

---

## Estimate

| WP | Scope | Est. |
|---|---|---|
| iOS-1 common `App()` extraction | M (shared w/ desktop) | 1–2 d |
| iOS-2 targets + framework | S | 0.5–1 d |
| iOS-3 `expect` actuals | M | 2 d |
| iOS-4 seam impls + Koin | M | 2 d |
| iOS-5 config seam + composition root | S–M | 1 d |
| iOS-6 Xcode app | S–M | 1–2 d |
| iOS-7 verify/CI/docs | S | 1 d |
| **Total** | | **~1.5–2 weeks** (matches the migration doc's "+iOS" delta) |

**Risk profile:** low-consequence, high-churn — same as Phase 4C. No user data at stake;
the danger is a long red native build and invisible formatting/resource bugs. Mitigation:
run the icons spike + native compile gate first; keep Android green; **prove every
runtime-resolved thing in the Simulator, not in the build log.**
