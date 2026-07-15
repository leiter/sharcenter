package cut.the.crap.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import cut.the.crap.platform.Clipboard
import cut.the.crap.platform.Sharer
import cut.the.crap.platform.UrlOpener
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.content.NavigationGraph
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.settings.SettingsViewModel
import cut.the.crap.ui.handleAction
import cut.the.crap.ui.theme.MyAppTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The desktop composition root — the counterpart to Android's `MainActivity.setContent`.
 *
 * The window itself is not a `ViewModelStoreOwner` (an Activity is), so we provide one at the root
 * for the app-scoped ViewModels resolved here; screens inside the [NavigationGraph] get their own
 * store from each nav back-stack entry, exactly as on Android.
 *
 * The two Android-specific action arms — database backup/restore — are no-ops here (Decision C,
 * deferred); [DesktopBackupManager] keeps the settings screen resolvable in the meantime.
 */
@Composable
fun App() {
    val storeOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }

    CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
        val settingsViewModel = koinViewModel<SettingsViewModel>()
        val linksViewModel = koinViewModel<LinksViewModel>()
        val postsViewModel = koinViewModel<PostsViewModel>()

        val clipboard = koinInject<Clipboard>()
        val urlOpener = koinInject<UrlOpener>()
        val sharer = koinInject<Sharer>()

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
                    onBackupDatabase = { /* desktop backup deferred (Decision C) */ },
                    onRestoreDatabase = { /* desktop restore deferred (Decision C) */ },
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
}
