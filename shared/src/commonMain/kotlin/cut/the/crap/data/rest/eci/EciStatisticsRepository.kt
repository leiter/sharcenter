package cut.the.crap.data.rest.eci

import cut.the.crap.data.rest.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import okio.IOException
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Source

/**
 * Loads and parses the "signatures per country" statistics for a European Citizens'
 * Initiative.
 *
 * DORMANT since the campaign swap: the Posts top bar now opens the Abu-Safiya action campaign
 * (`data/rest/campaign/`). Nothing navigates here — kept, and still bound in `repositoryModule`,
 * in case an initiative worth tracking comes along again.
 *
 * The public detail pages (e.g.
 * https://citizens-initiative.europa.eu/initiatives/details/2025/000005_en) render the
 * table client-side from the register API, so we call that API directly:
 *
 *     https://register.eci.ec.europa.eu/core/api/register/details/{year}/{number}
 *
 * and rebuild the same table (country, signatures, threshold, percentage) locally.
 */
interface EciStatisticsRepository {

    /**
     * Loads statistics from a full initiative detail-page URL, e.g.
     * `https://citizens-initiative.europa.eu/initiatives/details/2025/000005_en`.
     */
    suspend fun getStatistics(pageUrl: String): Result<EciStatistics>

    /**
     * Loads statistics for a specific initiative.
     *
     * @param year Registration year, e.g. 2025.
     * @param number Zero-padded initiative number as it appears in the URL, e.g. "000005".
     */
    suspend fun getStatistics(year: Int, number: String): Result<EciStatistics>
}

class EciStatisticsRepositoryImpl constructor(
    private val client: HttpClient,
) : EciStatisticsRepository {

    companion object {
        private const val DETAILS_ENDPOINT =
            "https://register.eci.ec.europa.eu/core/api/register/details"

        /** Matches ".../initiatives/details/2025/000005" with an optional "_en" locale suffix. */
        private val URL_PATTERN =
            Regex("""/details/(\d{4})/(\d+)(?:_[a-z]{2})?/?$""", RegexOption.IGNORE_CASE)
    }

    override suspend fun getStatistics(pageUrl: String): Result<EciStatistics> {
        val match = URL_PATTERN.find(pageUrl.trim())
            ?: return Result.Error(
                AppError.UnrecognisedUrl(pageUrl),
                retryable = false
            )
        val (year, number) = match.destructured
        return getStatistics(year.toInt(), number)
    }

    override suspend fun getStatistics(year: Int, number: String): Result<EciStatistics> {
        // Keep the zero-padding the API expects (e.g. "000005"), reject junk early.
        if (!number.matches(Regex("""\d+"""))) {
            return Result.Error(AppError.InvalidNumber(number), retryable = false)
        }

        return try {
            val dto = client.get("$DETAILS_ENDPOINT/$year/$number").body<EciDetailsDto>()
            Result.Success(dto.toStatistics(year, number))
        } catch (e: ClientRequestException) {
            when (e.response.status.value) {
                404 -> Result.Error(AppError.EciNotFound(year.toString(), number), e, retryable = false)
                else -> Result.Error(
                    AppError.Client(e.response.status.value, e.response.status.description),
                    e,
                    retryable = false
                )
            }
        } catch (e: ServerResponseException) {
            Result.Error(AppError.SourceServer(Source.ECI, e.response.status.value), e)
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.SourceTimeout(Source.ECI), e)
        } catch (e: ContentConvertException) {
            // A 2xx with a non-JSON/unexpected body (e.g. an error HTML page) — permanent.
            Result.Error(AppError.UnexpectedResponse, e, retryable = false)
        } catch (e: SerializationException) {
            Result.Error(AppError.ParseError, e, retryable = false)
        } catch (e: IOException) {
            Result.Error(AppError.Network(e.message), e)
        } catch (e: Exception) {
            Result.Error(AppError.LoadFailed(e.message), e)
        }
    }
}

/**
 * Maps the raw API payload into the domain table, applying the threshold table that
 * corresponds to the initiative's registration date and resolving country names.
 * Rows are ordered by country name, matching the portal.
 */
private fun EciDetailsDto.toStatistics(year: Int, number: String): EciStatistics {
    val registrationDate = EciReferenceData.parseDate(registrationDate)
    val thresholds = EciReferenceData.thresholdsForRegistration(registrationDate)

    val rows = (sosReport?.entry ?: emptyList()).map { entry ->
        val code = entry.countryCodeType.uppercase()
        EciCountrySignatures(
            countryCode = code,
            countryName = EciReferenceData.countryName(code),
            signatures = entry.total,
            threshold = thresholds[code.lowercase()],
            afterSubmission = entry.afterSubmission
        )
    }.sortedBy { it.countryName }

    // Localised signing links keyed by lower-case language code (e.g. "de" -> ".../?lg=de").
    val supportLinks = linguisticVersions.mapNotNull { version ->
        version.supportLink?.let { version.languageCode.lowercase() to it }
    }.toMap()

    return EciStatistics(
        year = year,
        number = number,
        registrationNumber = comRegNum,
        status = status,
        deadline = deadline,
        totalSignatures = sosReport?.totalSignatures ?: rows.sumOf { it.signatures },
        paperUpdateDate = sosReport?.updateDate,
        onlineUpdateDate = sosReport?.onlineSosUpdateDate,
        rows = rows,
        supportLinks = supportLinks
    )
}
