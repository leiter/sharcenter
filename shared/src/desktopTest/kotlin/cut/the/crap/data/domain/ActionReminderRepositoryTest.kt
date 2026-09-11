package cut.the.crap.data.domain

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cut.the.crap.data.db.SqlDelightActionReminderDao
import cut.the.crap.data.db.createDatabase
import cut.the.crap.data.db.sql.ShareDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActionReminderRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var repository: ActionReminderRepository

    private val window = TimeWindow(LocalTime(18, 0), LocalTime(20, 30))

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ShareDatabase.Schema.create(driver).value
        val db = createDatabase(driver)
        repository = ActionReminderRepositoryImpl(
            SqlDelightActionReminderDao(db.actionReminderQueries, Dispatchers.IO),
        )
    }

    @After
    fun tearDown() = driver.close()

    private fun reminder(
        schedule: ReminderSchedule = ReminderSchedule.Recurring(
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            window,
        ),
    ) = ActionReminder(
        campaignId = "abu-safiya",
        countryCode = "DE",
        language = "de",
        postToX = true,
        postToFacebook = false,
        schedule = schedule,
        posts = listOf(
            ReminderPost("DE-1", "Erster Post #AbuSafiya"),
            ReminderPost("DE-2", "Zweiter \"Post\" mit Umlauten äöü\nund Zeilenumbruch"),
        ),
        campaignVersion = 3,
        createdAt = 1_000,
        modifiedAt = 1_000,
    )

    @Test
    fun recurringReminderRoundTrips() = runTest {
        val original = reminder()
        val id = repository.insert(original)

        assertEquals(original.copy(id = id), repository.getById(id))
    }

    @Test
    fun onceReminderRoundTrips() = runTest {
        val original = reminder(ReminderSchedule.Once(LocalDate(2026, 9, 13), window))
        val id = repository.insert(original)

        assertEquals(original.copy(id = id), repository.getById(id))
    }

    @Test
    fun getEnabledSkipsDisabledReminders() = runTest {
        val keep = repository.insert(reminder())
        val off = repository.insert(reminder())
        repository.setEnabled(off, enabled = false)

        assertEquals(listOf(keep), repository.getEnabled().map { it.id })
        assertFalse(repository.getById(off)!!.enabled)
    }

    @Test
    fun recordFiredUpdatesRotationWithoutTouchingModifiedAt() = runTest {
        val id = repository.insert(reminder())
        repository.recordFired(id, nextPostIndex = 1, firedAt = 5_000, enabled = false)

        val fired = repository.getById(id)!!
        assertEquals(1, fired.nextPostIndex)
        assertEquals(5_000L, fired.lastFiredAt)
        assertFalse(fired.enabled)
        assertEquals(1_000L, fired.modifiedAt)
    }

    @Test
    fun updateReplacesTheRowAndStampsModifiedAt() = runTest {
        val id = repository.insert(reminder())
        repository.update(repository.getById(id)!!.copy(language = "en", postToFacebook = true))

        val updated = repository.getById(id)!!
        assertEquals("en", updated.language)
        assertTrue(updated.postToFacebook)
        assertTrue(updated.modifiedAt > 1_000)
    }

    @Test
    fun deleteRemovesTheRowFromTheObservedList() = runTest {
        val first = repository.insert(reminder())
        val second = repository.insert(reminder())
        repository.delete(first)

        assertEquals(listOf(second), repository.observeAll().first().map { it.id })
        assertNull(repository.getById(first))
    }

    @Test
    fun everyWeekdayRoundTripsThroughTheMask() {
        DayOfWeek.entries.forEach { day ->
            assertEquals(setOf(day), daysFromMask(maskFromDays(setOf(day))))
        }
        assertEquals(DayOfWeek.entries.toSet(), daysFromMask(maskFromDays(DayOfWeek.entries.toSet())))
    }

    @Test
    fun corruptPostsJsonLoadsAsNoPostsInsteadOfThrowing() {
        val row = reminder().toDbItem().copy(postsJson = "not json")

        assertTrue(row.toDomain().posts.isEmpty())
    }

    @Test
    fun onceRowWithUnreadableDateNeverFires() {
        val row = reminder(ReminderSchedule.Once(LocalDate(2026, 9, 13), window))
            .toDbItem()
            .copy(onceDate = "13.09.2026")

        assertEquals(ReminderSchedule.Recurring(emptySet(), window), row.toDomain().schedule)
    }
}
