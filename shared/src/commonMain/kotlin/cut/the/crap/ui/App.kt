package cut.the.crap.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
        val focusManager = LocalFocusManager.current
        val snackbarHostState = remember { SnackbarHostState() }

        // Render Notifier messages on the targets that have no system Toast. Android's binding is a
        // plain Notifier (the OS draws the Toast above the app), so this is a no-op there; iOS and
        // desktop bind an ObservableNotifier and would otherwise drop every message on the floor.
        // Keyed on the instance, not Unit: the binding is a Koin singleton, but keying on it means
        // a swapped binding re-subscribes instead of leaving a collector on the old flow.
        val observableNotifier = notifier as? ObservableNotifier
        if (observableNotifier != null) {
            LaunchedEffect(observableNotifier) {
                observableNotifier.notifications.collect { notification ->
                    // Not launched into `scope`: showSnackbar suspends until the message is
                    // dismissed, and collecting inline is what serialises a burst into a queue
                    // rather than having each new message cancel the one on screen.
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
            // Tap-outside-to-dismiss: a tap that reaches the root Surface is one no TextField,
            // button, or scrollable consumed, so clearing focus here hides the soft keyboard
            // without stealing taps from interactive content. detectTapGestures only fires on a
            // real tap (not a drag/scroll), so scrolling still works. Common to every platform.
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
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

                // Overlaid on the nav host rather than hosted by a root Scaffold: the screens bring
                // their own Scaffolds (and their own screen-local snackbar hosts), so a root one
                // would fight them over insets. This is the app-wide channel — the counterpart to
                // Android's Toast floating above whatever screen is showing.
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}
