# TODO List - BasicStateCodelab

> Last updated: 2026-08-01

## Critical / High Priority

- [x] ~~**Campaign list 401s on a fresh install — release blocker**~~ — FIXED (step 2a).
  Both campaign reads now use `signed_user_optional`, so an install with no identity sees the
  `featured` campaigns and can open them; a **bad** signature is still 401. Spec:
  `CAMPAIGN_SCHEMA_SPEC.md` §4.1 and `IDENTITY_SPEC.md` §4.5. No client code changed.
  Covered by 9 new checks in `test_campaigns.py` plus a no-`register()` case in
  `CampaignIntegrationTest` that was confirmed to fail against the pre-fix routes.
  **Ships only once the server is deployed** — see below.

### Deploy the campaign/identity server
The app changes for identity and campaigns are all merged, but the server they talk to is not
deployed. Until it is, a new build's campaign screen talks to routes that do not exist yet.

- [ ] `./deploy.sh` (PyNaCl is already installed on the host as `python3-nacl`).
- [ ] `python3 scripts/import_abu_safiya.py --owner <user_id>` so the campaign exists in the store.
- [ ] Verify with curl that the anonymous path works on the live host:
      `GET /api/campaigns/mine` and `GET /api/campaigns/abu-safiya` both **200 without a signature**,
      and `GET /api/abu-safiya` still byte-identical for shipped installs.

### Campaign screens have never run on a device
`campaign_list` and `campaign_detail` are covered by headless render tests only. The spec asks for a
desktop + Android runtime check that has not happened.
- [ ] Open both screens on Android and on the desktop app at least once.

### Production Deployment
- [ ] **Update production API URL** - `app/build.gradle.kts:41`
  - Current: `http://192.168.1.100:8080`
  - Needs: Production server URL before deploying

### iOS Share Extension — verification (code landed, commit `cc4afc6`)
Needs a Mac/Xcode; the Kotlin side is compile-verified but the Swift/Xcode side is unbuilt off-macOS.
- [ ] **Register `group.cut.the.crap` App Group in the Apple Developer account** for device builds
  (the simulator is lenient without it). Must match the entitlements on both targets and
  `APP_GROUP_ID` in `shared/src/iosMain/.../share/ShareInbox.kt`.
- [ ] **Regenerate the Xcode project**: `cd iosApp && xcodegen generate`, then build both targets.
- [ ] **Simulator smoke-tests** (per `~/.claude/plans/plan-the-share-extension-parsed-bee.md`):
  - [ ] Extension shows in the share sheet (Safari → Share → ShareCenter) and dismisses.
  - [ ] Link appears on next app open (snackbar + in the links list).
  - [ ] Foreground case: share while app is open, switch back → inbox drains.
  - [ ] Queue case: share 3 URLs while app closed → all 3 saved, no duplicates.
  - [ ] Handle case: share an X/Twitter profile URL → lands in the keyword/handle pool, not links.

### Crash Risk
- [ ] **ExportLinks dialog** - `MyEditDialog.kt:144`
  - `MyEditDialogStyle.ExportLinks -> TODO()` throws exception if triggered
  - Needs: Implement export links dialog UI (or remove this dialog style if unused)

## Medium Priority

### Incomplete Implementations

- [ ] **ImportExportScreen actions (Posts data)** - `ImportExportScreen.kt:89,105`
  - Export button: `onClick = { /* TODO: Implement export */ }` (disabled)
  - Import button: `onClick = { /* TODO: Implement import */ }` (disabled)
  - Note: Links import/export IS already working via the FAB in LinksScreen
  - Needs: Wire up to the existing import/export logic in `LinksImportExport.kt` or create a parallel implementation for posts

- [ ] **Link URL edit action** - `MyEditDialog.kt:109`
  - `onValueChange = { /* TODO: Add link edit action */ }`
  - The UI exists, needs to dispatch an action to update the link field in state

- [ ] **Description edit action** - `MyEditDialog.kt:118`
  - `onValueChange = { /* TODO: Add description edit action */ }`
  - The UI exists, needs to dispatch an action to update the description field in state

### Links Screen Keyword Dialog
- [ ] **Parse keywords from description** - `LinksScreen.kt:238`
  - `selectedItems = emptySet(), // TODO: Parse from currentEditingLink.description`

- [ ] **Handle keyword toggle** - `LinksScreen.kt:240`
  - Callback not implemented

- [ ] **Save keywords to description** - `LinksScreen.kt:243`
  - onDismiss/onConfirm doesn't save selected keywords back to the link

### Drag-and-Drop Reordering
- [ ] **Drag reorder logic commented out** - `LinksScreen.kt:358`
  - `// onMove(currentDraggingItemIndex, targetIndex)` is commented out
  - Visual drag animation works, but items don't actually reorder in the list
  - Needs: Wire up move action to persist reorder to database (via `sortByListPosition`)

### Test Infrastructure
- [ ] **Fix @Ignored tests** - `LinksViewModelTest.kt`
  - Several tests marked `@Ignore("Test causes coroutine leakage due to handleListFlow using Dispatchers.Default")`
  - Affected: `toggle selection`, `select all`, `delete all`, `fire job` tests
  - Root cause: `handleListFlow` uses `Dispatchers.Default` preventing test coroutine control
  - Fix: Inject dispatcher or use `withContext` with an injectable dispatcher

### UI/UX Improvements
- [ ] **SnackBar positioning** - `LinksScreen.kt:431`
  - Comment: "TODO there is a better fix using correct insets"
  - Needs: Proper `WindowInsets` handling for bottom-positioned SnackBar

- [x] ~~**String resources for overflow menu** - `LinksTopBar.kt`~~ — DONE. Overflow items now use
  dedicated resources (`links_bulk_favorite`, `links_bulk_tag_*`, `links_bulk_export`,
  `links_bulk_delete`, `links_bulk_deselect`) instead of the `app_name` placeholder.

## Low Priority / Nice to Have

### Commented-Out Features (Evaluate for Removal or Implementation)

1. ~~**Bulk favorite toggle** - `LinksActionHandlers.kt`~~ — DONE. `ToggleFavoritesForSelected`
   is now fully implemented (group toggle with snackbar) at `LinksActionHandlers.kt:443`.

2. **Legacy preferences** - `PrefsFile.kt:30-40`
   - Old preference methods for page transitions and edge-to-edge
   - Decision needed: Clean up or keep for future use?

3. **Alternative filter chip styling** - `MyFilterChip.kt`, `MyFilterChipRow.kt`
   - Commented theming approaches
   - Decision needed: Clean up

4. **Alternative domain regex** - `StringExtension.kt`
   - Commented regex pattern for domain extraction
   - Decision needed: Clean up or use

### X/Twitter Integration
- [ ] **Extract screen name after login** - `XLoginActivity.kt:240`
  - `val screenName: String? = null // Would need additional API call`
  - Enhancement: Add GraphQL call to fetch screen name after successful login

### Architecture Improvements
- [ ] **Unify filter state management**
  - Posts: In-memory filtering via `ContentItemManager`
  - Links: Database-level + in-memory hidden filters
  - Should be consistent across screens

- [ ] **Refactor activity-level action handlers**
  - `MainActivity.handleAction()` is large
  - Could extract into handler interfaces

- [ ] **Add use case/interactor layer**
  - ViewModels directly access repositories
  - Could add intermediate layer for complex business logic

### Testing
- [ ] **Add UI tests**
  - Started: `IdentityScreenTest` (4) and `CampaignScreensTest` (6) render for real via
    `runComposeUiTest` in `desktopTest`, and assert taps reach their destination
  - Still missing: navigation tests, and the older Links/Posts screens have none

### Documentation
- [ ] **Architecture decision records (ADRs)**
- [ ] **Data model schema documentation**
- [ ] **Database migration changelog**
- [ ] **Deployment guide**

### Code Quality
- [ ] **Remove/wrap debug logging**
  - Extensive `Log.d()` usage throughout codebase
  - Should be conditional on `BuildConfig.DEBUG` or use Timber

- [ ] **Extract magic numbers**
  - 5000ms subscription timeouts
  - 300ms debounce delays
  - 120000ms timestamp increments
  - Should be constants with meaningful names

- [ ] **Secrets management**
  - X API bearer token in `UrlResolver.kt`
  - Should use BuildConfig or secrets.properties

### Job Queue Backend
- [ ] **Job persistence** - `job_queue/README_QUEUE.md`
  - Jobs lost on server restart (in-memory only)
- [ ] **Job status tracking** - No way to check if a submitted job succeeded
- [ ] **Job cancellation** - Not implemented

## Completed

- [x] Lower minSdk to 26 for Huawei device compatibility (2026-02-05)
- [x] Links import/export via FAB (file picker for import, MediaStore for export)
- [x] ViewModel unit tests - `LinksViewModelTest`, `LinksViewModelFilterTest`, `PostsViewModelTest`, `SettingsViewModelTest`, `YouTubePreviewViewModelTest`
- [x] Fake repositories for all major dependencies (used in tests)
- [x] YouTube oEmbed metadata fetch on link insert and import
- [x] Handle/tag/keyword selection dialogs in Links screen (dialog structure; save logic still TODO)

---

## Task Categories Summary

| Category | Count |
|----------|-------|
| Critical | 5 |
| Medium | 9 |
| Low/Nice to Have | 15+ |

## Files with Open TODOs

| File | Line(s) | Description |
|------|---------|-------------|
| `app/build.gradle.kts` | 41 | Production URL |
| `MyEditDialog.kt` | 109, 118, 144 | Edit actions, ExportLinks crash |
| `LinksScreen.kt` | 238, 240, 243, 358, 431 | Keywords, drag reorder, SnackBar |
| `ImportExportScreen.kt` | 89, 105 | Import/Export buttons for posts |
| `LinksTopBar.kt` | 240, 245, 250 | String resources |
| `XLoginActivity.kt` | 240 | Screen name extraction |
| `LinksViewModelTest.kt` | multiple | @Ignored tests (Dispatchers issue) |
