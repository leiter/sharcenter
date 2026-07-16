package cut.the.crap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cut.the.crap.R

@Preview
@Composable
private fun Preview() {
    Column(modifier = Modifier
        .background(Color.White)
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {
        MyIconAction(
            iconPainter = painterResource(id = R.mipmap.ic_launcher_foreground),
            onClick = { }, contentDescription = "")

        MyIconAction(
            iconPainter = painterResource(id = R.mipmap.ic_launcher_foreground),
            onClick = {  }, contentDescription = "")

        MyIconAction(
            iconPainter = painterResource(id = R.mipmap.ic_launcher_foreground),
            showBadge = true,
            onClick = {  }, contentDescription = "")
    }
}
