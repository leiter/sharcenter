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
private val HANDLE_REGEX = Regex("""@\w+""")
private val TAG_REGEX = Regex("""#\w+""")

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
 * Highlights editor text: http(s) URLs are underlined and tinted [linkColor], @handles are
 * tinted [handleColor] and #tags [tagColor] (handles/tags that fall inside a URL are left
 * alone). Purely visual — the text length is unchanged, so [OffsetMapping.Identity] keeps
 * the cursor/selection mapping intact (important: the editor's @/# insert buttons rely on
 * accurate selection offsets).
 *
 * Caches the last transform so repeated [filter] calls for unchanged text are cheap.
 */
class LinkVisualTransformation(
    private val linkColor: Color,
    private val handleColor: Color,
    private val tagColor: Color,
) : VisualTransformation {
    private var lastText = ""
    private var lastResult = TransformedText(AnnotatedString(""), OffsetMapping.Identity)

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text == lastText) return lastResult
        lastText = text.text
        lastResult = applyStyles(text)
        return lastResult
    }

    private fun applyStyles(text: AnnotatedString): TransformedText {
        val content = text.text
        val urlRanges = findUrlRanges(content)
        // Skip @/# matches inside a URL (e.g. an "@" in a path or a "#fragment").
        val handleMatches = HANDLE_REGEX.findAll(content)
            .filter { m -> urlRanges.none { m.range.first in it } }
            .toList()
        val tagMatches = TAG_REGEX.findAll(content)
            .filter { m -> urlRanges.none { m.range.first in it } }
            .toList()

        if (urlRanges.isEmpty() && handleMatches.isEmpty() && tagMatches.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val styled = buildAnnotatedString {
            append(text)
            urlRanges.forEach { range ->
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start = range.first,
                    end = range.last + 1,
                )
            }
            handleMatches.forEach { m ->
                addStyle(SpanStyle(color = handleColor), m.range.first, m.range.last + 1)
            }
            tagMatches.forEach { m ->
                addStyle(SpanStyle(color = tagColor), m.range.first, m.range.last + 1)
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}
