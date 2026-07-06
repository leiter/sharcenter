package cut.the.crap.data.rest.bluesky

import cut.the.crap.R
import cut.the.crap.data.rest.Result
import cut.the.crap.tools.StringProvider
import cut.the.crap.tools.blueskyPostAtUri
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

class BlueskyRepositoryImpl @Inject constructor(
    private val client: HttpClient,
    private val strings: StringProvider
) : BlueskyRepository {

    companion object {
        private const val GET_POST_THREAD_URL =
            "https://public.api.bsky.app/xrpc/app.bsky.feed.getPostThread"
    }

    override suspend fun getPostMetadata(url: String): Result<BlueskyPostMetadata> {
        // Only post URLs carry an at:// URI to resolve; profile links have no post to fetch.
        val atUri = blueskyPostAtUri(url)
            ?: return Result.Error(strings.get(R.string.bsky_error_invalid_url, url), retryable = false)

        return try {
            // depth=0 keeps the payload to the focal post — replies aren't needed for metadata.
            val response = client.get(GET_POST_THREAD_URL) {
                parameter("uri", atUri)
                parameter("depth", 0)
            }.body<BlueskyThreadResponse>()

            BlueskyPostMetadata.fromThread(response)
                ?.let { Result.Success(it) }
                ?: Result.Error(strings.get(R.string.bsky_error_unavailable), retryable = false)
        } catch (e: ClientRequestException) {
            // 4xx — post deleted, blocked, or a bad at:// URI. Permanent.
            when (e.response.status.value) {
                400, 404 -> Result.Error(strings.get(R.string.bsky_error_not_found), e, retryable = false)
                else -> Result.Error(
                    strings.get(R.string.error_client, e.response.status.value, e.response.status.description),
                    e,
                    retryable = false
                )
            }
        } catch (e: ServerResponseException) {
            // 5xx — transient, worth retrying.
            Result.Error(strings.get(R.string.bsky_error_server, e.response.status.value), e)
        } catch (e: SocketTimeoutException) {
            Result.Error(strings.get(R.string.bsky_error_timeout), e)
        } catch (e: IOException) {
            Result.Error(strings.get(R.string.error_network, e.message ?: ""), e)
        } catch (e: Exception) {
            Result.Error(
                strings.get(R.string.bsky_error_fetch_failed, e.message ?: strings.get(R.string.error_unknown)),
                e
            )
        }
    }
}
