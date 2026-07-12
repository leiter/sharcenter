package cut.the.crap.data.rest.reddit

import cut.the.crap.R
import cut.the.crap.data.rest.Result
import cut.the.crap.tools.parseRedditUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import java.io.IOException
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Source

/**
 * Repository for fetching Reddit post metadata from the public oEmbed endpoint
 * (`https://www.reddit.com/oembed`). Reddit blocks unauthenticated `.json` access (403), so oEmbed
 * is the free path; it returns the title and author but no thumbnail. A browser-like User-Agent is
 * sent because Reddit rejects generic/absent agents.
 */
interface RedditRepository {
    /**
     * Fetches metadata for the Reddit post [url] points at.
     *
     * @param url a `https://www.reddit.com/r/{sub}/comments/{id}/…` post URL
     * @return [RedditPostMetadata] on success, or an error (non-post URL, blocked, network failure)
     */
    suspend fun getPostMetadata(url: String): Result<RedditPostMetadata>
}

class RedditRepositoryImpl constructor(
    private val client: HttpClient,
) : RedditRepository {

    companion object {
        private const val OEMBED_URL = "https://www.reddit.com/oembed"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    override suspend fun getPostMetadata(url: String): Result<RedditPostMetadata> {
        // Only posts have oEmbed content; subreddits, profiles and unresolved short links do not.
        val contentType = parseRedditUrl(url)?.contentType
        if (contentType != "post") {
            return Result.Error(AppError.InvalidUrl(Source.REDDIT, url), retryable = false)
        }

        return try {
            val response = client.get(OEMBED_URL) {
                parameter("url", url)
                header(HttpHeaders.UserAgent, USER_AGENT)
            }.body<RedditOEmbedResponse>()

            RedditPostMetadata.fromOEmbed(response)
                ?.let { Result.Success(it) }
                ?: Result.Error(AppError.Unavailable(Source.REDDIT), retryable = false)
        } catch (e: ClientRequestException) {
            // 4xx — post removed/private (404) or blocked (403/429). Permanent for enrichment.
            when (e.response.status.value) {
                403, 404, 429 -> Result.Error(AppError.NotFound(Source.REDDIT), e, retryable = false)
                else -> Result.Error(
                    AppError.Client(e.response.status.value, e.response.status.description),
                    e,
                    retryable = false
                )
            }
        } catch (e: ServerResponseException) {
            // 5xx — transient, worth retrying.
            Result.Error(AppError.SourceServer(Source.REDDIT, e.response.status.value), e)
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.SourceTimeout(Source.REDDIT), e)
        } catch (e: ContentConvertException) {
            // A non-JSON body (e.g. an HTML block page) returned with a 2xx status. Permanent.
            Result.Error(AppError.Unavailable(Source.REDDIT), e, retryable = false)
        } catch (e: SerializationException) {
            Result.Error(AppError.Unavailable(Source.REDDIT), e, retryable = false)
        } catch (e: IOException) {
            Result.Error(AppError.Network(e.message), e)
        } catch (e: Exception) {
            Result.Error(
                AppError.FetchFailed(Source.REDDIT, e.message),
                e
            )
        }
    }
}
