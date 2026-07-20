package cut.the.crap.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.platform.AppRestarter
import cut.the.crap.platform.Clipboard
import cut.the.crap.platform.NotificationDuration
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.ObservableNotifier
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
 * Database backup/restore/restart stay deferred on desktop (Decision C), but they are now wired
 * through the inert [cut.the.crap.desktop.DesktopBackupManager] / `DesktopAppRestarter` bindings
 * rather than empty lambdas — so a triggered backup/restore reports its "unsupported" result to the
 * user instead of doing nothing silently.
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
        val notifier = koinInject<Notifier>()
        val backupManager = koinInject<BackupManager>()
        val appRestarter = koinInject<AppRestarter>()

        val settings by settingsViewModel.settings.collectAsState()

        MyAppTheme(themePreference = settings.themePreference) {
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            // Desktop has no system Toast, so DesktopNotifier publishes to a flow that this collects
            // into the snackbar host below — the counterpart to the collector in the shared App().
            // Without it, every notifier.show() from the shared screens is silently dropped.
            val observableNotifier = notifier as? ObservableNotifier
            if (observableNotifier != null) {
                LaunchedEffect(observableNotifier) {
                    observableNotifier.notifications.collect { notification ->
                        snackbarHostState.showSnackbar(
                            message = notification.message,
                            duration = when (notification.duration) {
                                NotificationDuration.Short -> SnackbarDuration.Short
                                NotificationDuration.Long -> SnackbarDuration.Long
                            },
                        )
                    }
                }
            }

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
                Box(modifier = Modifier.fillMaxSize()) {
                    NavigationGraph(
                        navController = navController,
                        action = action,
                        linksViewModel = linksViewModel,
                        postsViewModel = postsViewModel,
                        settingsViewModel = settingsViewModel,
                    )

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}
