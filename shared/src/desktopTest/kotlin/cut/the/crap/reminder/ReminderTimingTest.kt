@file:OptIn(kotlin.time.ExperimentalTime::class)

package cut.the.crap.reminder

import cut.the.crap.data.domain.ActionReminder
import cut.the.crap.data.domain.ReminderPost
import cut.the.crap.data.domain.ReminderSchedule
import cut.the.crap.data.domain.TimeWindow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ReminderTimingTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private val window = TimeWindow(LocalTime(18, 0), LocalTime(20, 0))

    // 2026-09-11 is a Friday, 2026-09-14 the following Monday.
    private val mondaysAndFridays = ReminderSchedule.Recurring(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), window)

    private fun reminder(
        schedule: ReminderSchedule = mondaysAndFridays,
        enabled: Boolean = true,
        lastFiredAt: Instant? = null,
    ) = ActionReminder(
        id = 1,
        campaignId = "abu-safiya",
        countryCode = "DE",
        language = "de",
        postToX = true,
        postToFacebook = false,
        schedule = schedule,
        posts = listOf(ReminderPost("DE-1", "text")),
        campaignVersion = 1,
        enabled = enabled,
        lastFiredAt = lastFiredAt?.toEpochMilliseconds(),
    )

    private fun local(day: Int, hour: Int, minute: Int = 0, month: Int = 9): Instant =
        LocalDateTime(2026, month, day, hour, minute).toInstant(berlin)

    // --- Recurring ---

    @Test
    fun recurringIsDueFromWindowStartUntilJustBeforeItsEnd() {
        assertTrue(reminder().isDue(local(14, 18, 0), berlin))
        assertTrue(reminder().isDue(local(14, 19, 59), berlin))
        assertFalse(reminder().isDue(local(14, 20, 0), berlin))
        assertFalse(reminder().isDue(local(14, 17, 59), berlin))
    }

    @Test
    fun recurringIsNotDueOnOtherWeekdays() {
        assertFalse(reminder().isDue(local(15, 19), berlin)) // Tuesday
    }

    @Test
    fun recurringFiresOncePerOccurrence() {
        assertFalse(reminder(lastFiredAt = local(14, 18, 30)).isDue(local(14, 19), berlin))
        // Fired in last Friday's occurrence — Monday's is a new one.
        assertTrue(reminder(lastFiredAt = local(11, 18, 30)).isDue(local(14, 19), berlin))
    }

    @Test
    fun disabledReminderIsNeverDue() {
        assertFalse(reminder(enabled = false).isDue(local(14, 19), berlin))
    }

    @Test
    fun windowFollowsWallClockAcrossDaylightSavingChange() {
        // 2026-03-29 is the Sunday Europe springs forward: 18:30 is 16:30Z in CEST, not 17:30Z.
        val sundays = reminder(ReminderSchedule.Recurring(setOf(DayOfWeek.SUNDAY), window))
        assertTrue(sundays.isDue(Instant.parse("2026-03-29T16:30:00Z"), berlin))
        assertFalse(sundays.isDue(Instant.parse("2026-03-29T15:30:00Z"), berlin))
    }

    // --- Once ---

    private val onceMonday = ReminderSchedule.Once(LocalDate(2026, 9, 14), window)

    @Test
    fun onceIsDueInsideItsWindowOnItsDateOnly() {
        assertTrue(reminder(onceMonday).isDue(local(14, 18, 30), berlin))
        assertFalse(reminder(onceMonday).isDue(local(13, 18, 30), berlin))
    }

    @Test
    fun missedOnceFiresLateWithinTheGracePeriodOnly() {
        val windowEnd = local(14, 20, 0)
        assertTrue(reminder(onceMonday).isDue(windowEnd + MISSED_ONCE_GRACE - 1.minutes, berlin))
        assertFalse(reminder(onceMonday).isDue(windowEnd + MISSED_ONCE_GRACE + 1.minutes, berlin))
    }

    @Test
    fun onceThatAlreadyFiredIsNotDue() {
        assertFalse(reminder(onceMonday, lastFiredAt = local(14, 18, 5)).isDue(local(14, 19), berlin))
    }

    // --- nextWindowStart ---

    @Test
    fun nextWindowStartSkipsToTheNextMatchingDay() {
        // Friday evening after the window: next is Monday 18:00.
        assertEquals(local(14, 18), reminder().nextWindowStart(local(11, 21), berlin))
    }

    @Test
    fun nextWindowStartIsTheCurrentStartWhileDue() {
        assertEquals(local(14, 18), reminder().nextWindowStart(local(14, 19), berlin))
    }

    @Test
    fun nextWindowStartSkipsAnOccurrenceThatAlreadyFired() {
        val fired = reminder(lastFiredAt = local(14, 18, 10))
        // Monday's fired, so Friday 18:00 is next.
        assertEquals(local(18, 18), fired.nextWindowStart(local(14, 19), berlin))
    }

    @Test
    fun nextWindowStartIsNullWhenItNeverFiresAgain() {
        assertNull(reminder(enabled = false).nextWindowStart(local(14, 12), berlin))
        assertNull(reminder(ReminderSchedule.Recurring(emptySet(), window)).nextWindowStart(local(14, 12), berlin))
        assertNull(reminder(onceMonday, lastFiredAt = local(14, 18, 5)).nextWindowStart(local(14, 19), berlin))
        assertEquals(local(14, 18), reminder(onceMonday).nextWindowStart(local(12, 9), berlin))
    }
}
