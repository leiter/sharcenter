package cut.the.crap.data.rest

import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.data.rest.task.JobQueueRepositoryImpl
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
    abstract fun bindJobQueueRepository(
        impl: JobQueueRepositoryImpl
    ): JobQueueRepository
}
