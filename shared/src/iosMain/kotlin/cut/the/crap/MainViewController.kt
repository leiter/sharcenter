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

/** Dev API base URL for iOS. Change this to point at your job-queue server. */
private const val DEV_API_BASE_URL = "http://192.168.1.100:8080"

/**
 * Starts the Koin graph for iOS. Called once from the Swift `App` before the first
 * [MainViewController]. Named `setupKoin` rather than `initKoin` because Kotlin/Native renames
 * `init*` exports to `doInit*` in Swift; no parameters because Kotlin default arguments don't
 * bridge to Swift defaults — the config is the single [DEV_API_BASE_URL] constant above.
 *
 * The module set mirrors Android's `MyApplication`: the two iOS platform modules plus the shared
 * network/repository/share/viewModel modules. `AppConfig` is provided inline here (Android reads it
 * from `BuildConfig` in `androidAppModule`).
 */
fun setupKoin() {
    startKoin {
        modules(
            module { single { AppConfig(apiBaseUrl = DEV_API_BASE_URL, isDebug = true) } },
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
fun MainViewController(): UIViewController = ComposeUIViewController(
    // The Info.plist sets CADisableMinimumFrameDurationOnPhone=true (for high-refresh displays),
    // but we also opt out of Compose's *crash-on-startup* strict check so a plist/packaging hiccup
    // can never hard-fail launch. Performance is unaffected — the plist key still applies.
    configure = { enforceStrictPlistSanityCheck = false },
) {
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
