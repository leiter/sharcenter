package cut.the.crap.data.rest

import cut.the.crap.data.rest.bluesky.BlueskyRepository
import cut.the.crap.data.rest.bluesky.BlueskyRepositoryImpl
import cut.the.crap.data.rest.mastodon.MastodonRepository
import cut.the.crap.data.rest.mastodon.MastodonRepositoryImpl
import cut.the.crap.data.rest.eci.EciStatisticsRepository
import cut.the.crap.data.rest.eci.EciStatisticsRepositoryImpl
import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.data.rest.task.JobQueueRepositoryImpl
import cut.the.crap.tools.AndroidStringProvider
import cut.the.crap.tools.StringProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMessageRepository(
        impl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    abstract fun bindYouTubeRepository(
        impl: YouTubeRepositoryImpl
    ): YouTubeRepository

    @Binds
    abstract fun bindBlueskyRepository(
        impl: BlueskyRepositoryImpl
    ): BlueskyRepository

    @Binds
    abstract fun bindMastodonRepository(
        impl: MastodonRepositoryImpl
    ): MastodonRepository

    @Binds
    abstract fun bindJobQueueRepository(
        impl: JobQueueRepositoryImpl
    ): JobQueueRepository

    @Binds
    abstract fun bindEciStatisticsRepository(
        impl: EciStatisticsRepositoryImpl
    ): EciStatisticsRepository

    @Binds
    abstract fun bindStringProvider(
        impl: AndroidStringProvider
    ): StringProvider
}
