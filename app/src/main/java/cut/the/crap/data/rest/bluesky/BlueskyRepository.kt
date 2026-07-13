package cut.the.crap.data.rest.bluesky

import cut.the.crap.data.rest.Result
import cut.the.crap.tools.blueskyPostAtUri
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import java.io.IOException
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Source

/**
 * Repository for fetching Bluesky post metadata from the public AppView API
 * (`app.bsky.feed.getPostThread`), which needs no authentication for public posts.
 */
interface BlueskyRepository {
    /**
     * Fetches metadata for the Bluesky post [url] points at.
     *
     * @param url a `bsky.app/profile/{actor}/post/{rkey}` URL
     * @return [BlueskyPostMetadata] on success, or an error (invalid/non-post URL, network failure)
     */
    suspend fun getPostMetadata(url: String): Result<BlueskyPostMetadata>
}

class BlueskyRepositoryImpl constructor(
    private val client: HttpClient,
) : BlueskyRepository {

    companion object {
        private const val GET_POST_THREAD_URL =
            "https://public.api.bsky.app/xrpc/app.bsky.feed.getPostThread"
    }

    override suspend fun getPostMetadata(url: String): Result<BlueskyPostMetadata> {
        // Only post URLs carry an at:// URI to resolve; profile links have no post to fetch.
        val atUri = blueskyPostAtUri(url)
            ?: return Result.Error(AppError.InvalidUrl(Source.BLUESKY, url), retryable = false)

        return try {
            // depth=0 keeps the payload to the focal post — replies aren't needed for metadata.
            val response = client.get(GET_POST_THREAD_URL) {
                parameter("uri", atUri)
                parameter("depth", 0)
            }.body<BlueskyThreadResponse>()

            BlueskyPostMetadata.fromThread(response)
                ?.let { Result.Success(it) }
                ?: Result.Error(AppError.Unavailable(Source.BLUESKY), retryable = false)
        } catch (e: ClientRequestException) {
            // 4xx — post deleted, blocked, or a bad at:// URI. Permanent.
            when (e.response.status.value) {
                400, 404 -> Result.Error(AppError.NotFound(Source.BLUESKY), e, retryable = false)
                else -> Result.Error(
                    AppError.Client(e.response.status.value, e.response.status.description),
                    e,
                    retryable = false
                )
            }
        } catch (e: ServerResponseException) {
            // 5xx — transient, worth retrying.
            Result.Error(AppError.SourceServer(Source.BLUESKY, e.response.status.value), e)
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.SourceTimeout(Source.BLUESKY), e)
        } catch (e: IOException) {
            Result.Error(AppError.Network(e.message), e)
        } catch (e: Exception) {
            Result.Error(
                AppError.FetchFailed(Source.BLUESKY, e.message),
                e
            )
        }
    }
}
