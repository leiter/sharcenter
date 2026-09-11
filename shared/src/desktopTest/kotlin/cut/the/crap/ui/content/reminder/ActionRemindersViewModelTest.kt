@file:OptIn(kotlin.time.ExperimentalTime::class, ExperimentalCoroutinesApi::class)

package cut.the.crap.ui.content.reminder

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
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.data.rest.campaign.CampaignPost
import cut.the.crap.fake.FakeCampaignRepository
import cut.the.crap.platform.ReminderNotification
import cut.the.crap.platform.ReminderNotifier
import cut.the.crap.platform.ReminderScheduler
import cut.the.crap.reminder.ReminderDispatcher
import cut.the.crap.reminder.ReminderScheduleSync
import cut.the.crap.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionRemindersViewModelTest {

    private class FakeNotifier : ReminderNotifier {
        var allowed = true
        val cancelled = mutableListOf<Int>()
        override val canNotify: Boolean get() = allowed
        override suspend fun show(notification: ReminderNotification) = Unit
        override fun cancel(reminderId: Int) {
            cancelled += reminderId
        }
        override fun openSettings() = Unit
    }

    private class FakeScheduler : ReminderScheduler {
        var scheduled = false
        override val isSupported: Boolean = true
        override fun ensureScheduled() {
            scheduled = true
        }
        override fun cancel() {
            scheduled = false
        }
    }

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val berlin = TimeZone.of("Europe/Berlin")

    // Friday 2026-09-11, noon in Berlin.
    private val now = LocalDateTime(2026, 9, 11, 12, 0).toInstant(berlin)
    private val window = TimeWindow(LocalTime(18, 0), LocalTime(20, 0))

    private val campaign = Campaign(
        name = "abu-safiya",
        version = 3,
        locateUrl = null,
        id = "abu-safiya",
        countries = listOf(
            CampaignCountry(
                countryCode = "DE",
                countryName = "Deutschland",
                flag = "🇩🇪",
                defaultLanguage = "de",
                languages = listOf("de", "en"),
                url = "https://cutthecrap.link/de/abu-safiya",
                hasParliamentAction = false,
                posts = listOf(
                    CampaignPost("DE-1", "de", "eins"),
                    CampaignPost("DE-2", "de", "zwei"),
                    CampaignPost("DE-EN", "en", "one"),
                ),
            ),
        ),
    )

    private lateinit var driver: SqlDriver
    private lateinit var repository: ActionReminderRepository
    private val notifier = FakeNotifier()
    private val scheduler = FakeScheduler()
    private val hidden = MutableStateFlow(emptySet<String>())

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ShareDatabase.Schema.create(driver).value
        repository = ActionReminderRepositoryImpl(
            SqlDelightActionReminderDao(createDatabase(driver).actionReminderQueries, testDispatcher),
        )
    }

    @After
    fun tearDown() = driver.close()

    private fun viewModel() = ActionRemindersViewModel(
        repository = repository,
        campaignRepository = FakeCampaignRepository(campaign = campaign),
        dispatcher = ReminderDispatcher(repository, notifier, { hidden.value }, { berlin }),
        scheduleSync = ReminderScheduleSync(repository, scheduler),
        notifier = notifier,
        hiddenPostKeys = hidden,
        clock = { now },
        timeZone = { berlin },
    )

    private fun stored(
        language: String = "de",
        campaignVersion: Int = 3,
        posts: List<ReminderPost> = listOf(ReminderPost("DE-1", "eins"), ReminderPost("DE-2", "zwei")),
        nextPostIndex: Int = 0,
    ) = ActionReminder(
        campaignId = "abu-safiya",
        countryCode = "DE",
        language = language,
        postToX = true,
        postToFacebook = false,
        schedule = ReminderSchedule.Recurring(setOf(DayOfWeek.MONDAY), window),
        posts = posts,
        campaignVersion = campaignVersion,
        nextPostIndex = nextPostIndex,
    )

    private fun TestScope.collectEvents(model: ActionRemindersViewModel): List<ActionRemindersEvent> {
        val events = mutableListOf<ActionRemindersEvent>()
        // Unconfined, so each emission is recorded as it happens: advanceUntilIdle() stops once only
        // background work is left, and would never resume a backgroundScope collector on the
        // standard dispatcher.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.collect { events += it } }
        return events
    }

    private val validDraft = ReminderDraft(countryCode = "DE", language = "de")

    @Test
    fun `create saves a snapshot of the chosen posts and schedules the job`() = runTest(testDispatcher) {
        val model = viewModel()
        advanceUntilIdle()

        assertNull(model.create(validDraft))
        advanceUntilIdle()

        val saved = repository.getEnabled().single()
        assertEquals(listOf("DE-1", "DE-2"), saved.posts.map { it.id })
        assertEquals(3, saved.campaignVersion)
        assertIs<ReminderSchedule.Recurring>(saved.schedule)
        assertTrue(scheduler.scheduled)
        assertEquals(1, model.state.value.rows.size)
    }

    @Test
    fun `create rejects an invalid draft and saves nothing`() = runTest(testDispatcher) {
        val model = viewModel()
        advanceUntilIdle()

        assertEquals(ReminderDraftError.NoPlatform, model.create(validDraft.copy(postToX = false)))
        advanceUntilIdle()

        assertTrue(repository.getEnabled().isEmpty())
        assertFalse(scheduler.scheduled)
    }

    @Test
    fun `create asks for the permission when notifications are blocked`() = runTest(testDispatcher) {
        notifier.allowed = false
        val model = viewModel()
        advanceUntilIdle()
        val events = collectEvents(model)

        model.create(validDraft)
        advanceUntilIdle()

        assertEquals(listOf<ActionRemindersEvent>(ActionRemindersEvent.RequestNotificationPermission), events)
        assertFalse(model.state.value.canNotify)
    }

    @Test
    fun `reminders from an older campaign version get the current posts`() = runTest(testDispatcher) {
        val current = repository.insert(
            stored(campaignVersion = 1, posts = listOf(ReminderPost("OLD", "alt")), nextPostIndex = 5),
        )
        val orphaned = repository.insert(stored(language = "fr", campaignVersion = 1))

        viewModel()
        advanceUntilIdle()

        val refreshed = repository.getById(current)!!
        assertEquals(listOf("DE-1", "DE-2"), refreshed.posts.map { it.id })
        assertEquals(3, refreshed.campaignVersion)
        assertEquals(1, refreshed.nextPostIndex) // 5 mod 2
        assertTrue(refreshed.enabled)

        // Its language has no posts any more: kept, but disabled.
        val gone = repository.getById(orphaned)!!
        assertTrue(gone.posts.isEmpty())
        assertFalse(gone.enabled)
    }

    @Test
    fun `delete cancels the notification and undo brings the reminder back`() = runTest(testDispatcher) {
        val id = repository.insert(stored())
        val model = viewModel()
        advanceUntilIdle()
        val events = collectEvents(model)

        model.delete(repository.getById(id)!!)
        advanceUntilIdle()

        assertEquals(listOf(id), notifier.cancelled)
        assertTrue(repository.getEnabled().isEmpty())
        val deleted = assertIs<ActionRemindersEvent.Deleted>(events.single())

        model.undoDelete(deleted.reminder)
        advanceUntilIdle()

        assertEquals(listOf("DE-1", "DE-2"), repository.getEnabled().single().posts.map { it.id })
    }

    @Test
    fun `rows show the next post the user has not hidden`() = runTest(testDispatcher) {
        repository.insert(stored())
        hidden.value = setOf(campaignPostHideKey("abu-safiya", "DE", "DE-1"))

        val model = viewModel()
        advanceUntilIdle()

        val row = model.state.value.rows.single()
        assertEquals("DE-2", row.nextPost?.id)
        assertEquals("Deutschland", row.country?.countryName)
    }
}
