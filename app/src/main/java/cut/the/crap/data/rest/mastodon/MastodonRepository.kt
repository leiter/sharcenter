package cut.the.crap.data.rest.mastodon

import cut.the.crap.R
import cut.the.crap.data.rest.Result
import cut.the.crap.tools.mastodonStatusApiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import java.io.IOException
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Source

/**
 * Repository for fetching Mastodon post metadata from the public status API
 * (`GET /api/v1/statuses/{id}`), which needs no authentication for public posts. The request is
 * sent to the post's *origin* instance, derived from the shared URL.
 */
interface MastodonRepository {
    /**
     * Fetches metadata for the Mastodon post [url] points at.
     *
     * @param url a `https://{instance}/@{user}/{id}` post URL
     * @return [MastodonPostMetadata] on success, or an error (invalid/non-post URL, network failure)
     */
    suspend fun getPostMetadata(url: String): Result<MastodonPostMetadata>
}

class MastodonRepositoryImpl constructor(
    private val client: HttpClient,
) : MastodonRepository {

    override suspend fun getPostMetadata(url: String): Result<MastodonPostMetadata> {
        // Only post URLs resolve to a status endpoint; profile links have no post to fetch.
        val apiUrl = mastodonStatusApiUrl(url)
            ?: return Result.Error(AppError.InvalidUrl(Source.MASTODON, url), retryable = false)

        return try {
            val status = client.get(apiUrl).body<MastodonStatus>()

            MastodonPostMetadata.fromStatus(status)
                ?.let { Result.Success(it) }
                ?: Result.Error(AppError.Unavailable(Source.MASTODON), retryable = false)
        } catch (e: ClientRequestException) {
            // 4xx — post deleted (410), private/blocked (401/403) or not found (404). Permanent.
            when (e.response.status.value) {
                401, 403, 404, 410 -> Result.Error(AppError.NotFound(Source.MASTODON), e, retryable = false)
                else -> Result.Error(
                    AppError.Client(e.response.status.value, e.response.status.description),
                    e,
                    retryable = false
                )
            }
        } catch (e: ServerResponseException) {
            // 5xx — transient, worth retrying.
            Result.Error(AppError.SourceServer(Source.MASTODON, e.response.status.value), e)
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.SourceTimeout(Source.MASTODON), e)
        } catch (e: IOException) {
            Result.Error(AppError.Network(e.message), e)
        } catch (e: Exception) {
            Result.Error(
                AppError.FetchFailed(Source.MASTODON, e.message),
                e
            )
        }
    }
}
