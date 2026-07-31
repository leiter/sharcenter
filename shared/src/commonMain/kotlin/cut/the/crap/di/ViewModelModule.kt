package cut.the.crap.di

import cut.the.crap.data.rest.YouTubePreviewViewModel
import cut.the.crap.ui.components.ColorHistoryViewModel
import cut.the.crap.ui.content.campaign.CampaignDetailViewModel
import cut.the.crap.ui.content.campaign.CampaignListViewModel
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.settings.BackupViewModel
import cut.the.crap.ui.content.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * ViewModel bindings (formerly `@HiltViewModel`). Constructor params are resolved
 * from the other Koin modules by type.
 *
 * The DSL is `org.koin.core.module.dsl`, not `org.koin.androidx.viewmodel.dsl` — same functions,
 * but the multiplatform ones, so this module compiles in `commonMain` and the desktop app (WP8)
 * can install it unchanged.
 */
val viewModelModule = module {
    // Explicit factory rather than viewModelOf: LinksViewModel's dispatchers have Kotlin
    // default values, and the reflective `*Of` DSL cannot honour defaults — it would try to
    // resolve a CoroutineDispatcher binding and fail.
    viewModel {
        LinksViewModel(
            contentRepository = get(),
            repository = get(),
            fileAccess = get(),
            settingsRepository = get(),
            keywordRepository = get(),
            jobQueueRepository = get(),
            youTubeRepository = get(),
        )
    }
    viewModelOf(::PostsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::BackupViewModel)
    viewModelOf(::ColorHistoryViewModel)
    viewModelOf(::YouTubePreviewViewModel)
    viewModelOf(::CampaignListViewModel)
    // The campaign id comes from the navigation route, not from the graph, so it is passed in
    // rather than resolved — hence the explicit factory over viewModelOf.
    viewModel { (campaignId: String) -> CampaignDetailViewModel(get(), campaignId) }
}
