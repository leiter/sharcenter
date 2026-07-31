package cut.the.crap.ui.content.campaign

import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignContact
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.data.rest.campaign.CampaignPost
import cut.the.crap.fake.FakeCampaignRepository
import cut.the.crap.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CampaignDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val campaign = Campaign(
        name = "abu-safiya",
        version = 3,
        locateUrl = "https://cutthecrap.link/abu-safiya/locate",
        id = "abu-safiya",
        title = "Freiheit für Dr. Hussam Abu Safiya",
        role = "member",
        countries = listOf(
            CampaignCountry(
                countryCode = "de",
                countryName = "Deutschland",
                flag = "🇩🇪",
                defaultLanguage = "de",
                languages = listOf("de"),
                url = "https://cutthecrap.link/de/abu-safiya",
                hasParliamentAction = true,
                posts = listOf(CampaignPost("A", "de", "Text")),
                contacts = listOf(
                    CampaignContact("c1", "https://cutthecrap.link/de/abu-safiya/mdb", "Parlament", null)
                ),
            ),
            CampaignCountry(
                countryCode = "it",
                countryName = "Italia",
                flag = "🇮🇹",
                defaultLanguage = "it",
                languages = listOf("it"),
                url = "https://cutthecrap.link/it/abu-safiya",
                hasParliamentAction = false,
                posts = listOf(CampaignPost("IT-1", "it", "Testo")),
            ),
        ),
    )

    @Test
    fun `loads by id, without needing a campaign handed to it`() = runTest(testDispatcher) {
        // The point of taking an id: the old screen rendered "no campaign data available" whenever
        // it was reached without another view model having loaded the payload first.
        val repository = FakeCampaignRepository(campaign = campaign)

        val model = CampaignDetailViewModel(repository, "abu-safiya")
        advanceUntilIdle()

        assertEquals(listOf("abu-safiya"), repository.requestedIds)
        assertEquals("abu-safiya", model.state.value.campaign?.id)
        assertTrue(!model.state.value.loading)
    }

    @Test
    fun `exposes the countries that can be acted on`() = runTest(testDispatcher) {
        val model = CampaignDetailViewModel(FakeCampaignRepository(campaign = campaign), "abu-safiya")
        advanceUntilIdle()

        val loaded = model.state.value.campaign!!
        assertEquals(listOf("de", "it"), loaded.countriesWithPosts.map { it.countryCode })
        // The parliament flag finally selects for something: a country with somewhere to write to.
        assertEquals(listOf("de"), loaded.countriesWithContacts.map { it.countryCode })
        assertEquals(
            "https://cutthecrap.link/de/abu-safiya/mdb",
            loaded.countries.first().contacts.single().url,
        )
    }

    @Test
    fun `a disabled campaign is called out, not offered for retry`() = runTest(testDispatcher) {
        val repository = FakeCampaignRepository().apply { failure = FakeCampaignRepository.disabled() }

        val model = CampaignDetailViewModel(repository, "secret")
        advanceUntilIdle()

        assertTrue(model.state.value.isDisabled, "451 means a human switched it off (C6)")
        assertNull(model.state.value.campaign)
    }

    @Test
    fun `a missing campaign is an ordinary error, not a disabled one`() = runTest(testDispatcher) {
        val repository = FakeCampaignRepository().apply { failure = FakeCampaignRepository.notFound() }

        val model = CampaignDetailViewModel(repository, "gone")
        advanceUntilIdle()

        assertTrue(!model.state.value.isDisabled)
        assertTrue(model.state.value.error != null)
    }

    @Test
    fun `retrying forces a refresh rather than re-reading the cache`() = runTest(testDispatcher) {
        val repository = FakeCampaignRepository(campaign = campaign)
        val model = CampaignDetailViewModel(repository, "abu-safiya")
        advanceUntilIdle()

        model.load(forceRefresh = true)
        advanceUntilIdle()

        assertEquals(
            listOf("abu-safiya"),
            repository.forcedIds,
            "a retry that could be served from the cache would appear to do nothing",
        )
    }

    @Test
    fun `a failed refresh keeps the campaign on screen`() = runTest(testDispatcher) {
        val repository = FakeCampaignRepository(campaign = campaign)
        val model = CampaignDetailViewModel(repository, "abu-safiya")
        advanceUntilIdle()

        repository.failure = FakeCampaignRepository.notFound()
        model.load(forceRefresh = true)
        advanceUntilIdle()

        assertEquals("abu-safiya", model.state.value.campaign?.id)
        assertTrue(model.state.value.error != null)
    }
}
