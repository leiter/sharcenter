package cut.the.crap

import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import cut.the.crap.ui.App
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.httpClientEngine
import cut.the.crap.data.rest.networkModule
import cut.the.crap.data.rest.repositoryModule
import cut.the.crap.di.iosDatabaseModule
import cut.the.crap.di.iosPlatformModule
import cut.the.crap.di.viewModelModule
import cut.the.crap.share.shareModule
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val koin = startKoin {
        modules(
            module { single { AppConfig(apiBaseUrl = DEV_API_BASE_URL, isDebug = true) } },
            iosPlatformModule,
            iosDatabaseModule,
            networkModule,
            repositoryModule,
            shareModule,
            viewModelModule,
        )
    }.koin
    setupImageLoader()

    // Fire-and-forget daily database backup, mirroring Android's MainActivity.onCreate. Errors are
    // swallowed inside performDailyBackupIfNeeded (it returns false), so this never fails startup.
    CoroutineScope(Dispatchers.Default).launch {
        koin.get<BackupManager>().performDailyBackupIfNeeded()
    }
}

/**
 * Coil's singleton ImageLoader for iOS — the counterpart to Android's
 * `MyApplication : SingletonImageLoader.Factory`. Coil 3 ships no network fetcher, so without this
 * the link-card thumbnails (`AsyncImage`) never load on iOS. `setSafe` sets the factory only if one
 * isn't already installed. The fetcher gets its own Ktor client on the Darwin engine — separate from
 * the API client, which has a base-URL `defaultRequest` that must not be prepended to image URLs.
 */
private fun setupImageLoader() {
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { HttpClient(httpClientEngine()) }))
            }
            .crossfade(true)
            .build()
    }
}

/**
 * The Compose entry point the Xcode app embeds — the real shared [App], the same composable the
 * Android launcher renders (WP7 moved the screens and `NavigationGraph` into commonMain).
 */
fun MainViewController(): UIViewController = ComposeUIViewController(
    // The Info.plist sets CADisableMinimumFrameDurationOnPhone=true (for high-refresh displays),
    // but we also opt out of Compose's *crash-on-startup* strict check so a plist/packaging hiccup
    // can never hard-fail launch. Performance is unaffected — the plist key still applies.
    configure = { enforceStrictPlistSanityCheck = false },
) {
    App()
}
