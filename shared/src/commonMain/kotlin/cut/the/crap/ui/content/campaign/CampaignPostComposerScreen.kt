package cut.the.crap.ui.content.campaign

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.data.rest.campaign.CampaignPost
import cut.the.crap.platform.Notifier
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.action_back
import cut.the.crap.shared.resources.campaign_composer_title
import cut.the.crap.shared.resources.campaign_copy_post
import cut.the.crap.shared.resources.campaign_create_posts_button
import cut.the.crap.shared.resources.campaign_drafts_created
import cut.the.crap.shared.resources.campaign_hide_preview
import cut.the.crap.shared.resources.campaign_no_data
import cut.the.crap.shared.resources.campaign_post_copied
import cut.the.crap.shared.resources.campaign_select_countries
import cut.the.crap.shared.resources.campaign_show_preview
import cut.the.crap.shared.resources.campaign_variant
import cut.the.crap.shared.resources.campaign_variant_label
import cut.the.crap.shared.resources.chars
import cut.the.crap.ui.content.Screen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Lets the user pick campaign countries and turn the campaign's own posts into drafts in the
 * Posts list — one draft per language a country has posts in.
 *
 * Unlike the (dormant) ECI composer this templates nothing: the campaign site ships finished
 * copy, so a "variant" is simply which of a country's posts to take.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignPostComposerScreen(
    navController: NavHostController,
    campaign: Campaign?,
    onCreateDrafts: (List<String>) -> Unit
) {
    val notifier: Notifier = koinInject()
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf(CountryFilter.WITH_POSTS) }
    var sort by remember { mutableStateOf(CountrySort.REGISTRY) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var variant by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }

    val countries = campaign?.countries ?: emptyList()
    val visibleCountries = remember(countries, filter, sort) {
        countries.applyFilter(filter).applySort(sort)
    }

    // One draft per language of each selected country.
    val totalPosts = selected.sumOf { code ->
        countries.firstOrNull { it.countryCode == code }?.postCount ?: 0
    }

    // The variant picker offers as many slots as the richest visible country has.
    val variantSlots = visibleCountries.maxOfOrNull { it.variantCount } ?: 0

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.campaign_composer_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        val texts = selected
                            .mapNotNull { code -> countries.firstOrNull { it.countryCode == code } }
                            .flatMap { country -> country.postsForVariant(variant) }
                            .map { it.text }
                        onCreateDrafts(texts)
                        // The quantity is only known at click time, so the plural can't be
                        // hoisted into composition; resolve it in a coroutine instead.
                        scope.launch {
                            notifier.show(
                                getPluralString(
                                    Res.plurals.campaign_drafts_created, texts.size, texts.size
                                )
                            )
                        }
                        // Land on the Posts screen and drop the campaign screens from the back stack.
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    enabled = totalPosts > 0 && campaign != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        if (totalPosts > 0) pluralStringResource(
                            Res.plurals.campaign_create_posts_button, totalPosts, totalPosts
                        )
                        else stringResource(Res.string.campaign_select_countries)
                    )
                }
            }
        }
    ) { padding ->
        if (campaign == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.campaign_no_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            CountryFilterChips(countries, filter) { filter = it }
            CountrySortChips(sort) { sort = it }

            // Variant picker — only worth showing when some country offers a choice.
            if (variantSlots > 1) {
                VariantPicker(variantSlots, variant) { variant = it }
            }

            Spacer(Modifier.size(4.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(visibleCountries, key = { it.countryCode }) { country ->
                    SelectableCountryItem(
                        country = country,
                        selected = country.countryCode in selected,
                        expanded = country.countryCode in expanded,
                        variant = variant,
                        onToggleSelect = {
                            selected = if (country.countryCode in selected) {
                                selected - country.countryCode
                            } else {
                                selected + country.countryCode
                            }
                        },
                        onToggleExpand = {
                            expanded = if (country.countryCode in expanded) {
                                expanded - country.countryCode
                            } else {
                                expanded + country.countryCode
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VariantPicker(slots: Int, selectedIndex: Int, onSelect: (Int) -> Unit) {
    ChipRow {
        Text(
            stringResource(Res.string.campaign_variant_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
        )
        repeat(slots) { i ->
            FilterChip(
                selected = selectedIndex == i,
                onClick = { onSelect(i) },
                label = { Text(stringResource(Res.string.campaign_variant, i + 1)) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun SelectableCountryItem(
    country: CampaignCountry,
    selected: Boolean,
    expanded: Boolean,
    variant: Int,
    onToggleSelect: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val notifier: Notifier = koinInject()
    val clipboard = LocalClipboardManager.current
    // Resolved in composition so the (non-composable) callback can just use the String.
    val postCopiedMessage = stringResource(Res.string.campaign_post_copied)
    val copyPost: (String) -> Unit = { text ->
        clipboard.setText(AnnotatedString(text))
        notifier.show(postCopiedMessage)
    }

    // The chosen variant in each of the country's languages.
    val posts: List<CampaignPost> = remember(country, variant) { country.postsForVariant(variant) }
    val singleLanguage = posts.size == 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelect() },
                enabled = country.hasPosts
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "${country.flag} ${country.countryName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (country.hasPosts) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                CountrySubtitle(country)
            }
            if (country.hasPosts) {
                // With a single language there's only one post, so offer copy directly
                // in the header, to the left of the expand toggle.
                if (singleLanguage) {
                    IconButton(onClick = { copyPost(posts.first().text) }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = stringResource(Res.string.campaign_copy_post)
                        )
                    }
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) Res.string.campaign_hide_preview
                            else Res.string.campaign_show_preview
                        )
                    )
                }
            }
        }

        if (expanded && country.hasPosts) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                posts.forEach { post ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            // The campaign's own variant label ("IT-2 (breve)") is more useful
                            // than the bare language code once a country has several.
                            "${post.language.uppercase()} · ${post.id}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).padding(top = 8.dp)
                        )
                        // With multiple languages each post gets its own copy button.
                        if (!singleLanguage) {
                            IconButton(onClick = { copyPost(post.text) }) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(Res.string.campaign_copy_post)
                                )
                            }
                        }
                    }
                    Text(
                        post.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        pluralStringResource(Res.plurals.chars, post.text.length, post.text.length),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (post.text.length > 280) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
