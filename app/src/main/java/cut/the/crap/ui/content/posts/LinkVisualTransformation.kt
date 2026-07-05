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

// Punctuation that commonly trails a URL in prose and isn't part of it.
private const val TRAILING_PUNCTUATION = ".,;:!?)]}\"'»"

/** A validated URL and the character range it occupies in the source text. */
internal data class UrlSpan(val range: IntRange, val url: String)

/**
 * Finds candidate `http(s)://…` runs, strips trailing punctuation, and keeps only those
 * that are actually valid URLs (see [isValidUrl]). This is the single source of truth for
 * both highlighting and click handling, so an invalid candidate is neither styled nor
 * clickable.
 */
internal fun findUrlSpans(text: String): List<UrlSpan> =
    URL_REGEX.findAll(text).mapNotNull { match ->
        val trimmed = match.value.trimEnd { it in TRAILING_PUNCTUATION }
        if (trimmed.isEmpty() || !isValidUrl(trimmed)) return@mapNotNull null
        val start = match.range.first
        UrlSpan(start until (start + trimmed.length), trimmed)
    }.toList()

internal fun findUrlRanges(text: String): List<IntRange> = findUrlSpans(text).map { it.range }

/**
 * Returns the valid URL whose character range contains [offset] (a caret offset, so a tap
 * anywhere from the first char through just past the last still counts), or null if
 * [offset] doesn't fall on a link.
 */
internal fun findUrlAt(text: String, offset: Int): String? =
    findUrlSpans(text).firstOrNull { offset >= it.range.first && offset <= it.range.last + 1 }?.url

/**
 * A candidate counts as a valid URL when it parses, uses an http(s) scheme, and has a host
 * with a dot (a TLD). This rejects bare `https://`, scheme-only-with-garbage, and
 * host-without-a-dot cases while staying free of a stale hard-coded TLD list.
 */
private fun isValidUrl(candidate: String): Boolean = try {
    val uri = java.net.URI(candidate)
    (uri.scheme == "http" || uri.scheme == "https") &&
        !uri.host.isNullOrBlank() &&
        uri.host.contains(".")
} catch (e: java.net.URISyntaxException) {
    false
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
