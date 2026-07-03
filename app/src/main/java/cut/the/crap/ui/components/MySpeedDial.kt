package cut.the.crap.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cut.the.crap.R
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ListAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MySpeedDialFab(
    action: (Action) -> Unit = {},
    actions: List<MenuItem> = listOf(
        MenuItem(
            title = R.string.app_name,
            icon = Icons.Filled.SwapVert,
            actionPayload = ListAction.InvertList
        ),
        MenuItem(
            title = R.string.app_name,
            icon = Icons.Filled.FolderOpen,
            actionPayload = ListAction.InvertList
        )
    ),
) {
    var isExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.wrapContentSize(), // Changed from fillMaxSize to wrapContentSize
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            if (isExpanded) {
                actions.forEachIndexed { index, item ->
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        SecondaryFab(
                            text = stringResource(id = R.string.app_name),
                            //color = Color(0xFFBB86FC),
                            action = action,
                            item
                            //modifier = Modifier.scale(1f - index * 0.1f)
                        )
                    }
                }
            }
            // Main FAB
            FloatingActionButton(
                onClick = {
                    isExpanded = !isExpanded
                    if (!isExpanded) {
                        coroutineScope.launch {
                            delay(300) // Allow time for animations to finish before hiding
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = stringResource(R.string.speed_dial_cd)
                )
            }
        }
    }
}

@Composable
fun SecondaryFab(
    text: String,
    action: (Action) -> Unit,
    item: MenuItem,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = {  action(item.actionPayload) },
        modifier = modifier.size(48.dp),
        containerColor = item.containerColor ?: MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape
    ) {
        when {
            item.iconRes != null -> Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = "",
                tint = Color.Unspecified
            )

            item.icon != null -> Icon(
                imageVector = item.icon,
                contentDescription = "",
                tint = item.iconTint ?: MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}



@Composable
fun CircularSpeedDialFab() {
    var isExpanded by remember { mutableStateOf(false) }

    val actions = listOf("Action 1", "Action 2", "Action 3", "Action 4", "Action 5")
    val radius = 56.dp // Radius of the circular arrangement

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.BottomEnd),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Secondary FABs in a circular arrangement
        actions.forEachIndexed { index, action ->
            val angle = 2 * PI * index / actions.size // Calculate angle for each FAB
            val xOffset = radius * cos(angle).toFloat()
            val yOffset = radius * sin(angle).toFloat()

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
//                SecondaryFab(
//                    text = action,
//                    color = Color(0xFFBB86FC),
//                    modifier = Modifier
//                        .offset(
//                            x = xOffset,
//                            y = yOffset
//                        )
//                )
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = stringResource(R.string.speed_dial_cd)
            )
        }
    }
}

