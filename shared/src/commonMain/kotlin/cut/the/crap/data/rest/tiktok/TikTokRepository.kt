package cut.the.crap.data.rest.tiktok

import cut.the.crap.data.rest.Result
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
import okio.IOException
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Source

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
) : TikTokRepository {

    companion object {
        private const val OEMBED_URL = "https://www.tiktok.com/oembed"
    }

    override suspend fun getPostMetadata(url: String): Result<TikTokPostMetadata> {
        // Only video/photo posts have oEmbed content; profiles and unresolved short links do not.
        val contentType = parseTikTokUrl(url)?.contentType
        if (contentType != "video" && contentType != "photo") {
            return Result.Error(AppError.InvalidUrl(Source.TIKTOK, url), retryable = false)
        }

        return try {
            val response = client.get(OEMBED_URL) {
                parameter("url", url)
            }.body<TikTokOEmbedResponse>()

            TikTokPostMetadata.fromOEmbed(response)
                ?.let { Result.Success(it) }
                ?: Result.Error(AppError.Unavailable(Source.TIKTOK), retryable = false)
        } catch (e: ClientRequestException) {
            // 4xx — post removed, private, or region-blocked. Permanent.
            when (e.response.status.value) {
                400, 403, 404 -> Result.Error(AppError.NotFound(Source.TIKTOK), e, retryable = false)
                else -> Result.Error(
                    AppError.Client(e.response.status.value, e.response.status.description),
                    e,
                    retryable = false
                )
            }
        } catch (e: ServerResponseException) {
            // 5xx — transient, worth retrying.
            Result.Error(AppError.SourceServer(Source.TIKTOK, e.response.status.value), e)
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.SourceTimeout(Source.TIKTOK), e)
        } catch (e: ContentConvertException) {
            // A non-JSON error body returned with a 2xx status (deleted/unavailable post). Permanent.
            Result.Error(AppError.Unavailable(Source.TIKTOK), e, retryable = false)
        } catch (e: SerializationException) {
            Result.Error(AppError.Unavailable(Source.TIKTOK), e, retryable = false)
        } catch (e: IOException) {
            Result.Error(AppError.Network(e.message), e)
        } catch (e: Exception) {
            Result.Error(
                AppError.FetchFailed(Source.TIKTOK, e.message),
                e
            )
        }
    }
}
