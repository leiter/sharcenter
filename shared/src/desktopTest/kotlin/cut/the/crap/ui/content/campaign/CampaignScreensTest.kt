package cut.the.crap.ui.content.campaign

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.compose.rememberNavController
import cut.the.crap.data.preferences.CampaignHiddenPostsRepository
import cut.the.crap.data.preferences.createPreferencesStore
import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignContact
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.data.rest.campaign.CampaignPost
import cut.the.crap.data.rest.campaign.CampaignRepository
import cut.the.crap.fake.FakeCampaignRepository
import cut.the.crap.platform.Clipboard
import cut.the.crap.platform.ExternalApp
import cut.the.crap.platform.FileAccess
import cut.the.crap.platform.FileContents
import cut.the.crap.platform.FlowNotifier
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.PlatformUri
import cut.the.crap.platform.Sharer
import cut.the.crap.platform.UrlOpener
import okio.Path.Companion.toPath
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    private class NoOpClipboard : Clipboard {
        override fun copy(text: String) {}
        override fun paste(): String? = null
    }

    private class NoOpSharer : Sharer {
        override val isSupported = true
        override fun shareText(text: String, chooserTitle: String) {}
    }

    private fun composerModules(urlOpener: UrlOpener) = module {
        single<Notifier> { FlowNotifier() }
        single { urlOpener }
        single<Clipboard> { NoOpClipboard() }
        single<Sharer> { NoOpSharer() }
        single {
            val path = "${Files.createTempDirectory("campaign_hidden_posts_test")}/hidden.preferences_pb".toPath()
            CampaignHiddenPostsRepository(createPreferencesStore(name = "campaign_hidden_posts_test", path = path))
        }
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
    fun `hiding a campaign post via the overflow menu removes it from the composer`() = runComposeUiTest {
        setContent {
            KoinApplication(application = { modules(composerModules(RecordingUrlOpener())) }) {
                CampaignPostComposerScreen(
                    navController = rememberNavController(),
                    campaign = campaign,
                    onCreateDrafts = {},
                )
            }
        }
        waitForIdle()

        // The single "de" post is the only one, so its overflow trigger sits in the country header.
        onNodeWithContentDescription("More options").assertIsDisplayed()

        onNodeWithContentDescription("More options").performClick()
        waitForIdle()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        // The hide write goes through a DataStore Flow — one idle pass processes the click's
        // recomposition, a second lets the write land and the Flow re-emit into collectAsState.
        waitForIdle()

        // Hidden posts vanish immediately — no post left means no copy/overflow controls at all.
        onNodeWithContentDescription("More options").assertDoesNotExist()
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

    // --- Step 3: create + join, from CampaignListScreen ------------------------

    private class NoOpFileAccess : FileAccess {
        override suspend fun read(uri: PlatformUri): FileContents? = null
        override suspend fun readText(uri: PlatformUri): String? = null
        override suspend fun saveToDownloads(fileName: String, text: String): kotlin.Result<Unit> =
            kotlin.Result.success(Unit)
    }

    private fun listModules(repository: CampaignRepository) = module {
        single<Notifier> { FlowNotifier() }
        single { RecordingUrlOpener() as UrlOpener }
        single<CampaignRepository> { repository }
        single<FileAccess> { NoOpFileAccess() }
        single { AppConfig(apiBaseUrl = "https://example.test", campaignBaseUrl = "https://example.test", isDebug = true) }
    }

    @Test
    fun `the create FAB opens the paste-json dialog`() = runComposeUiTest {
        val repository = FakeCampaignRepository()
        setContent {
            KoinApplication(application = { modules(listModules(repository)) }) {
                CampaignListScreen(navController = rememberNavController(), viewModel = CampaignListViewModel(repository))
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Create a campaign").performClick()
        waitForIdle()

        onNodeWithText("Create campaign").assertIsDisplayed()
    }

    @Test
    fun `pasting a campaign and confirming terms creates it and refreshes the list`() = runComposeUiTest {
        val repository = FakeCampaignRepository()
        repository.nextSummary = FakeCampaignRepository.summary("c1", title = "New campaign", role = "owner")

        setContent {
            KoinApplication(application = { modules(listModules(repository)) }) {
                CampaignListScreen(navController = rememberNavController(), viewModel = CampaignListViewModel(repository))
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Create a campaign").performClick()
        waitForIdle()
        onNodeWithText("Paste the campaign JSON").performTextInput("""{"title":"New campaign","items":[]}""")
        onNodeWithTag(CAMPAIGN_CREATE_TERMS_TAG).performClick()
        onNodeWithText("Create").performClick()
        waitForIdle()

        assertEquals(1, repository.createdJson.size)
        // The dialog's own checkbox is the source of truth for confirmedTerms, patched into the
        // outgoing body regardless of what the pasted text said.
        assertTrue(repository.createdJson.single().contains("\"confirmedTerms\":true"))
        onNodeWithText("Create campaign").assertDoesNotExist()
        onNodeWithText("New campaign").assertIsDisplayed()
    }

    @Test
    fun `joining with a code redeems it and refreshes the list`() = runComposeUiTest {
        val repository = FakeCampaignRepository()
        repository.nextSummary = FakeCampaignRepository.summary("c1", title = "Joined campaign", role = "member")

        setContent {
            KoinApplication(application = { modules(listModules(repository)) }) {
                CampaignListScreen(navController = rememberNavController(), viewModel = CampaignListViewModel(repository))
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Join with code").performClick()
        waitForIdle()
        onNodeWithText("Invite code").performTextInput("ABCD2345")
        onNodeWithText("Join").performClick()
        waitForIdle()

        assertEquals(listOf("ABCD2345"), repository.joinedCodes)
        onNodeWithText("Join a campaign").assertDoesNotExist()
        onNodeWithText("Joined campaign").assertIsDisplayed()
    }

    @Test
    fun `a rejected join shows the error inline instead of closing`() = runComposeUiTest {
        val repository = FakeCampaignRepository().apply { failure = FakeCampaignRepository.notFound() }

        setContent {
            KoinApplication(application = { modules(listModules(repository)) }) {
                CampaignListScreen(navController = rememberNavController(), viewModel = CampaignListViewModel(repository))
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Join with code").performClick()
        waitForIdle()
        onNodeWithText("Invite code").performTextInput("NOPENOPE")
        onNodeWithText("Join").performClick()
        waitForIdle()

        onNodeWithText("That code didn't work", substring = true).assertIsDisplayed()
        // Still open — a failed join must not silently vanish.
        onNodeWithText("Join a campaign").assertIsDisplayed()
    }
}
