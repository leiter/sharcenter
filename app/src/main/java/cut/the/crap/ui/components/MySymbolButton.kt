package cut.the.crap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MySymbolButton(
    symbol: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    topPadding: Dp = 4.dp,
    bottomPadding: Dp = 4.dp
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            textAlign = TextAlign.Center,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else LocalContentColor.current,
            modifier = Modifier
                .padding(topPadding)
                .padding(bottom = bottomPadding),
            style = TextStyle(fontSize = 20.sp)
        )
    }
}


@Preview
@Composable
private fun Preview() {
    cut.the.crap.ui.theme.MyAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            MySymbolButton(
                symbol = "@",
                onClick = { },
                isSelected = true
            )

            MySymbolButton(
                symbol = "#",
                bottomPadding = 0.dp,
                onClick = {  },
                isSelected = true
            )

            MySymbolButton(
                symbol = "?",
                onClick = {  },
                isSelected = true
            )
        }
    }
}
