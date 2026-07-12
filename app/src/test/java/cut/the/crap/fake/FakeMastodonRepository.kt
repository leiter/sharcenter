package cut.the.crap.fake

import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.mastodon.MastodonPostMetadata
import cut.the.crap.data.rest.mastodon.MastodonRepository
import cut.the.crap.data.rest.AppError

/**
 * Fake implementation of MastodonRepository for testing.
 * Allows configuring success/failure responses and records the URLs fetched.
 */
class FakeMastodonRepository : MastodonRepository {

    private var nextResult: Result<MastodonPostMetadata> = Result.Success(
        MastodonPostMetadata(
            authorName = "Test Author",
            authorHandle = "test@mastodon.social",
            text = "Test post text",
            thumbnailUrl = "https://cdn.mastodon.social/thumb.jpg"
        )
    )
    private val fetchedUrls = mutableListOf<String>()

    override suspend fun getPostMetadata(url: String): Result<MastodonPostMetadata> {
        fetchedUrls.add(url)
        return nextResult
    }

    // Test helpers
    fun setSuccessResponse(metadata: MastodonPostMetadata) {
        nextResult = Result.Success(metadata)
    }

    fun setErrorResponse(message: String, exception: Throwable? = null) {
        nextResult = Result.Error(AppError.Unexpected(message), exception)
    }

    fun getFetchedUrls(): List<String> = fetchedUrls.toList()
}
