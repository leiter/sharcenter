# YouTube oEmbed Integration

This module provides YouTube video metadata fetching using the YouTube oEmbed API (no API key required!).

## Features

- ✅ **No API Key Required** - Uses public oEmbed endpoint
- ✅ **Multiple URL Formats** - Supports all YouTube URL formats
- ✅ **Type-Safe Error Handling** - Uses `Result<T>` wrapper
- ✅ **Comprehensive Metadata** - Title, channel, thumbnails, embed code
- ✅ **URL Validation** - Validates YouTube URLs and video IDs

## Supported URL Formats

The parser supports all common YouTube URL formats:

```kotlin
// Standard watch URLs
https://www.youtube.com/watch?v=dQw4w9WgXcQ
https://youtube.com/watch?v=dQw4w9WgXcQ
https://m.youtube.com/watch?v=dQw4w9WgXcQ

// Short URLs
https://youtu.be/dQw4w9WgXcQ

// Embed URLs
https://www.youtube.com/embed/dQw4w9WgXcQ

// Old-style URLs
https://www.youtube.com/v/dQw4w9WgXcQ
```

## Usage Examples

### 1. Basic Usage in ViewModel

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {

    private val _videoMetadata = MutableStateFlow<YouTubeVideoMetadata?>(null)
    val videoMetadata = _videoMetadata.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun fetchVideoInfo(url: String) {
        viewModelScope.launch {
            when (val result = youTubeRepository.getVideoMetadata(url)) {
                is Result.Success -> {
                    _videoMetadata.value = result.data
                    _error.value = null
                }
                is Result.Error -> {
                    _error.value = result.message
                    _videoMetadata.value = null
                }
            }
        }
    }
}
```

### 2. Using with Composable UI

```kotlin
@Composable
fun YouTubePreviewCard(
    url: String,
    viewModel: MyViewModel = hiltViewModel()
) {
    val metadata by viewModel.videoMetadata.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(url) {
        viewModel.fetchVideoInfo(url)
    }

    when {
        metadata != null -> {
            // Show video preview
            Card {
                Column {
                    AsyncImage(
                        model = metadata!!.thumbnailUrl,
                        contentDescription = metadata!!.title
                    )
                    Text(text = metadata!!.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = metadata!!.channelName, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        error != null -> {
            // Show error message
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
        }
        else -> {
            // Show loading
            CircularProgressIndicator()
        }
    }
}
```

### 3. Direct Repository Usage

```kotlin
suspend fun example(repository: YouTubeRepository) {
    // Using full URL
    val result1 = repository.getVideoMetadata("https://youtu.be/dQw4w9WgXcQ")

    // Using video ID directly
    val result2 = repository.getVideoMetadataById("dQw4w9WgXcQ")

    when (result1) {
        is Result.Success -> {
            val metadata = result1.data
            println("Title: ${metadata.title}")
            println("Channel: ${metadata.channelName}")
            println("Thumbnail: ${metadata.thumbnailUrl}")
        }
        is Result.Error -> {
            println("Error: ${result1.message}")
        }
    }
}
```

### 4. URL Parsing Utilities

```kotlin
// Extract video ID from any YouTube URL
val videoId = YouTubeUrlParser.extractVideoId("https://youtu.be/dQw4w9WgXcQ")
// Returns: "dQw4w9WgXcQ"

// Check if a URL is a YouTube URL
val isYouTube = YouTubeUrlParser.isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ")
// Returns: true

// Construct standard watch URL from video ID
val watchUrl = YouTubeUrlParser.constructWatchUrl("dQw4w9WgXcQ")
// Returns: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

## Data Models

### YouTubeVideoMetadata

The simplified metadata model returned by the repository:

```kotlin
data class YouTubeVideoMetadata(
    val videoId: String,           // "dQw4w9WgXcQ"
    val title: String,             // "Rick Astley - Never Gonna Give You Up"
    val channelName: String,       // "Rick Astley"
    val channelUrl: String,        // "https://www.youtube.com/@RickAstley"
    val thumbnailUrl: String,      // High-quality thumbnail URL
    val thumbnailWidth: Int,       // 480
    val thumbnailHeight: Int,      // 360
    val embedHtml: String          // Full embed HTML code
)
```

### YouTubeOEmbedResponse

The raw oEmbed API response (for advanced usage):

```kotlin
@Serializable
data class YouTubeOEmbedResponse(
    val title: String,
    val authorName: String,
    val authorUrl: String,
    val thumbnailUrl: String,
    val thumbnailWidth: Int,
    val thumbnailHeight: Int,
    val html: String,
    // ... and more fields
)
```

## Error Handling

The repository handles various error cases:

| Error Type | Description | Example Message |
|------------|-------------|-----------------|
| Invalid URL | Not a YouTube URL | "Invalid YouTube URL: ..." |
| Invalid Video ID | Malformed video ID | "Invalid YouTube video ID format: ..." |
| Video Not Found | 404 response | "Video not found or is private" |
| Private Video | 401/403 response | "Video is not accessible" |
| Timeout | Network timeout | "Request timed out. Please check your connection." |
| Network Error | No internet | "Network error: Unable to connect" |

## Testing

### Test with Real URLs

```kotlin
@Test
fun testYouTubeUrlParsing() {
    val videoId = YouTubeUrlParser.extractVideoId("https://youtu.be/dQw4w9WgXcQ")
    assertEquals("dQw4w9WgXcQ", videoId)
}
```

### Mock Repository for UI Tests

```kotlin
class FakeYouTubeRepository : YouTubeRepository {
    override suspend fun getVideoMetadata(youtubeUrl: String) = Result.Success(
        YouTubeVideoMetadata(
            videoId = "test123",
            title = "Test Video",
            channelName = "Test Channel",
            // ... other fields
        )
    )

    override suspend fun getVideoMetadataById(videoId: String) =
        getVideoMetadata("https://youtu.be/$videoId")
}
```

## Limitations

1. **No API Quota** - Since this uses oEmbed, there's no API quota limit
2. **Limited Data** - Only basic metadata (no view counts, likes, description, etc.)
3. **Public Videos Only** - Cannot fetch metadata for private/unlisted videos
4. **No Real-time Updates** - Metadata is fetched at request time only

## For More Metadata

If you need additional data (views, likes, description, duration), you'll need to:
1. Use YouTube Data API v3 (requires API key)
2. Add the Google APIs dependency
3. Implement a separate repository using the official API

See: https://developers.google.com/youtube/v3/getting-started

## Dependencies

No additional dependencies required! Uses existing Ktor setup.

```kotlin
// Already included in your project
implementation("io.ktor:ktor-client-core:2.3.5")
implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
```
