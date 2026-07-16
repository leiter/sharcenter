package cut.the.crap.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Note
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Row(modifier = Modifier.padding(16.dp)) {

        MyTabBarItem(
            selected = true,
            onClick = { },
            icon = rememberVectorPainter(image = Icons.Default.Drafts),
            label = "Drafts"
        )
        MyTabBarItem(
            selected = true,
            onClick = { },
            icon = rememberVectorPainter(image = Icons.Default.Edit),
            label = "Drafts"
        )
        MyTabBarItem(
            selected = true,
            onClick = { },
            icon = rememberVectorPainter(image = Icons.Default.Note),
            label = "Drafts"
        )

        MyTabBarItem(
            selected = false,
            onClick = { },
            icon = rememberVectorPainter(image = Icons.AutoMirrored.Default.Article),
            label = "Content"
        )
        MyTabBarItem(
            selected = false,
            onClick = { },
            icon = rememberVectorPainter(image = Icons.Default.Inventory2),
            label = "Content"
        )
        MyTabBarItem(
            selected = false,
            onClick = { },
            icon = rememberVectorPainter(image = Icons.AutoMirrored.Default.LibraryBooks),
            label = "Content"
        )
    }
}
