package cut.the.crap.ui.content.reminder

import cut.the.crap.data.domain.ReminderPost
import cut.the.crap.data.rest.campaign.Campaign
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Shortest window the create sheet accepts: twice the worker's 30-minute period, so every window
 * gets at least one run while the device is awake (spec §2.2).
 */
const val MIN_WINDOW_MINUTES = 60

/** The create sheet's form. Null country, language or date mean "not chosen yet". */
data class ReminderDraft(
    val countryCode: String? = null,
    val language: String? = null,
    val postToX: Boolean = true,
    val postToFacebook: Boolean = false,
    val recurring: Boolean = true,
    val days: Set<DayOfWeek> = WORKDAYS,
    val date: LocalDate? = null,
    val start: LocalTime = LocalTime(18, 0),
    val end: LocalTime = LocalTime(20, 0),
)

private val WORKDAYS = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

/** Why a [ReminderDraft] cannot be saved, in the order the sheet reports them. */
enum class ReminderDraftError {
    NoPlatform,
    NoCountry,
    NoPosts,
    NoWeekday,
    NoDate,
    DateInPast,
    WindowTooShort,
}

/** The first reason [this] cannot be saved against [campaign] on [today], or null when it can. */
fun ReminderDraft.validate(campaign: Campaign?, today: LocalDate): ReminderDraftError? {
    if (!postToX && !postToFacebook) return ReminderDraftError.NoPlatform
    if (countryCode == null || language == null) return ReminderDraftError.NoCountry
    if (campaign == null || campaign.reminderPostsFor(countryCode, language).isEmpty()) {
        return ReminderDraftError.NoPosts
    }
    if (recurring) {
        if (days.isEmpty()) return ReminderDraftError.NoWeekday
    } else {
        if (date == null) return ReminderDraftError.NoDate
        if (date < today) return ReminderDraftError.DateInPast
    }
    // Also rejects end <= start, i.e. a window across midnight.
    if (end.minuteOfDay() - start.minuteOfDay() < MIN_WINDOW_MINUTES) {
        return ReminderDraftError.WindowTooShort
    }
    return null
}

/** Snapshot of a country's posts in one language, in the campaign's own variant order. */
internal fun Campaign.reminderPostsFor(countryCode: String, language: String): List<ReminderPost> =
    countries.firstOrNull { it.countryCode == countryCode }
        ?.postsByLanguage?.get(language)
        .orEmpty()
        .map { ReminderPost(id = it.id, text = it.text) }

private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute
