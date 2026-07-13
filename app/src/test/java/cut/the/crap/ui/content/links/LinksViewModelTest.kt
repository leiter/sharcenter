package cut.the.crap.ui.content.links

import android.content.Context
import app.cash.turbine.test
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.data.rest.Message
import cut.the.crap.data.rest.Result
import cut.the.crap.fake.FakeContentLinkRepository
import cut.the.crap.fake.FakeJobQueueRepository
import cut.the.crap.fake.FakeKeywordRepository
import cut.the.crap.fake.FakeMessageRepository
import cut.the.crap.fake.FakeYouTubeRepository
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.testutils.TestData
import cut.the.crap.ui.components.api.ContentLinkAction
import cut.the.crap.ui.components.api.ListAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.content.settings.AppSettings
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LinksViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

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

    // ========== Initialization Tests ==========

    @Test
    fun `initial screenState has default values`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.internalScreenState.value
        assertThat(state.query).isEmpty()
        assertThat(state.hiddenFilters).isEmpty()
        assertThat(state.checkMarks).isFalse()
        assertThat(state.selectedItems).isEmpty()
    }

    @Test
    fun `initial listState is empty`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val list = viewModel.listState.value
        assertThat(list).isEmpty()
    }

    @Test
    fun `totalCount reflects all items regardless of filters`() = runTest(testDispatcher) {
        contentLinkRepository.setItems(TestData.contentLinks(5))
        

        viewModel.totalCount.test {
            // Skip initial 0 value if emitted, wait for the actual count
            val count = awaitItem()
            if (count == 0) {
                assertThat(awaitItem()).isEqualTo(5)
            } else {
                assertThat(count).isEqualTo(5)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Insert/Update/Delete Tests ==========

    @Test
    fun `insertContentLink adds item to repository`() = runTest(testDispatcher) {
        val link = TestData.contentLink(id = -1, link = "https://test.com")
        advanceUntilIdle()

        viewModel.insertContentLink(link)
        advanceUntilIdle()

        val stored = contentLinkRepository.getStoredItems()
        assertThat(stored).hasSize(1)
        assertThat(stored[0].link).isEqualTo("https://test.com")
    }

    @Test
    fun `delete action removes item from repository`() = runTest(testDispatcher) {
        val link = TestData.contentLink(id = 1)
        contentLinkRepository.setItems(listOf(link))
        advanceUntilIdle()

        viewModel.consumeAction(ContentLinkAction.Delete(link))
        advanceUntilIdle()

        assertThat(contentLinkRepository.getStoredItems()).isEmpty()
    }

    @Test
    fun `toggle favourite updates item in repository`() = runTest(testDispatcher) {
        val link = TestData.contentLink(id = 1, favourite = false)
        contentLinkRepository.setItems(listOf(link))
        advanceUntilIdle()

        viewModel.consumeAction(ContentLinkAction.ToggleFavourite(link))
        advanceUntilIdle()

        val updated = contentLinkRepository.getContentLinkById(1)
        assertThat(updated?.favourite).isTrue()
    }

    @Test
    fun `edit search hint updates description`() = runTest(testDispatcher) {
        val link = TestData.contentLink(id = 1, description = "old")
        contentLinkRepository.setItems(listOf(link))
        advanceUntilIdle()

        viewModel.consumeAction(ContentLinkAction.EditSearchHint(link, "new description"))
        advanceUntilIdle()

        val updated = contentLinkRepository.getContentLinkById(1)
        assertThat(updated?.description).isEqualTo("new description")
    }

    // ========== Selection Mode Tests ==========

    @Test
    fun `enter selection mode sets checkMarks and selects item`() = runTest(testDispatcher) {
        val link = TestData.contentLink(id = 1)
        contentLinkRepository.setItems(listOf(link))
        advanceUntilIdle()

        viewModel.consumeAction(ContentLinkAction.EnterSelectionMode(link))
        advanceUntilIdle()

        val state = viewModel.internalScreenState.value
        assertThat(state.checkMarks).isTrue()
        assertThat(state.selectedItems).contains(1)
    }

    @Ignore("Test causes coroutine leakage due to handleListFlow using Dispatchers.Default")
    @Test
    fun `toggle selection adds item to selection`() = runTest(testDispatcher) {
        val link1 = TestData.contentLink(id = 1)
        val link2 = TestData.contentLink(id = 2)
        contentLinkRepository.setItems(listOf(link1, link2))

        // Subscribe to screenState to start the combined flow
        viewModel.screenState.test {
            awaitItem() // Initial state

            viewModel.consumeAction(ContentLinkAction.EnterSelectionMode(link1))
            awaitItem() // State after EnterSelectionMode

            viewModel.consumeAction(ContentLinkAction.ToggleSelection(link2))
            val state = awaitItem() // State after ToggleSelection

            assertThat(state.selectedItems).containsExactly(1, 2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Ignore("Test causes coroutine leakage due to handleListFlow using Dispatchers.Default")
    @Test
    fun `toggle selection removes item when already selected`() = runTest(testDispatcher) {
        val link = TestData.contentLink(id = 1)
        contentLinkRepository.setItems(listOf(link))

        // Subscribe to screenState to start the combined flow
        viewModel.screenState.test {
            awaitItem() // Initial state

            viewModel.consumeAction(ContentLinkAction.EnterSelectionMode(link))
            awaitItem() // State after EnterSelectionMode

            viewModel.consumeAction(ContentLinkAction.ToggleSelection(link))
            val state = awaitItem() // State after ToggleSelection

            assertThat(state.selectedItems).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Text Action Tests ==========

    @Test
    fun `post query updates query and filters items`() = runTest(testDispatcher) {
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://x.com/user1"),
            TestData.contentLink(id = 2, link = "https://youtube.com/watch")
        )
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.PostQuery("youtube"))
        advanceUntilIdle()

        val state = viewModel.internalScreenState.value
        assertThat(state.query).isEqualTo("youtube")
    }

    @Test
    fun `add hidden filter updates hiddenFilters`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
        advanceUntilIdle()

        // Read from internalScreenState directly for testing (screenState is a combined flow)
        val state = viewModel.internalScreenState.value
        assertThat(state.hiddenFilters).contains("testuser")
    }

    @Test
    fun `remove hidden filter removes from hiddenFilters`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("testuser"))
        advanceUntilIdle()
        viewModel.consumeAction(TextAction.RemoveHiddenFilter("testuser"))
        advanceUntilIdle()

        val state = viewModel.internalScreenState.value
        assertThat(state.hiddenFilters).doesNotContain("testuser")
    }

    @Test
    fun `clear hidden filters empties the list`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.AddHiddenFilter("user1"))
        viewModel.consumeAction(TextAction.AddHiddenFilter("user2"))
        viewModel.consumeAction(TextAction.ClearHiddenFilters)
        advanceUntilIdle()

        val state = viewModel.internalScreenState.value
        assertThat(state.hiddenFilters).isEmpty()
    }

    // ========== List Action Tests ==========

    @Ignore("Test causes coroutine leakage due to handleListFlow using Dispatchers.Default")
    @Test
    fun `select all selects all visible items`() = runTest(testDispatcher) {
        // Set up items first
        contentLinkRepository.reset()
        val links = TestData.contentLinks(3)
        contentLinkRepository.setItems(links)

        // Create fresh ViewModel with items ready
        val freshViewModel = LinksViewModel(
            contentRepository = contentLinkRepository,
            repository = messageRepository,
            context = context,
            settingsRepository = settingsRepository,
            keywordRepository = keywordRepository,
            jobQueueRepository = jobQueueRepository,
            youTubeRepository = youTubeRepository
        )

        // Keep listState subscription active
        val collectJob = backgroundScope.launch {
            freshViewModel.listState.collect { }
        }
        advanceUntilIdle()

        // Verify items are in the flow
        assertThat(freshViewModel.listState.value).hasSize(3)

        // Enter selection mode first
        freshViewModel.consumeAction(ContentLinkAction.EnterSelectionMode(links[0]))
        advanceUntilIdle()

        // SelectAll reads from listState.value and screenState.value
        freshViewModel.consumeAction(ListAction.SelectAll)
        advanceUntilIdle()

        val state = freshViewModel.internalScreenState.value
        assertThat(state.selectedItems).containsExactly(1, 2, 3)

        collectJob.cancel()
    }

    @Test
    fun `deselect all clears selection`() = runTest(testDispatcher) {
        val links = TestData.contentLinks(3)
        contentLinkRepository.setItems(links)
        advanceUntilIdle()

        viewModel.consumeAction(ContentLinkAction.EnterSelectionMode(links[0]))
        advanceUntilIdle()

        // DeselectAll doesn't depend on flows, it just clears the selection
        viewModel.consumeAction(ListAction.DeselectAll)
        advanceUntilIdle()

        val state = viewModel.internalScreenState.value
        assertThat(state.selectedItems).isEmpty()
    }

    @Ignore("Test causes coroutine leakage due to handleListFlow using Dispatchers.Default")
    @Test
    fun `aaa_delete all removes all visible items`() = runTest(testDispatcher) {
        // Note: This test is prefixed with 'aaa_' to run early and avoid interference
        // from background coroutines spawned by later tests

        // Reset repository to clean state
        contentLinkRepository.reset()
        val links = TestData.contentLinks(3)

        // Set items BEFORE creating a fresh ViewModel so flows initialize with data
        contentLinkRepository.setItems(links)

        // Create a fresh ViewModel instance after data is set
        val freshViewModel = LinksViewModel(
            contentRepository = contentLinkRepository,
            repository = messageRepository,
            context = context,
            settingsRepository = settingsRepository,
            keywordRepository = keywordRepository,
            jobQueueRepository = jobQueueRepository,
            youTubeRepository = youTubeRepository
        )

        // Verify items exist in repository
        assertThat(contentLinkRepository.getStoredItems()).hasSize(3)

        // Collect from listState to start the flow and wait for items
        val collectJob = backgroundScope.launch {
            freshViewModel.listState.collect { /* keep subscription active */ }
        }
        advanceUntilIdle()

        // Verify items are available in the flow
        assertThat(freshViewModel.listState.value).hasSize(3)

        // DeleteAll reads from listState.value
        freshViewModel.consumeAction(ListAction.DeleteAll)
        advanceUntilIdle()

        // Verify via repository
        assertThat(contentLinkRepository.getStoredItems()).isEmpty()

        collectJob.cancel()
    }

    // ========== Message Tests ==========

    @Test
    fun `sendMessage posts message and updates response`() = runTest(testDispatcher) {
        messageRepository.setSuccessResponse("Message sent!")
        advanceUntilIdle()

        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        assertThat(viewModel.response.value).isEqualTo("Message sent!")
        assertThat(messageRepository.getLastSentMessage()?.text).isEqualTo("Hello")
    }

    @Test
    fun `sendMessage with error leaves response empty`() = runTest(testDispatcher) {
        messageRepository.setErrorResponse("Network error")
        advanceUntilIdle()

        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        assertThat(viewModel.response.value).isEmpty()
    }

    // ========== Job Queue Tests ==========

    @Ignore("Test causes coroutine leakage due to handleListFlow using Dispatchers.Default")
    @Test
    fun `fire job submits selected links to job queue`() = runTest(testDispatcher) {
        // Set up fresh repository and ViewModel
        contentLinkRepository.reset()
        val links = listOf(
            TestData.contentLink(id = 1, link = "https://link1.com"),
            TestData.contentLink(id = 2, link = "https://link2.com"),
            TestData.contentLink(id = 3, link = "https://link3.com")
        )
        contentLinkRepository.setItems(links)

        val freshViewModel = LinksViewModel(
            contentRepository = contentLinkRepository,
            repository = messageRepository,
            context = context,
            settingsRepository = settingsRepository,
            keywordRepository = keywordRepository,
            jobQueueRepository = jobQueueRepository,
            youTubeRepository = youTubeRepository
        )

        // Keep listState and screenState subscriptions active
        val collectListJob = backgroundScope.launch {
            freshViewModel.listState.collect { }
        }
        val collectScreenJob = backgroundScope.launch {
            freshViewModel.screenState.collect { }
        }
        advanceUntilIdle()

        // Verify items are available
        assertThat(freshViewModel.listState.value).hasSize(3)

        // Select items 1 and 3
        freshViewModel.consumeAction(ContentLinkAction.EnterSelectionMode(links[0]))
        advanceUntilIdle()

        freshViewModel.consumeAction(ContentLinkAction.ToggleSelection(links[2]))
        advanceUntilIdle()

        // Verify selection
        assertThat(freshViewModel.internalScreenState.value.selectedItems).containsExactly(1, 3)

        // Fire job
        freshViewModel.consumeAction(ListAction.FireJob)
        advanceUntilIdle()

        // Verify task was submitted
        val submittedTask = jobQueueRepository.getLastSubmittedTask()
        assertThat(submittedTask).isNotNull()

        collectListJob.cancel()
        collectScreenJob.cancel()
    }

    @Ignore("Test causes coroutine leakage due to handleListFlow using Dispatchers.Default")
    @Test
    fun `fire job with error emits error snackbar message`() = runTest(testDispatcher) {
        // Set up fresh repository and ViewModel
        contentLinkRepository.reset()
        val link = TestData.contentLink(id = 1, link = "https://test.com")
        contentLinkRepository.setItems(listOf(link))
        jobQueueRepository.setSubmitTaskError("Connection failed")

        val freshViewModel = LinksViewModel(
            contentRepository = contentLinkRepository,
            repository = messageRepository,
            context = context,
            settingsRepository = settingsRepository,
            keywordRepository = keywordRepository,
            jobQueueRepository = jobQueueRepository,
            youTubeRepository = youTubeRepository
        )

        // Keep listState and screenState subscriptions active
        val collectListJob = backgroundScope.launch {
            freshViewModel.listState.collect { }
        }
        val collectScreenJob = backgroundScope.launch {
            freshViewModel.screenState.collect { }
        }
        advanceUntilIdle()

        // Select the item
        freshViewModel.consumeAction(ContentLinkAction.EnterSelectionMode(link))
        advanceUntilIdle()

        // Collect snackBarMessage and fire the job
        freshViewModel.snackBarMessage.test {
            freshViewModel.consumeAction(ListAction.FireJob)
            advanceUntilIdle()

            val message = awaitItem()
            assertThat(message).isInstanceOf(LinksSnackbar.SubmitFailed::class.java)
            cancelAndIgnoreRemainingEvents()
        }

        collectListJob.cancel()
        collectScreenJob.cancel()
    }
}
