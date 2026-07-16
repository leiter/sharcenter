package cut.the.crap.di

import cut.the.crap.BuildConfig
import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.YouTubeMetadataBackfiller
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Android-only bindings that the now-commonMain `networkModule` / `repositoryModule` cannot carry:
 *
 * - [AppConfig] from `BuildConfig` (the base URL + debug flag `networkModule` reads via DI). iOS
 *   and desktop supply their own.
 * - [YouTubeMetadataBackfiller], which still uses the Context-bound DataStore delegate and so stays
 *   in :app. Its `Context` resolves from `androidContext()`.
 */
val androidAppModule = module {
    single { AppConfig(apiBaseUrl = BuildConfig.API_BASE_URL, isDebug = BuildConfig.DEBUG) }
    factoryOf(::YouTubeMetadataBackfiller)
}
