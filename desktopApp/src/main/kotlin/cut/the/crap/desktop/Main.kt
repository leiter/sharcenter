package cut.the.crap.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.networkModule
import cut.the.crap.data.rest.repositoryModule
import cut.the.crap.di.viewModelModule
import cut.the.crap.share.shareModule
import org.koin.compose.KoinContext
import cut.the.crap.identity.identityModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Desktop base URL for the message/job backend. There is no `BuildConfig` off Android, so the value
 * `:app` injects from `BuildConfig.API_BASE_URL` is a plain constant here. Points at the same local
 * dev server as the Android debug build for now.
 */
private const val API_BASE_URL = "http://192.168.1.100:8080"

/**
 * Origin of the action-campaign site (`/api/abu-safiya`) — a different host from [API_BASE_URL].
 * Point it at the local Flask server (e.g. "http://192.168.178.26:5000") when working on the
 * campaign itself.
 */
private const val CAMPAIGN_BASE_URL = "https://cutthecrap.link"
private const val IS_DEBUG = true

/**
 * The desktop entry point. Starts the shared Koin graph (desktop bindings + the common modules),
 * installs the Coil image loader the same way `MyApplication` does on Android, then opens the
 * single application window hosting [App]. [KoinContext] bridges the started global Koin into the
 * composition so `koinViewModel()` / `koinInject()` resolve.
 */
fun main() {
    startKoin {
        modules(
            // AppConfig is provided inline (no BuildConfig off Android); networkModule is a plain
            // module that reads it — mirrors iOS MainViewController.setupKoin().
            module {
                single {
                    AppConfig(
                        apiBaseUrl = API_BASE_URL,
                        campaignBaseUrl = CAMPAIGN_BASE_URL,
                        isDebug = IS_DEBUG,
                    )
                }
            },
            desktopPlatformModule,
            desktopDatabaseModule,
            identityModule,
            networkModule,
            repositoryModule,
            shareModule,
            viewModelModule,
        )
    }

    // Coil 3 ships no network fetcher, so register the Ktor 2 one — mirrors MyApplication on
    // Android, and reuses the HTTP stack :shared already depends on.
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ShareCenter",
        ) {
            KoinContext {
                App()
            }
        }
    }
}
