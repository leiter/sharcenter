package cut.the.crap.fake

import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.reddit.RedditPostMetadata
import cut.the.crap.data.rest.reddit.RedditRepository

/**
 * Fake implementation of RedditRepository for testing.
 * Allows configuring success/failure responses and records the URLs fetched.
 */
class FakeRedditRepository : RedditRepository {

    private var nextResult: Result<RedditPostMetadata> = Result.Success(
        RedditPostMetadata(
            authorName = "u/testuser",
            text = "Test post title",
            thumbnailUrl = null
        )
    )
    private val fetchedUrls = mutableListOf<String>()

    override suspend fun getPostMetadata(url: String): Result<RedditPostMetadata> {
        fetchedUrls.add(url)
        return nextResult
    }

    // Test helpers
    fun setSuccessResponse(metadata: RedditPostMetadata) {
        nextResult = Result.Success(metadata)
    }

    fun setErrorResponse(message: String, exception: Throwable? = null) {
        nextResult = Result.Error(message, exception)
    }

    fun getFetchedUrls(): List<String> = fetchedUrls.toList()
}
