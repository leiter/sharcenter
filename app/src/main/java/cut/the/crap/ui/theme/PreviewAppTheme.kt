package cut.the.crap.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cut.the.crap.ui.content.settings.ThemePreference

@Composable
fun PreviewAppTheme(
    darkTheme: Boolean,
    content: @Composable ()-> Unit
){
    val themePreference = if (darkTheme) ThemePreference.DARK else ThemePreference.LIGHT
    MyAppTheme(
        themePreference = themePreference,
        content = content
    )
}

typealias PreviewThemeWrapper = @Composable (@Composable () -> Unit) -> Unit

class PreviewAppThemeProvider : PreviewParameterProvider<PreviewThemeWrapper> {

    override val values: Sequence<PreviewThemeWrapper> = sequenceOf(

        @Composable { content -> PreviewAppTheme(darkTheme = true, content = content) },

        @Composable { content -> PreviewAppTheme(darkTheme = false, content = content) }

    )
}
