package cut.the.crap.di

import cut.the.crap.data.rest.YouTubePreviewViewModel
import cut.the.crap.ui.components.ColorHistoryViewModel
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.settings.BackupViewModel
import cut.the.crap.ui.content.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * ViewModel bindings (formerly `@HiltViewModel`). Constructor params are resolved
 * from the other Koin modules by type.
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
}
