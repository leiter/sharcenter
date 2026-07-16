package cut.the.crap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview() {
    Column {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            MySearchBar(query = "Search",
                expanded = true, onQueryChanged = {},
                onQuerySubmit = {}, onExpandedChanged ={} )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            MySearchBar(query = "Find",
                expanded = false, onQueryChanged = {},
                onQuerySubmit = {}, onExpandedChanged ={} )
        }

    }

}
