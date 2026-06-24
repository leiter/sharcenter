package cut.the.crap.data.rest

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import java.io.IOException
import javax.inject.Inject

/**
 * Repository for fetching YouTube video metadata using oEmbed API
 */
interface YouTubeRepository {
    /**
     * Fetches metadata for a YouTube video from its URL
     *
     * @param youtubeUrl The full YouTube URL (any format supported)
     * @return Result containing YouTubeVideoMetadata or error
     */
    suspend fun getVideoMetadata(youtubeUrl: String): Result<YouTubeVideoMetadata>

    /**
     * Fetches metadata for a YouTube video from its ID
     *
     * @param videoId The YouTube video ID (11 characters)
     * @return Result containing YouTubeVideoMetadata or error
     */
    suspend fun getVideoMetadataById(videoId: String): Result<YouTubeVideoMetadata>
}

class YouTubeRepositoryImpl @Inject constructor(
    private val client: HttpClient
) : YouTubeRepository {

    companion object {
        private const val OEMBED_BASE_URL = "https://www.youtube.com/oembed"
    }

    override suspend fun getVideoMetadata(youtubeUrl: String): Result<YouTubeVideoMetadata> {
        // Validate it's a YouTube URL
        if (!YouTubeUrlParser.isYouTubeUrl(youtubeUrl)) {
            return Result.Error("Invalid YouTube URL: $youtubeUrl")
        }

        // Extract video ID
        val videoId = YouTubeUrlParser.extractVideoId(youtubeUrl)
            ?: return Result.Error("Could not extract video ID from URL: $youtubeUrl")

        return getVideoMetadataById(videoId)
    }

    override suspend fun getVideoMetadataById(videoId: String): Result<YouTubeVideoMetadata> {
        // Validate video ID format (11 characters, alphanumeric with - and _)
        if (!videoId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return Result.Error("Invalid YouTube video ID format: $videoId")
        }

        return try {
            // Construct the watch URL for the oEmbed API
            val watchUrl = YouTubeUrlParser.constructWatchUrl(videoId)

            // Make request to YouTube oEmbed API
            val response = client.get(OEMBED_BASE_URL) {
                parameter("url", watchUrl)
                parameter("format", "json")
            }.body<YouTubeOEmbedResponse>()

            // Convert to our domain model
            val metadata = YouTubeVideoMetadata.fromOEmbed(videoId, response)
            Result.Success(metadata)

        } catch (e: ClientRequestException) {
            // 4xx errors - likely video not found or private
            when (e.response.status.value) {
                404 -> Result.Error("Video not found or is private", e)
                401, 403 -> Result.Error("Video is not accessible", e)
                else -> Result.Error(
                    "Client error: ${e.response.status.value} - ${e.response.status.description}",
                    e
                )
            }
        } catch (e: ServerResponseException) {
            // 5xx errors
            Result.Error(
                "YouTube server error: ${e.response.status.value}",
                e
            )
        } catch (e: SocketTimeoutException) {
            Result.Error("Request timed out. Please check your connection.", e)
        } catch (e: IOException) {
            Result.Error("Network error: ${e.message ?: "Unable to connect"}", e)
        } catch (e: Exception) {
            Result.Error("Failed to fetch video metadata: ${e.message ?: "Unknown error"}", e)
        }
    }
}
