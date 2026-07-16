package cut.the.crap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun Preview() {

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        MyFilterChip(
            text = "Todo",
            onCloseClicked = { },
            onLabelClicked = { }
        )

        MyFilterChip(text = "Todo",
            state = MyFilterChipState.Selected,
            onCloseClicked = { },
            onLabelClicked = { }
        )

        MyFilterChip(text = "Todo",
            state = MyFilterChipState.Disabled,
            onCloseClicked = { },
            onLabelClicked = { }
        )

        MyFilterChip(text = "Todo",
            onCloseClicked = { },
            onLabelClicked = { }
        )

    }

}
