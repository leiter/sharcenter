package cut.the.crap.ui.content.links

import android.content.Context
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.fake.FakeContentLinkRepository
import cut.the.crap.fake.FakeJobQueueRepository
import cut.the.crap.fake.FakeKeywordRepository
import cut.the.crap.fake.FakeMessageRepository
import cut.the.crap.fake.FakeYouTubeRepository
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.testutils.TestData
import cut.the.crap.tools.DescriptionParser
import cut.the.crap.ui.components.api.TextAction
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
 * Tests for LinksViewModel filter matching logic.
 * Covers short domain filters (OR/AND logic) and long keyword substring matching.
 *
 * `listState` is computed on `viewModelScope` (i.e. `Dispatchers.Main`), so each test runs on the
 * rule's dispatcher via `runTest(mainDispatcherRule.testDispatcher)` to share a single scheduler —
 * otherwise `advanceUntilIdle()` would not drive the ViewModel's flows. Because `listState` and
 * `screenState` are subscriber-gated, [filteredLinksAfter] keeps both collected while it applies
 * actions and reads the settled result.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinksViewModelFilterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var contentLinkRepository: FakeContentLinkRepository
    private lateinit var messageRepository: FakeMessageRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var keywordRepository: FakeKeywordRepository
    private lateinit var jobQueueRepository: FakeJobQueueRepository
    private lateinit var youTubeRepository: FakeYouTubeRepository
    private lateinit var context: Context
    private lateinit var viewModel: LinksViewModel

    // ALL_TIME so the ViewModel's init does not apply a date-range filter that would drop test items.
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

        context = mockk(relaxed = true)

        viewModel = LinksViewModel(
            contentRepository = contentLinkRepository,
            repository = messageRepository,
            context = context,
            settingsRepository = settingsRepository,
            keywordRepository = keywordRepository,
            jobQueueRepository = jobQueueRepository,
            youTubeRepository = youTubeRepository
        )
    }

    /**
     * Keep [LinksViewModel.listState] and [LinksViewModel.screenState] collected (both are hot,
     * subscriber-gated flows), run [actions] against the live ViewModel, let every coroutine settle,
     * then return the resulting filtered list.
     *
     * `screenState` must be subscribed before the actions run: `AddHiddenFilter` reads the current
     * filter list from `screenState.value`, so without an active collector a second add would
     * clobber the first.
     */
    private fun TestScope.filteredLinksAfter(actions: () -> Unit): List<ContentLink> {
        val listJob = launch { viewModel.listState.collect { } }
        val stateJob = launch { viewModel.screenState.collect { } }
        advanceUntilIdle()
        actions()
        advanceUntilIdle()
        val result = viewModel.listState.value
        listJob.cancel()
        stateJob.cancel()
        return result
    }

    // ========== Short Domain Filter Tests (<=3 chars) ==========

    @Test
    fun `short domain filter matches exact domain part - x matches x_com`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/user/status/123"),
                TestData.contentLink(id = 2, link = "https://youtube.com/watch?v=abc"),
                TestData.contentLink(id = 3, link = "https://example.com/page")
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
            }
            assertThat(filtered.map { it.id }).containsExactly(1)
        }

    @Test
    fun `short domain filter matches fb_com`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://fb.com/post/123"),
                TestData.contentLink(id = 2, link = "https://facebook.com/page"),
                TestData.contentLink(id = 3, link = "https://example.com/fb/page") // should NOT match
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("fb"))
            }
            assertThat(filtered.map { it.id }).containsExactly(1)
        }

    @Test
    fun `short domain filter does not match substring in path`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://example.com/x/page"), // x in path, not domain
                TestData.contentLink(id = 2, link = "https://x.com/user/status")   // x.com domain
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
            }
            // Only id=2 should match (domain is x.com)
            assertThat(filtered.map { it.id }).containsExactly(2)
        }

    @Test
    fun `short domain filter does not match letter in youtube metadata`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Regression: filtering to "x" must not keep YouTube items just because the letter "x"
            // appears in their free-text metadata (video title, channel, thumbnail URL).
            val youtubeDescription = DescriptionParser.serialize(
                metadata = listOf("Some Channel", "Amazing Video Extras", "https://i.ytimg.com/vi/abcXdef/hq.jpg"),
                handles = emptyList(),
                hashtags = emptyList(),
                keywords = emptyList()
            )
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/user/status/1"),
                TestData.contentLink(id = 2, link = "https://youtube.com/watch?v=abcXdef", description = youtubeDescription)
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
            }
            assertThat(filtered.map { it.id }).containsExactly(1)
        }

    // ========== Long Keyword Filter Tests (>3 chars) ==========

    @Test
    fun `long keyword filter uses substring matching in link`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/testuser/status/1"),
                TestData.contentLink(id = 2, link = "https://x.com/otheruser/status/2"),
                TestData.contentLink(id = 3, link = "https://youtube.com/testuser")
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
            }
            assertThat(filtered.map { it.id }).containsExactly(1, 3)
        }

    @Test
    fun `long keyword filter uses substring matching in description`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/user1", description = "Great content here"),
                TestData.contentLink(id = 2, link = "https://x.com/user2", description = "Another post"),
                TestData.contentLink(id = 3, link = "https://x.com/user3", description = "More content")
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("content"))
            }
            assertThat(filtered.map { it.id }).containsExactly(1, 3)
        }

    // ========== OR Logic for Username Filters ==========

    @Test
    fun `multiple username filters use OR logic - matches ANY`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/alice/status/1"),
                TestData.contentLink(id = 2, link = "https://x.com/bob/status/2"),
                TestData.contentLink(id = 3, link = "https://x.com/charlie/status/3")
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                // Add username filters (>3 chars = OR logic)
                viewModel.consumeAction(TextAction.AddHiddenFilter("alice"))
                viewModel.consumeAction(TextAction.AddHiddenFilter("charlie"))
            }
            // Should match alice OR charlie
            assertThat(filtered.map { it.id }).containsExactly(1, 3)
        }

    // ========== AND Logic for Domain Filters ==========

    @Test
    fun `multiple short domain filters use AND logic - matches ALL`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Short filters match on the URL domain or an exact tag token. AND logic means an item
            // must satisfy BOTH. A single URL has one domain, so id=3 satisfies both by being on the
            // x.com domain AND carrying an "fb" hashtag.
            val fbTagged = DescriptionParser.serialize(
                metadata = emptyList(),
                handles = emptyList(),
                hashtags = listOf("fb"),
                keywords = emptyList()
            )
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/user"),                       // x domain only
                TestData.contentLink(id = 2, link = "https://fb.com/user"),                       // fb domain only
                TestData.contentLink(id = 3, link = "https://x.com/user", description = fbTagged) // x domain + fb tag
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                // Add domain filters (<=3 chars = AND logic)
                viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
                viewModel.consumeAction(TextAction.AddHiddenFilter("fb"))
            }
            assertThat(filtered.map { it.id }).containsExactly(3)
        }

    // ========== Mixed Filters ==========

    @Test
    fun `mixed username and domain filters apply correct logic`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://x.com/alice/status/1"),
                TestData.contentLink(id = 2, link = "https://fb.com/alice/post"),
                TestData.contentLink(id = 3, link = "https://x.com/bob/status/2")
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                // "alice" is a username filter (>3 chars, OR logic)
                // "x" is a domain filter (<=3 chars, AND logic)
                viewModel.consumeAction(TextAction.AddHiddenFilter("alice"))
                viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
            }
            // Username filter "alice" (OR): matches 1 and 2
            // Domain filter "x" (AND): must match x.com domain
            // Combined: id=1 matches both (alice in URL, x.com domain)
            assertThat(filtered.map { it.id }).containsExactly(1)
        }

    // ========== Clear Filters ==========

    @Test
    fun `clear filters shows all items again`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = TestData.contentLinks(5)
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("user1"))
                viewModel.consumeAction(TextAction.ClearHiddenFilters)
            }
            assertThat(filtered).hasSize(5)
        }

    // ========== Case Insensitivity ==========

    @Test
    fun `filters are case insensitive`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = listOf(
                TestData.contentLink(id = 1, link = "https://X.COM/USER/status/1"),
                TestData.contentLink(id = 2, link = "https://x.com/user/status/2"),
                TestData.contentLink(id = 3, link = "https://youtube.com/TestUser")
            )
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
            }
            assertThat(filtered.map { it.id }).containsExactly(3)
        }

    // ========== Empty Filters ==========

    @Test
    fun `no filters shows all items`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val links = TestData.contentLinks(5)
            contentLinkRepository.setItems(links)

            val filtered = filteredLinksAfter { }
            assertThat(filtered).hasSize(5)
        }

    @Test
    fun `adding duplicate filter does not create duplicates`() =
        runTest(mainDispatcherRule.testDispatcher) {
            filteredLinksAfter {
                viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
                viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
            }

            val state = viewModel.internalScreenState.value
            assertThat(state.hiddenFilters.count { it == "testuser" }).isEqualTo(1)
        }
}
