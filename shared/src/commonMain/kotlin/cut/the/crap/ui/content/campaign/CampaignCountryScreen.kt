package cut.the.crap.ui.content.campaign

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.action_back
import cut.the.crap.shared.resources.campaign_countries_title
import cut.the.crap.shared.resources.campaign_create_posts_cd
import cut.the.crap.shared.resources.campaign_no_data
import cut.the.crap.shared.resources.campaign_parliament_badge
import cut.the.crap.ui.components.BottomNavigationBar
import org.jetbrains.compose.resources.stringResource

/**
 * Standalone screen listing the countries the action campaign covers. The [campaign] is loaded
 * before navigation (from the Posts top bar), so this screen only renders — it shows a fallback
 * if navigated to without data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignCountryScreen(
    navController: NavHostController,
    campaign: Campaign?
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.campaign_countries_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                actions = {
                    if (campaign != null && campaign.countriesWithPosts.isNotEmpty()) {
                        IconButton(onClick = { navController.navigate("campaign_composer") }) {
                            Icon(
                                imageVector = Icons.Filled.Campaign,
                                contentDescription = stringResource(Res.string.campaign_create_posts_cd)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (campaign == null) {
                Text(
                    text = stringResource(Res.string.campaign_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                var filter by remember { mutableStateOf(CountryFilter.ALL) }
                var sort by remember { mutableStateOf(CountrySort.REGISTRY) }

                val visibleCountries = remember(campaign, filter, sort) {
                    campaign.countries.applyFilter(filter).applySort(sort)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    CountryFilterChips(campaign.countries, filter) { filter = it }
                    CountrySortChips(sort) { sort = it }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(visibleCountries, key = { it.countryCode }) { country ->
                            CampaignCountryItem(country)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampaignCountryItem(country: CampaignCountry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${country.flag} ${country.countryName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                // Countries without posts are listed but visibly inert — the composer can't use them.
                color = if (country.hasPosts) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (country.hasParliamentAction) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(Res.string.campaign_parliament_badge)) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        CountrySubtitle(country)
    }
}
