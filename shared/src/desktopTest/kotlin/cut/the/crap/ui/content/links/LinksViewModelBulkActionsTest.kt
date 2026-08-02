package cut.the.crap.ui.content.links

import cut.the.crap.fake.FakeFileAccess

import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.fake.FakeContentLinkRepository
import cut.the.crap.fake.FakeJobQueueRepository
import cut.the.crap.fake.FakeKeywordRepository
import cut.the.crap.fake.FakeMessageRepository
import cut.the.crap.fake.FakeYouTubeRepository
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.testutils.TestData
import cut.the.crap.tools.DescriptionParser
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.ContentLinkAction
import cut.the.crap.ui.components.api.ListAction
import cut.the.crap.ui.content.settings.AppSettings
import cut.the.crap.ui.content.settings.DateRangePreset
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for LinksViewModel bulk selection actions: bulk favorite (group toggle), bulk delete of
 * only the selected items, and bulk tag (additive, idempotent) across the selection.
 *
 * Like [LinksViewModelFilterTest], each test runs on the rule's dispatcher and keeps `listState`
 * and `screenState` collected while acting — both are subscriber-gated, and the bulk handlers read
 * the selected ids from `screenState` and the resolved links from `listState`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinksViewModelBulkActionsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var contentLinkRepository: FakeContentLinkRepository
    private lateinit var messageRepository: FakeMessageRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var keywordRepository: FakeKeywordRepository
    private lateinit var jobQueueRepository: FakeJobQueueRepository
    private lateinit var youTubeRepository: FakeYouTubeRepository
    private lateinit var fileAccess: FakeFileAccess
    private lateinit var viewModel: LinksViewModel

    // ALL_TIME so init does not apply a date-range filter that would drop test items.
    private val settingsFlow = MutableStateFlow(
        AppSettings(linksDateRangePreset = DateRangePreset.ALL_TIME)
    )

    @Before
    fun setup() {
        contentLinkRepository = FakeContentLinkRepository()
        messageRepository = FakeMessageRepository()
        keywordRepository = FakeKeywordRepository()
        jobQueueRepository = FakeJobQueueRepository()
        youTubeRepository = FakeYouTubeRepository()

        settingsRepository = mockk(relaxed = true)
        coEvery { settingsRepository.settingsFlow } returns settingsFlow

        fileAccess = FakeFileAccess()

        viewModel = LinksViewModel(
            contentRepository = contentLinkRepository,
            repository = messageRepository,
            fileAccess = fileAccess,
            settingsRepository = settingsRepository,
            keywordRepository = keywordRepository,
            jobQueueRepository = jobQueueRepository,
            youTubeRepository = youTubeRepository,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    /** Keep the subscriber-gated flows collected while [actions] run against the live ViewModel. */
    private fun TestScope.withCollectors(actions: () -> Unit) {
        val listJob = launch { viewModel.listState.collect { } }
        val stateJob = launch { viewModel.screenState.collect { } }
        advanceUntilIdle()
        actions()
        advanceUntilIdle()
        listJob.cancel()
        stateJob.cancel()
    }

    @Test
    fun `bulk favorite favourites every selected item and leaves the rest`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/a"),
                TestData.contentLink(id = 2, link = "https://x.com/b"),
                TestData.contentLink(id = 3, link = "https://x.com/c")
            )
            contentLinkRepository.setItems(links)

            withCollectors {
                viewModel.consumeAction(ContentLinkAction.EnterSelectionMode(links[0]))
                viewModel.consumeAction(ContentLinkAction.ToggleSelection(links[2]))
                viewModel.consumeAction(ListAction.ToggleFavoritesForSelected)
            }

            val stored = contentLinkRepository.getStoredItems().associateBy { it.id }
            assertThat(stored.getValue(1).favourite).isTrue()
            assertThat(stored.getValue(3).favourite).isTrue()
            assertThat(stored.getValue(2).favourite).isFalse()
        }

    @Test
    fun `bulk favorite clears favourites when all selected are already favourite`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/a", favourite = true),
                TestData.contentLink(id = 2, link = "https://x.com/b", favourite = true)
            )
            contentLinkRepository.setItems(links)

            withCollectors {
                viewModel.consumeAction(ContentLinkAction.EnterSelectionMode(links[0]))
                viewModel.consumeAction(ContentLinkAction.ToggleSelection(links[1]))
                viewModel.consumeAction(ListAction.ToggleFavoritesForSelected)
            }

            val stored = contentLinkRepository.getStoredItems()
            assertThat(stored.all { !it.favourite }).isTrue()
        }

    @Test
    fun `bulk delete removes only the selected items`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/a"),
                TestData.contentLink(id = 2, link = "https://x.com/b"),
                TestData.contentLink(id = 3, link = "https://x.com/c")
            )
            contentLinkRepository.setItems(links)

            withCollectors {
                viewModel.consumeAction(ContentLinkAction.EnterSelectionMode(links[1]))
                viewModel.consumeAction(ListAction.DeleteSelected)
            }

            assertThat(contentLinkRepository.getStoredItems().map { it.id })
                .containsExactly(1, 3)
            // Deleting the whole selection also leaves selection mode.
            assertThat(viewModel.screenState.value.checkMarks).isFalse()
            assertThat(viewModel.screenState.value.selectedItems).isEmpty()
        }

    @Test
    fun `bulk tag adds the hashtag to every selected item without duplicating existing ones`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val alreadyTagged = DescriptionParser.serialize(
                metadata = emptyList(),
                handles = emptyList(),
                hashtags = listOf("news"),
                keywords = emptyList()
            )
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/a"),
                TestData.contentLink(id = 2, link = "https://x.com/b", description = alreadyTagged),
                TestData.contentLink(id = 3, link = "https://x.com/c")
            )
            contentLinkRepository.setItems(links)

            withCollectors {
                viewModel.consumeAction(ContentLinkAction.EnterSelectionMode(links[0]))
                viewModel.consumeAction(ContentLinkAction.ToggleSelection(links[1]))
                viewModel.consumeAction(ListAction.TagSelected(ChipsType.Tag, listOf("news")))
            }

            val stored = contentLinkRepository.getStoredItems().associateBy { it.id }
            // id 1 (was untagged) gains the hashtag.
            assertThat(LinkMetadata.getHashtags(stored.getValue(1))).containsExactly("news")
            // id 2 already had it — still exactly one, no duplicate.
            assertThat(LinkMetadata.getHashtags(stored.getValue(2))).containsExactly("news")
            // id 3 was not selected — untouched.
            assertThat(LinkMetadata.getHashtags(stored.getValue(3))).isEmpty()
        }
}
