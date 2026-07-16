package cut.the.crap.ui.content.eci

import cut.the.crap.platform.Notifier

import org.koin.compose.koinInject

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString

import org.jetbrains.compose.resources.StringResource

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.action_back
import cut.the.crap.shared.resources.chars
import cut.the.crap.shared.resources.eci_band_margin
import cut.the.crap.shared.resources.eci_band_push
import cut.the.crap.shared.resources.eci_band_safe
import cut.the.crap.shared.resources.eci_band_unknown
import cut.the.crap.shared.resources.eci_composer_title
import cut.the.crap.shared.resources.eci_copy_post
import cut.the.crap.shared.resources.eci_create_posts_button
import cut.the.crap.shared.resources.eci_drafts_created
import cut.the.crap.shared.resources.eci_filter_below
import cut.the.crap.shared.resources.eci_filter_chip
import cut.the.crap.shared.resources.eci_filter_eligible
import cut.the.crap.shared.resources.eci_filter_margin
import cut.the.crap.shared.resources.eci_filter_none
import cut.the.crap.shared.resources.eci_hide_preview
import cut.the.crap.shared.resources.eci_label_colon
import cut.the.crap.shared.resources.eci_no_statistics
import cut.the.crap.shared.resources.eci_not_collecting
import cut.the.crap.shared.resources.eci_post_copied
import cut.the.crap.shared.resources.eci_row_needed
import cut.the.crap.shared.resources.eci_select_countries
import cut.the.crap.shared.resources.eci_show_preview
import cut.the.crap.shared.resources.eci_sort_alpha
import cut.the.crap.shared.resources.eci_sort_closest
import cut.the.crap.shared.resources.eci_sort_label
import cut.the.crap.shared.resources.eci_sort_needed
import cut.the.crap.shared.resources.eci_tone_margin
import cut.the.crap.shared.resources.eci_tone_push
import cut.the.crap.shared.resources.eci_variant
import cut.the.crap.data.rest.eci.EciBand
import cut.the.crap.data.rest.eci.EciCountrySignatures
import cut.the.crap.data.rest.eci.EciGeneratedPost
import cut.the.crap.data.rest.eci.EciPostGenerator
import cut.the.crap.data.rest.eci.EciReferenceData
import cut.the.crap.data.rest.eci.EciStatistics
import cut.the.crap.ui.content.Screen

private enum class CountryFilter(val labelRes: StringResource) {
    NONE(Res.string.eci_filter_none),
    ELIGIBLE(Res.string.eci_filter_eligible),
    BELOW(Res.string.eci_filter_below),
    MARGIN(Res.string.eci_filter_margin)
}

private enum class CountrySort(val labelRes: StringResource) {
    CLOSEST_TO_AIM(Res.string.eci_sort_closest),
    FEWEST_NEEDED(Res.string.eci_sort_needed),
    ALPHABETICAL(Res.string.eci_sort_alpha)
}

/**
 * Lets the user pick countries that haven't reached their aim and generate localised
 * motivational posts (one per official language) as drafts in the Posts list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EciPostComposerScreen(
    navController: NavHostController,
    statistics: EciStatistics?,
    onCreateDrafts: (List<String>) -> Unit
) {
    val notifier: Notifier = koinInject()
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf(CountryFilter.BELOW) }
    var sort by remember { mutableStateOf(CountrySort.CLOSEST_TO_AIM) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var belowVariant by remember { mutableIntStateOf(0) }
    var marginVariant by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }

    val rows = statistics?.rows ?: emptyList()
    val visibleRows = remember(rows, filter, sort) { rows.applyFilter(filter).applySort(sort) }

    // Total posts = one per official language of each selected country.
    val totalPosts = selected.sumOf { code ->
        EciReferenceData.officialLanguages(code).size
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.eci_composer_title)) },
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
                            .mapNotNull { code -> rows.firstOrNull { it.countryCode == code } }
                            .flatMap { row ->
                                EciPostGenerator.generateForCountry(
                                    row, statistics!!, belowVariant, marginVariant
                                )
                            }
                            .map { it.text }
                        onCreateDrafts(texts)
                        // The quantity is only known at click time, so the plural can't be
                        // hoisted into composition; resolve it in a coroutine instead.
                        scope.launch {
                            notifier.show(
                                getPluralString(
                                    Res.plurals.eci_drafts_created, texts.size, texts.size
                                )
                            )
                        }
                        // Land on the Posts screen and drop the ECI screens from the back stack.
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    enabled = totalPosts > 0 && statistics != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        if (totalPosts > 0) pluralStringResource(
                            Res.plurals.eci_create_posts_button, totalPosts, totalPosts
                        )
                        else stringResource(Res.string.eci_select_countries)
                    )
                }
            }
        }
    ) { padding ->
        if (statistics == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.eci_no_statistics),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!statistics.isCollectionOpen) {
                Text(
                    stringResource(Res.string.eci_not_collecting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Filter chips
            ChipRow {
                CountryFilter.values().forEach { f ->
                    val count = rows.applyFilter(f).count()
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = {
                            Text(
                                stringResource(
                                    Res.string.eci_filter_chip,
                                    stringResource(f.labelRes),
                                    count
                                )
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Sort chips
            ChipRow {
                Text(
                    stringResource(Res.string.eci_sort_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
                )
                CountrySort.values().forEach { s ->
                    FilterChip(
                        selected = sort == s,
                        onClick = { sort = s },
                        label = { Text(stringResource(s.labelRes)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Tone variant pickers
            val hasBelow = visibleRows.any { it.band == EciBand.BELOW_THRESHOLD }
            val hasMargin = visibleRows.any { it.band == EciBand.BUILDING_MARGIN }
            if (hasBelow) {
                VariantPicker(stringResource(Res.string.eci_tone_push), belowVariant) { belowVariant = it }
            }
            if (hasMargin) {
                VariantPicker(stringResource(Res.string.eci_tone_margin), marginVariant) { marginVariant = it }
            }

            Spacer(Modifier.size(4.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(visibleRows, key = { it.countryCode }) { row ->
                    CountryRow(
                        row = row,
                        statistics = statistics,
                        selected = row.countryCode in selected,
                        expanded = row.countryCode in expanded,
                        belowVariant = belowVariant,
                        marginVariant = marginVariant,
                        onToggleSelect = {
                            selected = if (row.countryCode in selected) {
                                selected - row.countryCode
                            } else {
                                selected + row.countryCode
                            }
                        },
                        onToggleExpand = {
                            expanded = if (row.countryCode in expanded) {
                                expanded - row.countryCode
                            } else {
                                expanded + row.countryCode
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        content = content
    )
}

@Composable
private fun VariantPicker(label: String, selectedIndex: Int, onSelect: (Int) -> Unit) {
    ChipRow {
        Text(
            stringResource(Res.string.eci_label_colon, label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
        )
        listOf(0, 1).forEach { i ->
            FilterChip(
                selected = selectedIndex == i,
                onClick = { onSelect(i) },
                label = { Text(stringResource(Res.string.eci_variant, i + 1)) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun CountryRow(
    row: EciCountrySignatures,
    statistics: EciStatistics,
    selected: Boolean,
    expanded: Boolean,
    belowVariant: Int,
    marginVariant: Int,
    onToggleSelect: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val eligible = row.isEligibleForPost
    val flag = EciReferenceData.flagEmoji(row.countryCode)
    val percent = row.thresholdFraction?.let { " (${(it * 100).toInt()}%)" } ?: ""
    val bandLabel = when (row.band) {
        EciBand.BELOW_THRESHOLD -> stringResource(Res.string.eci_band_push)
        EciBand.BUILDING_MARGIN -> stringResource(Res.string.eci_band_margin)
        EciBand.SAFE -> stringResource(Res.string.eci_band_safe)
        EciBand.UNKNOWN -> stringResource(Res.string.eci_band_unknown)
    }

    val notifier: Notifier = koinInject()
    val clipboard = LocalClipboardManager.current
    // Resolved in composition so the (non-composable) callback can just use the String.
    val postCopiedMessage = stringResource(Res.string.eci_post_copied)
    val copyPost: (String) -> Unit = { text ->
        clipboard.setText(AnnotatedString(text))
        notifier.show(postCopiedMessage)
    }

    // One generated post per official language of the country.
    val posts: List<EciGeneratedPost> = remember(row, statistics, belowVariant, marginVariant, eligible) {
        if (eligible) EciPostGenerator.generateForCountry(row, statistics, belowVariant, marginVariant)
        else emptyList()
    }
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
                enabled = eligible
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "$flag ${row.countryName}$percent",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (eligible) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                val remaining = row.remainingToAim
                Text(
                    text = if (remaining != null)
                        stringResource(Res.string.eci_row_needed, bandLabel, remaining)
                    else bandLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (eligible) {
                // With a single language there's only one post, so offer copy directly
                // in the header, to the left of the expand toggle.
                if (singleLanguage) {
                    IconButton(onClick = { copyPost(posts.first().text) }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = stringResource(Res.string.eci_copy_post)
                        )
                    }
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) Res.string.eci_hide_preview else Res.string.eci_show_preview
                        )
                    )
                }
            }
        }

        if (expanded && eligible) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                posts.forEach { post ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            post.language.uppercase(),
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
                                    contentDescription = stringResource(Res.string.eci_copy_post)
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

private fun List<EciCountrySignatures>.applyFilter(filter: CountryFilter) = when (filter) {
    CountryFilter.NONE -> this
    CountryFilter.ELIGIBLE -> filter { it.isEligibleForPost }
    CountryFilter.BELOW -> filter { it.band == EciBand.BELOW_THRESHOLD }
    CountryFilter.MARGIN -> filter { it.band == EciBand.BUILDING_MARGIN }
}

private fun List<EciCountrySignatures>.applySort(sort: CountrySort) = when (sort) {
    // progressToAim is 0..1; use -1 for rows without an aim so they sort last (descending).
    CountrySort.CLOSEST_TO_AIM ->
        sortedByDescending { it.progressToAim ?: -1.0 }
    // remainingToAim ascending; rows without an aim sort last.
    CountrySort.FEWEST_NEEDED ->
        sortedBy { it.remainingToAim ?: Long.MAX_VALUE }
    CountrySort.ALPHABETICAL ->
        sortedBy { it.countryName }
}
