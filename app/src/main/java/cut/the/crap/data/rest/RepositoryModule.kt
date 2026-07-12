package cut.the.crap.data.rest

import cut.the.crap.data.preferences.ColorHistoryRepository
import cut.the.crap.data.preferences.SettingsRepository
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
import cut.the.crap.tools.AndroidStringProvider
import cut.the.crap.tools.StringProvider
import org.koin.core.module.dsl.factoryOf
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
    factoryOf(::AndroidStringProvider) bind StringProvider::class

    // Previously provided implicitly by Hilt via constructors (unscoped).
    factoryOf(::SettingsRepository)
    factoryOf(::ColorHistoryRepository)
    factoryOf(::YouTubeMetadataBackfiller)
}
