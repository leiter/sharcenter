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

## Phase 1 — Foundation & de-risked sequencing

> **RESEQUENCED (2026-07-12, approved).** The original plan created the `shared` KMP
> module *and* bumped the toolchain while Hilt/Room/kapt were still in place. But
> **Hilt has no KMP support** (its Gradle plugin only applies to pure Android
> modules), so splitting first would force nearly all code to stay in the Android
> module anyway — churn with little gain, stacked on a risky AGP 9 bump. Instead we
> remove the Android-only blockers *inside the current single module* first, then
> split against a clean, kapt-free codebase. Revised order:
>
> 1. **Version catalog** (this phase) — foundational, low-risk.
> 2. **Hilt → Koin** (was Phase 2) — in the single module, verifiable on Android.
> 3. **Room → SQLDelight** (was Phase 3) — in the single module, verifiable on Android.
> 4. **Split `shared` / `androidApp`** (the original Phase 1 module work) — now clean.
> 5. iOS / Desktop / macOS targets (was Phase 7).
> 6. **AGP 9 / Gradle 9 / Kotlin 2.4 bump — DECOUPLED**, done when convenient, not
>    entangled with the restructure.
>
> The current toolchain (AGP 8.10.1 / Kotlin 2.1.21 / Gradle 8.14.3) already supports
> Compose Multiplatform + SQLDelight + Koin, so the bump is not a prerequisite.

**Phase 1 scope (this step): version catalog.**

- [ ] Introduce a **version catalog** (`gradle/libs.versions.toml`) capturing all
      current plugin + library versions; migrate root and `app` build scripts to it.
- [ ] Keep the current toolchain and single-module structure — no behavior change.

**Exit:** green build + 208 unit tests + on-device migration test still pass, with all
versions centralised in `libs.versions.toml`.

(The former Phase 1 module-split tasks — `kotlin("multiplatform")`, `shared`/`androidApp`,
`jvmTarget → 17`, dropping kapt — move to **step 4** above, after Koin + SQLDelight land.)

---

## Phase 2 — DI: Hilt → Koin  ✅ DONE (2026-07-12)
**Goal:** remove the single largest Android-only coupling (Koin 4.0.0).

- [x] Added Koin (`koin-android`, `koin-androidx-compose`; `koin-test` for verify).
- [x] Rewrote the 4 Hilt modules as Koin `module { }` in place: `AppModule`→`databaseModule`,
      `NetworkModule`→`networkModule`, `RepositoryModule`→`repositoryModule`,
      `ShareModule`→`shareModule`; `@Provides @Singleton`→`single`, `@Binds`→
      `factoryOf(::Impl) bind Iface::class`. Scoping preserved (`@Singleton`→`single`,
      unscoped→`factory`).
- [x] Stripped `@Inject`/`@Singleton`/`@ApplicationContext`/`javax.inject` from 31 files;
      constructors kept, resolved by `singleOf`/`factoryOf`/`viewModelOf`. The
      previously-implicit `@Inject` singletons (`SettingsRepository`, `ColorHistoryRepository`,
      `YouTubeMetadataBackfiller`) are now explicit Koin definitions.
- [x] 6 `@HiltViewModel` → `viewModelOf(...)` in `di/ViewModelModule.kt`; the 2
      `hiltViewModel()` composable sites → `koinViewModel()`.
- [x] `@AndroidEntryPoint` removed from 3 activities; `@Inject lateinit var`→`by inject()`,
      `by viewModels()`→`by viewModel()`.
- [x] `@HiltAndroidApp` `MyApplication` → `startKoin { androidContext(...); modules(...) }`.
- [x] `@ApplicationContext Context` → `androidContext()`; `dagger.Lazy<AppDatabase>` →
      `kotlin.Lazy<AppDatabase>` provided as `single { lazy { get() } }` (backup manager).
- [x] Added `KoinGraphTest` — static `verify()` of the whole graph (build-time
      missing-binding check; Ktor `HttpClientEngine`/`HttpClientConfig` declared as
      `extraTypes` since the factory lambda supplies them).

**Verification:** compiles clean; **209 unit tests / 0 failures / 6 skipped** (incl. Koin
verify); installed the **minified** debug build on-device and confirmed Koin starts under
R8 and the Posts screen loads real data (full VM→repo→Room graph resolves at runtime).

**Exit:** no `dagger`/`hilt`/`javax.inject` code remains (only doc comments); app runs on Koin. ✅

**Exit:** app runs on Koin; no `dagger`/`hilt`/kapt references remain.

---

## Phase 3 — Database: Room → SQLDelight  ✅ DONE (2026-07-12)
**Goal:** a DB layer that runs on Android, desktop-JVM (macOS incl.), iOS and macOS-native.
**SQLDelight 2.0.2. kapt is now gone entirely (Room was its last user).**

- [x] SQLDelight plugin + `databases { create("ShareDatabase") { packageName = "cut.the.crap.data.db.sql" } }`.
- [x] `.sq` files under `src/main/sqldelight/cut/the/crap/data/db/sql/` reproduce the v5
      schema, **preserving the legacy physical table names verbatim** (`tweets_table`,
      `prepared_tweets_table` was dead code and dropped, `handle_tag_table`,
      `content_items_table`, `subjects_table`, `post/link_subject_cross_ref`) and the
      DB file name `app_database` — so existing installs open unchanged.
      Note: `.sq` files need `import kotlin.Boolean; import kotlin.Int;` for `AS Int` /
      `AS Boolean` to resolve; `Int` columns need `IntColumnAdapter` (Boolean is native).
- [x] Every DAO method ported to a named SQLDelight statement; Flow queries use
      `.asFlow().mapToList(dispatcher)`. Room's `@Insert(REPLACE)` + `autoGenerate`
      semantics reproduced with paired `insert` / `insertWithId` statements (id == 0 ⇒
      omit the PK so SQLite assigns it).
- [x] Cross-ref tables recreated with `ON DELETE CASCADE`; the driver sets
      `PRAGMA foreign_keys=ON` (Room's default) so cascade still fires.
- [x] **Migrations:** `1..4.sqm` mirror the former Room `MIGRATION_1_2 … 4_5` exactly.
      SQLDelight's derived schema version is **5 — identical to the `user_version` Room
      wrote**, so an up-to-date install opens with no spurious upgrade.
- [x] Driver via `createDriver(context)` (`AndroidSqliteDriver`) + `createDatabase(driver)`
      supplying the Int adapters. Becomes `expect fun` at the module-split step, with the
      JDBC driver on desktop and the native driver on iOS/macOS.
- [x] **Kept the DAO contracts and `*DB` row models** (now plain Kotlin in `Entities.kt`,
      no Room annotations) and implemented them in `SqlDelightDaos.kt`. Consequence: the
      four repositories, the domain mappers and the **entire UI layer are unchanged** —
      Koin simply rebinds the DAO interfaces to the new impls.
- [x] `DatabaseBackupManager` reworked off Room: holds `Lazy<SqlDriver>` instead of
      `Lazy<AppDatabase>`; schema-version constant now sourced from `DatabaseFactory`.
- [x] Removed Room `AppDatabase`, the Room `Migration` objects, the kapt plugin, all
      `room-*` deps and the schema-assets wiring.
- [x] Build: the custom `instrumentation` build type needs `matchingFallbacks += "debug"`
      (SQLDelight's AAR only publishes debug/release variants).

**Verification:**
- **212 unit tests / 0 failures**, incl. 3 new SQLDelight tests run on the **JVM JDBC
  driver — the same driver desktop/macOS will use**: schema version is 5; a Room-created
  v4 DB migrates to v5 with data intact and CASCADE enforced; a fresh install round-trips
  rows through the Int/Boolean adapters. Koin graph `verify()` still passes.
- **On-device (Pixel 7a):** the instrumented `MigrationTest` passes, exercising the
  *Android* driver path (`AndroidSqliteDriver` auto-migrating a v4 DB on open).
- **Minified debug build on-device against the real production database:** Posts screen
  renders the same rows as before the swap, and the Links screen loads **all 1481 items**
  from `tweets_table` with the `handle_tag_table` keyword join, sorting and filtering —
  no `SQLiteException`, no missing tables, no Koin resolution errors.

**Exit:** all DB access flows through SQLDelight; Room and kapt fully removed. ✅

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
