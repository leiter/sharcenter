package cut.the.crap.data.domain

import cut.the.crap.tools.currentTimeMillis
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * One post a reminder can surface — a snapshot of a
 * [cut.the.crap.data.rest.campaign.CampaignPost], taken so firing never needs the network.
 *
 * @property id The campaign's variant label (e.g. "DE-2"), unique only within a country's language.
 */
@Serializable
data class ReminderPost(val id: String, val text: String)

/** A same-day local time range: [start] inclusive, [end] exclusive. */
data class TimeWindow(val start: LocalTime, val end: LocalTime)

/** When a reminder may fire. Both kinds are evaluated in the device's current time zone. */
sealed interface ReminderSchedule {
    val window: TimeWindow

    /** Every week on [days], inside [window]. */
    data class Recurring(val days: Set<DayOfWeek>, override val window: TimeWindow) : ReminderSchedule

    /** A single [date], inside [window]. The reminder disables itself once it has fired. */
    data class Once(val date: LocalDate, override val window: TimeWindow) : ReminderSchedule
}

/**
 * A schedule that surfaces a prepared campaign post as a notification, rotating through [posts]
 * — see doc/ACTION_REMINDER_SPEC.md.
 *
 * @property posts Snapshot of the country + language's posts (spec §3.2 covers refreshing it).
 * @property nextPostIndex Rotation position; taken modulo the number of posts still visible, so it
 *   stays meaningful when posts are hidden or the snapshot shrinks.
 * @property lastFiredAt Epoch millis of the last notification, which stops a window firing twice.
 */
data class ActionReminder(
    val id: Int = 0,
    val campaignId: String,
    val countryCode: String,
    val language: String,
    val postToX: Boolean,
    val postToFacebook: Boolean,
    val schedule: ReminderSchedule,
    val posts: List<ReminderPost>,
    val campaignVersion: Int,
    val nextPostIndex: Int = 0,
    val enabled: Boolean = true,
    val lastFiredAt: Long? = null,
    val createdAt: Long = currentTimeMillis(),
    val modifiedAt: Long = currentTimeMillis(),
)
