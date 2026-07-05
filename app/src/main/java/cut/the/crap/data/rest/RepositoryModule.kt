package cut.the.crap.data.rest

import cut.the.crap.data.rest.eci.EciStatisticsRepository
import cut.the.crap.data.rest.eci.EciStatisticsRepositoryImpl
import cut.the.crap.data.rest.parser.HtmlSourceParser
import cut.the.crap.data.rest.parser.JsonSourceParser
import cut.the.crap.data.rest.parser.SourceParser
import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.data.rest.task.JobQueueRepositoryImpl
import cut.the.crap.tools.AndroidStringProvider
import cut.the.crap.tools.StringProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

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

    @Binds
    abstract fun bindEciStatisticsRepository(
        impl: EciStatisticsRepositoryImpl
    ): EciStatisticsRepository

    // Source parsers, one per SourceFormat. The repository injects the whole Set and picks
    // the one that supports its fetched source's format; add a format by adding a binding.
    @Binds
    @IntoSet
    abstract fun bindJsonSourceParser(impl: JsonSourceParser): SourceParser

    @Binds
    @IntoSet
    abstract fun bindHtmlSourceParser(impl: HtmlSourceParser): SourceParser

    @Binds
    abstract fun bindStringProvider(
        impl: AndroidStringProvider
    ): StringProvider
}
