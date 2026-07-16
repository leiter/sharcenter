package cut.the.crap.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.platform.AppRestarter
import cut.the.crap.platform.Clipboard
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.Sharer
import cut.the.crap.platform.UrlOpener
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.content.NavigationGraph
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.settings.SettingsViewModel
import cut.the.crap.ui.theme.MyAppTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The whole app UI, for every platform.
 *
 * Was `MainActivity.setContent { … }`. Everything it needs now resolves from the Koin graph and the
 * `commonMain` seams, so each launcher is a one-liner: Android's `MainActivity` calls
 * `setContent { App() }` and iOS's `MainViewController()` wraps it in a `ComposeUIViewController`.
 *
 * The ViewModels come from [koinViewModel] rather than being passed in — on Android that resolves
 * against the Activity's `ViewModelStoreOwner` exactly as `by viewModel()` did, so the instances
 * are unchanged.
 */
@Composable
fun App() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val linksViewModel: LinksViewModel = koinViewModel()
    val postsViewModel: PostsViewModel = koinViewModel()

    val clipboard: Clipboard = koinInject()
    val urlOpener: UrlOpener = koinInject()
    val sharer: Sharer = koinInject()
    val notifier: Notifier = koinInject()
    val backupManager: BackupManager = koinInject()
    val appRestarter: AppRestarter = koinInject()

    val settings by settingsViewModel.settings.collectAsState()

    MyAppTheme(themePreference = settings.themePreference) {

        val navController = rememberNavController()
        val scope = rememberCoroutineScope()

        val action: (Action) -> Unit = {
            handleAction(
                action = it,
                linksViewModel = linksViewModel,
                postsViewModel = postsViewModel,
                navController = navController,
                scope = scope,
                clipboard = clipboard,
                urlOpener = urlOpener,
                sharer = sharer,
                notifier = notifier,
                backupManager = backupManager,
                appRestarter = appRestarter,
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavigationGraph(
                navController = navController,
                action = action,
                linksViewModel = linksViewModel,
                postsViewModel = postsViewModel,
                settingsViewModel = settingsViewModel,
            )
        }
    }
}
