# Dead-code cleanup TODO

Findings from a project-wide scan for copy-paste / dead code (license boilerplate,
commented-out code, unused functions). Legitimate explanatory comments (e.g. the URL
pattern notes in `SocialMediaParser.kt`, test rationale, `MessageRepository` 4xx/5xx
notes) were deliberately **excluded** — only genuine dead code is listed.

## Done
- [x] Deleted `spotless/copyright.kt` — orphaned Apache-2.0 license-header template for
      the Spotless plugin, which is fully disabled. Nothing referenced it.
- [x] Removed the commented-out `buildscript {}` and Spotless blocks from `build.gradle.kts`.
- [x] `data/storage/PrefsFile.kt` — removed two commented-out function blocks
      (`getPageTransition`/`setPageTransition`, `isEdge2EdgeEnabled`/`setEdgeToEdge`)
      and their now-orphaned private constants `KEY_EDGE_TO_EDGE`, `KEY_PAGE_TRANSITION`.
- [x] `ui/components/MyFilterChip.kt` — removed `//val token = MyAppTheme.component.filterChips`.

## Pending — commented-out code to remove
- [ ] `ui/components/MyFilterChip.kt`
  - `//color = textColor(...)` / `//style = MyAppTheme...bodyBold` inside the `Text(...)` (~line 169)
  - `//style = MyAppTheme...bodyBold` inside `textMeasurer.measure(...)` (~line 222)
  - `//    val color = ...` inside `backgroundColor()` (~line 241)
  - commented-out `textColor(state)` function at end of file (~lines 250-259)
  - trailing `//iconProvider.closeIcon` after `Icons.Filled.Close` (~line 193)
- [ ] `ui/components/MyChip.kt` — three repeated `//color = textColor(...)` / `//style = ...`
      pairs in the `Text(...)` calls (~lines 221-222, 245-246, 260-261)
- [ ] `ui/components/MySpeedDial.kt`
  - inline `//color = Color(0xFFBB86FC)` and `//modifier = Modifier.scale(...)` in `SecondaryFab(...)` call (~lines 78, 81)
  - `CircularSpeedDialFab()` — unused composable whose body is entirely commented out (see "Larger candidates" below)
- [ ] `ui/components/MySearchBar.kt` — `//containerColor = Color.White,` (~line 142)
- [ ] `ui/components/MyFilterChipRow.kt` — `//val filterChipToken = ...` (~line 36)
- [ ] `ui/components/MyEditDialog.kt` — commented-out `Checkbox(...)` block (~lines 128-131)
- [ ] `ui/components/MyTabItem.kt` — commented `colors` param (~line 43), `//style = ...caption` (~line 56), `//colors = colors,` (~line 59)
- [ ] `data/storage/FileHelper.kt` — `// Notify user` + `//showToast(...)` (dead, after `return`) (~lines 123-124, 129)
- [ ] `ui/content/posts/PostsScreen.kt` — `//action()` in empty onClick (~line 241); commented `FilledTonalIconButton` block in `navigationIcon` (~lines 248-253)
- [ ] `ui/content/share/TryMeScreen.kt` — commented `FilledTonalIconButton` block in `navigationIcon` (~lines 64-69)
- [ ] `MainActivity.kt` — trailing list of commented-out `Icons.*` references at end of file (~lines 248-255)
- [ ] `ui/content/links/LinksScreen.kt` — `//onMove(currentDraggingItemIndex, targetIndex)` (~line 358)
- [ ] `tools/StringExtension.kt` — `// private val domainRegex = Regex(...)` (~line 31)

## Larger candidates — need a decision before removing
- [ ] `ui/content/links/LinksActionHandlers.kt` — `ListAction.ToggleFavoritesForSelected`
      branch has its entire body commented out, leaving an empty `viewModelScope.launch {}`.
      This is an **unfinished feature**, not just dead code. Decide: implement it, or
      reduce to an explicit no-op stub.
- [ ] `ui/components/MySpeedDial.kt` — the whole file (`MySpeedDial`, `SecondaryFab`,
      `CircularSpeedDialFab`) appears **unused** (no references found). Consider deleting
      the entire file rather than just cleaning its comments.
- [ ] `ui/content/settings/ImportExportScreen.kt` (~line 87) — "Placeholder buttons
      (disabled for now)". Placeholder/unfinished UI; confirm whether to keep.

## Notes
- Verify the project still compiles after the cleanup: `./gradlew assembleDebug`.
