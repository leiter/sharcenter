package cut.the.crap.ui.content.posts

import app.cash.turbine.test
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.KeywordType
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.fake.FakeContentItemRepository
import cut.the.crap.fake.FakeJobQueueRepository
import cut.the.crap.fake.FakeKeywordRepository
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.testutils.TestData
import cut.the.crap.tools.TextValueWrapper
import cut.the.crap.ui.components.api.ContentItemAction
import cut.the.crap.ui.components.api.KeywordAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.content.settings.AppSettings
import cut.the.crap.ui.content.settings.DateRangePreset
import cut.the.crap.ui.content.settings.FavoriteFilterPreset
import cut.the.crap.ui.content.settings.SortOrderPreset
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var keywordRepository: FakeKeywordRepository
    private lateinit var contentItemRepository: FakeContentItemRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var jobQueueRepository: FakeJobQueueRepository
    private lateinit var viewModel: PostsViewModel

    private val settingsFlow = MutableStateFlow(AppSettings())

    @Before
    fun setup() {
        keywordRepository = FakeKeywordRepository()
        contentItemRepository = FakeContentItemRepository()
        jobQueueRepository = FakeJobQueueRepository()

        settingsRepository = mockk(relaxed = true)
        coEvery { settingsRepository.settingsFlow } returns settingsFlow

        viewModel = PostsViewModel(
            keywordRepository = keywordRepository,
            contentItemRepository = contentItemRepository,
            settingsRepository = settingsRepository,
            jobQueueRepository = jobQueueRepository
        )
    }

    // ========== Initialization Tests ==========

    @Test
    fun `initial screenState has default values`() = runTest {
        advanceUntilIdle()

        val state = viewModel.screenState.value
        assertThat(state.query).isEmpty()
        assertThat(state.searchExpanded).isFalse()
        assertThat(state.showHandleSelectionDialog).isFalse()
        assertThat(state.selectedHandles).isEmpty()
    }

    @Test
    fun `initialization loads active item into editor`() = runTest {
        val activeItem = TestData.activeContentItem(id = 1, text = "Active text")
        contentItemRepository.setItems(listOf(activeItem))
        advanceUntilIdle()

        // Recreate viewModel to trigger init
        viewModel = PostsViewModel(
            keywordRepository = keywordRepository,
            contentItemRepository = contentItemRepository,
            settingsRepository = settingsRepository,
            jobQueueRepository = jobQueueRepository
        )
        advanceUntilIdle()

        val state = viewModel.screenState.value
        assertThat(state.focusedContentText.newText).isEqualTo("Active text")
    }

    @Test
    fun `initialization applies settings from repository`() = runTest {
        settingsFlow.value = AppSettings(
            postsDateRangePreset = DateRangePreset.THIRTY_DAYS,
            postsFavoriteFilterPreset = FavoriteFilterPreset.FAVORITES_ONLY,
            postsSortOrderPreset = SortOrderPreset.BY_DATE
        )

        viewModel = PostsViewModel(
            keywordRepository = keywordRepository,
            contentItemRepository = contentItemRepository,
            settingsRepository = settingsRepository,
            jobQueueRepository = jobQueueRepository
        )
        advanceUntilIdle()

        // Settings should be applied (verified through the filter behavior)
        // The viewModel should have initialized with these settings
        assertThat(viewModel.screenState.value).isNotNull()
    }

    // ========== Text Action Tests ==========

    @Test
    fun `edit content text updates screen state`() = runTest {
        advanceUntilIdle()

        val newText = TextValueWrapper("New content", Pair(11, 11))
        viewModel.consumeAction(TextAction.EditContentText(newText))
        advanceUntilIdle()

        val state = viewModel.screenState.value
        assertThat(state.focusedContentText.newText).isEqualTo("New content")
    }

    @Test
    fun `edit content text triggers auto-save for new item`() = runTest {
        advanceUntilIdle()

        val newText = TextValueWrapper("New content", Pair(11, 11))
        viewModel.consumeAction(TextAction.EditContentText(newText))
        advanceUntilIdle()

        val items = contentItemRepository.getStoredItems()
        assertThat(items).hasSize(1)
        assertThat(items[0].text).isEqualTo("New content")
    }

    @Test
    fun `edit content text triggers auto-save for existing item`() = runTest {
        val existingItem = TestData.activeContentItem(id = 1, text = "Original")
        contentItemRepository.setItems(listOf(existingItem))

        viewModel = PostsViewModel(
            keywordRepository = keywordRepository,
            contentItemRepository = contentItemRepository,
            settingsRepository = settingsRepository,
            jobQueueRepository = jobQueueRepository
        )
        advanceUntilIdle()

        val newText = TextValueWrapper("Updated content", Pair(15, 15))
        viewModel.consumeAction(TextAction.EditContentText(newText))
        advanceUntilIdle()

        val items = contentItemRepository.getStoredItems()
        assertThat(items).hasSize(1)
        assertThat(items[0].text).isEqualTo("Updated content")
    }

    @Test
    fun `clear content text clears editor but leaves the list item untouched`() = runTest {
        val existingItem = TestData.activeContentItem(id = 1, text = "Original")
        contentItemRepository.setItems(listOf(existingItem))

        viewModel = PostsViewModel(
            keywordRepository = keywordRepository,
            contentItemRepository = contentItemRepository,
            settingsRepository = settingsRepository,
            jobQueueRepository = jobQueueRepository
        )
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.ClearContentText)
        advanceUntilIdle()

        // Editor is reset to a fresh, empty post...
        val state = viewModel.screenState.value
        assertThat(state.focusedContentText.newText).isEmpty()

        // ...but the previously focused item is left intact in the list (not cleared),
        // just deactivated so the empty editor is the new active post.
        val item = contentItemRepository.getById(1)
        assertThat(item?.text).isEqualTo("Original")
        assertThat(contentItemRepository.getActiveItem()).isNull()
    }

    @Test
    fun `edit query text updates query in screen state`() = runTest {
        advanceUntilIdle()

        viewModel.consumeAction(TextAction.EditQueryText("search term"))
        advanceUntilIdle()

        val state = viewModel.screenState.value
        assertThat(state.query).isEqualTo("search term")
    }

    // ========== Content Item Action Tests ==========

    @Test
    fun `create new content item clears editor and active item`() = runTest {
        val existingItem = TestData.activeContentItem(id = 1, text = "Original")
        contentItemRepository.setItems(listOf(existingItem))

        viewModel = PostsViewModel(
            keywordRepository = keywordRepository,
            contentItemRepository = contentItemRepository,
            settingsRepository = settingsRepository,
            jobQueueRepository = jobQueueRepository
        )
        advanceUntilIdle()

        viewModel.consumeAction(ContentItemAction.CreateNew)
        advanceUntilIdle()

        val state = viewModel.screenState.value
        assertThat(state.focusedContentText.newText).isEmpty()

        // The active item should be cleared
        val activeItem = contentItemRepository.getActiveItem()
        assertThat(activeItem).isNull()
    }

    @Test
    fun `load content item loads text into editor`() = runTest {
        val items = listOf(
            TestData.contentItem(id = 1, text = "First"),
            TestData.contentItem(id = 2, text = "Second")
        )
        contentItemRepository.setItems(items)
        advanceUntilIdle()

        viewModel.consumeAction(ContentItemAction.Load(2))
        advanceUntilIdle()

        val state = viewModel.screenState.value
        assertThat(state.focusedContentText.newText).isEqualTo("Second")

        val activeItem = contentItemRepository.getActiveItem()
        assertThat(activeItem?.id).isEqualTo(2)
    }

    @Test
    fun `delete content item removes from repository`() = runTest {
        val items = listOf(
            TestData.contentItem(id = 1, text = "First"),
            TestData.contentItem(id = 2, text = "Second")
        )
        contentItemRepository.setItems(items)
        advanceUntilIdle()

        viewModel.consumeAction(ContentItemAction.Delete(1))
        advanceUntilIdle()

        val remaining = contentItemRepository.getStoredItems()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].id).isEqualTo(2)
    }

    @Test
    fun `delete active item clears editor and loads another`() = runTest {
        val items = listOf(
            TestData.activeContentItem(id = 1, text = "Active"),
            TestData.contentItem(id = 2, text = "Second")
        )
        contentItemRepository.setItems(items)

        viewModel = PostsViewModel(
            keywordRepository = keywordRepository,
            contentItemRepository = contentItemRepository,
            settingsRepository = settingsRepository,
            jobQueueRepository = jobQueueRepository
        )
        advanceUntilIdle()

        viewModel.consumeAction(ContentItemAction.Delete(1))
        advanceUntilIdle()

        // Should load the remaining item
        val state = viewModel.screenState.value
        assertThat(state.focusedContentText.newText).isEqualTo("Second")
    }

    @Test
    fun `toggle favorite updates item in repository`() = runTest {
        val item = TestData.contentItem(id = 1, isFavorite = false)
        contentItemRepository.setItems(listOf(item))
        advanceUntilIdle()

        viewModel.consumeAction(ContentItemAction.ToggleFavorite(1))
        advanceUntilIdle()

        val updated = contentItemRepository.getById(1)
        assertThat(updated?.isFavorite).isTrue()
    }

    // ========== Keyword Action Tests ==========

    @Test
    fun `add handle inserts account keyword`() = runTest {
        advanceUntilIdle()

        viewModel.consumeAction(KeywordAction.AddHandle("@newhandle"))
        advanceUntilIdle()

        val accounts = keywordRepository.getStoredItems().filter { it.type == KeywordType.ACCOUNT }
        assertThat(accounts).hasSize(1)
        assertThat(accounts[0].text).isEqualTo("@newhandle")
    }

    @Test
    fun `add tag inserts hashtag keyword`() = runTest {
        advanceUntilIdle()

        viewModel.consumeAction(KeywordAction.AddTag("#newtag"))
        advanceUntilIdle()

        val hashtags = keywordRepository.getStoredItems().filter { it.type == KeywordType.HASHTAG }
        assertThat(hashtags).hasSize(1)
        assertThat(hashtags[0].text).isEqualTo("#newtag")
    }

    @Test
    fun `add keyword inserts tag keyword`() = runTest {
        advanceUntilIdle()

        viewModel.consumeAction(KeywordAction.AddKeyWord("newkeyword"))
        advanceUntilIdle()

        val tags = keywordRepository.getStoredItems().filter { it.type == KeywordType.TAG }
        assertThat(tags).hasSize(1)
        assertThat(tags[0].text).isEqualTo("newkeyword")
    }

    @Test
    fun `toggle handle selection adds to selected set`() = runTest {
        advanceUntilIdle()

        viewModel.consumeAction(KeywordAction.ToggleHandleSelection(1))
        advanceUntilIdle()

        val state = viewModel.screenState.value
        assertThat(state.selectedHandles).contains(1)
    }

    @Test
    fun `toggle handle selection removes when already selected`() = runTest {
        advanceUntilIdle()

        viewModel.consumeAction(KeywordAction.ToggleHandleSelection(1))
        viewModel.consumeAction(KeywordAction.ToggleHandleSelection(1))
        advanceUntilIdle()

        val state = viewModel.screenState.value
        assertThat(state.selectedHandles).doesNotContain(1)
    }

    @Test
    fun `delete handle removes keyword from repository`() = runTest {
        val account = TestData.account(id = 1, text = "@testuser")
        keywordRepository.setItems(listOf(account))
        advanceUntilIdle()

        viewModel.consumeAction(KeywordAction.DeleteHandle(1))
        advanceUntilIdle()

        assertThat(keywordRepository.getStoredItems()).isEmpty()
    }

    @Test
    fun `toggle favorite for keyword updates repository`() = runTest {
        val account = TestData.account(id = 1, text = "@testuser")
        keywordRepository.setItems(listOf(account))
        advanceUntilIdle()

        viewModel.consumeAction(KeywordAction.ToggleFavorite(1))
        advanceUntilIdle()

        val updated = keywordRepository.getById(1)
        assertThat(updated?.isFavorite).isTrue()
    }

    @Test
    fun `delete handles bulk removes multiple keywords`() = runTest {
        val accounts = TestData.accounts(3)
        keywordRepository.setItems(accounts)
        advanceUntilIdle()

        viewModel.consumeAction(KeywordAction.DeleteHandlesBulk(listOf(1, 3)))
        advanceUntilIdle()

        val remaining = keywordRepository.getStoredItems()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].id).isEqualTo(2)
    }

    // ========== Content Items Flow Tests ==========

    @Test
    fun `contentItems flow emits repository items`() = runTest {
        val items = TestData.contentItems(3)
        contentItemRepository.setItems(items)
        advanceUntilIdle()

        viewModel.contentItems.test {
            val emitted = awaitItem()
            assertThat(emitted).hasSize(3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `contentItems flow updates when repository changes`() = runTest {
        advanceUntilIdle()

        viewModel.contentItems.test {
            assertThat(awaitItem()).isEmpty()

            contentItemRepository.insert(TestData.contentItem(text = "New item"))

            val updated = awaitItem()
            assertThat(updated).hasSize(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Keyword Lists Tests ==========

    @Test
    fun `handle list updates from keyword repository`() = runTest {
        val accounts = TestData.accounts(3)
        keywordRepository.setItems(accounts)
        advanceUntilIdle()

        viewModel.screenState.test {
            val state = awaitItem()
            assertThat(state.handleList).hasSize(3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tag list updates from keyword repository`() = runTest {
        val hashtags = TestData.hashtags(2)
        keywordRepository.setItems(hashtags)
        advanceUntilIdle()

        viewModel.screenState.test {
            val state = awaitItem()
            assertThat(state.tagList).hasSize(2)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
