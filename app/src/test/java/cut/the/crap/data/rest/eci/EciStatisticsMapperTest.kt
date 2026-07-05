package cut.the.crap.data.rest.eci

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.rest.parser.ParsedDocument
import cut.the.crap.data.rest.parser.ParsedTable
import cut.the.crap.data.rest.parser.ParsedValue
import org.junit.Test

/**
 * Verifies the generic→concrete bridge: rows are enriched with the ECI-specific threshold
 * (for the registration date) and country name, links become a language-keyed map, and
 * rows come out ordered by country name — mirroring the old DTO mapping's behavior.
 */
class EciStatisticsMapperTest {

    @Test
    fun `enriches rows with threshold and country name, sorted by name`() {
        val stats = EciStatisticsMapper.map(sampleDocument(), year = 2020, number = "000005")

        // Ordered by country name: France, Germany, Malta.
        assertThat(stats.rows.map { it.countryName }).containsExactly("France", "Germany", "Malta").inOrder()

        val germany = stats.rows.first { it.countryCode == "DE" }
        assertThat(germany.countryName).isEqualTo("Germany")
        // Registration 15/02/2020 selects the 2020-02-01 threshold table (DE = 67680).
        assertThat(germany.threshold).isEqualTo(67680)
        assertThat(germany.signatures).isEqualTo(69_666L)
    }

    @Test
    fun `builds language-keyed support links and carries identity and total`() {
        val stats = EciStatisticsMapper.map(sampleDocument(), year = 2020, number = "000005")

        assertThat(stats.year).isEqualTo(2020)
        assertThat(stats.number).isEqualTo("000005")
        assertThat(stats.totalSignatures).isEqualTo(550_872L)
        assertThat(stats.supportLinks).containsEntry("de", "https://eci.ec.europa.eu/005/public/?lg=de")
    }

    private fun sampleDocument(): ParsedDocument = ParsedDocument(
        fields = mapOf(
            EciSchema.FIELD_TOTAL to ParsedValue.Num(550_872.0),
            EciSchema.FIELD_REGISTRATION_NUMBER to ParsedValue.Str("ECI(2020)000005"),
            EciSchema.FIELD_STATUS to ParsedValue.Str("ONGOING"),
            EciSchema.FIELD_DEADLINE to ParsedValue.Str("15/07/2026"),
            EciSchema.FIELD_REGISTRATION_DATE to ParsedValue.Str("15/02/2020"),
        ),
        tables = mapOf(
            EciSchema.TABLE_SIGNATURES to ParsedTable(
                key = EciSchema.TABLE_SIGNATURES,
                columns = emptyList(),
                rows = listOf(
                    row("DE", 69_666, false),
                    row("FR", 444_506, false),
                    row("MT", 1_052, true),
                )
            ),
            EciSchema.TABLE_SUPPORT_LINKS to ParsedTable(
                key = EciSchema.TABLE_SUPPORT_LINKS,
                columns = emptyList(),
                rows = listOf(
                    mapOf(
                        EciSchema.COL_LANGUAGE to ParsedValue.Str("DE"),
                        EciSchema.COL_LINK to ParsedValue.Str("https://eci.ec.europa.eu/005/public/?lg=de"),
                    ),
                )
            ),
        )
    )

    private fun row(code: String, signatures: Int, afterSubmission: Boolean) = mapOf(
        EciSchema.COL_COUNTRY to ParsedValue.Str(code),
        EciSchema.COL_SIGNATURES to ParsedValue.Num(signatures.toDouble()),
        EciSchema.COL_AFTER_SUBMISSION to ParsedValue.Bool(afterSubmission),
    )
}
