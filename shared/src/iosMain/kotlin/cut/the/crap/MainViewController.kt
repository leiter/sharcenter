package cut.the.crap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.networkModule
import cut.the.crap.data.rest.repositoryModule
import cut.the.crap.di.iosDatabaseModule
import cut.the.crap.di.iosPlatformModule
import cut.the.crap.di.viewModelModule
import cut.the.crap.share.shareModule
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

/**
 * Starts the Koin graph for iOS. Called once from the Swift `App` before the first
 * [MainViewController]. The base URL / debug flag are parameters so the Xcode app can supply them
 * (from Info.plist or a build setting) rather than hard-coding — they default to the dev server.
 *
 * The module set mirrors Android's `MyApplication`: the two iOS platform modules plus the shared
 * network/repository/share/viewModel modules. `AppConfig` is provided inline here (Android reads it
 * from `BuildConfig` in `androidAppModule`).
 */
fun initKoin(
    apiBaseUrl: String = "http://192.168.1.100:8080",
    isDebug: Boolean = true,
) {
    startKoin {
        modules(
            module { single { AppConfig(apiBaseUrl = apiBaseUrl, isDebug = isDebug) } },
            iosPlatformModule,
            iosDatabaseModule,
            networkModule,
            repositoryModule,
            shareModule,
            viewModelModule,
        )
    }
}

/**
 * The Compose entry point the Xcode app embeds. Renders a placeholder until the shared `App()`
 * composable exists — WP7 moves the screens (and `NavigationGraph`) into commonMain, at which point
 * this becomes `ComposeUIViewController { App() }`.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    PlaceholderApp()
}

@Composable
private fun PlaceholderApp() {
    MaterialTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ShareCenter — iOS shell running. Shared UI lands in WP7.")
        }
    }
}
