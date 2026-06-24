package cut.the.crap.ui.components

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import cut.the.crap.ui.components.api.Action

data class MenuItem(
    val title: Int,
    val icon: ImageVector,
    val actionPayload: Action,
    val containerColor: Color? = null,  // Optional custom color
    val iconTint: Color? = null         // Optional icon tint
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
                    leadingIcon = { Icon(it.icon, contentDescription = null) },
                    text = { Text(stringResource(id = it.title)) },
                    onClick = {
                        expanded = false
                        action(it.actionPayload)
                    }
                )
            }
        }
    }
}