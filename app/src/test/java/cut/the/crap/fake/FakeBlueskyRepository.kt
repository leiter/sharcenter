package cut.the.crap.fake

import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.bluesky.BlueskyPostMetadata
import cut.the.crap.data.rest.bluesky.BlueskyRepository

/**
 * Fake implementation of BlueskyRepository for testing.
 * Allows configuring success/failure responses and records the URLs fetched.
 */
class FakeBlueskyRepository : BlueskyRepository {

    private var nextResult: Result<BlueskyPostMetadata> = Result.Success(
        BlueskyPostMetadata(
            authorName = "Test Author",
            authorHandle = "test.bsky.social",
            text = "Test post text",
            thumbnailUrl = "https://cdn.bsky.app/thumb.jpg"
        )
    )
    private val fetchedUrls = mutableListOf<String>()

    override suspend fun getPostMetadata(url: String): Result<BlueskyPostMetadata> {
        fetchedUrls.add(url)
        return nextResult
    }

    // Test helpers
    fun setSuccessResponse(metadata: BlueskyPostMetadata) {
        nextResult = Result.Success(metadata)
    }

    fun setErrorResponse(message: String, exception: Throwable? = null) {
        nextResult = Result.Error(message, exception)
    }

    fun getFetchedUrls(): List<String> = fetchedUrls.toList()
}
