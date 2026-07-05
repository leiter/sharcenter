package cut.the.crap.ui.content.posts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

private val URL_REGEX = Regex("""https?://\S+""")

internal fun findUrlRanges(text: String): List<IntRange> =
    URL_REGEX.findAll(text).map { it.range }.toList()

/**
 * Returns the URL substring whose character range contains [offset] (a caret offset,
 * so a tap anywhere from the first char through just past the last still counts), or
 * null if [offset] doesn't fall on a link.
 */
internal fun findUrlAt(text: String, offset: Int): String? {
    val range = findUrlRanges(text).firstOrNull { offset >= it.first && offset <= it.last + 1 }
        ?: return null
    return text.substring(range.first, range.last + 1)
}

/**
 * Underlines and tints any http(s) URLs in the field text. Purely visual — the text
 * length is unchanged, so [OffsetMapping.Identity] keeps the cursor/selection mapping
 * intact (important: the editor's @/# insert buttons rely on accurate selection offsets).
 *
 * Caches the last transform so repeated [filter] calls for unchanged text are cheap.
 */
class LinkVisualTransformation(private val linkColor: Color) : VisualTransformation {
    private var lastText = ""
    private var lastResult = TransformedText(AnnotatedString(""), OffsetMapping.Identity)

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text == lastText) return lastResult
        lastText = text.text
        lastResult = applyLinkStyles(text)
        return lastResult
    }

    private fun applyLinkStyles(text: AnnotatedString): TransformedText {
        val ranges = findUrlRanges(text.text)
        if (ranges.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val styled = buildAnnotatedString {
            append(text)
            ranges.forEach { range ->
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start = range.first,
                    end = range.last + 1,
                )
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}
