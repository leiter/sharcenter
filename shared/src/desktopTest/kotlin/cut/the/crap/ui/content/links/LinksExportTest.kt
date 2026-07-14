package cut.the.crap.ui.content.links

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.fake.FakeContentLinkRepository
import cut.the.crap.fake.FakeFileAccess
import cut.the.crap.fake.FakeJobQueueRepository
import cut.the.crap.fake.FakeKeywordRepository
import cut.the.crap.fake.FakeMessageRepository
import cut.the.crap.fake.FakeYouTubeRepository
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.testutils.TestData
import cut.the.crap.ui.content.settings.AppSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers the export path, which until WP6 could not be unit-tested at all: it took a
 * `java.io.OutputStream` that only a real `ContentResolver` could produce, so the only way to
 * exercise it was to tap through the app and then go looking in Downloads.
 *
 * The ViewModel now names the file and hands the text to the [cut.the.crap.platform.FileAccess]
 * seam, so a fake can just record what would have been written.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinksExportTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var contentLinkRepository: FakeContentLinkRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var fileAccess: FakeFileAccess
    private lateinit var viewModel: LinksViewModel

    private val settingsFlow = MutableStateFlow(AppSettings())

    @Before
    fun setup() {
        contentLinkRepository = FakeContentLinkRepository()
        settingsRepository = mockk(relaxed = true)
        coEvery { settingsRepository.settingsFlow } returns settingsFlow
        fileAccess = FakeFileAccess()

        viewModel = LinksViewModel(
            contentRepository = contentLinkRepository,
            repository = FakeMessageRepository(),
            fileAccess = fileAccess,
            settingsRepository = settingsRepository,
            keywordRepository = FakeKeywordRepository(),
            jobQueueRepository = FakeJobQueueRepository(),
            youTubeRepository = FakeYouTubeRepository(),
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
        )
    }

    /**
     * Seeds one link and marks it selected, returning its id.
     *
     * `listState` is a `WhileSubscribed` flow, so it stays empty until something collects it —
     * hence the background collector.
     */
    private fun TestScope.selectOneLink(): Int {
        val link = TestData.xLink(id = 1, username = "jack")
        contentLinkRepository.setItems(listOf(link))
        backgroundScope.launch { viewModel.listState.collect { } }
        advanceUntilIdle()

        viewModel.internalScreenState.value =
            viewModel.internalScreenState.value.copy(selectedItems = listOf(link.id))
        return link.id
    }

    @Test
    fun `exporting with nothing selected writes no file`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val result = viewModel.exportSelectedItems(fileAccess)

        assertThat(result).isEqualTo(LinksSnackbar.ExportNoItemsSelected)
        assertThat(fileAccess.saved).isEmpty()
    }

    @Test
    fun `a failed write surfaces as ExportFailed rather than claiming success`() =
        runTest(testDispatcher) {
            selectOneLink()
            fileAccess.saveFailure = RuntimeException("disk full")

            val result = viewModel.exportSelectedItems(fileAccess)

            assertThat(result).isEqualTo(LinksSnackbar.ExportFailed("disk full"))
            assertThat(fileAccess.saved).isEmpty()
        }

    @Test
    fun `a successful export writes the link into a timestamped txt file`() =
        runTest(testDispatcher) {
            selectOneLink()

            val result = viewModel.exportSelectedItems(fileAccess)

            assertThat(result).isEqualTo(LinksSnackbar.ExportSucceeded(1))
            assertThat(fileAccess.saved).hasSize(1)

            val (fileName, contents) = fileAccess.saved.single()
            // The screen used to build this name; the ViewModel owns it now.
            assertThat(fileName).matches("""links_export_\d{4}-\d{2}-\d{2}_\d{6}\.txt""")
            assertThat(contents).contains("https://x.com/jack/status/1")
        }
}
