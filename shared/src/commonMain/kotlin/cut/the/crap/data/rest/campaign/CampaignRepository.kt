package cut.the.crap.data.rest.campaign

import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.Source
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import okio.IOException

/**
 * Loads campaigns — the list this install can act on, and the full payload of one of them.
 *
 * Requests are signed by the `CtcSignature` plugin, so nothing here mentions identity.
 */
interface CampaignRepository {

    /**
     * The campaigns this install can act on: owned, joined, and the operator-curated one the app
     * ships with. Summaries only — one cheap request for the list screen.
     */
    suspend fun list(): Result<List<CampaignSummary>>

    /** The full payload of one campaign: countries, posts and contact actions. */
    suspend fun get(id: String, forceRefresh: Boolean = false): Result<Campaign>

    /**
     * The bundled campaign via the unsigned legacy alias.
     *
     * Kept because it is the one path that works with no identity at all, and because its path is
     * a hardcoded constant in shipped clients (`CAMPAIGN_SCHEMA_SPEC.md` C7).
     */
    suspend fun getCampaign(): Result<Campaign>

    /**
     * Creates a campaign from [rawJson] — the exact text a user pasted or picked from a file,
     * built by the web campaign-builder page (`/campaign-builder`) to match `POST /api/campaigns`'s
     * body shape. Sent as-is: the server validates it, so there is no second copy of that
     * validation logic here. Returns the new campaign's summary, with the caller as `owner`.
     */
    suspend fun create(rawJson: String): Result<CampaignSummary>

    /**
     * Replaces every item of [campaignId] with [rawItemsJson] (an object with an `items` array,
     * same shape as `create`'s `items`) — the "managed from the app" edit path (owner/editor only).
     */
    suspend fun replaceItems(campaignId: String, rawItemsJson: String): Result<Unit>

    /** Creates an invite code for [campaignId] (owner/editor only). */
    suspend fun invite(campaignId: String, role: String, expiresAt: Long? = null, maxUses: Int? = null): Result<String>

    /** Redeems [code], joining the campaign it belongs to. */
    suspend fun join(code: String): Result<CampaignSummary>

    /** Leaves [campaignId]. The owner cannot leave their own campaign — the server rejects that. */
    suspend fun leave(campaignId: String): Result<Unit>
}

class CampaignRepositoryImpl constructor(
    private val client: HttpClient,
    private val config: AppConfig,
) : CampaignRepository {

    companion object {
        private const val LEGACY_PATH = "/api/abu-safiya"
        private const val LIST_PATH = "/api/campaigns/mine"
        private const val CAMPAIGN_PATH = "/api/campaigns"

        /** The campaign the app has always shipped with. */
        const val BUNDLED_CAMPAIGN_ID = "abu-safiya"
    }

    // Loaded campaigns, kept in memory so moving between the detail screen and the composer does
    // not re-download ~47 kB. Deliberately not persistent: CAMPAIGN_SCHEMA_SPEC §6.4 puts the
    // durable cache in SQLDelight keyed by `version`, and that belongs with the offline outbox
    // rather than half-built here.
    private val cache = mutableMapOf<String, Campaign>()
    private val cacheLock = Mutex()

    override suspend fun list(): Result<List<CampaignSummary>> =
        call { client.get(url(LIST_PATH)) }
            .map { response -> response.body<CampaignListDto>().campaigns.map { it.toDomain() } }

    override suspend fun get(id: String, forceRefresh: Boolean): Result<Campaign> {
        if (!forceRefresh) {
            cacheLock.withLock { cache[id] }?.let { return Result.Success(it) }
        }
        val result = call { client.get(url("$CAMPAIGN_PATH/${id.encodeURLPathPart()}")) }
            .map { response -> response.body<CampaignDto>().toCampaign() }
        if (result is Result.Success) cacheLock.withLock { cache[id] = result.data }
        return result
    }

    override suspend fun getCampaign(): Result<Campaign> =
        call { client.get(url(LEGACY_PATH)) }
            .map { response -> response.body<CampaignDto>().toCampaign() }

    override suspend fun create(rawJson: String): Result<CampaignSummary> =
        call {
            client.post(url(CAMPAIGN_PATH)) {
                setBody(TextContent(rawJson, ContentType.Application.Json))
            }
        }.map { response -> response.body<CampaignSummaryDto>().toDomain() }

    override suspend fun replaceItems(campaignId: String, rawItemsJson: String): Result<Unit> =
        call {
            client.post(url("$CAMPAIGN_PATH/${campaignId.encodeURLPathPart()}/items")) {
                setBody(TextContent(rawItemsJson, ContentType.Application.Json))
            }
        }.map { }

    override suspend fun invite(
        campaignId: String,
        role: String,
        expiresAt: Long?,
        maxUses: Int?,
    ): Result<String> =
        call {
            client.post(url("$CAMPAIGN_PATH/${campaignId.encodeURLPathPart()}/invite")) {
                contentType(ContentType.Application.Json)
                setBody(InviteRequestDto(role, expiresAt, maxUses))
            }
        }.map { response -> response.body<InviteResponseDto>().code }

    override suspend fun join(code: String): Result<CampaignSummary> =
        call {
            client.post(url("$CAMPAIGN_PATH/join")) {
                contentType(ContentType.Application.Json)
                setBody(JoinRequestDto(code))
            }
        }.map { response -> response.body<CampaignSummaryDto>().toDomain() }

    override suspend fun leave(campaignId: String): Result<Unit> =
        call {
            client.delete(url("$CAMPAIGN_PATH/${campaignId.encodeURLPathPart()}/members/me"))
        }.map { }

    private fun url(path: String) = config.campaignBaseUrl.trimEnd('/') + path

    /**
     * Runs [block] and classifies the outcome.
     *
     * The shared client is built without `expectSuccess`, so Ktor raises no
     * Client/ServerResponseException on a 4xx/5xx — it just hands back the error body and
     * `body<CampaignDto>()` fails with a confusing transformation error. Classify the status here
     * instead, once, rather than in each call above.
     */
    private suspend inline fun call(block: () -> HttpResponse): Result<HttpResponse> = try {
        val response = block()
        val status = response.status
        when {
            status.value == 404 ->
                Result.Error(AppError.NotFound(Source.CAMPAIGN), retryable = false)
            // 451: the operator disabled this campaign (C6). Permanent from the client's side —
            // retrying will not bring it back, and the UI must say so rather than spin.
            status.value == 451 ->
                Result.Error(AppError.Client(451, status.description), retryable = false)
            status.value >= 500 ->
                Result.Error(AppError.SourceServer(Source.CAMPAIGN, status.value))
            !status.isSuccess() ->
                Result.Error(AppError.Client(status.value, status.description), retryable = false)
            else -> Result.Success(response)
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

private inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> try {
        Result.Success(transform(data))
    } catch (e: SerializationException) {
        Result.Error(AppError.ParseError, e, retryable = false)
    } catch (e: ContentConvertException) {
        Result.Error(AppError.Unavailable(Source.CAMPAIGN), e, retryable = false)
    }
    is Result.Error -> this
}

private fun CampaignSummaryDto.toDomain() = CampaignSummary(
    id = id,
    title = title,
    description = description,
    state = state,
    version = version,
    role = role,
    featured = featured,
    countryCount = countryCount,
    postCount = postCount,
    contactCount = contactCount,
)

/**
 * Maps the raw payload into the domain model. Countries keep the server's registry order;
 * a post without an explicit language falls back to its country's default.
 */
private fun CampaignDto.toCampaign(): Campaign = Campaign(
    name = campaign,
    version = version,
    locateUrl = locateUrl,
    id = id,
    title = title,
    description = description,
    role = role,
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
                },
            contacts = country.contacts
                .filter { it.url.isNotBlank() }
                .map { contact ->
                    CampaignContact(
                        id = contact.id,
                        url = contact.url,
                        label = contact.label,
                        note = contact.note,
                    )
                },
        )
    }
)
