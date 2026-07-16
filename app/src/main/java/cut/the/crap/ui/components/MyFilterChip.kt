package cut.the.crap.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.app_name
import cut.the.crap.ui.components.MyFilterChipTag.CONTAINER
import cut.the.crap.ui.components.MyFilterChipTag.DEFAULT_SUFFIX
import cut.the.crap.ui.components.MyFilterChipTag.ICON
import cut.the.crap.ui.components.MyFilterChipTag.SKELETON
import cut.the.crap.ui.components.MyFilterChipTag.TEXT
import cut.the.crap.ui.theme.conditional
import cut.the.crap.ui.theme.customClick
import cut.the.crap.ui.theme.shimmerModifier
import cut.the.crap.ui.theme.textDependentHeight
import cut.the.crap.ui.theme.textDependentSize

enum class MyFilterChipState {
    Unselected, Selected, Disabled
}

sealed interface MyFilterChipData {
    val title: String
    data class ThreeFold(
        override val title: String,


    ): MyFilterChipData
}

@Composable
fun MyFilterChip(
    text: String,
    onCloseClicked: () -> Unit,
    onLabelClicked: () -> Unit,
    modifier: Modifier = Modifier,
    state: MyFilterChipState = MyFilterChipState.Unselected,
    stringProvider: MyFilterStringProvider = MyFilterDefaults.stringProvider,
    iconProvider: MyFilterIconProvider = MyFilterDefaults.iconProvider,
    showSkeleton: Boolean = false,
    testTagSuffix: String = DEFAULT_SUFFIX,
) {

    if (showSkeleton) {
        FilterChipSkeleton(
            text = text,
            modifier = modifier,
            testTagSuffix = testTagSuffix
        )
    } else {
        FilterChip(
            text = text,
            onCloseClicked = onCloseClicked,
            onLabelClicked = onLabelClicked,
            state = state,
            modifier = Modifier.animateContentSize(),
            iconProvider = iconProvider,
            stringProvider = stringProvider,
            testTagSuffix = testTagSuffix
        )
    }
}

const val paddingVertical = 4f
const val paddingHorizontal = 16f
@Composable
private fun FilterChip(
    text: String,
    modifier: Modifier,
    state: MyFilterChipState,
    onCloseClicked: () -> Unit,
    onLabelClicked: () -> Unit,
    stringProvider: MyFilterStringProvider,
    iconProvider: MyFilterIconProvider,
    testTagSuffix: String,
) {
    val isSelected = state == MyFilterChipState.Selected
    val touchTargetSize = 48

    val fontScale = LocalDensity.current.fontScale
    val multiplier = if (fontScale < 1) 1f else fontScale

    val (filterFocus, closeFocus) = remember {
        FocusRequester.createRefs()
    }

    Box(modifier = modifier
        .focusRequester(filterFocus)
        .focusProperties {
            next = closeFocus
            right = closeFocus
        }
        .customClick(
            onClick = onLabelClicked,
            role = Role.Checkbox,
            noRipple = true,
            enabled = state != MyFilterChipState.Disabled,
            clickLabel = text
        )
        .conditional(
            state != MyFilterChipState.Disabled,
            Modifier.semantics {
                selected = state == MyFilterChipState.Selected
            }
        )
        .heightIn(touchTargetSize.dp)
        .uniqueTestTag(testTagSuffix, CONTAINER),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier.textDependentHeight(touchTargetSize.dp),
            contentAlignment = Alignment.CenterEnd) {
            Column(
                modifier = Modifier
                    .heightIn(touchTargetSize.dp)
                    .padding(vertical = paddingVertical.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier
                    .textDependentHeight(28.dp)
                    .background(backgroundColor(state = state), RoundedCornerShape(24.dp))
                    .conditional(
                        isSelected, Modifier.padding(end = (paddingHorizontal * multiplier).dp),
                    ), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .conditional(
                                isSelected,
                                Modifier.padding(start = (touchTargetSize * multiplier).dp),
                                Modifier.padding(horizontal = (paddingHorizontal * multiplier).dp)
                            )
                            .uniqueTestTag(testTagSuffix, TEXT),
                        maxLines = 1,
                        //color = textColor(state = state),
                        //style = MyAppTheme.base.typography.bodyBold
                    )
                }
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .textDependentSize(touchTargetSize.dp, touchTargetSize.dp)
                    .focusRequester(closeFocus)
                    .focusProperties {
                        previous = filterFocus
                        left = filterFocus
                    }
                    .customClick(
                        onClick = onCloseClicked,
                        role = Role.Button,
                        clickLabel = text
                    )
                    .uniqueTestTag(testTagSuffix, ICON),
                contentAlignment = Alignment.Center) {
                MyIconAction(
                    modifier = Modifier.size(16.dp),
                    iconPainter = rememberVectorPainter(image = Icons.Filled.Close),//iconProvider.closeIcon,
                    onClick = onCloseClicked,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = stringProvider.unselectDescriptionTemplate // format string
                )
            }

        }
    }
}

@Composable
fun FilterChipSkeleton(
    text: String,
    modifier: Modifier = Modifier,
    testTagSuffix: String = DEFAULT_SUFFIX,
) {
    val fontScale = LocalDensity.current.fontScale
    val multiplier = if (fontScale < 1) 1f else fontScale

    Box(modifier = modifier
        .textDependentHeight(48.dp)
        .uniqueTestTag(testTagSuffix, SKELETON),
        contentAlignment = Alignment.Center
    ) {

        val textMeasurer = rememberTextMeasurer()
        val textLayoutResult = textMeasurer.measure(
            text = text,
            //style = MyAppTheme.base.typography.bodyBold
        )
        val widthDp = with(LocalDensity.current) {
            textLayoutResult.size.width.toDp()
        } + (2* paddingHorizontal * multiplier).dp
        Box(
            modifier = Modifier
                .height(
                    (20 * multiplier + paddingVertical).dp)
                .width(widthDp)
                .shimmerModifier(shape = RoundedCornerShape(
                    24.dp)
                )
        )
    }
}

@Composable
private fun backgroundColor(state: MyFilterChipState): Color {
//    val color =
//        MyAppTheme.component.filterChips.color.background
    return when (state) {
        MyFilterChipState.Unselected -> MaterialTheme.colorScheme.surface
        MyFilterChipState.Selected -> MaterialTheme.colorScheme.primary
        MyFilterChipState.Disabled -> MaterialTheme.colorScheme.surfaceDim
    }
}

//@Composable
//private fun textColor(state: MyFilterChipState): Color {
//    val color =
//        MyAppTheme.component.filterChips.color.text
//    return when (state) {
//        MyFilterChipState.Unselected -> color.default
//        MyFilterChipState.Selected -> color.selected
//        MyFilterChipState.Disabled -> color.disabled
//    }
//}

@Immutable
data class MyFilterStringProvider(
    val unselectDescriptionTemplate: String,
)

@Immutable
data class MyFilterIconProvider(
    val closeIcon: Painter,
)

object MyFilterDefaults {
    val stringProvider: MyFilterStringProvider @Composable get() = strings()
    val iconProvider: MyFilterIconProvider @Composable get() = icons()

    @Composable
    fun strings(
        unselectDescriptionTemplate: String = stringResource(Res.string.app_name),
    ): MyFilterStringProvider {
        return MyFilterStringProvider(
            unselectDescriptionTemplate = unselectDescriptionTemplate
        )
    }

    @Composable
    fun icons(
        closeIcon: Painter = rememberVectorPainter(image = Icons.Filled.Close),
    ): MyFilterIconProvider {
        return MyFilterIconProvider(
            closeIcon = closeIcon
        )
    }
}

private fun Modifier.uniqueTestTag(suffix: String, tag: String) =
    testTag("MyFilter_${tag}_${suffix}")

private object MyFilterChipTag {
    const val DEFAULT_SUFFIX = "Default"
    const val CONTAINER = "Container"
    const val TEXT = "Text"
    const val ICON = "Icon"
    const val SKELETON = "Default"
}
