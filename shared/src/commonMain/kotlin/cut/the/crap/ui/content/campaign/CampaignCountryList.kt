package cut.the.crap.ui.content.campaign

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.campaign_country_languages
import cut.the.crap.shared.resources.campaign_country_no_posts
import cut.the.crap.shared.resources.campaign_filter_chip
import cut.the.crap.shared.resources.campaign_filter_none
import cut.the.crap.shared.resources.campaign_filter_parliament
import cut.the.crap.shared.resources.campaign_filter_with_posts
import cut.the.crap.shared.resources.campaign_sort_alpha
import cut.the.crap.shared.resources.campaign_sort_label
import cut.the.crap.shared.resources.campaign_sort_registry
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The country-list vocabulary both campaign screens share: how the list can be narrowed and
 * ordered, and the chip rows that drive it. The picker and the composer show the same list for
 * different purposes, so only their defaults differ (the composer starts on [WITH_POSTS], since
 * countries without posts can't produce drafts).
 */
internal enum class CountryFilter(val labelRes: StringResource) {
    ALL(Res.string.campaign_filter_none),
    WITH_POSTS(Res.string.campaign_filter_with_posts),
    PARLIAMENT(Res.string.campaign_filter_parliament)
}

internal enum class CountrySort(val labelRes: StringResource) {
    REGISTRY(Res.string.campaign_sort_registry),
    ALPHABETICAL(Res.string.campaign_sort_alpha)
}

internal fun List<CampaignCountry>.applyFilter(filter: CountryFilter) = when (filter) {
    CountryFilter.ALL -> this
    CountryFilter.WITH_POSTS -> filter { it.hasPosts }
    CountryFilter.PARLIAMENT -> filter { it.hasParliamentAction }
}

internal fun List<CampaignCountry>.applySort(sort: CountrySort) = when (sort) {
    // The campaign's own registry order puts the primary countries first.
    CountrySort.REGISTRY -> this
    CountrySort.ALPHABETICAL -> sortedBy { it.countryName }
}

@Composable
internal fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        content = content
    )
}

/** Filter chips, each labelled with how many countries it would leave. */
@Composable
internal fun CountryFilterChips(
    countries: List<CampaignCountry>,
    selected: CountryFilter,
    onSelect: (CountryFilter) -> Unit
) {
    ChipRow {
        CountryFilter.entries.forEach { filter ->
            val count = countries.applyFilter(filter).count()
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        stringResource(
                            Res.string.campaign_filter_chip,
                            stringResource(filter.labelRes),
                            count
                        )
                    )
                },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
internal fun CountrySortChips(selected: CountrySort, onSelect: (CountrySort) -> Unit) {
    ChipRow {
        Text(
            stringResource(Res.string.campaign_sort_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
        )
        CountrySort.entries.forEach { sort ->
            FilterChip(
                selected = selected == sort,
                onClick = { onSelect(sort) },
                label = { Text(stringResource(sort.labelRes)) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

/** A country's languages and post count — or a note that it has no posts yet. */
@Composable
internal fun CountrySubtitle(country: CampaignCountry, modifier: Modifier = Modifier) {
    Text(
        text = if (country.hasPosts) {
            stringResource(
                Res.string.campaign_country_languages,
                country.languages.joinToString(", ") { it.uppercase() },
                country.posts.size
            )
        } else {
            stringResource(Res.string.campaign_country_no_posts)
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
