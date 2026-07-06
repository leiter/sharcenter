package cut.the.crap.fake

import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.tiktok.TikTokPostMetadata
import cut.the.crap.data.rest.tiktok.TikTokRepository

/**
 * Fake implementation of TikTokRepository for testing.
 * Allows configuring success/failure responses and records the URLs fetched.
 */
class FakeTikTokRepository : TikTokRepository {

    private var nextResult: Result<TikTokPostMetadata> = Result.Success(
        TikTokPostMetadata(
            authorName = "Test Author",
            authorHandle = "testuser",
            text = "Test caption",
            thumbnailUrl = "https://cdn.tiktok.com/thumb.jpg"
        )
    )
    private val fetchedUrls = mutableListOf<String>()

    override suspend fun getPostMetadata(url: String): Result<TikTokPostMetadata> {
        fetchedUrls.add(url)
        return nextResult
    }

    // Test helpers
    fun setSuccessResponse(metadata: TikTokPostMetadata) {
        nextResult = Result.Success(metadata)
    }

    fun setErrorResponse(message: String, exception: Throwable? = null) {
        nextResult = Result.Error(message, exception)
    }

    fun getFetchedUrls(): List<String> = fetchedUrls.toList()
}
