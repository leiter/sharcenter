# KMP Migration Plan — ShareCenter (`cut.the.crap`)

**Targets:** Android · iOS · Desktop (JVM: Linux/Windows/macOS) · macOS-native
**Status:** Planning — no migration code written yet
**Last updated:** 2026-07-12
**Baseline branch:** `feature/color-subjects`

> This plan is derived from a code audit of the current single-module Android app
> (120 main Kotlin files). It converts the app to Kotlin Multiplatform + Compose
> Multiplatform, replaces **Hilt with Koin** and **Room with SQLDelight**, and
> introduces `expect`/`actual` seams for every platform-bound edge.

---

## 1. Why these technology swaps

| Concern | Today (Android-only) | After migration | Reason |
|---|---|---|---|
| **Database** | Room 2.8.4 + kapt | **SQLDelight 2.x** | Mature drivers for JVM-desktop (incl. macOS), iOS/macOS-native, and Android. Typed SQL, no kapt/KSP-Android lock-in. Keeps existing table names & schema. |
| **DI** | Hilt (Android-only) | **Koin** | Pure-Kotlin, multiplatform; works in `commonMain`. |
| **Annotation proc.** | kapt | **KSP** (only where needed) | kapt is JVM-only; SQLDelight & Koin need no kapt. |
| **HTTP engine** | Ktor + hardcoded OkHttp; raw OkHttp3 in `UrlResolver` | **Ktor with per-platform engine** (OkHttp/Darwin/Java) | OkHttp is JVM/Android-only. |
| **UI** | Compose (AndroidX) | **Compose Multiplatform** (JetBrains) | Same API, runs on all four targets. |
| **Resources** | `R.string` / `R.drawable` | **Compose Multiplatform resources** (`Res.string`) | `R` is Android-generated. |
| **Date/time & I/O** | `java.time`, `java.text`, `java.io.File` | **kotlinx-datetime**, **kotlinx-io / Okio** | JVM-only APIs. |

### Why SQLDelight over Room-KMP (given the desktop + macOS requirement)
- Room-KMP targets Android/iOS/JVM/native but is comparatively new on **desktop-JVM
  and Apple-native**; driver ergonomics and community support for those targets are
  thinner.
- SQLDelight has production-proven drivers for **all four** targets:
  `AndroidSqliteDriver`, `NativeSqliteDriver` (iOS/macOS), and the JVM
  `JdbcSqliteDriver` (desktop, including macOS desktop).
- Your schema uses **legacy raw table names** (`tweets_table`,
  `prepared_tweets_table`, …) and hand-written SQL already — this maps cleanly to
  SQLDelight `.sq` files with zero data-format change. Existing on-device Android
  databases keep working untouched.

---

## 2. Target module structure

```
ShareCenter/
├── settings.gradle.kts            # kotlin("multiplatform"), Compose MP, SQLDelight, KSP
├── shared/                        # NEW — the multiplatform core
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/            # domain, repos, viewmodels, Compose UI, Koin, SQLDelight .sq
│       ├── commonTest/            # existing unit tests migrate here
│       ├── androidMain/           # actuals: OkHttp engine, Android drivers, intents, Toast…
│       ├── jvmMain/               # desktop actuals: JDBC driver, Java/OkHttp engine, AWT file I/O
│       ├── iosMain/               # actuals: Darwin engine, Native driver, UIActivityViewController
│       └── macosMain/             # actuals: Native driver, Darwin engine, AppKit share/clipboard
├── androidApp/                    # thin Android launcher (MainActivity, ShareActivity, manifest)
├── desktopApp/                    # NEW — Compose Desktop main()
└── iosApp/ + macosApp/            # NEW — Xcode entry points calling into shared
```

> **Phasing note:** you do *not* create all app modules at once. Phase 1 stands up
> `shared` + `androidApp` and keeps the Android app green. Desktop/iOS/macOS
> launchers arrive in Phase 7 once the common core compiles.

---

## Phase 0 — Baseline & safety net  ✅ DONE (2026-07-12)
**Goal:** lock a known-good starting point; make behavior verifiable before moving anything.

- [x] **Baseline scope decided:** merge `feature/color-subjects` into `kmp-migration`
      so the baseline is **DB v5** (SubjectDB + `post/link_subject_cross_ref`, color
      history). Merge was clean (no conflicts).
- [x] Confirm green build + all unit tests pass — forced full rerun:
      **208 tests, 0 failures, 6 skipped** (24 suites); `compileDebugSources` +
      `testDebugUnitTest` green.
- [x] **Validated the Room v4→v5 migration on-device** (Pixel 7a) via an automated
      `MigrationTestHelper` test (`app/src/androidTest/.../data/db/MigrationTest.kt`):
      seeded v4 rows survive, new subject/cross-ref tables are created, and Room's
      schema-5 validation passes. This is the reference behaviour the SQLDelight port
      must reproduce in Phase 3.
- [x] Snapshot the exported Room schemas — `4.json` + `5.json` present under
      `app/schemas/cut.the.crap.data.db.AppDatabase/` (source of truth for Phase 3).
- [ ] **DATA-LAYER FEATURE FREEZE (in effect):** no new entities/DAOs/migrations or
      schema changes on `kmp-migration` until the SQLDelight port (Phase 3) lands.
      Bug-fix-only for the DB layer during the migration window.
- [x] Tagged `pre-kmp-baseline` at commit `7b4aa95` (Phase 0 changes committed on `fb6341d`+`7b4aa95`).

**Build note (new):** running instrumented tests required a non-minified build type.
The `debug` type deliberately minifies/obfuscates (strips Kotlin stdlib the AndroidX
test runner needs), so Phase 0 added an `instrumentation` build type + `testBuildType =
"instrumentation"`. Run device tests with `./gradlew connectedInstrumentationAndroidTest`.

**Exit:** reproducible green build ✅, migration verified on-device ✅, schemas archived ✅.

---

## Phase 1 — Build restructure to a KMP skeleton
**Goal:** app still ships on Android, but now builds from a multiplatform `shared` module.

- [ ] Introduce a **version catalog** (`gradle/libs.versions.toml`) if not present.
- [ ] Add plugins: `kotlin("multiplatform")`, `org.jetbrains.compose`,
      `com.android.library` (for `shared`), `com.android.application` (for `androidApp`),
      `app.cash.sqldelight`, `com.google.devtools.ksp`.
- [ ] Create `shared` with `android()` + `jvm("desktop")` targets first
      (defer `iosX64/iosArm64/iosSimulatorArm64/macosArm64` until Phase 7 to keep
      the early loop fast).
- [ ] Move `MainActivity`, `ShareActivity`, `XLoginActivity`, manifest, and
      Android resources into `androidApp`. Everything else stays put *for now* under
      `shared/src/androidMain` (compiles as-is against Android) — we relocate to
      `commonMain` incrementally in later phases.
- [ ] **Drop kapt**; the only remaining processors are KSP-based (SQLDelight generates
      without KSP; Koin annotations optional).
- [ ] `jvmTarget` → 17 (Compose MP baseline); reconcile `compileOptions`.

**Risk:** Gradle plugin/version alignment (AGP ↔ Kotlin ↔ Compose MP ↔ SQLDelight).
See the standing constraint that most dep bumps are gated on the AGP 9 / Gradle 9 /
Kotlin 2.4 migration — **fold that coordinated bump into this phase** rather than fighting it twice.

**Exit:** `androidApp` builds and runs identically to today, sourced from `shared`.

---

## Phase 2 — DI: Hilt → Koin
**Goal:** remove the single largest Android-only coupling (47 annotation sites).

- [ ] Add Koin (`koin-core`, `koin-android`, `koin-compose-viewmodel`).
- [ ] Rewrite `di/AppModule` + `data/rest/NetworkModule` as Koin `module { }` DSL.
      `@Provides`/`@Singleton` → `single { }`, `@Binds` → `single<Iface> { Impl(get()) }`.
- [ ] `@Inject constructor(...)` classes: keep the constructors; register each in a
      Koin module (or adopt Koin Annotations + KSP to auto-generate).
- [ ] `@HiltViewModel` (5 ViewModels) → `viewModel { }` / `koinViewModel()` at call sites.
- [ ] Replace `@AndroidEntryPoint` + `hiltViewModel()` in composables with `koinViewModel()`.
- [ ] Replace the Hilt `Application` with `startKoin { }` in `androidApp`.
- [ ] Replace `@ApplicationContext` injection in `AndroidStringProvider`,
      `FileHelper`, `DatabaseBackupManager`, `SettingsRepository` with a Koin-provided
      platform `Context` (Android) / no-op elsewhere.
- [ ] Add a Koin **`verify()`** test to catch missing bindings at build time.

**Risk:** medium-high but mechanical; do it while everything is still Android-only so
you can diff behavior 1:1.

**Exit:** app runs on Koin; no `dagger`/`hilt`/kapt references remain.

---

## Phase 3 — Database: Room → SQLDelight
**Goal:** a DB layer that runs on Android, desktop-JVM (macOS incl.), iOS, and macOS-native.

Current DB surface (from `data/db/AppDatabase.kt` + color-subjects work):
`ContentLinkDB (tweets_table)`, `DraftPostDB (prepared_tweets_table)`, `KeywordDB`,
`ContentItemDB`, `SubjectDB (subjects_table)`, and join tables
`post_subject_cross_ref`, `link_subject_cross_ref` (v5).

- [ ] Add SQLDelight plugin + `sqldelight { databases { create("AppDatabase") { … } } }`.
- [ ] Write `.sq` files in `commonMain/sqldelight/` — **one per table**, preserving the
      legacy table names verbatim so existing Android databases open unchanged:
      ```sql
      -- ContentLink.sq
      CREATE TABLE tweets_table (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        link TEXT NOT NULL,
        added INTEGER NOT NULL,
        position INTEGER NOT NULL DEFAULT 0,
        description TEXT NOT NULL DEFAULT '',
        favourite INTEGER NOT NULL DEFAULT 0,
        hideItem INTEGER NOT NULL DEFAULT 0
      );
      selectById: SELECT * FROM tweets_table WHERE id = ?;
      insert: INSERT OR REPLACE INTO tweets_table(...) VALUES (...);
      ```
- [ ] Port every DAO method (`@Query`/`@Insert`/`@Update`/`@Delete`) to a named
      SQLDelight statement. Flow-returning queries → `.asFlow().mapToList(dispatcher)`.
- [ ] Recreate the cross-ref join tables with `FOREIGN KEY … ON DELETE CASCADE`
      (SQLDelight honors `PRAGMA foreign_keys` — set it in the driver).
- [ ] **Migrations:** translate the archived Room schema versions (`1.json`→`5.json`)
      into SQLDelight `.sqm` migration files (`1.sqm`, `2.sqm`, …). Set the schema
      version so an existing v5 Android DB is recognized as already-migrated.
- [ ] Provide the driver via `expect fun createDriver(): SqlDriver`:
   - `androidMain`: `AndroidSqliteDriver(schema, context, "app.db")`
   - `jvmMain` (desktop): `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY…)` → file path under `AppDirs`
   - `iosMain` / `macosMain`: `NativeSqliteDriver(schema, "app.db")`
- [ ] Wrap the generated queries behind your **existing repository interfaces** so
      the domain/UI layers don't change (`SubjectRepository`, `ContentItem…` etc.).
- [ ] Rework `DatabaseBackupManager` — it currently reaches into
      `SupportSQLiteDatabase`/`SQLiteDatabase`. Replace with a portable export
      (copy the DB file via kotlinx-io / driver checkpoint) behind an `expect`.
- [ ] Migrate DAO unit tests to `commonTest` using the in-memory JDBC driver.

**Risk:** highest-value, highest-care phase. Keep Room and SQLDelight side-by-side on a
branch until the SQLDelight path passes the full test suite **and** opens a real v5
Android DB with data intact.

**Exit:** all DB access flows through SQLDelight; Room, `androidx.room`, and the
`app/schemas` kapt wiring are removed.

---

## Phase 4 — Networking: one portable HTTP stack
**Goal:** eliminate JVM-only engines from shared code.

- [ ] `NetworkModule` + `JobQueueRepository`: replace `HttpClient(OkHttp)` with
      `HttpClient(engine) { … }` where `engine` comes from
      `expect fun httpEngine(): HttpClientEngineFactory<*>`:
      Android → `OkHttp`, desktop-JVM → `OkHttp`/`Java`, iOS/macOS → `Darwin`.
- [ ] **Rewrite `tools/UrlResolver.kt`** (raw `okhttp3.OkHttpClient`, incl. the
      no-redirect client) on the Ktor client — this is the one place bypassing Ktor.
      Redirect control → `HttpClient { followRedirects = false }`.
- [ ] Bump Ktor to a current 2.x/3.x line consistent with the Kotlin version chosen in Phase 1.
- [ ] Keep kotlinx.serialization models as-is (already multiplatform).

**Exit:** no `okhttp3.*` import outside `androidMain`/`jvmMain` actuals.

---

## Phase 5 — Platform `expect`/`actual` seams
**Goal:** give every Android platform service a common interface with per-platform bindings.
Reuse the pattern you already have (`StringProvider` is the template).

| Concern | Files today | Common seam | Android / Desktop / Apple actual |
|---|---|---|---|
| **Strings/resources** | `StringProvider`, 12 non-UI `R.string` users | `Res.string.*` (Compose MP resources) | Move strings to `commonMain/composeResources`; replace `resId: Int` with typed resources |
| **Toasts/snackbars** | `Toast` ×8 | `Notifier.show(msg)` | Toast / desktop snackbar / iOS-macOS overlay |
| **Clipboard** | `ClipboardManager`, `ClipData` | `Clipboard.copy(text)` | AndroidClipboard / AWT / UIPasteboard / NSPasteboard |
| **Share intents** | `FacebookIntent`, `TwitterIntent`, `TranslateIntent`, `Action.kt` | `Sharer.share(payload)` | `Intent.ACTION_SEND` / desktop URL-open / `UIActivityViewController` / `NSSharingService` |
| **File I/O & pickers** | `FileHelper`, `PrefsFile`, `MediaStore`, `ContentResolver`, `java.io.File` | `FileStore` + kotlinx-io/Okio | SAF / AWT FileDialog / `UIDocumentPicker` |
| **Date/time & format** | `java.time`, `SimpleDateFormat`, `NumberFormat`, `Locale` | kotlinx-datetime + small formatter seam | JVM formatters / `NSDateFormatter` |
| **UUID / URL-encode** | `java.util.UUID`, `java.net.URLEncoder` | Kotlin `Uuid` (stdlib 2.x) + Ktor `encodeURLPath` | — |
| **Logging** | `android.util.Log` ×10 | tiny `Log` expect (or Napier/Kermit) | Logcat / stdout / NSLog |
| **WebView login** | `XLoginActivity`, `WebView`, `CookieManager` | `LoginFlow` interface | Android WebView / desktop JCEF-or-browser / WKWebView. **Lowest priority** — likely stays Android-first initially. |

- [ ] Introduce seams **incrementally**: add the interface, bind the Android actual,
      keep the app green, repeat. Don't big-bang this table.

**Exit:** no `android.*` import in any file destined for `commonMain`.

---

## Phase 6 — Relocate code into `commonMain`
**Goal:** with edges abstracted, physically move the portable core into `commonMain`.

Move in dependency order (leaves first):
1. [ ] Domain models (`data/domain`), pure utils, `ColorPicker` (already KMP-safe).
2. [ ] Repository interfaces + SQLDelight-backed implementations.
3. [ ] Ktor REST repositories (`data/rest/**`) once they use the common engine + `Res.string`.
4. [ ] ViewModels (`androidx.lifecycle.ViewModel` is multiplatform — verify version).
5. [ ] Compose UI (`ui/**`) once it uses Compose MP resources, `Notifier`, `Sharer`, `koinViewModel()`.

- [ ] After each move, the Android app must still build & pass tests (`commonTest`).

**Exit:** `androidApp` contains only the launcher, manifest, and Android-specific actuals.

---

## Phase 7 — Add Desktop, iOS, and macOS targets
**Goal:** light up the new platforms now that the core is common.

- [ ] Enable `iosX64/iosArm64/iosSimulatorArm64` and `macosArm64/macosX64` in `shared`.
- [ ] **Desktop:** `desktopApp` with Compose `application { Window { App() } }`;
      bind JDBC driver, Java/OkHttp engine, AWT file/clipboard actuals.
- [ ] **iOS/macOS:** Xcode projects calling `MainViewController()`/`MainKt`; bind
      Native driver + Darwin engine + AppKit/UIKit actuals.
- [ ] Provide per-platform DB file paths (AppDirs on desktop, `NSDocumentDirectory` on Apple).
- [ ] Stub or Android-restrict features that are genuinely Android-only until designed
      (WebView login, `MediaStore` gallery import) — expose a "not available on this
      platform" capability flag rather than blocking the build.

**Exit:** all four targets launch and exercise list/CRUD + networking end-to-end.

---

## Phase 8 — Testing, CI, and cleanup
- [ ] All existing unit tests run under `commonTest` (MockK → consider `kotlin-test` +
      fakes where MockK is JVM-limited; Turbine and coroutines-test are multiplatform).
- [ ] Add SQLDelight migration tests (open v1…v5 fixtures, assert schema).
- [ ] Koin `verify()` in CI.
- [ ] CI matrix: assemble Android, desktop, iOS (macOS runner), macOS-native.
- [ ] Delete dead Android-only code paths and stale `app/schemas` Room artifacts.
- [ ] Update `doc/DESCRIPTION.md` / `README` to describe the multiplatform structure.

---

## 3. Effort & risk summary

| Phase | Scope | Risk | Notes |
|---|---|---|---|
| 0 Baseline | small | low | Verify v4→v5 first |
| 1 Build skeleton | medium | **high** | Coupled to AGP9/Gradle9/Kotlin2.4 bump |
| 2 Hilt→Koin | medium | med | Mechanical, 47 sites |
| 3 Room→SQLDelight | large | **high** | Data-integrity critical; keep dual-path |
| 4 Networking | medium | med | `UrlResolver` rewrite is the crux |
| 5 expect/actual seams | large | med | Incremental; broad but shallow |
| 6 Move to commonMain | medium | low | Purely relocation once seams exist |
| 7 New targets | medium | med | First real desktop/Apple bring-up |
| 8 Test/CI | medium | low | Ongoing |

**Nothing in the current architecture is a dead-end for KMP.** The clean
`data/domain` + repository-interface layering and the existing `StringProvider` seam
mean the core is already ~70% common-ready; the work concentrates in DI, the DB swap,
the build restructure, and the networking engine.

## 4. Suggested branch strategy
- One long-lived `feature/kmp` integration branch.
- Land Phases 0–2 there while the app stays Android-only and diffable 1:1.
- Do Phase 3 (DB) on a sub-branch with Room and SQLDelight side-by-side until parity.
- Keep `androidApp` shippable at the end of every phase.
