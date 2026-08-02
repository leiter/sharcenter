package cut.the.crap.ui.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.content.campaign.CampaignDetailScreen
import cut.the.crap.ui.content.campaign.CampaignDetailViewModel
import cut.the.crap.ui.content.campaign.CampaignListScreen
import cut.the.crap.ui.content.campaign.CampaignPostComposerScreen
import cut.the.crap.ui.content.links.LinkScreen
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.posts.PostsScreen
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.posts.updateContentItemSortOrders
import cut.the.crap.ui.content.settings.BackupManagementScreen
import cut.the.crap.ui.content.settings.ImportExportScreen
import cut.the.crap.ui.content.settings.SettingsScreen
import cut.the.crap.ui.content.settings.SettingsViewModel
import cut.the.crap.ui.content.settings.identity.IdentityScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
        composable("backup_management") {
            BackupManagementScreen(
                navController = navController
            )
        }
        // Only reachable from Settings, which hides the entry where identityModule is not loaded
        // (iOS — see rememberIdentitySupported).
        composable("identity") {
            IdentityScreen(
                navController = navController
            )
        }
        composable("campaign_list") {
            CampaignListScreen(navController = navController)
        }
        composable(
            "campaign_detail/{campaignId}",
            arguments = listOf(navArgument("campaignId") { type = NavType.StringType }),
        ) { entry ->
            val campaignId = entry.arguments?.read { getStringOrNull("campaignId") }.orEmpty()
            CampaignDetailScreen(
                navController = navController,
                campaignId = campaignId,
                viewModel = koinViewModel { parametersOf(campaignId) },
            )
        }
        composable(
            "campaign_composer/{campaignId}",
            arguments = listOf(navArgument("campaignId") { type = NavType.StringType }),
        ) { entry ->
            val campaignId = entry.arguments?.read { getStringOrNull("campaignId") }.orEmpty()
            // Its own view model instance. Composer is now the second stop (reached straight from
            // the list) and detail the third, reached from here — but the repository serves the
            // campaign from memory once loaded either way, so visiting both costs at most one request.
            val detailViewModel: CampaignDetailViewModel = koinViewModel { parametersOf(campaignId) }
            CampaignPostComposerScreen(
                navController = navController,
                campaign = detailViewModel.state.collectAsState().value.campaign,
                onCreateDrafts = { texts -> postsViewModel.createDraftPosts(texts) }
            )
        }
    }
}
