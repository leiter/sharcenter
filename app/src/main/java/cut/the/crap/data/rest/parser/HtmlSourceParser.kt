package cut.the.crap.data.rest.parser

import cut.the.crap.R
import cut.the.crap.data.rest.Result
import cut.the.crap.tools.StringProvider
import javax.inject.Inject

/**
 * Registered [SourceParser] for [SourceFormat.HTML] that proves the format-dispatch seam:
 * the repository can already resolve an HTML source to *a* parser. The actual DOM/selector
 * extraction is a future increment (it needs an HTML parsing library), so [parse] reports a
 * non-retryable error for now.
 */
class HtmlSourceParser @Inject constructor(
    private val strings: StringProvider,
) : SourceParser {

    override fun supports(format: SourceFormat): Boolean = format == SourceFormat.HTML

    override fun parse(source: RawSource, schema: ParseSchema): Result<ParsedDocument> =
        Result.Error(strings.get(R.string.parser_error_html_unsupported), retryable = false)
}
