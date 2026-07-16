package cut.the.crap.ui.content.links

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.navigation.compose.rememberNavController
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
//        val itemListFlow = MutableStateFlow(emptyList())  //mockedLinkItems
        val totalCountFlow = MutableStateFlow(3)
        val screenStateFlow = MutableStateFlow(LinksScreenState())
        val snackBarFlow = MutableSharedFlow<LinksSnackbar>()

        LinkScreen(
            action = {},
            itemList = MutableStateFlow(emptyList()),  // itemListFlow
            totalCount = totalCountFlow,
            screenState = screenStateFlow,
            navController = rememberNavController(),
            snackBarMessages = snackBarFlow
        )
    }
}
