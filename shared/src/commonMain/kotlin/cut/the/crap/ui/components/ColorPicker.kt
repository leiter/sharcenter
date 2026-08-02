package cut.the.crap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * An HSV colour picker: a saturation/value square, a hue slider and an editable hex field.
 *
 * The component is deliberately built only from multiplatform Compose APIs
 * (`androidx.compose.ui.graphics`, `foundation`, `material3`) and pure-Kotlin maths — no
 * `android.graphics.*`, no `toArgb`, no `String.format`. It can move to `commonMain` unchanged
 * when the project migrates to Kotlin Multiplatform.
 *
 * Hue/saturation/value are the internal source of truth (rather than the emitted [Color]) so that
 * dragging value or saturation to zero — where the colour is a pure grey and hue is mathematically
 * undefined — does not lose the user's chosen hue.
 *
 * @param initialColor the colour the picker opens on. Changing it resets the internal HSV state.
 * @param onColorChanged invoked with the live colour on every adjustment.
 * @param recentColors previously picked colours, most-recent first, shown as a tappable history
 *   strip below the picker. Empty hides the strip. Tapping a swatch loads that colour.
 * @param recentPreviewCount how many history swatches to show before the strip offers to expand.
 */
@Composable
fun ColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    showHexField: Boolean = true,
    // Label for the hex input field. Passed in (not resolved here) so this component stays free of
    // Android string-resource lookups, keeping it portable for the planned KMP migration.
    hexLabel: String = "Hex",
    recentColors: List<Color> = emptyList(),
    recentPreviewCount: Int = 12,
) {
    val initialHsv = remember(initialColor) { initialColor.toHsv() }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv.hue) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv.saturation) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv.value) }

    val currentColor = Color.hsv(hue, saturation, value)

    // Keep the latest callback without restarting the long-lived pointerInput coroutines.
    val latestOnColorChanged by rememberUpdatedState(onColorChanged)
    val emit = { latestOnColorChanged(Color.hsv(hue, saturation, value)) }

    // Load an arbitrary colour into the HSV source of truth (used by the hex field and history).
    val applyColor: (Color) -> Unit = { c ->
        val hsv = c.toHsv()
        hue = hsv.hue
        saturation = hsv.saturation
        value = hsv.value
        emit()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SaturationValueArea(
            hue = hue,
            saturation = saturation,
            value = value,
            onChange = { s, v ->
                saturation = s
                value = v
                emit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f),
        )

        HueSlider(
            hue = hue,
            onHueChange = {
                hue = it
                emit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        )

        if (showHexField) {
            HexRow(
                color = currentColor,
                hexLabel = hexLabel,
                onHexColor = applyColor,
            )
        }

        if (recentColors.isNotEmpty()) {
            ColorHistoryStrip(
                colors = recentColors,
                previewCount = recentPreviewCount,
                selectedColor = currentColor,
                onSelect = applyColor,
            )
        }
    }
}

/**
 * A modal [ColorPicker] with Cancel / OK actions. [onConfirm] receives the chosen colour only when
 * the user confirms; dismissing discards the selection.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    hexLabel: String,
    recentColors: List<Color> = emptyList(),
) {
    var picked by remember { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            ColorPicker(
                initialColor = initialColor,
                onColorChanged = { picked = it },
                modifier = Modifier.fillMaxWidth(),
                hexLabel = hexLabel,
                recentColors = recentColors,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

@Composable
private fun SaturationValueArea(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hueColor = Color.hsv(hue, 1f, 1f)
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { pos -> onChange(sv(pos.x, size.width), sv(pos.y, size.height, invert = true)) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos -> onChange(sv(pos.x, size.width), sv(pos.y, size.height, invert = true)) },
                ) { change, _ ->
                    onChange(sv(change.position.x, size.width), sv(change.position.y, size.height, invert = true))
                }
            },
    ) {
        // Saturation: white → full-hue colour, left to right.
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
        // Value: transparent → black, top to bottom.
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        val cx = saturation * size.width
        val cy = (1f - value) * size.height
        val r = 9.dp.toPx()
        // Dark halo then white ring so the thumb reads on any background.
        drawCircle(Color.Black.copy(alpha = 0.6f), radius = r, center = Offset(cx, cy), style = Stroke(width = 3.dp.toPx()))
        drawCircle(Color.White, radius = r, center = Offset(cx, cy), style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hueColors = remember {
        listOf(0, 60, 120, 180, 240, 300, 360).map { Color.hsv(it.toFloat(), 1f, 1f) }
    }
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures { pos -> onHueChange(sv(pos.x, size.width) * 360f) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos -> onHueChange(sv(pos.x, size.width) * 360f) },
                ) { change, _ -> onHueChange(sv(change.position.x, size.width) * 360f) }
            },
    ) {
        drawRect(brush = Brush.horizontalGradient(hueColors))
        val x = (hue / 360f) * size.width
        val cy = size.height / 2f
        val r = size.height / 2f - 2.dp.toPx()
        drawCircle(Color.Black.copy(alpha = 0.6f), radius = r, center = Offset(x, cy), style = Stroke(width = 3.dp.toPx()))
        drawCircle(Color.White, radius = r, center = Offset(x, cy), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun HexRow(
    color: Color,
    onHexColor: (Color) -> Unit,
    hexLabel: String,
) {
    var hexText by remember { mutableStateOf("#" + color.toHexString()) }

    // Reflect slider/square changes back into the field, without fighting the user's own typing.
    LaunchedEffect(color) {
        val hx = color.toHexString()
        if (!hexText.equals(hx, ignoreCase = true) && !hexText.equals("#$hx", ignoreCase = true)) {
            hexText = "#$hx"
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedTextField(
            value = hexText,
            onValueChange = { input ->
                hexText = input.uppercase().take(7)
                parseHexColor(input)?.let(onHexColor)
            },
            singleLine = true,
            label = { Text(hexLabel) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * History strip: a row of small filled circles for previously picked [colors] (most-recent first).
 * Tapping a swatch loads that colour into the picker. When there are more colours than
 * [previewCount], a trailing "+N" swatch expands the strip into a scrollable grid of all of them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorHistoryStrip(
    colors: List<Color>,
    previewCount: Int,
    selectedColor: Color,
    onSelect: (Color) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedHex = selectedColor.toHexString()
    val hasMore = colors.size > previewCount

    if (expanded) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.forEach { color ->
                ColorDot(
                    color = color,
                    selected = color.toHexString() == selectedHex,
                    onClick = { onSelect(color) },
                )
            }
            CollapseDot(onClick = { expanded = false })
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            colors.take(previewCount).forEach { color ->
                ColorDot(
                    color = color,
                    selected = color.toHexString() == selectedHex,
                    onClick = { onSelect(color) },
                )
            }
            if (hasMore) {
                ExpandDot(remaining = colors.size - previewCount, onClick = { expanded = true })
            }
        }
    }
}

private val SwatchSize = 28.dp

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SwatchSize)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

/** Trailing swatch that shows how many more colours are hidden and expands the strip on tap. */
@Composable
private fun ExpandDot(remaining: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SwatchSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$remaining",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Trailing swatch that collapses the expanded strip back to the preview row. */
@Composable
private fun CollapseDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SwatchSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "−",  // minus sign
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Pure-Kotlin colour maths — all multiplatform-safe.
// ---------------------------------------------------------------------------

private class Hsv(val hue: Float, val saturation: Float, val value: Float)

/** Clamp a pointer coordinate to a 0f..1f fraction of [extent], optionally inverting the axis. */
private fun sv(coordinate: Float, extent: Int, invert: Boolean = false): Float {
    if (extent <= 0) return 0f
    val f = (coordinate / extent).coerceIn(0f, 1f)
    return if (invert) 1f - f else f
}

/** RGB → HSV. [Color] channels are sRGB floats in 0f..1f, matching [Color.hsv]. */
private fun Color.toHsv(): Hsv {
    val r = red
    val g = green
    val b = blue
    val cMax = max(r, max(g, b))
    val cMin = min(r, min(g, b))
    val delta = cMax - cMin

    val hue = when {
        delta == 0f -> 0f
        cMax == r -> 60f * (((g - b) / delta) % 6f)
        cMax == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val saturation = if (cMax == 0f) 0f else delta / cMax
    return Hsv(hue, saturation, cMax)
}

/** Six-digit uppercase RRGGBB, no leading '#'. Avoids `String.format` for KMP portability. */
fun Color.toHexString(): String {
    fun channel(c: Float) = (c * 255f).roundToInt().coerceIn(0, 255)
    return channel(red).toHex2() + channel(green).toHex2() + channel(blue).toHex2()
}

private fun Int.toHex2(): String {
    val digits = "0123456789ABCDEF"
    return "${digits[(this shr 4) and 0x0F]}${digits[this and 0x0F]}"
}

/** Public counterpart of [parseHexColor]: `#RRGGBB` or `RRGGBB` → [Color], or null if invalid. */
fun colorFromHex(input: String): Color? = parseHexColor(input)

/** Parse `#RRGGBB` or `RRGGBB` (case-insensitive). Returns null for anything else. */
private fun parseHexColor(input: String): Color? {
    val hex = input.trim().removePrefix("#")
    if (hex.length != 6) return null
    val r = hex.substring(0, 2).toIntOrNull(16) ?: return null
    val g = hex.substring(2, 4).toIntOrNull(16) ?: return null
    val b = hex.substring(4, 6).toIntOrNull(16) ?: return null
    return Color(red = r, green = g, blue = b)
}
