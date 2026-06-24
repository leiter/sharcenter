package cut.the.crap.fake

import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.YouTubeRepository
import cut.the.crap.data.rest.YouTubeVideoMetadata
import cut.the.crap.testutils.TestData

/**
 * Fake implementation of YouTubeRepository for testing.
 * Allows configuring success/failure responses for tests.
 */
class FakeYouTubeRepository : YouTubeRepository {

    private var nextResult: Result<YouTubeVideoMetadata> = Result.Success(TestData.youtubeMetadata())
    private val fetchedUrls = mutableListOf<String>()
    private val fetchedIds = mutableListOf<String>()

    override suspend fun getVideoMetadata(youtubeUrl: String): Result<YouTubeVideoMetadata> {
        fetchedUrls.add(youtubeUrl)
        return nextResult
    }

    override suspend fun getVideoMetadataById(videoId: String): Result<YouTubeVideoMetadata> {
        fetchedIds.add(videoId)
        return nextResult
    }

    // Test helpers
    fun setNextResult(result: Result<YouTubeVideoMetadata>) {
        nextResult = result
    }

    fun setSuccessResponse(metadata: YouTubeVideoMetadata) {
        nextResult = Result.Success(metadata)
    }

    fun setErrorResponse(message: String, exception: Throwable? = null) {
        nextResult = Result.Error(message, exception)
    }

    fun getFetchedUrls(): List<String> = fetchedUrls.toList()

    fun getFetchedIds(): List<String> = fetchedIds.toList()

    fun reset() {
        nextResult = Result.Success(TestData.youtubeMetadata())
        fetchedUrls.clear()
        fetchedIds.clear()
    }
}
