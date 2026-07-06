package cut.the.crap.share

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ShareModule {

    /**
     * Ordered chain of handlers consulted for each shared URL. The first whose
     * [SharedLinkHandler.recognizes] returns true owns the share, so [GenericSharedLinkHandler] —
     * which recognizes everything — must stay last. Register new platform handlers ahead of it.
     */
    @Provides
    fun provideSharedLinkHandlers(
        x: XSharedLinkHandler,
        youTube: YouTubeSharedLinkHandler,
        bluesky: BlueskySharedLinkHandler,
        generic: GenericSharedLinkHandler
    ): List<SharedLinkHandler> = listOf(x, youTube, bluesky, generic)
}
