package cut.the.crap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun Preview() {

    val filterTitles = listOf(
        "Accounts", "Stock", "Investments",
        "Finance", "Button", "Upcoming"
    )
    val filters = remember {
        val list = mutableStateListOf<MyFilterChipItem>()
        list.addAll(
            filterTitles.mapIndexed { index, item ->
                MyFilterChipItem(
                    text = item,
                    state = when (index % 3){
                        0 -> MyFilterChipState.Unselected
                        1 -> MyFilterChipState.Selected
                        else -> MyFilterChipState.Disabled
                    }
                )
            }
        )
        list
    }

    Column(modifier = Modifier.padding(16.dp)) {
        MyFilterChipRow(
            filterList = filters ,
            onFilterClicked = {},
            onCloseClicked = {}
        )

    }

}
