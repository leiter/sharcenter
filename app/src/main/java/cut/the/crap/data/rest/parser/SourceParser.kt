package cut.the.crap.data.rest.parser

import cut.the.crap.data.rest.Result

/**
 * Extracts a [ParsedDocument] from a [RawSource] according to a [ParseSchema].
 *
 * This is the extension point behind the campaign generalization: parsing is driven by
 * data (the schema says *what* to pull and from *where*), not by hand-written per-source
 * code, and the output is a generic map-like document rather than a concrete domain type.
 * A campaign registers one implementation per [SourceFormat] it understands (JSON today,
 * HTML next); the repository picks the parser that [supports] the fetched source's format.
 * Adding a new source format is therefore a matter of adding a parser — no transport or
 * repository code changes.
 *
 * Implementations must not perform I/O; the payload arrives already fetched in
 * [RawSource.content]. Malformed input is reported as [Result.Error], not thrown.
 */
interface SourceParser {

    /** Whether this parser can decode a source in the given [format]. */
    fun supports(format: SourceFormat): Boolean

    /**
     * Extracts [schema]'s declared fields and tables from [source]. Returns [Result.Error]
     * on malformed input (typically non-retryable — the same bytes will never parse).
     * Paths that are absent in the payload yield [ParsedValue.Null] rather than an error.
     */
    fun parse(source: RawSource, schema: ParseSchema): Result<ParsedDocument>
}
