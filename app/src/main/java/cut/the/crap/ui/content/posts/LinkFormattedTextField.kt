package cut.the.crap.ui.content.posts

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Editable text field that underlines http(s) URLs (via [LinkVisualTransformation]) and
 * opens the tapped link. Because it's editable, tapping normally just places the cursor;
 * the link handler intercepts on the [PointerEventPass.Initial] pass and only consumes
 * the gesture when the tap actually lands on a URL, leaving ordinary editing untouched.
 *
 * The tap-to-offset mapping requires the pointer input to sit in the *same coordinate
 * space* as the text, so [innerTextField] is wrapped in a tightly-fitting Box (the outer
 * Box carries the border/padding). Note: offsets aren't corrected for the field's own
 * vertical scroll, so hit-testing is accurate for content that fits without scrolling.
 */
@Composable
fun LinkFormattedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    placeholder: String? = null,
) {
    val contentColor = LocalContentColor.current
    val linkColor = MaterialTheme.colorScheme.primary
    val transformation = remember(linkColor) { LinkVisualTransformation(linkColor) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val uriHandler = LocalUriHandler.current

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // Read the latest layout/text from inside the long-lived pointerInput coroutine.
    val currentLayout by rememberUpdatedState(layoutResult)
    val currentText by rememberUpdatedState(value.text)

    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val borderWidth = if (isFocused) 2.dp else 1.dp
    val shape = RoundedCornerShape(4.dp)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle.merge(TextStyle(color = contentColor)),
        visualTransformation = transformation,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        onTextLayout = { layoutResult = it },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .border(borderWidth, borderColor, shape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (value.text.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = textStyle.merge(
                            TextStyle(color = contentColor.copy(alpha = 0.5f))
                        ),
                    )
                }
                Box(
                    modifier = Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            val layout = currentLayout ?: return@awaitEachGesture
                            val offset = layout.getOffsetForPosition(down.position)
                            val url = findUrlAt(currentText, offset) ?: return@awaitEachGesture
                            // Tap is on a link: consume the down so the field doesn't move
                            // the cursor, then open the link when the tap is released.
                            down.consume()
                            val up = waitForUpOrCancellation(PointerEventPass.Initial)
                            if (up != null) {
                                up.consume()
                                uriHandler.openUri(url)
                            }
                        }
                    },
                ) {
                    innerTextField()
                }
            }
        },
    )
}
