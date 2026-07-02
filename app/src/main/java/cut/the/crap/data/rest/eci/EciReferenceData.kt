package cut.the.crap.data.rest.eci

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Static reference data used to reconstruct the ECI "signatures per country" table.
 *
 * Both the per-country signature thresholds and the country names are baked into the
 * portal's front-end bundle rather than served by the API, so we mirror them here.
 * Values ported from register.eci.ec.europa.eu (front-end `Ao` threshold function and
 * `go` country-name map).
 */
internal object EciReferenceData {

    /** Registration dates arrive as "dd/MM/yyyy". */
    val REGISTRATION_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Minimum statements of support per member state, keyed by lower-case ISO alpha-2 code.
     * Which table applies depends on the initiative's registration date — the thresholds are
     * revised as the EU's population figures change. Ordered newest-first below.
     */
    private val THRESHOLDS_FROM_2020_02_01 = mapOf(
        "at" to 13395, "be" to 14805, "bg" to 11985, "cy" to 4230, "cz" to 14805,
        "dk" to 9870, "ee" to 4935, "fi" to 9870, "fr" to 55695, "de" to 67680,
        "gr" to 14805, "hu" to 14805, "ie" to 9165, "it" to 53580, "lv" to 5640,
        "lt" to 7755, "lu" to 4230, "mt" to 4230, "nl" to 20445, "pl" to 36660,
        "pt" to 14805, "ro" to 23265, "sk" to 9870, "si" to 5640, "es" to 41595,
        "se" to 14805, "hr" to 8460
    )

    private val THRESHOLDS_FROM_2020_01_01 = mapOf(
        "at" to 13518, "be" to 15771, "bg" to 12767, "cy" to 4506, "cz" to 15771,
        "dk" to 9763, "ee" to 4506, "fi" to 9763, "fr" to 55574, "de" to 72096,
        "gr" to 15771, "hu" to 15771, "ie" to 8261, "it" to 54823, "lv" to 6008,
        "lt" to 8261, "lu" to 4506, "mt" to 4506, "nl" to 19526, "pl" to 38301,
        "pt" to 15771, "ro" to 24032, "sk" to 9763, "si" to 6008, "es" to 40554,
        "se" to 15020, "gb" to 54823, "hr" to 8261
    )

    private val THRESHOLDS_FROM_2014_07_01 = mapOf(
        "at" to 13500, "be" to 15750, "bg" to 12750, "cy" to 4500, "cz" to 15750,
        "dk" to 9750, "ee" to 4500, "fi" to 9750, "fr" to 55500, "de" to 72000,
        "gr" to 15750, "hu" to 15750, "ie" to 8250, "it" to 54750, "lv" to 6000,
        "lt" to 8250, "lu" to 4500, "mt" to 4500, "nl" to 19500, "pl" to 38250,
        "pt" to 15750, "ro" to 24000, "sk" to 9750, "si" to 6000, "es" to 40500,
        "se" to 15000, "gb" to 54750, "hr" to 8250
    )

    private val THRESHOLDS_FROM_2012_04_01 = mapOf(
        "at" to 14250, "be" to 16500, "bg" to 13500, "cy" to 4500, "cz" to 16500,
        "dk" to 9750, "ee" to 4500, "fi" to 9750, "fr" to 55500, "de" to 74250,
        "gr" to 16500, "hu" to 16500, "ie" to 9000, "it" to 54750, "lv" to 6750,
        "lt" to 9000, "lu" to 4500, "mt" to 4500, "nl" to 19500, "pl" to 38250,
        "pt" to 16500, "ro" to 24750, "sk" to 9750, "si" to 6000, "es" to 40500,
        "se" to 15000, "gb" to 54750, "hr" to 9000
    )

    /**
     * Returns the threshold table in force for an initiative registered on [registrationDate],
     * keyed by lower-case country code. Empty when the date predates the first known table.
     * The `>` comparisons mirror the portal's own branch order (strictly-after each cutoff).
     */
    fun thresholdsForRegistration(registrationDate: LocalDate?): Map<String, Int> = when {
        registrationDate == null -> emptyMap()
        registrationDate.isAfter(LocalDate.of(2020, 2, 1)) -> THRESHOLDS_FROM_2020_02_01
        registrationDate.isAfter(LocalDate.of(2020, 1, 1)) -> THRESHOLDS_FROM_2020_01_01
        registrationDate.isAfter(LocalDate.of(2014, 7, 1)) -> THRESHOLDS_FROM_2014_07_01
        registrationDate.isAfter(LocalDate.of(2012, 4, 1)) -> THRESHOLDS_FROM_2012_04_01
        else -> emptyMap()
    }

    /** English country names keyed by lower-case ISO alpha-2 code. */
    private val COUNTRY_NAMES = mapOf(
        "at" to "Austria", "be" to "Belgium", "bg" to "Bulgaria", "cy" to "Cyprus",
        "cz" to "Czechia", "de" to "Germany", "dk" to "Denmark", "ee" to "Estonia",
        "es" to "Spain", "fi" to "Finland", "fr" to "France", "gr" to "Greece",
        "hr" to "Croatia", "hu" to "Hungary", "ie" to "Ireland", "it" to "Italy",
        "lt" to "Lithuania", "lu" to "Luxembourg", "lv" to "Latvia", "mt" to "Malta",
        "nl" to "Netherlands", "pl" to "Poland", "pt" to "Portugal", "ro" to "Romania",
        "se" to "Sweden", "si" to "Slovenia", "sk" to "Slovakia", "gb" to "United Kingdom"
    )

    /** Country name for a code (any case), falling back to the upper-case code if unknown. */
    fun countryName(countryCode: String): String =
        COUNTRY_NAMES[countryCode.lowercase()] ?: countryCode.uppercase()

    /** Parses a "dd/MM/yyyy" date, or returns null if it is missing/malformed. */
    fun parseDate(value: String?): LocalDate? = value?.let {
        runCatching { LocalDate.parse(it, REGISTRATION_DATE_FORMAT) }.getOrNull()
    }
}
