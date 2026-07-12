package cut.the.crap.data.rest.tiktok

import cut.the.crap.R
import cut.the.crap.data.rest.Result
import cut.the.crap.tools.StringProvider
import cut.the.crap.tools.parseTikTokUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * Repository for fetching TikTok post metadata from the public oEmbed endpoint
 * (`https://www.tiktok.com/oembed`), which needs no authentication.
 */
interface TikTokRepository {
    /**
     * Fetches metadata for the TikTok post [url] points at.
     *
     * @param url a canonical `https://www.tiktok.com/@{user}/video|photo/{id}` URL
     * @return [TikTokPostMetadata] on success, or an error (non-post URL, network failure)
     */
    suspend fun getPostMetadata(url: String): Result<TikTokPostMetadata>
}

class TikTokRepositoryImpl constructor(
    private val client: HttpClient,
    private val strings: StringProvider
) : TikTokRepository {

    companion object {
        private const val OEMBED_URL = "https://www.tiktok.com/oembed"
    }

    override suspend fun getPostMetadata(url: String): Result<TikTokPostMetadata> {
        // Only video/photo posts have oEmbed content; profiles and unresolved short links do not.
        val contentType = parseTikTokUrl(url)?.contentType
        if (contentType != "video" && contentType != "photo") {
            return Result.Error(strings.get(R.string.tiktok_error_invalid_url, url), retryable = false)
        }

        return try {
            val response = client.get(OEMBED_URL) {
                parameter("url", url)
            }.body<TikTokOEmbedResponse>()

            TikTokPostMetadata.fromOEmbed(response)
                ?.let { Result.Success(it) }
                ?: Result.Error(strings.get(R.string.tiktok_error_unavailable), retryable = false)
        } catch (e: ClientRequestException) {
            // 4xx — post removed, private, or region-blocked. Permanent.
            when (e.response.status.value) {
                400, 403, 404 -> Result.Error(strings.get(R.string.tiktok_error_not_found), e, retryable = false)
                else -> Result.Error(
                    strings.get(R.string.error_client, e.response.status.value, e.response.status.description),
                    e,
                    retryable = false
                )
            }
        } catch (e: ServerResponseException) {
            // 5xx — transient, worth retrying.
            Result.Error(strings.get(R.string.tiktok_error_server, e.response.status.value), e)
        } catch (e: SocketTimeoutException) {
            Result.Error(strings.get(R.string.tiktok_error_timeout), e)
        } catch (e: ContentConvertException) {
            // A non-JSON error body returned with a 2xx status (deleted/unavailable post). Permanent.
            Result.Error(strings.get(R.string.tiktok_error_unavailable), e, retryable = false)
        } catch (e: SerializationException) {
            Result.Error(strings.get(R.string.tiktok_error_unavailable), e, retryable = false)
        } catch (e: IOException) {
            Result.Error(strings.get(R.string.error_network, e.message ?: ""), e)
        } catch (e: Exception) {
            Result.Error(
                strings.get(R.string.tiktok_error_fetch_failed, e.message ?: strings.get(R.string.error_unknown)),
                e
            )
        }
    }
}
