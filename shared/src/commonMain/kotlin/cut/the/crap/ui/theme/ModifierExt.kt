package cut.the.crap.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun Modifier.customClick(
    onClick: () -> Unit,
    role: Role,
    enabled:Boolean = true,
    clickLabel: String? = null,
    noRipple: Boolean = false,
    interactionSource: MutableInteractionSource? = null
): Modifier = this.composed {
    clickable(
        onClick = onClick,
        onClickLabel = clickLabel,
        enabled = enabled,
        role = role,
        indication = if(noRipple)null else LocalIndication.current,
        interactionSource = interactionSource ?: remember {
            MutableInteractionSource()
        }
    )
}

fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier,
    ifFalse: Modifier = Modifier,
): Modifier {
    return if (condition) {
        this.then(ifTrue)
    } else {
        this.then(ifFalse)
    }
}
fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier,
): Modifier {
    return if (condition) {
        this.then(ifTrue)
    } else {
        this
    }
}

@Composable
fun Modifier.textDependentSize(width: Dp, height: Dp): Modifier {
    val fontScale = LocalDensity.current.fontScale
    val multiplier = if (fontScale<1f)1f else fontScale
    return this then Modifier.size(width = width*multiplier,height = height*multiplier)
}
@Composable
fun Modifier.textDependentHeight(height: Dp): Modifier {
    val fontScale = LocalDensity.current.fontScale
    val multiplier = if (fontScale<1f)1f else fontScale
    return this then Modifier.height(height = height*multiplier)
}


fun Modifier.shimmerModifier(
    shape: Shape = RoundedCornerShape(4.dp)
): Modifier = this.then(Modifier.composed {

    var size by remember{
        mutableStateOf(IntSize.Zero)
    }
    val startColor = MaterialTheme.colorScheme.primary.copy(alpha =0.4f)
    val endColor = MaterialTheme.colorScheme.primary.copy(alpha =0.3f)
    val transition = rememberInfiniteTransition(label = "infinite Transition")
    val startOffsetX by transition.animateFloat(
        initialValue = -TWO *size.width.toFloat(),
        targetValue = TWO *size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetStart")
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                startColor,
                endColor,
                startColor,
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        ),
        shape = shape
    ).onGloballyPositioned { size = it.size }


})

private const val TWO = 2
private const val DURATION = 1000