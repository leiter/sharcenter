package cut.the.crap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cut.the.crap.R
import cut.the.crap.ui.components.IconActionTag.DEFAULT_SUFFIX
import cut.the.crap.ui.components.IconActionTag.ICON
import cut.the.crap.ui.components.IconActionTag.ICON_ACTION
import cut.the.crap.ui.components.IconActionTag.SKELETON
import cut.the.crap.ui.theme.conditional
import cut.the.crap.ui.theme.shimmerModifier

@Composable
fun MyIconAction(
    iconPainter: Painter,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    showSkeleton: Boolean = false,
    enabled: Boolean = true,
    showBadge: Boolean = false,
    interactionSource: MutableInteractionSource = remember {
        MutableInteractionSource()
    },
    testingTagSuffix: String = DEFAULT_SUFFIX,
) {
    if (showSkeleton) {
        MyIconActionSkeleton(
            modifier = modifier,
            testingTagSuffix = testingTagSuffix
        )
    } else {

        val touchTargetSize = 48
        val contentColor = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(0.38f)
            iconTint != null -> iconTint
            else -> MaterialTheme.colorScheme.primary
        }

        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            content = {
                Box(
                    modifier = modifier
                        .minimumInteractiveComponentSize()
                        .size(touchTargetSize.dp)
                        .clickable(
                            onClick = onClick,
                            enabled = enabled,
                            role = Role.Button,
                            interactionSource = interactionSource,

                            indication = ripple(bounded = false,
                                radius = touchTargetSize.dp / 2)
                        )
                        .uniqueTestTag(testingTagSuffix, ICON_ACTION),

                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .conditional(showBadge,
                                Modifier.graphicsLayer {
                                    clip = true
                                    shape = CircleMaskShape(
                                        radius = MASK_RADIUS.dp.toPx(),
                                        offsetX = (touchTargetSize - ICON_SIZE - TWO * MASK_RADIUS).dp.toPx(),
                                        offsetY = 0f
                                    )
                                })
                            .size(24.dp)
                            .uniqueTestTag(testingTagSuffix, ICON),
                        painter = iconPainter,
                        contentDescription = contentDescription
                    )
                    if (showBadge) {
                        Box(modifier = Modifier
                            .padding(start = 14.dp, bottom = 15.dp)
                            .size(8.dp)
                            .background(
                                Color.Red, //MyAppTheme.component.iconAction.color.icon.dot,
                                CircleShape)
                        )
                    }
                }
            }
        )
    }
}

private const val ICON_SIZE = 20f
private const val MASK_RADIUS = 5f
private const val TWO = 2

@Composable
fun MyIconActionSkeleton(
    modifier: Modifier = Modifier,
    testingTagSuffix: String = DEFAULT_SUFFIX,
) {

    Box(
        modifier = modifier
            .size(44.dp)
            .uniqueTestTag(testingTagSuffix, SKELETON),
        contentAlignment = Alignment.Center) {
        Box(modifier = Modifier
            .size(24.dp)
            .shimmerModifier(shape = RectangleShape))
    }
}

private fun Modifier.uniqueTestTag(suffix: String, tag: String) =
    testTag("MyIconAction_${tag}_${suffix}")

private object IconActionTag {
    const val DEFAULT_SUFFIX = "Default"
    const val ICON_ACTION = "Default"
    const val ICON = "Default"
    const val SKELETON = "Default"
}

class CircleMaskShape(private val radius: Float, private val offsetX: Float, private val offsetY: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            addRect(Rect(Offset.Zero, size))
            addOval(
                Rect(
                    Offset(offsetX, offsetY),
                    Offset(offsetX + radius * 2, offsetY + radius * 2),
                )
            )
            fillType = PathFillType.EvenOdd
        }
        return Outline.Generic(path = path)
    }
}
