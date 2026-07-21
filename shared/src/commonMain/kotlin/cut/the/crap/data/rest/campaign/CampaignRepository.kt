package cut.the.crap.data.rest.campaign

import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.Source
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import okio.IOException

/**
 * Loads the action campaign — its countries, their languages and the posts written for them —
 * from the campaign site's JSON endpoint.
 */
interface CampaignRepository {

    /** Loads the campaign currently served at [AppConfig.campaignBaseUrl]. */
    suspend fun getCampaign(): Result<Campaign>
}

class CampaignRepositoryImpl constructor(
    private val client: HttpClient,
    private val config: AppConfig,
) : CampaignRepository {

    companion object {
        private const val CAMPAIGN_PATH = "/api/abu-safiya"
    }

    override suspend fun getCampaign(): Result<Campaign> {
        // The shared client defaults to the job-queue backend, which is a different host; an
        // absolute URL overrides that default.
        val url = config.campaignBaseUrl.trimEnd('/') + CAMPAIGN_PATH

        return try {
            val response = client.get(url)
            val status = response.status

            // The shared client is built without `expectSuccess`, so Ktor raises no
            // Client/ServerResponseException on a 4xx/5xx — it just hands back the error body and
            // `body<CampaignDto>()` fails with a confusing transformation error. Classify the
            // status ourselves instead.
            when {
                status.value == 404 ->
                    Result.Error(AppError.NotFound(Source.CAMPAIGN), retryable = false)
                status.value >= 500 ->
                    Result.Error(AppError.SourceServer(Source.CAMPAIGN, status.value))
                !status.isSuccess() -> Result.Error(
                    AppError.Client(status.value, status.description),
                    retryable = false
                )
                else -> Result.Success(response.body<CampaignDto>().toCampaign())
            }
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.SourceTimeout(Source.CAMPAIGN), e)
        } catch (e: ContentConvertException) {
            // A 2xx with a non-JSON/unexpected body (e.g. an error HTML page) — permanent.
            Result.Error(AppError.Unavailable(Source.CAMPAIGN), e, retryable = false)
        } catch (e: SerializationException) {
            Result.Error(AppError.ParseError, e, retryable = false)
        } catch (e: IOException) {
            Result.Error(AppError.Network(e.message), e)
        } catch (e: Exception) {
            Result.Error(AppError.FetchFailed(Source.CAMPAIGN, e.message), e)
        }
    }
}

/**
 * Maps the raw payload into the domain model. Countries keep the server's registry order;
 * a post without an explicit language falls back to its country's default.
 */
private fun CampaignDto.toCampaign(): Campaign = Campaign(
    name = campaign,
    version = version,
    locateUrl = locateUrl,
    countries = countries.map { country ->
        CampaignCountry(
            countryCode = country.code,
            countryName = country.name.ifBlank { country.code.uppercase() },
            flag = country.flag,
            defaultLanguage = country.defaultLang,
            languages = country.langs,
            url = country.url,
            hasParliamentAction = country.parliament,
            posts = country.posts
                .filter { it.text.isNotBlank() }
                .map { post ->
                    CampaignPost(
                        id = post.id,
                        language = post.lang.ifBlank { country.defaultLang },
                        text = post.text
                    )
                }
        )
    }
)
