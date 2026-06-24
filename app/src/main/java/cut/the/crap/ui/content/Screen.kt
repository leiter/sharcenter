package cut.the.crap.ui.content

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cut.the.crap.R
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.posts.PostsScreen
import cut.the.crap.ui.content.posts.updateContentItemSortOrders
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.links.LinkScreen
import cut.the.crap.ui.content.settings.ImportExportScreen
import cut.the.crap.ui.content.settings.SettingsScreen
import cut.the.crap.ui.content.settings.SettingsViewModel

@Immutable
sealed class Screen(val route: String, val icon: ImageVector, @StringRes val title: Int) {
    @Immutable
    data object Home : Screen("home", Icons.AutoMirrored.Default.Send, R.string.screen_posts)
    @Immutable
    data object Search : Screen("search", Icons.Default.Link, R.string.screen_links)
    @Immutable
    data object Profile : Screen("profile", Icons.Default.Settings, R.string.screen_profile)
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    action: (Action) -> Unit,
    linksViewModel: LinksViewModel,
    postsViewModel: PostsViewModel,
    settingsViewModel: SettingsViewModel
) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            PostsScreen(
                action = action,
                navController = navController,
                screenState = postsViewModel.screenState,
                contentItems = postsViewModel.contentItems,
                onContentItemsReordered = { items ->
                    postsViewModel.updateContentItemSortOrders(items)
                },
                snackBarEvents = postsViewModel.snackBarEvents
            )
        }
        composable(Screen.Search.route) {
            LinkScreen(
                action = action,
                itemList = linksViewModel.listState,
                totalCount = linksViewModel.totalCount,
                screenState = linksViewModel.screenState,
                navController = navController,
                snackBarMessages = linksViewModel.snackBarMessage
            )
        }
        composable(Screen.Profile.route) {
            SettingsScreen(
                action = action,
                navController = navController,
                settings = settingsViewModel.settings,
                onSettingsChanged = settingsViewModel::updateSettings
            )
        }
        composable("import_export") {
            ImportExportScreen(
                action = action,
                navController = navController
            )
        }
    }
}
