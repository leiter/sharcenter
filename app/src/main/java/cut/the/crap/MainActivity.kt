package cut.the.crap


import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.data.preferences.initPreferencesPath
import cut.the.crap.data.rest.YouTubeMetadataBackfiller
import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.data.rest.task.ShareLinksTask
import cut.the.crap.ui.App

import cut.the.crap.data.rest.networkModule
import cut.the.crap.data.rest.repositoryModule
import cut.the.crap.di.androidAppModule
import cut.the.crap.di.platformModule
import cut.the.crap.di.databaseModule
import cut.the.crap.di.viewModelModule
import cut.the.crap.share.shareModule
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Must precede startKoin: the preferences stores are built by the Koin graph, and they
        // need to know where filesDir is. The desktop app resolves its own path and skips this.
        initPreferencesPath(this)
        startKoin {
            androidContext(this@MyApplication)
            modules(
                androidAppModule,
                platformModule,
                databaseModule,
                networkModule,
                repositoryModule,
                shareModule,
                viewModelModule,
            )
        }
    }

    /**
     * Provides the app-wide Coil [ImageLoader] used by every AsyncImage (currently the link
     * thumbnails). Explicitly wires a memory cache and a persistent disk cache so thumbnails
     * survive scrolling and app restarts.
     *
     * Coil 3 ships no network fetcher, so one is registered explicitly; the Ktor 2 fetcher
     * reuses the HTTP stack the app already depends on (and works on desktop later).
     *
     * Cache-header behaviour: Coil 2 needed `respectCacheHeaders(false)` here because some
     * thumbnail hosts (e.g. YouTube) send short-lived / no-store Cache-Control headers that
     * would otherwise force a re-download. Coil 3 ignores cache headers *by default* — you
     * opt in via the `coil-network-cache-control` artifact, which we deliberately do not add.
     * So omitting the old call preserves the previous behaviour.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

/**
 * The Android launcher. The UI itself is `App()` in `:shared/commonMain` (WP7), so this only starts
 * the background jobs that are Android-specific and hands off to Compose. The seams and ViewModels
 * `App()` needs come from the Koin graph, not from here.
 */
class MainActivity : ComponentActivity() {

    val databaseBackupManager: BackupManager by inject()

    val jobQueueRepository: JobQueueRepository by inject()

    val youTubeMetadataBackfiller: YouTubeMetadataBackfiller by inject()

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // Perform daily database backup if needed (first app start of the day)
        lifecycleScope.launch {
            databaseBackupManager.performDailyBackupIfNeeded()
        }

        // Backfill YouTube metadata for older links that were saved without it
        lifecycleScope.launch {
            youTubeMetadataBackfiller.backfillMissing()
        }

        // DEV: Submit test job to job queue server on app start (debug builds only)
        if (BuildConfig.DEBUG) {
            lifecycleScope.launch {
                val testTask = ShareLinksTask(
                    links = listOf(
                        "https://example.com/article1",
                        "https://example.com/article2",
                        "https://twitter.com/test/status/123"
                    )
                )
                jobQueueRepository.submitTask(testTask)
            }
        }

        setContent { App() }
    }
}
