package cut.the.crap.data.rest.eci

import cut.the.crap.data.rest.parser.FieldSpec
import cut.the.crap.data.rest.parser.ParseSchema
import cut.the.crap.data.rest.parser.SourceFormat
import cut.the.crap.data.rest.parser.TableSpec
import cut.the.crap.data.rest.parser.ValueType

/**
 * The ECI campaign expressed as data: the [ParseSchema] that tells the generic JSON parser
 * how to pull the "signatures per country" statistics out of the register API payload
 * (`https://register.eci.ec.europa.eu/core/api/register/details/{year}/{number}`).
 *
 * This replaces the hand-written `EciDetailsDto` → domain mapping's *extraction* half.
 * `EciStatisticsMapper` consumes the resulting `ParsedDocument` and layers on the
 * ECI-specific enrichment (thresholds, country names) that isn't part of parsing.
 *
 * Being a [ParseSchema] value (a `@Serializable` type), this could later move into an asset
 * or remote config unchanged; it lives in code for now.
 */
internal object EciSchema {

    // Output keys shared between the schema and EciStatisticsMapper.
    const val FIELD_TOTAL = "total"
    const val FIELD_REGISTRATION_NUMBER = "registrationNumber"
    const val FIELD_STATUS = "status"
    const val FIELD_DEADLINE = "deadline"
    const val FIELD_REGISTRATION_DATE = "registrationDate"
    const val FIELD_ONLINE_UPDATE_DATE = "onlineUpdateDate"
    const val FIELD_PAPER_UPDATE_DATE = "paperUpdateDate"

    const val TABLE_SIGNATURES = "signatures"
    const val COL_COUNTRY = "country"
    const val COL_SIGNATURES = "signatures"
    const val COL_AFTER_SUBMISSION = "afterSubmission"

    const val TABLE_SUPPORT_LINKS = "supportLinks"
    const val COL_LANGUAGE = "language"
    const val COL_LINK = "link"

    val SCHEMA = ParseSchema(
        format = SourceFormat.JSON,
        fields = listOf(
            FieldSpec(FIELD_TOTAL, "sosReport.totalSignatures", ValueType.NUMBER),
            FieldSpec(FIELD_REGISTRATION_NUMBER, "comRegNum", ValueType.STRING),
            FieldSpec(FIELD_STATUS, "status", ValueType.STRING),
            FieldSpec(FIELD_DEADLINE, "deadline", ValueType.STRING),
            FieldSpec(FIELD_REGISTRATION_DATE, "registrationDate", ValueType.STRING),
            FieldSpec(FIELD_ONLINE_UPDATE_DATE, "sosReport.onlineSosUpdateDate", ValueType.STRING),
            FieldSpec(FIELD_PAPER_UPDATE_DATE, "sosReport.updateDate", ValueType.STRING),
        ),
        tables = listOf(
            TableSpec(
                key = TABLE_SIGNATURES,
                rowsPath = "sosReport.entry",
                columns = listOf(
                    FieldSpec(COL_COUNTRY, "countryCodeType", ValueType.STRING),
                    FieldSpec(COL_SIGNATURES, "total", ValueType.NUMBER),
                    FieldSpec(COL_AFTER_SUBMISSION, "afterSubmission", ValueType.BOOLEAN),
                )
            ),
            TableSpec(
                key = TABLE_SUPPORT_LINKS,
                rowsPath = "linguisticVersions",
                columns = listOf(
                    FieldSpec(COL_LANGUAGE, "languageCode", ValueType.STRING),
                    FieldSpec(COL_LINK, "supportLink", ValueType.STRING),
                )
            ),
        )
    )
}
