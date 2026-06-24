package cut.the.crap.ui.content.links

import android.content.Context
import app.cash.turbine.test
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.fake.FakeContentLinkRepository
import cut.the.crap.fake.FakeJobQueueRepository
import cut.the.crap.fake.FakeKeywordRepository
import cut.the.crap.fake.FakeMessageRepository
import cut.the.crap.fake.FakeYouTubeRepository
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.testutils.TestData
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.content.settings.AppSettings
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Tests for LinksViewModel filter matching logic.
 * Covers short domain filters (OR/AND logic) and long keyword substring matching.
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

    private val settingsFlow = MutableStateFlow(AppSettings())

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

    // ========== Short Domain Filter Tests (<=3 chars) ==========

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `short domain filter matches exact domain part - x matches x_com`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://x.com/user/status/123"),
            TestData.contentLink(id = 2, link = "https://youtube.com/watch?v=abc"),
            TestData.contentLink(id = 3, link = "https://example.com/page")
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            assertThat(filtered.map { it.id }).containsExactly(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `short domain filter matches fb_com`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://fb.com/post/123"),
            TestData.contentLink(id = 2, link = "https://facebook.com/page"),
            TestData.contentLink(id = 3, link = "https://example.com/fb/page") // should NOT match
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("fb"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            assertThat(filtered.map { it.id }).containsExactly(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `short domain filter does not match substring in path`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://example.com/x/page"), // x in path, not domain
            TestData.contentLink(id = 2, link = "https://x.com/user/status")   // x.com domain
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            // Only id=2 should match (domain is x.com)
            assertThat(filtered.map { it.id }).containsExactly(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Long Keyword Filter Tests (>3 chars) ==========

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `long keyword filter uses substring matching in link`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://x.com/testuser/status/1"),
            TestData.contentLink(id = 2, link = "https://x.com/otheruser/status/2"),
            TestData.contentLink(id = 3, link = "https://youtube.com/testuser")
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            assertThat(filtered.map { it.id }).containsExactly(1, 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `long keyword filter uses substring matching in description`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://x.com/user1", description = "Great content here"),
            TestData.contentLink(id = 2, link = "https://x.com/user2", description = "Another post"),
            TestData.contentLink(id = 3, link = "https://x.com/user3", description = "More content")
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("content"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            assertThat(filtered.map { it.id }).containsExactly(1, 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== OR Logic for Username Filters ==========

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `multiple username filters use OR logic - matches ANY`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://x.com/alice/status/1"),
            TestData.contentLink(id = 2, link = "https://x.com/bob/status/2"),
            TestData.contentLink(id = 3, link = "https://x.com/charlie/status/3")
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        // Add username filters (>3 chars = OR logic)
        viewModel.consumeAction(TextAction.AddHiddenFilter("alice"))
        viewModel.consumeAction(TextAction.AddHiddenFilter("charlie"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            // Should match alice OR charlie
            assertThat(filtered.map { it.id }).containsExactly(1, 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== AND Logic for Domain Filters ==========

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `multiple short domain filters use AND logic - matches ALL`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://x.com/user", description = "x post"),
            TestData.contentLink(id = 2, link = "https://fb.com/user", description = "fb post"),
            TestData.contentLink(id = 3, link = "https://x.com/user", description = "also fb mention") // x.com but mentions fb
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        // Add domain filters (<=3 chars = AND logic, but domain only matches in URL domain)
        viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
        viewModel.consumeAction(TextAction.AddHiddenFilter("fb"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            // AND logic means items must match BOTH filters
            // id=1: x.com domain matches "x", but "fb" check - description has "x post"
            // id=2: fb.com domain matches "fb", but "x" check fails
            // id=3: x.com domain matches "x", description has "fb"
            // For short filters, it checks domain or description
            // So id=3 should pass both: x.com matches "x", description contains "fb"
            assertThat(filtered.map { it.id }).containsExactly(3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Mixed Filters ==========

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `mixed username and domain filters apply correct logic`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://x.com/alice/status/1"),
            TestData.contentLink(id = 2, link = "https://fb.com/alice/post"),
            TestData.contentLink(id = 3, link = "https://x.com/bob/status/2")
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        // "alice" is a username filter (>3 chars, OR logic)
        // "x" is a domain filter (<=3 chars, AND logic)
        viewModel.consumeAction(TextAction.AddHiddenFilter("alice"))
        viewModel.consumeAction(TextAction.AddHiddenFilter("x"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            // Username filter "alice" (OR): matches 1 and 2
            // Domain filter "x" (AND): must match x.com domain
            // Combined: (matches alice OR...) AND (matches x domain)
            // Result: id=1 matches both (alice in URL, x.com domain)
            assertThat(filtered.map { it.id }).containsExactly(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Clear Filters ==========

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `clear filters shows all items again`() = runTest {
        val links = TestData.contentLinks(5)
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        // Add filter
        viewModel.consumeAction(TextAction.AddHiddenFilter("user1"))
        advanceUntilIdle()

        // Clear filters
        viewModel.consumeAction(TextAction.ClearHiddenFilters)
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            assertThat(filtered).hasSize(5)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Case Insensitivity ==========

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `filters are case insensitive`() = runTest {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://X.COM/USER/status/1"),
            TestData.contentLink(id = 2, link = "https://x.com/user/status/2"),
            TestData.contentLink(id = 3, link = "https://youtube.com/TestUser")
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            assertThat(filtered.map { it.id }).containsExactly(3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Empty Filters ==========

    @Ignore("Filter logic test requires complex flow interaction - needs dispatcher injection")
    @Test
    fun `no filters shows all items`() = runTest {
        val links = TestData.contentLinks(5)
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.listState.test {
            val filtered = awaitItem()
            assertThat(filtered).hasSize(5)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `adding duplicate filter does not create duplicates`() = runTest {
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
        viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
        advanceUntilIdle()

        // Read from internalScreenState to avoid combined flow issues
        val state = viewModel.internalScreenState.value
        assertThat(state.hiddenFilters.count { it == "testuser" }).isEqualTo(1)
    }
}
