package cut.the.crap.ui.content.campaign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.data.rest.campaign.CampaignSummary
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.action_back
import cut.the.crap.shared.resources.campaign_badge_featured
import cut.the.crap.shared.resources.campaign_badge_member
import cut.the.crap.shared.resources.campaign_badge_owner
import cut.the.crap.shared.resources.campaign_list_contacts
import cut.the.crap.shared.resources.campaign_list_counts
import cut.the.crap.shared.resources.campaign_list_empty_body
import cut.the.crap.shared.resources.campaign_list_empty_title
import cut.the.crap.shared.resources.campaign_list_failed
import cut.the.crap.shared.resources.campaign_list_retry
import cut.the.crap.shared.resources.campaign_list_title
import cut.the.crap.ui.components.BottomNavigationBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The campaigns this install can act on.
 *
 * Replaces the country list that used to sit behind the Posts top bar. That screen showed one
 * hardcoded campaign's countries with no `onClick` anywhere; this one is a list of campaigns, and
 * every row goes somewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignListScreen(
    navController: NavHostController,
    viewModel: CampaignListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.campaign_list_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(Res.string.campaign_list_retry),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            when {
                // An error with nothing to show. A failed refresh over an existing list falls
                // through to the list instead — blanking what the user was reading would be worse
                // than briefly showing something slightly stale.
                state.error != null && state.campaigns.isEmpty() -> Message(
                    title = stringResource(Res.string.campaign_list_failed),
                    body = null,
                    onRetry = viewModel::refresh,
                )

                state.isEmpty -> Message(
                    title = stringResource(Res.string.campaign_list_empty_title),
                    body = stringResource(Res.string.campaign_list_empty_body),
                    onRetry = null,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.campaigns, key = { it.id }) { campaign ->
                        CampaignRow(campaign) {
                            navController.navigate("campaign_detail/${campaign.id}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampaignRow(campaign: CampaignSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    campaign.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Badge(campaign)
            }
            campaign.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(
                    Res.string.campaign_list_counts,
                    campaign.countryCount,
                    campaign.postCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (campaign.contactCount > 0) {
                Text(
                    stringResource(Res.string.campaign_list_contacts, campaign.contactCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Yours", "joined" or "included with the app" — three different relationships, three labels. */
@Composable
private fun Badge(campaign: CampaignSummary) {
    val label = when {
        campaign.role == "owner" -> stringResource(Res.string.campaign_badge_owner)
        campaign.isMine -> stringResource(Res.string.campaign_badge_member)
        campaign.featured -> stringResource(Res.string.campaign_badge_featured)
        else -> return
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun Message(title: String, body: String?, onRetry: (() -> Unit)?) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            body?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            onRetry?.let {
                OutlinedButton(onClick = it) {
                    Text(stringResource(Res.string.campaign_list_retry))
                }
            }
        }
    }
}
