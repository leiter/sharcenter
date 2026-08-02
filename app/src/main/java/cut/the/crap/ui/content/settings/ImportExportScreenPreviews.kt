package cut.the.crap.ui.content.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.navigation.compose.rememberNavController
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun ImportExportScreenPreview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        ImportExportScreen(
            action = {},
            navController = rememberNavController()
        )
    }
}
