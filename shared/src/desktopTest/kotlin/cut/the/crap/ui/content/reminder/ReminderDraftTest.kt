package cut.the.crap.ui.content.reminder

import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.data.rest.campaign.CampaignPost
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderDraftTest {

    private val today = LocalDate(2026, 9, 11)

    private val campaign = Campaign(
        name = "abu-safiya",
        version = 1,
        locateUrl = null,
        countries = listOf(
            CampaignCountry(
                countryCode = "DE",
                countryName = "Deutschland",
                flag = "🇩🇪",
                defaultLanguage = "de",
                languages = listOf("de"),
                url = "https://cutthecrap.link/de/abu-safiya",
                hasParliamentAction = false,
                posts = listOf(CampaignPost("DE-1", "de", "eins")),
            ),
        ),
    )

    private val valid = ReminderDraft(countryCode = "DE", language = "de")

    @Test
    fun validDraftPasses() {
        assertNull(valid.validate(campaign, today))
    }

    @Test
    fun needsAtLeastOnePlatform() {
        assertEquals(
            ReminderDraftError.NoPlatform,
            valid.copy(postToX = false, postToFacebook = false).validate(campaign, today),
        )
    }

    @Test
    fun needsACountryAndLanguageWithPosts() {
        assertEquals(ReminderDraftError.NoCountry, valid.copy(countryCode = null).validate(campaign, today))
        assertEquals(ReminderDraftError.NoPosts, valid.copy(language = "fr").validate(campaign, today))
        assertEquals(ReminderDraftError.NoPosts, valid.validate(campaign = null, today = today))
    }

    @Test
    fun recurringNeedsAWeekday() {
        assertEquals(ReminderDraftError.NoWeekday, valid.copy(days = emptySet()).validate(campaign, today))
    }

    @Test
    fun onceNeedsADateThatHasNotPassed() {
        val once = valid.copy(recurring = false)
        assertEquals(ReminderDraftError.NoDate, once.validate(campaign, today))
        assertEquals(ReminderDraftError.DateInPast, once.copy(date = LocalDate(2026, 9, 10)).validate(campaign, today))
        assertNull(once.copy(date = today).validate(campaign, today))
    }

    @Test
    fun windowMustBeAtLeastAnHourOnTheSameDay() {
        val from18 = valid.copy(start = LocalTime(18, 0))
        assertNull(from18.copy(end = LocalTime(19, 0)).validate(campaign, today))
        assertEquals(ReminderDraftError.WindowTooShort, from18.copy(end = LocalTime(18, 59)).validate(campaign, today))
        // Ending "earlier" would mean crossing midnight, which v1 does not support.
        assertEquals(ReminderDraftError.WindowTooShort, from18.copy(end = LocalTime(1, 0)).validate(campaign, today))
    }
}
