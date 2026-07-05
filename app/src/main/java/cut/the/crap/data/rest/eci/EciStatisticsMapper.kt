package cut.the.crap.data.rest.eci

import cut.the.crap.data.rest.parser.ParsedDocument

/**
 * Bridges the generic [ParsedDocument] produced by the schema-driven parser back into the
 * concrete [EciStatistics] the existing ECI screens consume.
 *
 * The parser only *extracts* (country code, raw signatures, links); this mapper layers on
 * the ECI-specific enrichment that is not part of parsing and stays hand-written: resolving
 * the threshold table for the registration date, localising country names, and building the
 * per-language signing-link map. Replaces the old private `EciDetailsDto.toStatistics()`.
 *
 * Rows are ordered by country name, matching the portal.
 */
internal object EciStatisticsMapper {

    fun map(doc: ParsedDocument, year: Int, number: String): EciStatistics {
        val registration = EciReferenceData.parseDate(doc.field(EciSchema.FIELD_REGISTRATION_DATE).asStringOrNull())
        val thresholds = EciReferenceData.thresholdsForRegistration(registration)

        val rows = doc.table(EciSchema.TABLE_SIGNATURES)?.rows.orEmpty().mapNotNull { row ->
            val code = row[EciSchema.COL_COUNTRY]?.asStringOrNull()?.uppercase() ?: return@mapNotNull null
            EciCountrySignatures(
                countryCode = code,
                countryName = EciReferenceData.countryName(code),
                signatures = row[EciSchema.COL_SIGNATURES]?.asLongOrNull() ?: 0L,
                threshold = thresholds[code.lowercase()],
                afterSubmission = row[EciSchema.COL_AFTER_SUBMISSION]?.asBooleanOrNull() ?: false
            )
        }.sortedBy { it.countryName }

        // Localised signing links keyed by lower-case language code (e.g. "de" -> ".../?lg=de").
        val supportLinks = doc.table(EciSchema.TABLE_SUPPORT_LINKS)?.rows.orEmpty().mapNotNull { row ->
            val language = row[EciSchema.COL_LANGUAGE]?.asStringOrNull()?.lowercase() ?: return@mapNotNull null
            val link = row[EciSchema.COL_LINK]?.asStringOrNull() ?: return@mapNotNull null
            language to link
        }.toMap()

        return EciStatistics(
            year = year,
            number = number,
            registrationNumber = doc.field(EciSchema.FIELD_REGISTRATION_NUMBER).asStringOrNull(),
            status = doc.field(EciSchema.FIELD_STATUS).asStringOrNull(),
            deadline = doc.field(EciSchema.FIELD_DEADLINE).asStringOrNull(),
            totalSignatures = doc.field(EciSchema.FIELD_TOTAL).asLongOrNull() ?: rows.sumOf { it.signatures },
            paperUpdateDate = doc.field(EciSchema.FIELD_PAPER_UPDATE_DATE).asStringOrNull(),
            onlineUpdateDate = doc.field(EciSchema.FIELD_ONLINE_UPDATE_DATE).asStringOrNull(),
            rows = rows,
            supportLinks = supportLinks
        )
    }
}
