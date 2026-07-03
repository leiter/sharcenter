package cut.the.crap.ui.content.eci

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import cut.the.crap.R
import cut.the.crap.data.rest.eci.EciCountrySignatures
import cut.the.crap.data.rest.eci.EciStatistics
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import java.text.NumberFormat

// Column weights shared by the header and every data row so they stay aligned.
private const val WEIGHT_COUNTRY = 2.2f
private const val WEIGHT_SIGNATURES = 1.6f
private const val WEIGHT_THRESHOLD = 1.3f
private const val WEIGHT_PERCENTAGE = 1.3f

/**
 * Renders the ECI "Number of signatures per country" table for a parsed [EciStatistics].
 *
 * Columns mirror the portal: Country · Signatures · Threshold · Percentage. Countries
 * that have reached their threshold are highlighted, and an asterisk marks figures
 * recorded after the initiative was submitted (as on the source page).
 */
@Composable
fun EciStatisticsTable(
    statistics: EciStatistics,
    modifier: Modifier = Modifier,
    // The rows to render (already filtered/sorted by the caller). Defaults to all rows.
    rows: List<EciCountrySignatures> = statistics.rows,
    // Total to show in the footer, matching [rows]. Defaults to the initiative-wide total.
    totalSignatures: Long = statistics.totalSignatures
) {
    // NumberFormat instances are relatively expensive; keep them across recompositions.
    val integerFormat = remember { NumberFormat.getIntegerInstance() }
    val percentFormat = remember {
        NumberFormat.getPercentInstance().apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val hasAfterSubmission = rows.any { it.afterSubmission }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.eci_table_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            val updatedText = statistics.onlineUpdateDate
                ?.let { stringResource(R.string.eci_table_updated, it) }
            val subtitle = listOfNotNull(statistics.registrationNumber, updatedText)
                .takeIf { it.isNotEmpty() }?.joinToString(" · ")
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell(stringResource(R.string.eci_col_country), WEIGHT_COUNTRY, TextAlign.Start)
                HeaderCell(stringResource(R.string.eci_col_signatures), WEIGHT_SIGNATURES, TextAlign.End)
                HeaderCell(stringResource(R.string.eci_col_threshold), WEIGHT_THRESHOLD, TextAlign.End)
                HeaderCell(stringResource(R.string.eci_col_percentage), WEIGHT_PERCENTAGE, TextAlign.End)
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            rows.forEach { row ->
                CountryRow(row, integerFormat, percentFormat)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

            // Total row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BodyCell(
                    text = stringResource(R.string.eci_total),
                    weight = WEIGHT_COUNTRY,
                    align = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface,
                    emphasised = true
                )
                BodyCell(
                    text = integerFormat.format(totalSignatures),
                    weight = WEIGHT_SIGNATURES,
                    align = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface,
                    emphasised = true
                )
                Spacer(modifier = Modifier.weight(WEIGHT_THRESHOLD + WEIGHT_PERCENTAGE))
            }

            if (hasAfterSubmission) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.eci_after_submission_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CountryRow(
    row: EciCountrySignatures,
    integerFormat: NumberFormat,
    percentFormat: NumberFormat
) {
    val fraction = row.thresholdFraction
    val thresholdReached = fraction != null && fraction >= 1.0
    val percentageColor = when {
        thresholdReached -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val signaturesText = integerFormat.format(row.signatures) + if (row.afterSubmission) "*" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BodyCell(
            text = row.countryName,
            weight = WEIGHT_COUNTRY,
            align = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface
        )
        BodyCell(
            text = signaturesText,
            weight = WEIGHT_SIGNATURES,
            align = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurface
        )
        BodyCell(
            text = row.threshold?.let { integerFormat.format(it) } ?: stringResource(R.string.eci_not_available),
            weight = WEIGHT_THRESHOLD,
            align = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BodyCell(
            text = fraction?.let { percentFormat.format(it) } ?: stringResource(R.string.eci_not_available),
            weight = WEIGHT_PERCENTAGE,
            align = TextAlign.End,
            color = percentageColor,
            emphasised = thresholdReached
        )
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: TextAlign
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        textAlign = align,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RowScope.BodyCell(
    text: String,
    weight: Float,
    align: TextAlign,
    color: Color,
    emphasised: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        textAlign = align,
        style = if (emphasised) {
            MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        } else {
            MaterialTheme.typography.bodyMedium
        },
        color = color
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EciStatisticsTable(statistics = sampleStatistics())
        }
    }
}

private fun sampleStatistics() = EciStatistics(
    year = 2025,
    number = "000005",
    registrationNumber = "ECI(2025)000005",
    status = "ONGOING",
    deadline = "15/07/2026",
    totalSignatures = 550_872,
    paperUpdateDate = "11/03/2026",
    onlineUpdateDate = "01/07/2026 20:45",
    supportLinks = emptyMap(),
    rows = listOf(
        EciCountrySignatures("AT", "Austria", 5_037, 13_395, afterSubmission = false),
        EciCountrySignatures("BE", "Belgium", 35_212, 14_805, afterSubmission = false),
        EciCountrySignatures("DE", "Germany", 69_666, 67_680, afterSubmission = false),
        EciCountrySignatures("FR", "France", 444_506, 55_695, afterSubmission = false),
        EciCountrySignatures("MT", "Malta", 1_052, 4_230, afterSubmission = true)
    )
)
