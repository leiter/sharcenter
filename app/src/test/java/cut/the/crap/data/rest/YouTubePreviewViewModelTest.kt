package cut.the.crap.data.rest

import app.cash.turbine.test
import cut.the.crap.fake.FakeYouTubeRepository
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.testutils.TestData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YouTubePreviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var repository: FakeYouTubeRepository
    private lateinit var viewModel: YouTubePreviewViewModel

    @Before
    fun setup() {
        repository = FakeYouTubeRepository()
        viewModel = YouTubePreviewViewModel(repository)
    }

    @Test
    fun `initial state is Idle`() = runTest(testDispatcher) {
        assertThat(viewModel.state.value).isEqualTo(YouTubePreviewState.Idle)
    }

    @Test
    fun `fetchVideoMetadata returns Success with correct metadata`() = runTest(testDispatcher) {
        val metadata = TestData.youtubeMetadata(
            videoId = "test123",
            title = "Test Video"
        )
        repository.setSuccessResponse(metadata)

        // Initial state
        assertThat(viewModel.state.value).isInstanceOf(YouTubePreviewState.Idle::class.java)

        // Trigger fetch and run to completion
        viewModel.fetchVideoMetadata("https://www.youtube.com/watch?v=test123")
        advanceUntilIdle()

        // Check Success state
        val successState = viewModel.state.value as YouTubePreviewState.Success
        assertThat(successState.metadata.videoId).isEqualTo("test123")
        assertThat(successState.metadata.title).isEqualTo("Test Video")
    }

    @Test
    fun `fetchVideoMetadata returns Error on failure`() = runTest(testDispatcher) {
        val errorMessage = "Video not found"
        repository.setErrorResponse(errorMessage)

        // Initial state
        assertThat(viewModel.state.value).isInstanceOf(YouTubePreviewState.Idle::class.java)

        // Trigger fetch and run to completion
        viewModel.fetchVideoMetadata("https://www.youtube.com/watch?v=invalid")
        advanceUntilIdle()

        // Check Error state
        val errorState = viewModel.state.value as YouTubePreviewState.Error
        assertThat(errorState.message).isEqualTo(errorMessage)
    }

    @Test
    fun `fetchVideoMetadataById returns Success with correct metadata`() = runTest(testDispatcher) {
        val metadata = TestData.youtubeMetadata(
            videoId = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up"
        )
        repository.setSuccessResponse(metadata)

        // Initial state
        assertThat(viewModel.state.value).isInstanceOf(YouTubePreviewState.Idle::class.java)

        // Trigger fetch and run to completion
        viewModel.fetchVideoMetadataById("dQw4w9WgXcQ")
        advanceUntilIdle()

        // Check Success state
        val successState = viewModel.state.value as YouTubePreviewState.Success
        assertThat(successState.metadata.videoId).isEqualTo("dQw4w9WgXcQ")
        assertThat(successState.metadata.title).isEqualTo("Never Gonna Give You Up")
    }

    @Test
    fun `fetchVideoMetadataById returns Error on failure`() = runTest(testDispatcher) {
        val errorMessage = "Invalid video ID"
        repository.setErrorResponse(errorMessage)

        // Initial state
        assertThat(viewModel.state.value).isInstanceOf(YouTubePreviewState.Idle::class.java)

        // Trigger fetch and run to completion
        viewModel.fetchVideoMetadataById("invalid")
        advanceUntilIdle()

        // Check Error state
        val errorState = viewModel.state.value as YouTubePreviewState.Error
        assertThat(errorState.message).isEqualTo(errorMessage)
    }

    @Test
    fun `reset returns state to Idle from Success`() = runTest(testDispatcher) {
        val metadata = TestData.youtubeMetadata()
        repository.setSuccessResponse(metadata)

        // Get to Success state
        viewModel.fetchVideoMetadataById("test123")
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(YouTubePreviewState.Success::class.java)

        // Reset
        viewModel.reset()
        assertThat(viewModel.state.value).isInstanceOf(YouTubePreviewState.Idle::class.java)
    }

    @Test
    fun `reset returns state to Idle from Error`() = runTest(testDispatcher) {
        repository.setErrorResponse("Some error")

        // Get to Error state
        viewModel.fetchVideoMetadataById("test123")
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(YouTubePreviewState.Error::class.java)

        // Reset
        viewModel.reset()
        assertThat(viewModel.state.value).isInstanceOf(YouTubePreviewState.Idle::class.java)
    }

    @Test
    fun `fetchVideoMetadata records URL in repository`() = runTest(testDispatcher) {
        val url = "https://www.youtube.com/watch?v=abc123"
        viewModel.fetchVideoMetadata(url)
        advanceUntilIdle()

        assertThat(repository.getFetchedUrls()).contains(url)
    }

    @Test
    fun `fetchVideoMetadataById records video ID in repository`() = runTest(testDispatcher) {
        val videoId = "abc123xyz"
        viewModel.fetchVideoMetadataById(videoId)
        advanceUntilIdle()

        assertThat(repository.getFetchedIds()).contains(videoId)
    }
}
