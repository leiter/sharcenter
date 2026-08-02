package cut.the.crap.data.rest

/** The remote source a failure came from, for source-specific messaging. */
enum class Source {
    BLUESKY,
    MASTODON,
    REDDIT,
    TIKTOK,
    YOUTUBE,
    ECI,
    CAMPAIGN,
}

/**
 * A typed failure. The data layer describes *what went wrong*; the UI decides *how to say it*.
 *
 * This replaces the previous approach of building localised strings inside repositories via
 * `StringProvider.get(R.string.…)`, which pinned the data layer to Android resources. Keeping
 * errors typed means the repositories are pure Kotlin (`commonMain`), and each platform's UI
 * maps them to its own localised text.
 *
 * [debugText] is developer-facing only (logs) — never show it to users.
 */
sealed interface AppError {

    val debugText: String

    // --- Transport-level, source-independent ---

    data class Client(val status: Int, val description: String) : AppError {
        override val debugText get() = "Client error: $status - $description"
    }

    data class Server(val status: Int, val description: String) : AppError {
        override val debugText get() = "Server error: $status - $description"
    }

    data object Timeout : AppError {
        override val debugText get() = "Request timed out"
    }

    /** [detail] is null when the cause carried no message. */
    data class Network(val detail: String?) : AppError {
        override val debugText get() = "Network error: ${detail ?: "unable to connect"}"
    }

    data class Unexpected(val detail: String?) : AppError {
        override val debugText get() = "Unexpected error: ${detail ?: "unknown"}"
    }

    // --- Source-scoped content failures ---

    data class InvalidUrl(val source: Source, val url: String) : AppError {
        override val debugText get() = "$source: invalid url '$url'"
    }

    data class NotFound(val source: Source) : AppError {
        override val debugText get() = "$source: not found"
    }

    data class Unavailable(val source: Source) : AppError {
        override val debugText get() = "$source: unavailable"
    }

    data class SourceServer(val source: Source, val status: Int) : AppError {
        override val debugText get() = "$source: server error $status"
    }

    data class SourceTimeout(val source: Source) : AppError {
        override val debugText get() = "$source: timed out"
    }

    data class FetchFailed(val source: Source, val detail: String?) : AppError {
        override val debugText get() = "$source: fetch failed - ${detail ?: "unknown"}"
    }

    // --- YouTube-specific ---

    data class NoVideoId(val url: String) : AppError {
        override val debugText get() = "YouTube: no video id in url '$url'"
    }

    data class InvalidVideoId(val id: String) : AppError {
        override val debugText get() = "YouTube: invalid video id '$id'"
    }

    data object NotAccessible : AppError {
        override val debugText get() = "YouTube: video not accessible"
    }

    // --- ECI-specific ---

    data class UnrecognisedUrl(val url: String) : AppError {
        override val debugText get() = "ECI: unrecognised url '$url'"
    }

    data object UnexpectedResponse : AppError {
        override val debugText get() = "ECI: unexpected response"
    }

    data object ParseError : AppError {
        override val debugText get() = "ECI: parse error"
    }

    data class InvalidNumber(val number: String) : AppError {
        override val debugText get() = "ECI: invalid registration number '$number'"
    }

    data class LoadFailed(val detail: String?) : AppError {
        override val debugText get() = "ECI: load failed - ${detail ?: "unknown"}"
    }

    /** ECI's not-found message names the initiative (year + registration number). */
    data class EciNotFound(val year: String, val number: String) : AppError {
        override val debugText get() = "ECI: initiative $year/$number not found"
    }
}
