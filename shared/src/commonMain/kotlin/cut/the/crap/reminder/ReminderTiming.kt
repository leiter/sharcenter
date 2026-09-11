@file:OptIn(kotlin.time.ExperimentalTime::class)

package cut.the.crap.reminder

import cut.the.crap.data.domain.ActionReminder
import cut.the.crap.data.domain.ReminderSchedule
import cut.the.crap.data.domain.TimeWindow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * How late a missed one-off reminder may still fire (device off or dozing through its window).
 * A missed recurring window is simply skipped — the next one is never far away. Spec §4.2.
 */
val MISSED_ONCE_GRACE: Duration = 12.hours

/** One concrete occurrence of a reminder's window: [start] inclusive, [end] exclusive. */
private data class Occurrence(val start: Instant, val end: Instant)

/**
 * The window on [date] as instants. Wall-clock times are resolved in [tz] at check time, so a
 * DST change or travel moves the instant and keeps the local time the user picked.
 */
private fun TimeWindow.on(date: LocalDate, tz: TimeZone) = Occurrence(
    start = date.atTime(start).toInstant(tz),
    end = date.atTime(end).toInstant(tz),
)

private fun ActionReminder.hasFiredIn(occurrence: Occurrence): Boolean =
    lastFiredAt?.let { it >= occurrence.start.toEpochMilliseconds() } ?: false

/** Whether the reminder should fire at [now]: inside a window occurrence it has not fired in yet. */
fun ActionReminder.isDue(now: Instant, tz: TimeZone): Boolean {
    if (!enabled) return false
    return when (val schedule = schedule) {
        is ReminderSchedule.Recurring -> {
            // Windows never cross midnight, so only today's occurrence can contain now.
            val today = now.toLocalDateTime(tz).date
            today.dayOfWeek in schedule.days &&
                schedule.window.on(today, tz).let { now >= it.start && now < it.end && !hasFiredIn(it) }
        }

        is ReminderSchedule.Once -> schedule.window.on(schedule.date, tz).let {
            now >= it.start && now < it.end + MISSED_ONCE_GRACE && !hasFiredIn(it)
        }
    }
}

/**
 * Start of the next window occurrence the reminder will fire in, for the list row. When [now] is
 * inside an occurrence that has not fired yet, that occurrence's (past) start is returned — the
 * reminder is due. Null when it never fires again: disabled, a spent one-off, or no weekdays.
 */
fun ActionReminder.nextWindowStart(now: Instant, tz: TimeZone): Instant? {
    if (!enabled) return null
    return when (val schedule = schedule) {
        is ReminderSchedule.Recurring -> {
            val today = now.toLocalDateTime(tz).date
            // Today plus a full week, so "today, but the window already passed" finds next week's.
            (0..7).asSequence()
                .map { today.plus(it, DateTimeUnit.DAY) }
                .filter { it.dayOfWeek in schedule.days }
                .map { schedule.window.on(it, tz) }
                .firstOrNull { now < it.end && !hasFiredIn(it) }
                ?.start
        }

        is ReminderSchedule.Once -> schedule.window.on(schedule.date, tz)
            .takeIf { now < it.end + MISSED_ONCE_GRACE && !hasFiredIn(it) }
            ?.start
    }
}
