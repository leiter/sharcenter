package cut.the.crap.ui.content.eci

import cut.the.crap.data.rest.eci.EciCountrySignatures
import cut.the.crap.data.rest.eci.EciStatistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper

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
