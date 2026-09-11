# Action Reminder Spec — Scheduled Campaign Posting Reminders

**Status:** Specification only. No code yet.
**Depends on:** `CAMPAIGN_SCHEMA_SPEC.md` — the campaign payload and `CampaignPost` model.
**Scope:** `:shared` (commonMain + androidMain) and `:app` (DI, manifest, one activity). Android
first; desktop and iOS declare the feature unsupported (§5.3).
**Last updated:** 2026-09-11

---

## 0. Why this exists

Campaign posts only get published when someone remembers to open the composer. The action
reminder manager lets the user say "nudge me on weekdays between 18:00 and 20:00" (or "once, on
Saturday morning"), and at that time a notification arrives carrying a **prepared Abu-Safiya post**:
one tap opens X or Facebook with the text already in place.

---

## 1. Design decisions

| # | Decision | Rationale |
|---|---|---|
| R1 | **Scheduling and notifications are interfaces bound by Koin, not `expect`/`actual`.** The permission request *is* `expect`/`actual`. | Same reasoning as `Notifier.kt` / `UrlOpener.kt`: the Android side needs a `Context`, which `expect` objects cannot carry. The permission request is a composable that needs an activity-result launcher — the exact shape `rememberFilePicker` already uses `expect`/`actual` for. |
| R2 | **Named "reminder", not "action".** | `Action` is already the app-wide sealed UI-event hierarchy (`ui/components/api/Action.kt`). A second meaning would make every grep ambiguous. |
| R3 | **Both recurring and one-off schedules.** | Recurring (weekdays + time window) is the habit; one-off (date + time window) is the "there is a debate on Thursday" case. |
| R4 | **Each reminder rotates through its posts.** | X rejects a status whose text duplicates an earlier one, and the same post every day reads as spam anyway. Hidden posts (`CampaignHiddenPostsRepository`) are skipped. |
| R5 | **Posts are snapshotted into the reminder row.** | `CampaignRepositoryImpl` caches only in memory, so a background worker would find nothing and would need the network at fire time. The snapshot makes the worker offline and deterministic; it is refreshed opportunistically (§3.2). |
| R6 | **Notification buttons launch their target directly.** | Android 12+ forbids notification "trampolines" (a receiver/service that then starts an activity). The X button's `PendingIntent` *is* the X intent; the Facebook button targets a tiny activity of ours because it must copy the text first (§5.2). |
| R7 | **WorkManager, inexact, one periodic dispatcher.** | "Sometime between 18:00 and 20:00" does not need exact alarms, which would cost the `SCHEDULE_EXACT_ALARM` permission and Play review friction. One periodic worker that asks "what is due?" survives reboots and app updates on its own and keeps all timing logic in testable common code. |
| R8 | **Rotation advances when the notification is shown, not when the user acts.** | We cannot observe whether a post was actually published. Advancing on show keeps the next reminder fresh without pretending to track completion. |
| R9 | **Abu-Safiya only in v1; the schema is campaign-agnostic.** | `campaignId` is stored per row, so opening this to other campaigns later is a UI change, not a migration. |

---

## 2. User flow

### 2.1 Reminder screen (`action_reminders`)

Entry: a bell `IconButton` in `CampaignDetailScreen`'s top bar, next to the composer action — shown
only for `abu-safiya` and only when `ReminderScheduler.isSupported`. Tapping a reminder
notification's body also lands here.

The list shows one row per reminder:

- country flag + language + platform icons (X / Facebook),
- schedule summary ("Mon–Fri, 18:00–20:00" / "Sat 13 Sep, 09:00–11:00"),
- next window start, and which post comes next (variant label, e.g. "DE-2"),
- an enabled switch; overflow menu with **Send now** and **Delete**.

**Send now** runs the same path as the worker for that one reminder (shows the notification,
advances rotation). It doubles as the preview and as the manual verification hook (§7.3).
**Delete** removes the row and shows an undo snackbar; the notification, if showing, is cancelled.

A banner appears when notifications are not allowed (§5.4), with a button to fix it.

### 2.2 Create sheet

| Field | Source / rule |
|---|---|
| Country | `campaign.countriesWithPosts` |
| Language | that country's `postsByLanguage.keys`, defaulting to `defaultLanguage` |
| Platforms | X and/or Facebook — at least one |
| Schedule | **Recurring:** weekday multi-select (≥ 1) · **Once:** date (today or later) |
| Window | start and end time (M3 `TimePicker`); end > start, same day, ≥ 60 min |

The 60-minute floor exists because the dispatcher runs every 30 minutes (§4.3); a shorter window
could fall between two runs. Windows crossing midnight are rejected in v1.

Saving the **first** reminder triggers the notification permission request (§5.4).

### 2.3 The notification

- **Title:** "Time to post: Abu Safiya" (campaign `displayTitle`).
- **Body:** the post text, `BigTextStyle` so the whole post is readable before tapping.
- **Actions:** "Post on X" and/or "Post on Facebook", per the reminder's platforms.
- **Tap body:** opens the reminder screen.
- Channel `campaign_reminders`, default importance, auto-cancel. Notification id = reminder id, so a
  reminder never stacks multiple notifications.

---

## 3. Data model

### 3.1 Table

New file `ActionReminder.sq`, migration `6.sqm` (schema 6 → 7):

```sql
CREATE TABLE IF NOT EXISTS action_reminders_table (
    id INTEGER AS Int PRIMARY KEY AUTOINCREMENT NOT NULL,
    campaignId TEXT NOT NULL,
    countryCode TEXT NOT NULL,
    language TEXT NOT NULL,
    postToX INTEGER AS Boolean NOT NULL,
    postToFacebook INTEGER AS Boolean NOT NULL,
    scheduleType TEXT NOT NULL,                  -- 'RECURRING' | 'ONCE'
    daysOfWeek INTEGER AS Int NOT NULL DEFAULT 0, -- bitmask, Monday = 1 … Sunday = 64 (RECURRING)
    onceDate TEXT,                               -- ISO yyyy-MM-dd (ONCE)
    windowStartMinute INTEGER AS Int NOT NULL,   -- minutes after local midnight
    windowEndMinute INTEGER AS Int NOT NULL,
    postsJson TEXT NOT NULL,                     -- snapshot: [{"id":"DE-1","text":"…"}, …]
    campaignVersion INTEGER AS Int NOT NULL,
    nextPostIndex INTEGER AS Int NOT NULL DEFAULT 0,
    enabled INTEGER AS Boolean NOT NULL DEFAULT 1,
    lastFiredAt INTEGER,                         -- epoch millis
    createdAt INTEGER NOT NULL,
    modifiedAt INTEGER NOT NULL
);
```

Times are **wall-clock** and evaluated in `TimeZone.currentSystemDefault()` at check time, so DST
changes and travel follow the user rather than a fixed UTC instant. The new `Int` columns need
`IntColumnAdapter` entries in `createDatabase` (`DatabaseAdapters.kt`).

The table lives in `app_database`, so `DatabaseBackupManager`'s daily copy includes reminders —
intended: a restored install keeps its schedule.

### 3.2 Snapshot refresh

Whenever the reminder screen has a freshly loaded campaign whose `version` is newer than a row's
`campaignVersion`, that row's `postsJson` is rebuilt for its country + language and
`nextPostIndex` is clamped into range. If the country/language no longer has posts, the row is
disabled and flagged in the list rather than deleted.

### 3.3 Prerequisite fix — `CURRENT_SCHEMA_VERSION`

`DatabaseAdapters.kt` hardcodes `CURRENT_SCHEMA_VERSION = 5` ("1..4.sqm -> version 5"), but
`5.sqm` has since landed and `ShareDatabase.Schema.version` is **6** (asserted in
`SqlDelightMigrationTest`). `DatabaseBackupManager.validateBackup` compares a backup's
`user_version` against that constant — so **every backup made by the current build (v6) is
already rejected as "from a newer app version"**. This must be fixed before (and independently
of) this feature: derive the constant from `ShareDatabase.Schema.version` so it can never drift
again.

---

## 4. Scheduling logic (commonMain)

### 4.1 Types

```kotlin
sealed interface ReminderSchedule {
    val window: TimeWindow                 // start/end as LocalTime
    data class Recurring(val days: Set<DayOfWeek>, override val window: TimeWindow) : ReminderSchedule
    data class Once(val date: LocalDate, override val window: TimeWindow) : ReminderSchedule
}
```

`kotlinx-datetime` 0.7.1 is already a dependency; note that 0.7 moved `Instant`/`Clock` to
`kotlin.time` (opt-in `ExperimentalTime` on Kotlin 2.2).

### 4.2 Pure functions

- `isDue(reminder, now, tz): Boolean` — `now` lies inside an occurrence of the window **and**
  `lastFiredAt` is not inside that same occurrence (never twice per window).
- `nextWindowStart(reminder, now, tz): Instant?` — for the list row; `null` for a spent `Once`.
- **Missed one-off:** if a `Once` window passed entirely without firing (device off, Doze), it
  fires late on the next run, up to 12 h after the window end, then never. A missed recurring
  window is simply skipped — the next one is never far away.

### 4.3 `ReminderDispatcher`

Common class; the Android worker is a thin shell around it.

```
suspend fun dispatch(now: Instant)
  for each enabled reminder where isDue(…):
    posts = snapshot minus hidden (campaignPostHideKey(campaignId, countryCode, post.id))
    if posts empty → skip (log), leave rotation untouched
    post = posts[nextPostIndex mod posts.size]
    reminderNotifier.show(ReminderNotification(…post…))
    nextPostIndex += 1; lastFiredAt = now; if Once → enabled = false
```

**Send now** calls the same per-reminder step with the due check bypassed.

The dispatcher is scheduled as unique periodic work (`KEEP`) every **30 minutes**, enqueued from
`MyApplication.onCreate` and after any create/enable; cancelled when no enabled reminder remains.

---

## 5. Platform seams

### 5.1 commonMain

```kotlin
interface ReminderScheduler {
    val isSupported: Boolean
    fun ensureScheduled()
    fun cancel()
}

interface ReminderNotifier {
    /** Permission granted and channel not blocked. */
    val canNotify: Boolean
    fun show(notification: ReminderNotification)
    fun cancel(reminderId: Int)
    fun openSettings()
}

data class ReminderNotification(
    val reminderId: Int,
    val title: String,
    val text: String,
    val xUrl: String?,          // TwitterIntent.PostTweet(text).url, when postToX
    val facebookText: String?,  // when postToFacebook
)

@Composable
expect fun rememberNotificationPermissionRequester(onResult: (granted: Boolean) -> Unit): () -> Unit
```

Intent URLs are built in common code from the existing `TwitterIntent` / `FacebookIntent`, so the
platform side only wraps them.

### 5.2 Android

| Piece | Where | Notes |
|---|---|---|
| `AndroidReminderScheduler` | shared/androidMain | `WorkManager.enqueueUniquePeriodicWork` |
| `ReminderWorker` | shared/androidMain | `CoroutineWorker` + `KoinComponent`; calls `dispatcher.dispatch(Clock.System.now())` |
| `AndroidReminderNotifier` | shared/androidMain | `NotificationCompat`; creates the channel; `canNotify` = `areNotificationsEnabled()` |
| `ReminderFacebookActivity` | :app, next to `ShareActivity` | Transparent, `exported=false`: copies text via `Clipboard`, opens `FacebookIntent.SharePost` via `UrlOpener`, `finish()` |
| Permission requester | shared/androidMain | `ActivityResultContracts.RequestPermission(POST_NOTIFICATIONS)` on API 33+; granted below |
| DI | `PlatformModule.kt` | bind scheduler + notifier; dispatcher + repository in the common modules |

- **X button:** `PendingIntent.getActivity` wrapping the same `ACTION_VIEW` intent
  `AndroidUrlOpener` builds. Extract that intent construction (package pin when X is installed)
  into a shared helper so both use one code path.
- **Body tap:** `PendingIntent` to `MainActivity` with an extra naming the route. `MainActivity`
  has no intent handling today; add `onCreate`/`onNewIntent` handling that publishes to a small
  Koin-held `NavigationRequests` flow which `App()` collects and forwards to its `NavController` —
  this keeps `App()`'s signature unchanged for desktop and iOS.
- `PendingIntent` flags: `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`; request codes unique per
  reminder × action.
- **Manifest:** `POST_NOTIFICATIONS`; the Facebook activity. No `RECEIVE_BOOT_COMPLETED` — WorkManager
  declares its own.
- **Small icon:** none exists (`app/src/main/res` has only mipmaps). Add a monochrome vector
  `ic_stat_reminder`.
- **Dependency:** `androidx.work:work-runtime-ktx` in `shared` androidMain, `work-testing` for
  androidTest. 2.9.0 is already in the local Gradle cache; pick the newest release that still builds
  on AGP 8.10.1 — most bumps in this project are gated on the AGP 9 migration.

### 5.3 Desktop and iOS

`UnsupportedReminderScheduler` (`isSupported = false`) and a no-op notifier; the bell is hidden, so
nothing half-works. iOS later: `UNUserNotificationCenter` with calendar triggers — iOS has no
general periodic background execution, so there the schedule would be materialised into pending
local notifications instead of dispatched by a worker. That changes R7 for iOS only; the data
model and screen carry over unchanged.

### 5.4 Permission

`targetSdk` is 37, so on API 33+ `POST_NOTIFICATIONS` is a runtime permission. Asked when the
first reminder is saved, never at app start. If denied, the reminder is still saved and the screen
shows the §2.1 banner; its button re-requests, or — after Android stops showing the dialog — calls
`ReminderNotifier.openSettings()` (the app's notification settings page).

---

## 6. UI code

- `ActionRemindersScreen` + `ActionRemindersViewModel` in `ui/content/reminder/`, route
  `action_reminders` in `NavigationGraph.kt`.
- The ViewModel loads the campaign through `CampaignRepository.get(BUNDLED_CAMPAIGN_ID)` for the
  create sheet and for snapshot refresh (§3.2); the list itself comes from the repository flow and
  works offline.
- Strings in `composeResources/values/strings.xml` only (the shared resources are English-only).
  Apostrophes unescaped — CMP renders the backslash (see commit `300ac5e`).
- No `android.*`, `toArgb` or `String.format` in commonMain; use `LocaleFormat` for dates/times.

---

## 7. Testing

### 7.1 `desktopTest`

- Schedule math: window boundaries (start inclusive, end exclusive), DST spring-forward/fall-back,
  never twice per occurrence, missed-`Once` grace at 11 h 59 m vs 12 h 01 m.
- Dispatcher with fakes: rotation wrap-around, hidden posts skipped, all hidden → no notification
  and no advance, `Once` disables itself, **Send now** bypasses the due check.
- Repository against `JdbcSqliteDriver`; `SqlDelightMigrationTest` updated to schema 7 with a
  6 → 7 migration case.
- ViewModel: create validation (§2.2 rules), snapshot refresh on newer campaign version.

### 7.2 Instrumented (`connectedInstrumentationAndroidTest`)

- `ReminderWorker` via `WorkManagerTestInitHelper` / `TestDriver`: a due reminder produces an
  active notification with the expected actions.
- Backup restore accepts a v7 backup (guards §3.3).

### 7.3 On device

Emulator or device: create a reminder whose window contains "now", use **Send now**, tap
"Post on X" (app installed and not installed → browser), tap "Post on Facebook" (text on
clipboard, sharer opens), tap the body (lands on the reminder screen, also from a cold start).
Then let the periodic worker fire once on its own.

---

## 8. Build order

| Step | Content | Separate commit |
|---|---|---|
| 0 | §3.3 schema-version fix + a test that pins it to `Schema.version` | yes — pre-existing bug |
| 1 | Table, `6.sqm`, repository, migration test | |
| 2 | Schedule types, `isDue`/`nextWindowStart`, dispatcher, tests | |
| 3 | Android: dependency, notifier + channel, worker, scheduler, Facebook activity, manifest, DI, app-start scheduling | |
| 4 | Screen, ViewModel, create sheet, permission flow, navigation + notification deep link, entry button | |
| 5 | Instrumented tests, on-device check (§7.3) | |

---

## 9. Out of scope (v1)

- Campaigns other than Abu-Safiya (R9), contact actions (`mailto:`), Instagram.
- Exact-time alarms, snooze, windows across midnight.
- Knowing whether the user actually posted; any server-side coordination — that is
  `CAMPAIGN_SCHEMA_SPEC.md`'s assignment/claim model, and reminders could later feed it.
- iOS and desktop implementations (§5.3).
