package cut.the.crap.data.rest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Example ViewModel demonstrating YouTube metadata fetching
 *
 * Usage in a Composable:
 * ```
 * @Composable
 * fun YouTubePreviewScreen(viewModel: YouTubePreviewViewModel = koinViewModel()) {
 *     val state by viewModel.state.collectAsState()
 *
 *     when (state) {
 *         is YouTubePreviewState.Idle -> { /* Show initial UI */ }
 *         is YouTubePreviewState.Loading -> { /* Show loading */ }
 *         is YouTubePreviewState.Success -> {
 *             val metadata = (state as YouTubePreviewState.Success).metadata
 *             // Display video preview card
 *         }
 *         is YouTubePreviewState.Error -> {
 *             val error = (state as YouTubePreviewState.Error).error.localized()
 *             // Display error message
 *         }
 *     }
 * }
 * ```
 */
class YouTubePreviewViewModel constructor(
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {

    private val _state = MutableStateFlow<YouTubePreviewState>(YouTubePreviewState.Idle)
    val state: StateFlow<YouTubePreviewState> = _state.asStateFlow()

    /**
     * Fetches metadata for a YouTube video from any valid YouTube URL
     *
     * @param url The YouTube URL (supports all common formats)
     */
    fun fetchVideoMetadata(url: String) {
        viewModelScope.launch {
            _state.value = YouTubePreviewState.Loading

            when (val result = youTubeRepository.getVideoMetadata(url)) {
                is Result.Success -> {
                    _state.value = YouTubePreviewState.Success(result.data)
                }
                is Result.Error -> {
                    _state.value = YouTubePreviewState.Error(result.error)
                }
            }
        }
    }

    /**
     * Fetches metadata for a YouTube video from a video ID
     *
     * @param videoId The 11-character YouTube video ID
     */
    fun fetchVideoMetadataById(videoId: String) {
        viewModelScope.launch {
            _state.value = YouTubePreviewState.Loading

            when (val result = youTubeRepository.getVideoMetadataById(videoId)) {
                is Result.Success -> {
                    _state.value = YouTubePreviewState.Success(result.data)
                }
                is Result.Error -> {
                    _state.value = YouTubePreviewState.Error(result.error)
                }
            }
        }
    }

    /**
     * Resets the state back to idle
     */
    fun reset() {
        _state.value = YouTubePreviewState.Idle
    }
}

/**
 * UI state for YouTube video preview
 */
sealed class YouTubePreviewState {
    /**
     * Initial state, no video has been requested
     */
    data object Idle : YouTubePreviewState()

    /**
     * Loading video metadata
     */
    data object Loading : YouTubePreviewState()

    /**
     * Successfully fetched video metadata
     */
    data class Success(val metadata: YouTubeVideoMetadata) : YouTubePreviewState()

    /**
     * Error occurred while fetching metadata
     */
    data class Error(val error: AppError) : YouTubePreviewState()
}
