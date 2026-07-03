package cut.the.crap.ui.content.eci

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.R
import cut.the.crap.data.rest.eci.EciCountrySignatures
import cut.the.crap.data.rest.eci.EciStatistics
import cut.the.crap.ui.components.BottomNavigationBar

/**
 * Standalone screen showing the European Citizens' Initiative "signatures per country"
 * table. The [statistics] are loaded before navigation (from the Posts top bar), so this
 * screen only renders — it shows a fallback if navigated to without data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EciStatisticsScreen(
    navController: NavHostController,
    statistics: EciStatistics?
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.eci_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (statistics != null && statistics.eligibleRows.isNotEmpty()) {
                        IconButton(onClick = { navController.navigate("eci_post_composer") }) {
                            Icon(
                                imageVector = Icons.Filled.Campaign,
                                contentDescription = stringResource(R.string.eci_create_posts_cd)
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
            if (statistics == null) {
                Text(
                    text = stringResource(R.string.eci_no_statistics),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                var filter by remember { mutableStateOf(StatFilter.ALL) }
                var sort by remember { mutableStateOf(StatSort.ALPHABETICAL) }

                val visibleRows = remember(statistics, filter, sort) {
                    statistics.rows.applyFilter(filter).applySort(sort)
                }
                // Keep the initiative-wide total when unfiltered; otherwise sum the shown rows.
                val visibleTotal = if (filter == StatFilter.ALL) {
                    statistics.totalSignatures
                } else {
                    visibleRows.sumOf { it.signatures }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter chips
                    ChipRow {
                        StatFilter.values().forEach { f ->
                            val count = statistics.rows.applyFilter(f).count()
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.eci_filter_chip,
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
                            stringResource(R.string.eci_sort_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
                        )
                        StatSort.values().forEach { s ->
                            FilterChip(
                                selected = sort == s,
                                onClick = { sort = s },
                                label = { Text(stringResource(s.labelRes)) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        EciStatisticsTable(
                            statistics = statistics,
                            rows = visibleRows,
                            totalSignatures = visibleTotal
                        )
                    }
                }
            }
        }
    }
}

private enum class StatFilter(@StringRes val labelRes: Int) {
    ALL(R.string.eci_filter_none),
    REACHED(R.string.eci_filter_reached),
    BELOW(R.string.eci_filter_below)
}

private enum class StatSort(@StringRes val labelRes: Int) {
    PERCENTAGE(R.string.eci_sort_percentage),
    SIGNATURES(R.string.eci_sort_signatures),
    ALPHABETICAL(R.string.eci_sort_alpha)
}

private fun List<EciCountrySignatures>.applyFilter(filter: StatFilter) = when (filter) {
    StatFilter.ALL -> this
    // Only countries with a known threshold can be classified as reached / below.
    StatFilter.REACHED -> filter { val f = it.thresholdFraction; f != null && f >= 1.0 }
    StatFilter.BELOW -> filter { val f = it.thresholdFraction; f != null && f < 1.0 }
}

private fun List<EciCountrySignatures>.applySort(sort: StatSort) = when (sort) {
    // Rows without a threshold sort last (descending).
    StatSort.PERCENTAGE -> sortedByDescending { it.thresholdFraction ?: -1.0 }
    StatSort.SIGNATURES -> sortedByDescending { it.signatures }
    StatSort.ALPHABETICAL -> sortedBy { it.countryName }
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        content = content
    )
}
