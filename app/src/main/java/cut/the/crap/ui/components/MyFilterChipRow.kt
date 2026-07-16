package cut.the.crap.ui.components

import cut.the.crap.tools.randomUuid
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cut.the.crap.ui.components.FilterChipRowTag.DEFAULT_SUFFIX

@Composable
fun MyFilterChipRow(
    filterList: List<MyFilterChipItem>,
    onFilterClicked: (MyFilterChipItem) -> Unit,
    onCloseClicked: (MyFilterChipItem) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: LazyListState = rememberLazyListState(),
    testTagSuffix: String = DEFAULT_SUFFIX,
) {

    //val filterChipToken = MyAppTheme.component.filterChipsRow

    val previousSelection = remember {
        mutableStateListOf<MyFilterChipItem>()
    }
    val selectedFilters by remember {
        derivedStateOf {
            var currentSelection =
                filterList.filter { it.state == MyFilterChipState.Selected }
            if (previousSelection.isEmpty()) {
                previousSelection.addAll(currentSelection)
            } else {
                if (currentSelection.size > previousSelection.size) {
                    val diff = currentSelection - previousSelection
                    previousSelection.add(0, diff[0])
                    currentSelection = previousSelection
                } else {
                    val diff = previousSelection - currentSelection.toSet()
                    previousSelection.remove(diff[0])
                }

            }
            currentSelection
        }
    }
    val sortedOptions by remember {
        derivedStateOf {
            filterList.sortedBy {
                if (it in selectedFilters) {
                    selectedFilters.indexOf(it) - selectedFilters.size
                } else {
                    filterList.indexOf(it)
                }
            }
        }
    }

    LazyRow(
        state = scrollState,
        modifier = modifier.testTag("${testTagSuffix}_MyFilterChipRow"),
    ) {
        item {
            Spacer(modifier = Modifier.width(16.dp))
        }
        itemsIndexed(sortedOptions, key = { _, item -> item.id }) { index, filter ->
            MyFilterChip(
                text = filter.text,
                onCloseClicked = { onCloseClicked(filter) },
                onLabelClicked = { onFilterClicked(filter) },
                state = filter.state,
                testTagSuffix = testTagSuffix
            )
            if (index + 1 != filterList.size) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        item {
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

private fun Boolean.toSelectedState(): MyFilterChipState {
    return when (this) {
        true -> MyFilterChipState.Selected
        false -> MyFilterChipState.Unselected
    }
}

@Immutable
class MyFilterChipItem(
    val text: String,
    state: MyFilterChipState,
    val id: String = randomUuid(),
) {
    var state by mutableStateOf(state)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MyFilterChipItem) return false
        return this.id == other.id && this.state == other.state
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }
}

private object FilterChipRowTag {
    const val DEFAULT_SUFFIX = "Default"
}
fun MyFilterChipState.toggle(): MyFilterChipState {
    return when (this) {
        MyFilterChipState.Unselected -> MyFilterChipState.Selected
        MyFilterChipState.Selected -> MyFilterChipState.Unselected
        MyFilterChipState.Disabled -> this
    }
}
