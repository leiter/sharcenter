package cut.the.crap.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import cut.the.crap.data.db.SqlDelightActionReminderDao
import cut.the.crap.data.db.createDatabase
import cut.the.crap.data.db.sql.ShareDatabase
import cut.the.crap.data.domain.ActionReminder
import cut.the.crap.data.domain.ActionReminderRepository
import cut.the.crap.data.domain.ActionReminderRepositoryImpl
import cut.the.crap.data.domain.ReminderPost
import cut.the.crap.data.domain.ReminderSchedule
import cut.the.crap.data.domain.TimeWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/**
 * The Android half of action reminders on a real system: WorkManager runs [ReminderWorker], and
 * the system notification appears with its buttons (doc/ACTION_REMINDER_SPEC.md §7.2). The
 * decision logic itself is covered on the desktop target.
 *
 * Requires a device/emulator; runs on the non-minified `instrumentation` build type:
 *   ./gradlew connectedInstrumentationAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ReminderWorkerTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    private lateinit var driver: SqlDriver
    private lateinit var repository: ActionReminderRepository

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        // The worker re-syncs its own schedule; keep that out of the real WorkManager.
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        driver = AndroidSqliteDriver(ShareDatabase.Schema, context, name = null) // in-memory
        repository = ActionReminderRepositoryImpl(
            SqlDelightActionReminderDao(createDatabase(driver).actionReminderQueries, Dispatchers.IO),
        )
        // The worker resolves these from the app's Koin graph (started by MyApplication); point
        // them at the in-memory database so the test never touches the app's real reminders.
        // The notifier stays the real AndroidReminderNotifier.
        loadKoinModules(
            module {
                single { ReminderDispatcher(repository, get(), { emptySet() }) }
                single { ReminderScheduleSync(repository, get()) }
            },
        )
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
        driver.close()
    }

    @Test
    fun aDueReminderIsShownWithBothButtonsAndAdvancesItsRotation() {
        runBlocking {
            // Due all day, every day — except the window's last minute, 23:59.
            val id = repository.insert(
                ActionReminder(
                    campaignId = "abu-safiya",
                    countryCode = "DE",
                    language = "de",
                    postToX = true,
                    postToFacebook = true,
                    schedule = ReminderSchedule.Recurring(
                        DayOfWeek.entries.toSet(),
                        TimeWindow(LocalTime(0, 0), LocalTime(23, 59)),
                    ),
                    posts = listOf(
                        ReminderPost("DE-1", "Erster Post https://cutthecrap.link/de/abu-safiya"),
                        ReminderPost("DE-2", "Zweiter Post"),
                    ),
                    campaignVersion = 1,
                ),
            )

            val result = TestListenableWorkerBuilder<ReminderWorker>(context).build().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            val shown = notificationManager.activeNotifications.single {
                it.tag == AndroidReminderNotifier.NOTIFICATION_TAG && it.id == id
            }.notification
            assertEquals(
                listOf("Post on X", "Post on Facebook"),
                shown.actions.map { it.title.toString() },
            )
            assertEquals(
                "Erster Post https://cutthecrap.link/de/abu-safiya",
                shown.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            )
            assertNotNull(shown.contentIntent)

            val stored = repository.getById(id)!!
            assertEquals(1, stored.nextPostIndex)
            assertNotNull(stored.lastFiredAt)
        }
    }

    @Test
    fun theSchedulerKeepsExactlyOneUniqueJobAndCancelsIt() {
        val scheduler = AndroidReminderScheduler(context)
        val workManager = WorkManager.getInstance(context)

        // KEEP: scheduling again must not add a second job or reset the first.
        scheduler.ensureScheduled()
        scheduler.ensureScheduled()
        val scheduled = workManager.getWorkInfosForUniqueWork(AndroidReminderScheduler.WORK_NAME).get()
        assertEquals(1, scheduled.size)
        // Not a specific state: the test WorkManager executor may already have moved a periodic
        // job from ENQUEUED to RUNNING (or beyond) by the time this reads it. The property under
        // test is uniqueness — that KEEP didn't add a second job — not which state it is in.
        assertTrue(scheduled.single().state != WorkInfo.State.CANCELLED)

        scheduler.cancel()
        val cancelled = workManager.getWorkInfosForUniqueWork(AndroidReminderScheduler.WORK_NAME).get()
        assertEquals(WorkInfo.State.CANCELLED, cancelled.single().state)
    }
}
