package cut.the.crap.ui.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.posts.PostsScreen
import cut.the.crap.ui.content.posts.updateContentItemSortOrders
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.links.LinkScreen
import cut.the.crap.ui.content.settings.BackupManagementScreen
import cut.the.crap.ui.content.settings.ImportExportScreen
import cut.the.crap.ui.content.campaign.CampaignCountryScreen
import cut.the.crap.ui.content.campaign.CampaignPostComposerScreen
import cut.the.crap.ui.content.settings.SettingsScreen
import cut.the.crap.ui.content.settings.SettingsViewModel

// The [Screen] destinations now live in :shared/commonMain; this graph and the screen composables
// it wires up stay in :app until WP7 moves the UI over.
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
                campaignLoading = postsViewModel.campaignLoading,
                campaignEvents = postsViewModel.campaignEvents,
                onLoadCampaign = postsViewModel::loadCampaign
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
        composable("campaign_countries") {
            CampaignCountryScreen(
                navController = navController,
                campaign = postsViewModel.campaign.collectAsState().value
            )
        }
        composable("campaign_composer") {
            CampaignPostComposerScreen(
                navController = navController,
                campaign = postsViewModel.campaign.collectAsState().value,
                onCreateDrafts = { texts -> postsViewModel.createDraftPosts(texts) }
            )
        }
    }
}
