package cut.the.crap.data.rest.eci

import cut.the.crap.R
import cut.the.crap.data.rest.Result
import cut.the.crap.tools.StringProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import java.io.IOException
import javax.inject.Inject

/**
 * Loads and parses the "signatures per country" statistics for a European Citizens'
 * Initiative.
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

class EciStatisticsRepositoryImpl @Inject constructor(
    private val client: HttpClient,
    private val strings: StringProvider
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
                strings.get(R.string.eci_error_unrecognised_url, pageUrl),
                retryable = false
            )
        val (year, number) = match.destructured
        return getStatistics(year.toInt(), number)
    }

    override suspend fun getStatistics(year: Int, number: String): Result<EciStatistics> {
        // Keep the zero-padding the API expects (e.g. "000005"), reject junk early.
        if (!number.matches(Regex("""\d+"""))) {
            return Result.Error(strings.get(R.string.eci_error_invalid_number, number), retryable = false)
        }

        return try {
            val dto = client.get("$DETAILS_ENDPOINT/$year/$number").body<EciDetailsDto>()
            Result.Success(dto.toStatistics(year, number))
        } catch (e: ClientRequestException) {
            when (e.response.status.value) {
                404 -> Result.Error(strings.get(R.string.eci_error_not_found, year.toString(), number), e, retryable = false)
                else -> Result.Error(
                    strings.get(R.string.error_client, e.response.status.value, e.response.status.description),
                    e,
                    retryable = false
                )
            }
        } catch (e: ServerResponseException) {
            Result.Error(strings.get(R.string.eci_error_server, e.response.status.value), e)
        } catch (e: SocketTimeoutException) {
            Result.Error(strings.get(R.string.eci_error_timeout), e)
        } catch (e: ContentConvertException) {
            // A 2xx with a non-JSON/unexpected body (e.g. an error HTML page) — permanent.
            Result.Error(strings.get(R.string.eci_error_unexpected_response), e, retryable = false)
        } catch (e: SerializationException) {
            Result.Error(strings.get(R.string.eci_error_parse), e, retryable = false)
        } catch (e: IOException) {
            Result.Error(strings.get(R.string.error_network, e.message ?: strings.get(R.string.error_network_fallback)), e)
        } catch (e: Exception) {
            Result.Error(strings.get(R.string.eci_error_load_failed, e.message ?: strings.get(R.string.error_unknown)), e)
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
