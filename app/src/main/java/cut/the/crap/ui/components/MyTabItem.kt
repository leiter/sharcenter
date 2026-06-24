package cut.the.crap.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cut.the.crap.ui.components.TabBarItemTag.CONTAINER
import cut.the.crap.ui.components.TabBarItemTag.DEFAULT_SUFFIX

@Composable
fun RowScope.MyTabBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    iconContentDescription: String? = null,
//    colors: NavigationBarItemColors = MyTabBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    testTagSuffix: String = DEFAULT_SUFFIX,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(painter = icon, contentDescription = iconContentDescription) },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                //style = MyAppTheme.base.typography.caption
            )
        },
//        colors = colors,
        modifier = modifier.uniqueTestTag(testTagSuffix),
        enabled = true,
        alwaysShowLabel = true,
        interactionSource = interactionSource
    )
}

private fun Modifier.uniqueTestTag(suffix: String, tag: String = CONTAINER) =
    testTag("MyTabBarItem_${tag}_${suffix}")

private object TabBarItemTag {
    const val DEFAULT_SUFFIX = "Default"
    const val CONTAINER = "CONTAINER"
}

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
