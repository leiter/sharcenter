package cut.the.crap.ui.content.campaign

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.compose.rememberNavController
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignContact
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.data.rest.campaign.CampaignPost
import cut.the.crap.fake.FakeCampaignRepository
import cut.the.crap.platform.ExternalApp
import cut.the.crap.platform.FlowNotifier
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.UrlOpener
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Renders the campaign list and detail screens.
 *
 * What these hold down is the thing the original complaint was about: **taps go somewhere**. The
 * old country screen had no `onClick` at all, so it looked interactive and was not. Here a country
 * row opens its page and a parliament chip opens its contact action, and both are asserted by
 * recording what the [UrlOpener] was asked to open.
 */
@OptIn(ExperimentalTestApi::class)
class CampaignScreensTest {

    private class RecordingUrlOpener : UrlOpener {
        val opened = mutableListOf<String>()
        override fun open(url: String, preferApp: ExternalApp?) {
            opened += url
        }
    }

    private fun testModules(urlOpener: UrlOpener) = module {
        single<Notifier> { FlowNotifier() }
        single { urlOpener }
    }

    private val campaign = Campaign(
        name = "abu-safiya",
        version = 1,
        locateUrl = "https://cutthecrap.link/abu-safiya/locate",
        id = "abu-safiya",
        title = "Freiheit für Abu Safiya",
        description = "Ein Kinderarzt in Haft.",
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
        ),
    )

    @Test
    fun `the list shows a campaign and its badge`() = runComposeUiTest {
        val repository = FakeCampaignRepository(
            summaries = listOf(
                FakeCampaignRepository.summary(
                    "abu-safiya", "Freiheit für Abu Safiya",
                    featured = true, countryCount = 42, postCount = 137,
                )
            )
        )

        setContent {
            KoinApplication(application = { modules(testModules(RecordingUrlOpener())) }) {
                CampaignListScreen(
                    navController = rememberNavController(),
                    viewModel = CampaignListViewModel(repository),
                )
            }
        }
        waitForIdle()

        onNodeWithText("Freiheit für Abu Safiya").assertIsDisplayed()
        onNodeWithText("Included").assertIsDisplayed()
        onNodeWithText("42 countries · 137 posts").assertIsDisplayed()
    }

    @Test
    fun `an empty list says so instead of looking broken`() = runComposeUiTest {
        setContent {
            KoinApplication(application = { modules(testModules(RecordingUrlOpener())) }) {
                CampaignListScreen(
                    navController = rememberNavController(),
                    viewModel = CampaignListViewModel(FakeCampaignRepository()),
                )
            }
        }
        waitForIdle()

        onNodeWithText("No campaigns yet").assertIsDisplayed()
    }

    @Test
    fun `a country row opens its campaign page`() = runComposeUiTest {
        val urlOpener = RecordingUrlOpener()
        setContent {
            KoinApplication(application = { modules(testModules(urlOpener)) }) {
                CampaignDetailScreen(
                    navController = rememberNavController(),
                    campaignId = "abu-safiya",
                    viewModel = CampaignDetailViewModel(
                        FakeCampaignRepository(campaign = campaign), "abu-safiya"
                    ),
                )
            }
        }
        waitForIdle()

        onNodeWithText("🇩🇪 Deutschland").performClick()
        waitForIdle()

        // The field that was parsed and never used.
        assertEquals(listOf("https://cutthecrap.link/de/abu-safiya"), urlOpener.opened)
    }

    @Test
    fun `the parliament chip opens the contact action`() = runComposeUiTest {
        val urlOpener = RecordingUrlOpener()
        setContent {
            KoinApplication(application = { modules(testModules(urlOpener)) }) {
                CampaignDetailScreen(
                    navController = rememberNavController(),
                    campaignId = "abu-safiya",
                    viewModel = CampaignDetailViewModel(
                        FakeCampaignRepository(campaign = campaign), "abu-safiya"
                    ),
                )
            }
        }
        waitForIdle()

        onNodeWithText("Parlament").performClick()
        waitForIdle()

        assertEquals(listOf("https://cutthecrap.link/de/abu-safiya/mdb"), urlOpener.opened)
    }

    @Test
    fun `find my country opens the locate url`() = runComposeUiTest {
        val urlOpener = RecordingUrlOpener()
        setContent {
            KoinApplication(application = { modules(testModules(urlOpener)) }) {
                CampaignDetailScreen(
                    navController = rememberNavController(),
                    campaignId = "abu-safiya",
                    viewModel = CampaignDetailViewModel(
                        FakeCampaignRepository(campaign = campaign), "abu-safiya"
                    ),
                )
            }
        }
        waitForIdle()

        onNodeWithText("Find my country").performClick()
        waitForIdle()

        assertEquals(listOf("https://cutthecrap.link/abu-safiya/locate"), urlOpener.opened)
    }

    @Test
    fun `a disabled campaign says a human switched it off`() = runComposeUiTest {
        val repository = FakeCampaignRepository().apply { failure = FakeCampaignRepository.disabled() }

        setContent {
            KoinApplication(application = { modules(testModules(RecordingUrlOpener())) }) {
                CampaignDetailScreen(
                    navController = rememberNavController(),
                    campaignId = "secret",
                    viewModel = CampaignDetailViewModel(repository, "secret"),
                )
            }
        }
        waitForIdle()

        onNodeWithText("This campaign was switched off").assertIsDisplayed()
        // No retry button: retrying cannot bring it back, and offering one would be a lie.
        onNodeWithText("Try again").assertDoesNotExist()
    }
}
