package cut.the.crap.share

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Shared-link handler chain (formerly the Hilt `ShareModule`).
 *
 * Ordered chain consulted for each shared URL. The first whose
 * [SharedLinkHandler.recognizes] returns true owns the share, so [GenericSharedLinkHandler] —
 * which recognizes everything — must stay last. Register new platform handlers ahead of it.
 */
val shareModule = module {
    factoryOf(::XSharedLinkHandler)
    factoryOf(::YouTubeSharedLinkHandler)
    factoryOf(::BlueskySharedLinkHandler)
    factoryOf(::MastodonSharedLinkHandler)
    factoryOf(::TikTokSharedLinkHandler)
    factoryOf(::RedditSharedLinkHandler)
    factoryOf(::GenericSharedLinkHandler)

    factory<List<SharedLinkHandler>> {
        listOf(
            get<XSharedLinkHandler>(),
            get<YouTubeSharedLinkHandler>(),
            get<BlueskySharedLinkHandler>(),
            get<MastodonSharedLinkHandler>(),
            get<TikTokSharedLinkHandler>(),
            get<RedditSharedLinkHandler>(),
            get<GenericSharedLinkHandler>(),
        )
    }

    // The platform-agnostic share pipeline. Android's ShareReceiverActivity and a future iOS Share
    // Extension both resolve this rather than each reimplementing resolve/save/enrich.
    factory { SharedUrlProcessor(get(), get(), get()) }
}
