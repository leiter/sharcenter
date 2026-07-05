package cut.the.crap.data.rest.parser

/**
 * An already-fetched, undecoded payload handed to a [SourceParser].
 *
 * Keeping the transport (HTTP, file, clipboard, …) separate from parsing is the whole
 * point of the generalization: the repository is responsible for *obtaining* the bytes,
 * a [SourceParser] is responsible for *interpreting* them. That split is what lets one
 * campaign accept several formats, and several campaigns share one transport.
 *
 * @property content The raw, undecoded payload (JSON text, HTML markup, …).
 * @property format The format of [content], used to pick a parser.
 * @property url The origin the payload was fetched from, when known. A parser may use it
 *   to recover identity that lives in the request rather than the body (e.g. the ECI
 *   year/number that appear in the endpoint path, not the JSON).
 */
data class RawSource(
    val content: String,
    val format: SourceFormat,
    val url: String? = null
)
