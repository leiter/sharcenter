package cut.the.crap.data.rest.parser

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.eci.EciSchema
import cut.the.crap.tools.StringProvider
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Verifies the generic, schema-driven JSON extraction against the ECI schema — the proof
 * that `EciSchema.SCHEMA` pulls the right fields/tables out of a register-API payload.
 */
class JsonSourceParserTest {

    private val parser = JsonSourceParser(
        json = Json { ignoreUnknownKeys = true; isLenient = true },
        strings = FakeStringProvider,
    )

    @Test
    fun `extracts scalar fields, typed`() {
        val doc = parseOrFail(SAMPLE_JSON)

        assertThat(doc.field(EciSchema.FIELD_TOTAL).asLongOrNull()).isEqualTo(550_872L)
        assertThat(doc.field(EciSchema.FIELD_REGISTRATION_NUMBER).asStringOrNull()).isEqualTo("ECI(2020)000005")
        assertThat(doc.field(EciSchema.FIELD_STATUS).asStringOrNull()).isEqualTo("ONGOING")
        assertThat(doc.field(EciSchema.FIELD_DEADLINE).asStringOrNull()).isEqualTo("15/07/2026")
        assertThat(doc.field(EciSchema.FIELD_ONLINE_UPDATE_DATE).asStringOrNull()).isEqualTo("01/07/2026 20:45")
    }

    @Test
    fun `extracts the signatures table with typed cells`() {
        val table = parseOrFail(SAMPLE_JSON).table(EciSchema.TABLE_SIGNATURES)
        assertThat(table).isNotNull()
        assertThat(table!!.rows).hasSize(3)

        val de = table.rows.first { it[EciSchema.COL_COUNTRY]?.asStringOrNull() == "DE" }
        assertThat(de[EciSchema.COL_SIGNATURES]?.asLongOrNull()).isEqualTo(69_666L)
        assertThat(de[EciSchema.COL_AFTER_SUBMISSION]?.asBooleanOrNull()).isFalse()

        val mt = table.rows.first { it[EciSchema.COL_COUNTRY]?.asStringOrNull() == "MT" }
        assertThat(mt[EciSchema.COL_AFTER_SUBMISSION]?.asBooleanOrNull()).isTrue()
    }

    @Test
    fun `extracts the support-links table`() {
        val table = parseOrFail(SAMPLE_JSON).table(EciSchema.TABLE_SUPPORT_LINKS)
        assertThat(table!!.rows).hasSize(2)
        val de = table.rows.first { it[EciSchema.COL_LANGUAGE]?.asStringOrNull() == "DE" }
        assertThat(de[EciSchema.COL_LINK]?.asStringOrNull()).isEqualTo("https://eci.ec.europa.eu/005/public/?lg=de")
    }

    @Test
    fun `missing paths become Null rather than failing`() {
        // sosReport absent entirely: total/entry paths resolve to nothing.
        val doc = parseOrFail("""{ "comRegNum": "ECI(2020)000005" }""")
        assertThat(doc.field(EciSchema.FIELD_TOTAL)).isEqualTo(ParsedValue.Null)
        assertThat(doc.table(EciSchema.TABLE_SIGNATURES)!!.rows).isEmpty()
    }

    @Test
    fun `malformed json is a non-retryable error`() {
        val result = parser.parse(RawSource("not json", SourceFormat.JSON), EciSchema.SCHEMA)
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).retryable).isFalse()
    }

    private fun parseOrFail(content: String): ParsedDocument {
        val result = parser.parse(RawSource(content, SourceFormat.JSON), EciSchema.SCHEMA)
        assertThat(result).isInstanceOf(Result.Success::class.java)
        return (result as Result.Success).data
    }

    private object FakeStringProvider : StringProvider {
        override fun get(resId: Int): String = "res:$resId"
        override fun get(resId: Int, vararg formatArgs: Any): String = "res:$resId"
    }

    companion object {
        private val SAMPLE_JSON = """
            {
              "comRegNum": "ECI(2020)000005",
              "registrationDate": "15/02/2020",
              "status": "ONGOING",
              "deadline": "15/07/2026",
              "linguisticVersions": [
                { "languageCode": "DE", "supportLink": "https://eci.ec.europa.eu/005/public/?lg=de" },
                { "languageCode": "FR", "supportLink": "https://eci.ec.europa.eu/005/public/?lg=fr" }
              ],
              "sosReport": {
                "totalSignatures": 550872,
                "updateDate": "11/03/2026",
                "onlineSosUpdateDate": "01/07/2026 20:45",
                "entry": [
                  { "countryCodeType": "DE", "total": 69666, "afterSubmission": false },
                  { "countryCodeType": "FR", "total": 444506, "afterSubmission": false },
                  { "countryCodeType": "MT", "total": 1052, "afterSubmission": true }
                ]
              }
            }
        """.trimIndent()
    }
}
