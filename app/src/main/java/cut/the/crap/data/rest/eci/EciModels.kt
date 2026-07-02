package cut.the.crap.data.rest.eci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Raw JSON for a single initiative as returned by the European Citizens' Initiative
 * register API:
 *
 *     https://register.eci.ec.europa.eu/core/api/register/details/{year}/{number}
 *
 * Only the fields needed to build the "signatures per country" table are declared;
 * the client is configured with `ignoreUnknownKeys = true`, so the many other fields
 * (members, funding, logo, …) are simply ignored.
 */
@Serializable
internal data class EciDetailsDto(
    @SerialName("comRegNum")
    val comRegNum: String? = null,

    /** Registration date, formatted "dd/MM/yyyy". Selects which threshold table applies. */
    @SerialName("registrationDate")
    val registrationDate: String? = null,

    @SerialName("status")
    val status: String? = null,

    @SerialName("sosReport")
    val sosReport: EciSosReportDto? = null
)

/** "Statements of support" report — the per-country signature statistics. */
@Serializable
internal data class EciSosReportDto(
    @SerialName("totalSignatures")
    val totalSignatures: Long? = null,

    /** Date paper signatures were last reported by the organisers ("dd/MM/yyyy"). */
    @SerialName("updateDate")
    val updateDate: String? = null,

    /** Timestamp of the last online signature count ("dd/MM/yyyy HH:mm"). */
    @SerialName("onlineSosUpdateDate")
    val onlineSosUpdateDate: String? = null,

    @SerialName("entry")
    val entry: List<EciSosEntryDto> = emptyList()
)

@Serializable
internal data class EciSosEntryDto(
    /** ISO 3166-1 alpha-2 country code, e.g. "DE", "FR". */
    @SerialName("countryCodeType")
    val countryCodeType: String,

    /** Number of statements of support (online + reported paper) for this country. */
    @SerialName("total")
    val total: Long,

    /**
     * True when the count was recorded after the initiative was submitted for
     * verification. The portal marks such figures with an asterisk.
     */
    @SerialName("afterSubmission")
    val afterSubmission: Boolean = false
)

// ---------------------------------------------------------------------------
// Domain models exposed to the rest of the app
// ---------------------------------------------------------------------------

/**
 * One row of the "Number of signatures per country" table.
 *
 * @property countryCode ISO alpha-2 code, upper-case (e.g. "DE").
 * @property countryName Localised country name (falls back to the code if unknown).
 * @property signatures Statements of support collected in this country.
 * @property threshold Minimum signatures required in this country, or `null` when no
 *   threshold applies (e.g. a non-EU code or a registration date outside the known tables).
 * @property afterSubmission Whether the figure was recorded after submission (asterisk on the site).
 */
data class EciCountrySignatures(
    val countryCode: String,
    val countryName: String,
    val signatures: Long,
    val threshold: Int?,
    val afterSubmission: Boolean
) {
    /**
     * Signatures as a fraction of the country threshold (1.0 == threshold reached),
     * or `null` when there is no threshold. Multiply by 100 for a percentage.
     */
    val thresholdFraction: Double?
        get() = threshold?.takeIf { it > 0 }?.let { signatures.toDouble() / it }
}

/**
 * The parsed statistics table for one initiative.
 *
 * @property rows Per-country rows, ordered by country name (as on the portal).
 * @property totalSignatures Total statements of support across all countries.
 */
data class EciStatistics(
    val year: Int,
    val number: String,
    val registrationNumber: String?,
    val status: String?,
    val totalSignatures: Long,
    val paperUpdateDate: String?,
    val onlineUpdateDate: String?,
    val rows: List<EciCountrySignatures>
)
