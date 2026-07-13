package cut.the.crap.ui.components

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.DrawableResource

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import cut.the.crap.ui.components.api.Action
import java.nio.file.WatchEvent

data class MenuItem(
    val title: StringResource,
    val icon: ImageVector? = null,
    val actionPayload: Action,
    val containerColor: Color? = null,  // Optional custom color
    val iconTint: Color? = null,        // Optional icon tint
    val iconRes: DrawableResource? = null            // Optional drawable (e.g. multi-color brand logo); takes precedence over [icon]
)

@Composable
fun MyPopupMenu(
    action: (Action) -> Unit,
    menuItems: List<MenuItem>,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true }) {
            Icon(Icons.Filled.Menu, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            menuItems.forEach {
                DropdownMenuItem(
                    leadingIcon = {
                        when {
                            // Brand/multi-color logos: render un-tinted so their own colors are preserved
                            it.iconRes != null -> Box(
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(it.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                            }

                            it.icon != null -> Icon(it.icon, contentDescription = null)
                        }
                    },
                    text = { Text(stringResource(it.title)) },
                    onClick = {
                        expanded = false
                        action(it.actionPayload)
                    }
                )
            }
        }
    }
}