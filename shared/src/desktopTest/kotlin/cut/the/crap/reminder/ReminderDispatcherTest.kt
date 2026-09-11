@file:OptIn(kotlin.time.ExperimentalTime::class)

package cut.the.crap.reminder

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cut.the.crap.data.db.SqlDelightActionReminderDao
import cut.the.crap.data.db.createDatabase
import cut.the.crap.data.db.sql.ShareDatabase
import cut.the.crap.data.domain.ActionReminder
import cut.the.crap.data.domain.ActionReminderRepository
import cut.the.crap.data.domain.ActionReminderRepositoryImpl
import cut.the.crap.data.domain.ReminderPost
import cut.the.crap.data.domain.ReminderSchedule
import cut.the.crap.data.domain.TimeWindow
import cut.the.crap.data.preferences.campaignPostHideKey
import cut.the.crap.platform.ReminderNotification
import cut.the.crap.platform.ReminderNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReminderDispatcherTest {

    private class FakeNotifier : ReminderNotifier {
        var allowed = true
        val shown = mutableListOf<ReminderNotification>()
        override val canNotify: Boolean get() = allowed
        override suspend fun show(notification: ReminderNotification) {
            shown += notification
        }
        override fun cancel(reminderId: Int) = Unit
        override fun openSettings() = Unit
    }

    private val berlin = TimeZone.of("Europe/Berlin")
    private val window = TimeWindow(LocalTime(18, 0), LocalTime(20, 0))

    // Monday 2026-09-14, 18:30 in Berlin.
    private val insideMondayWindow = LocalDateTime(2026, 9, 14, 18, 30).toInstant(berlin)

    private lateinit var driver: SqlDriver
    private lateinit var repository: ActionReminderRepository
    private val notifier = FakeNotifier()
    private var hidden = emptySet<String>()
    private lateinit var dispatcher: ReminderDispatcher

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ShareDatabase.Schema.create(driver).value
        repository = ActionReminderRepositoryImpl(
            SqlDelightActionReminderDao(createDatabase(driver).actionReminderQueries, Dispatchers.IO),
        )
        dispatcher = ReminderDispatcher(repository, notifier, { hidden }, { berlin })
    }

    @After
    fun tearDown() = driver.close()

    private fun reminder(
        schedule: ReminderSchedule = ReminderSchedule.Recurring(setOf(DayOfWeek.MONDAY), window),
        nextPostIndex: Int = 0,
        postToX: Boolean = true,
        postToFacebook: Boolean = false,
    ) = ActionReminder(
        campaignId = "abu-safiya",
        countryCode = "DE",
        language = "de",
        postToX = postToX,
        postToFacebook = postToFacebook,
        schedule = schedule,
        posts = listOf(ReminderPost("DE-1", "first post"), ReminderPost("DE-2", "second post")),
        campaignVersion = 1,
        nextPostIndex = nextPostIndex,
    )

    @Test
    fun dueReminderShowsItsNextPostAndAdvancesTheRotation() = runTest {
        val id = repository.insert(reminder())

        assertEquals(1, dispatcher.dispatch(insideMondayWindow))

        val shown = notifier.shown.single()
        assertEquals(id, shown.reminderId)
        assertEquals("DE-1", shown.postLabel)
        assertEquals("first post", shown.text)
        assertNotNull(shown.xUrl)
        assertNull(shown.facebookUrl)

        val stored = repository.getById(id)!!
        assertEquals(1, stored.nextPostIndex)
        assertEquals(insideMondayWindow.toEpochMilliseconds(), stored.lastFiredAt)
        assertTrue(stored.enabled)
    }

    @Test
    fun rotationWrapsAround() = runTest {
        val id = repository.insert(reminder(nextPostIndex = 1))

        dispatcher.dispatch(insideMondayWindow)

        assertEquals("DE-2", notifier.shown.single().postLabel)
        assertEquals(0, repository.getById(id)!!.nextPostIndex)
    }

    @Test
    fun hiddenPostsAreSkipped() = runTest {
        hidden = setOf(campaignPostHideKey("abu-safiya", "DE", "DE-1"))
        repository.insert(reminder())

        dispatcher.dispatch(insideMondayWindow)

        assertEquals("DE-2", notifier.shown.single().postLabel)
    }

    @Test
    fun reminderWithEveryPostHiddenShowsNothingAndKeepsItsRotation() = runTest {
        hidden = setOf(
            campaignPostHideKey("abu-safiya", "DE", "DE-1"),
            campaignPostHideKey("abu-safiya", "DE", "DE-2"),
        )
        val id = repository.insert(reminder(nextPostIndex = 1))

        assertEquals(0, dispatcher.dispatch(insideMondayWindow))

        assertTrue(notifier.shown.isEmpty())
        assertEquals(1, repository.getById(id)!!.nextPostIndex)
        assertNull(repository.getById(id)!!.lastFiredAt)
    }

    @Test
    fun aReminderFiresOnlyOncePerWindow() = runTest {
        repository.insert(reminder())

        dispatcher.dispatch(insideMondayWindow)
        dispatcher.dispatch(insideMondayWindow)

        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun onceReminderDisablesItselfAfterFiring() = runTest {
        val id = repository.insert(reminder(ReminderSchedule.Once(LocalDate(2026, 9, 14), window)))

        dispatcher.dispatch(insideMondayWindow)

        assertEquals(1, notifier.shown.size)
        assertFalse(repository.getById(id)!!.enabled)
    }

    @Test
    fun reminderOutsideItsWindowDoesNotFire() = runTest {
        repository.insert(reminder())

        assertEquals(0, dispatcher.dispatch(LocalDateTime(2026, 9, 14, 12, 0).toInstant(berlin)))
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun nothingFiresOrAdvancesWhileNotificationsAreBlocked() = runTest {
        notifier.allowed = false
        val id = repository.insert(reminder())

        assertEquals(0, dispatcher.dispatch(insideMondayWindow))
        assertFalse(dispatcher.sendNow(id, insideMondayWindow))

        assertEquals(0, repository.getById(id)!!.nextPostIndex)
    }

    @Test
    fun facebookTargetGetsASharerUrl() = runTest {
        repository.insert(reminder(postToX = false, postToFacebook = true))

        dispatcher.dispatch(insideMondayWindow)

        val shown = notifier.shown.single()
        assertNull(shown.xUrl)
        assertTrue(shown.facebookUrl!!.contains("facebook.com"))
    }

    @Test
    fun sendNowFiresOutsideTheScheduleAndLeavesAOneOffEnabled() = runTest {
        val id = repository.insert(reminder(ReminderSchedule.Once(LocalDate(2026, 12, 24), window)))

        assertTrue(dispatcher.sendNow(id, insideMondayWindow))

        assertEquals(1, notifier.shown.size)
        val stored = repository.getById(id)!!
        assertTrue(stored.enabled)
        assertEquals(1, stored.nextPostIndex)
    }
}
