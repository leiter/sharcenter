package cut.the.crap.data.rest

import cut.the.crap.data.preferences.COLOR_HISTORY_STORE
import cut.the.crap.data.preferences.ColorHistoryRepository
import cut.the.crap.data.preferences.SETTINGS_STORE
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.data.preferences.createPreferencesStore
import cut.the.crap.data.rest.bluesky.BlueskyRepository
import cut.the.crap.data.rest.bluesky.BlueskyRepositoryImpl
import cut.the.crap.data.rest.eci.EciStatisticsRepository
import cut.the.crap.data.rest.eci.EciStatisticsRepositoryImpl
import cut.the.crap.data.rest.mastodon.MastodonRepository
import cut.the.crap.data.rest.mastodon.MastodonRepositoryImpl
import cut.the.crap.data.rest.reddit.RedditRepository
import cut.the.crap.data.rest.reddit.RedditRepositoryImpl
import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.data.rest.task.JobQueueRepositoryImpl
import cut.the.crap.data.rest.tiktok.TikTokRepository
import cut.the.crap.data.rest.tiktok.TikTokRepositoryImpl
import cut.the.crap.tools.UrlResolver
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Remote-repository interface bindings (formerly the Hilt `RepositoryModule` `@Binds`),
 * plus the settings/backfill collaborators Hilt previously provided implicitly via
 * `@Inject` constructors. `factory` mirrors the previous unscoped lifetime.
 */
val repositoryModule = module {
    factoryOf(::MessageRepositoryImpl) bind MessageRepository::class
    factoryOf(::YouTubeRepositoryImpl) bind YouTubeRepository::class
    factoryOf(::BlueskyRepositoryImpl) bind BlueskyRepository::class
    factoryOf(::MastodonRepositoryImpl) bind MastodonRepository::class
    factoryOf(::TikTokRepositoryImpl) bind TikTokRepository::class
    factoryOf(::RedditRepositoryImpl) bind RedditRepository::class
    factoryOf(::JobQueueRepositoryImpl) bind JobQueueRepository::class
    factoryOf(::EciStatisticsRepositoryImpl) bind EciStatisticsRepository::class

    // The preferences stores. These MUST be singles: DataStore throws if two live instances share
    // a file, and the repositories above are factories. Previously the Android
    // `preferencesDataStore` delegate hid this by caching the store on the Context.
    single(named(SETTINGS_STORE)) { createPreferencesStore(SETTINGS_STORE) }
    single(named(COLOR_HISTORY_STORE)) { createPreferencesStore(COLOR_HISTORY_STORE) }

    // Resolves /i/status/ and short links; holds a Ktor client. Was a static object.
    factoryOf(::UrlResolver)

    // Previously provided implicitly by Hilt via constructors (unscoped).
    factory { SettingsRepository(get(named(SETTINGS_STORE))) }
    factory { ColorHistoryRepository(get(named(COLOR_HISTORY_STORE))) }
    factoryOf(::YouTubeMetadataBackfiller)
}
