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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.platform.UrlOpener
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.action_back
import cut.the.crap.shared.resources.campaign_create_posts_cd
import cut.the.crap.shared.resources.campaign_detail_countries
import cut.the.crap.shared.resources.campaign_detail_disabled_body
import cut.the.crap.shared.resources.campaign_detail_disabled_title
import cut.the.crap.shared.resources.campaign_detail_failed
import cut.the.crap.shared.resources.campaign_detail_locate
import cut.the.crap.shared.resources.campaign_detail_open_country
import cut.the.crap.shared.resources.campaign_detail_publish
import cut.the.crap.shared.resources.campaign_detail_publish_desc
import cut.the.crap.shared.resources.campaign_detail_reach_out
import cut.the.crap.shared.resources.campaign_detail_reach_out_desc
import cut.the.crap.shared.resources.campaign_list_retry
import cut.the.crap.shared.resources.campaign_parliament_badge
import cut.the.crap.ui.components.BottomNavigationBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * One campaign: what to publish, whom to write to, and which countries it covers.
 *
 * This is where `CampaignCountry.url`, `hasParliamentAction` and `Campaign.locateUrl` stop being
 * parsed-and-ignored. All three were in the payload from the start and had no behaviour anywhere;
 * here a country row opens its page, the parliament chip opens the contact action, and locate
 * opens the geolocating entry point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailScreen(
    navController: NavHostController,
    campaignId: String,
    viewModel: CampaignDetailViewModel,
) {
    val state by viewModel.state.collectAsState()
    val urlOpener: UrlOpener = koinInject()
    val campaign = state.campaign

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(campaign?.displayTitle ?: campaignId) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = {
                    if (campaign != null && campaign.countriesWithPosts.isNotEmpty()) {
                        IconButton(onClick = { navController.navigate("campaign_composer/$campaignId") }) {
                            Icon(
                                imageVector = Icons.Filled.Campaign,
                                contentDescription = stringResource(Res.string.campaign_create_posts_cd),
                            )
                        }
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
                state.isDisabled -> Message(
                    title = stringResource(Res.string.campaign_detail_disabled_title),
                    body = stringResource(Res.string.campaign_detail_disabled_body),
                    onRetry = null,
                )

                campaign == null && state.error != null -> Message(
                    title = stringResource(Res.string.campaign_detail_failed),
                    body = null,
                    onRetry = { viewModel.load(forceRefresh = true) },
                )

                campaign != null -> Body(campaign, campaignId, navController, urlOpener)

                else -> Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun Body(
    campaign: Campaign,
    campaignId: String,
    navController: NavHostController,
    urlOpener: UrlOpener,
) {
    var filter by remember { mutableStateOf(CountryFilter.ALL) }
    var sort by remember { mutableStateOf(CountrySort.REGISTRY) }
    val visibleCountries = remember(campaign, filter, sort) {
        campaign.countries.applyFilter(filter).applySort(sort)
    }
    val onFilter: (CountryFilter) -> Unit = { filter = it }
    val onSort: (CountrySort) -> Unit = { sort = it }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        campaign.description?.takeIf { it.isNotBlank() }?.let { description ->
            item { Text(description, style = MaterialTheme.typography.bodyMedium) }
        }

        // "Find my country": the geolocating entry point. Parsed since the first version of the
        // payload, never once opened.
        campaign.locateUrl?.takeIf { it.isNotBlank() }?.let { locateUrl ->
            item {
                OutlinedButton(
                    onClick = { urlOpener.open(locateUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.campaign_detail_locate)) }
            }
        }

        if (campaign.countriesWithPosts.isNotEmpty()) {
            item {
                LaneCard(
                    title = stringResource(Res.string.campaign_detail_publish),
                    body = stringResource(Res.string.campaign_detail_publish_desc),
                    action = stringResource(Res.string.campaign_detail_publish),
                    onAction = { navController.navigate("campaign_composer/$campaignId") },
                )
            }
        }

        if (campaign.countriesWithContacts.isNotEmpty()) {
            item {
                LaneCard(
                    title = stringResource(Res.string.campaign_detail_reach_out),
                    body = stringResource(Res.string.campaign_detail_reach_out_desc),
                    action = null,
                    onAction = null,
                )
            }
        }

        item {
            Text(
                stringResource(Res.string.campaign_detail_countries),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // The filter and sort chips come over from the old country screen. Forty-two countries is
        // too many to scan, and "parliament action" as a filter finally selects for something the
        // user can act on rather than for a decorative chip.
        item { CountryFilterChips(campaign.countries, filter, onFilter) }
        item { CountrySortChips(sort, onSort) }

        items(visibleCountries, key = { it.countryCode }) { country ->
            CountryRow(country, urlOpener)
        }
    }
}

@Composable
private fun LaneCard(title: String, body: String, action: String?, onAction: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (action != null && onAction != null) {
                Button(onClick = onAction) { Text(action) }
            }
        }
    }
}

@Composable
private fun CountryRow(country: CampaignCountry, urlOpener: UrlOpener) {
    val hasPage = country.url.isNotBlank()
    val openCountry = stringResource(Res.string.campaign_detail_open_country, country.countryName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // Only clickable when there is something to open — a row that reacts to a tap by
                // doing nothing is exactly what made the old screen feel broken.
                if (hasPage) Modifier.clickable { urlOpener.open(country.url) } else Modifier
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${country.flag} ${country.countryName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (country.hasPosts) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CountrySubtitle(country)
        }

        // The parliament chip, with a destination at last.
        country.contacts.firstOrNull()?.let { contact ->
            AssistChip(
                onClick = { urlOpener.open(contact.url) },
                label = { Text(contact.label ?: stringResource(Res.string.campaign_parliament_badge)) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        if (hasPage) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = openCountry,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
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
