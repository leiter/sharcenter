package cut.the.crap.data.rest.parser

import kotlinx.serialization.Serializable

/**
 * The wire format of a [RawSource] to be parsed into a [ParsedDocument].
 *
 * A campaign's [ParseSchema] declares which format its source uses; the repository
 * fetches the raw payload and dispatches to the [SourceParser] that [SourceParser.supports]
 * the format. JSON is the format used by the proof-of-concept ECI campaign; HTML is the
 * next planned format (its parser slots in behind the same interface with no repository
 * changes).
 */
@Serializable
enum class SourceFormat {
    JSON,
    HTML;

    companion object {
        /**
         * Best-effort format detection from an HTTP `Content-Type` header (or any MIME-ish
         * hint). Returns `null` when the type is unknown so the caller can fall back to the
         * format it already knows it requested.
         */
        fun fromContentType(contentType: String?): SourceFormat? = when {
            contentType == null -> null
            contentType.contains("json", ignoreCase = true) -> JSON
            contentType.contains("html", ignoreCase = true) -> HTML
            else -> null
        }
    }
}
