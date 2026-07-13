package cut.the.crap.ui.content

import org.jetbrains.compose.resources.StringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.screen_links
import cut.the.crap.shared.resources.screen_posts
import cut.the.crap.shared.resources.screen_profile
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.posts.PostsScreen
import cut.the.crap.ui.content.posts.updateContentItemSortOrders
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.links.LinkScreen
import cut.the.crap.ui.content.settings.BackupManagementScreen
import cut.the.crap.ui.content.settings.ImportExportScreen
import cut.the.crap.ui.content.eci.EciPostComposerScreen
import cut.the.crap.ui.content.eci.EciStatisticsScreen
import cut.the.crap.ui.content.settings.SettingsScreen
import cut.the.crap.ui.content.settings.SettingsViewModel

@Immutable
sealed class Screen(val route: String, val icon: ImageVector, val title: StringResource) {
    @Immutable
    data object Home : Screen("home", Icons.AutoMirrored.Default.Send, Res.string.screen_posts)
    @Immutable
    data object Search : Screen("search", Icons.Default.Link, Res.string.screen_links)
    @Immutable
    data object Profile : Screen("profile", Icons.Default.Settings, Res.string.screen_profile)
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
                snackBarEvents = postsViewModel.snackBarEvents,
                eciLoading = postsViewModel.eciLoading,
                eciEvents = postsViewModel.eciEvents,
                onLoadEciStatistics = postsViewModel::loadEciStatistics
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
        composable("backup_management") {
            BackupManagementScreen(
                navController = navController
            )
        }
        composable("eci_statistics") {
            EciStatisticsScreen(
                navController = navController,
                statistics = postsViewModel.eciStatistics.collectAsState().value
            )
        }
        composable("eci_post_composer") {
            EciPostComposerScreen(
                navController = navController,
                statistics = postsViewModel.eciStatistics.collectAsState().value,
                onCreateDrafts = { texts -> postsViewModel.createDraftPosts(texts) }
            )
        }
    }
}
