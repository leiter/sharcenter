package cut.the.crap.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import cut.the.crap.R
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Source

/**
 * Turns a typed [AppError] into localised, user-facing text.
 *
 * This is the UI half of the split: the data layer (in `:shared/commonMain`) says *what* went
 * wrong, and this — the only place that knows about Android resources — says *how to phrase it*.
 * That's what lets the repositories be pure Kotlin.
 *
 * When the UI moves to Compose Multiplatform these `R.string` lookups become `Res.string`, and
 * this file moves to `commonMain` with it. Nothing else has to change.
 */
fun AppError.localized(context: Context): String = when (this) {
    // Transport-level
    is AppError.Client -> context.getString(R.string.error_client, status, description)
    is AppError.Server -> context.getString(R.string.error_server, status, description)
    AppError.Timeout -> context.getString(R.string.error_timeout)
    is AppError.Network -> context.getString(
        R.string.error_network,
        detail ?: context.getString(R.string.error_network_fallback),
    )
    is AppError.Unexpected -> context.getString(
        R.string.error_unexpected,
        detail ?: context.getString(R.string.error_unknown),
    )

    // Source-scoped
    is AppError.InvalidUrl -> context.getString(source.invalidUrlRes(), url)
    is AppError.NotFound -> context.getString(source.notFoundRes())
    is AppError.Unavailable -> context.getString(source.unavailableRes())
    is AppError.SourceServer -> context.getString(source.serverRes(), status)
    is AppError.SourceTimeout -> context.getString(source.timeoutRes())
    is AppError.FetchFailed -> context.getString(
        source.fetchFailedRes(),
        detail ?: context.getString(R.string.error_unknown),
    )

    // YouTube
    is AppError.NoVideoId -> context.getString(R.string.yt_error_no_video_id, url)
    is AppError.InvalidVideoId -> context.getString(R.string.yt_error_invalid_video_id, id)
    AppError.NotAccessible -> context.getString(R.string.yt_error_not_accessible)

    // ECI
    is AppError.UnrecognisedUrl -> context.getString(R.string.eci_error_unrecognised_url, url)
    AppError.UnexpectedResponse -> context.getString(R.string.eci_error_unexpected_response)
    AppError.ParseError -> context.getString(R.string.eci_error_parse)
    is AppError.InvalidNumber -> context.getString(R.string.eci_error_invalid_number, number)
    is AppError.LoadFailed -> context.getString(
        R.string.eci_error_load_failed,
        detail ?: context.getString(R.string.error_unknown),
    )
    is AppError.EciNotFound -> context.getString(R.string.eci_error_not_found, year, number)
}

/** Composable convenience for rendering an [AppError] in the UI. */
@Composable
fun AppError.localized(): String = localized(LocalContext.current)

private fun Source.invalidUrlRes() = when (this) {
    Source.BLUESKY -> R.string.bsky_error_invalid_url
    Source.MASTODON -> R.string.mastodon_error_invalid_url
    Source.REDDIT -> R.string.reddit_error_invalid_url
    Source.TIKTOK -> R.string.tiktok_error_invalid_url
    Source.YOUTUBE -> R.string.yt_error_invalid_url
    Source.ECI -> R.string.eci_error_unrecognised_url
}

private fun Source.notFoundRes() = when (this) {
    Source.BLUESKY -> R.string.bsky_error_not_found
    Source.MASTODON -> R.string.mastodon_error_not_found
    Source.REDDIT -> R.string.reddit_error_not_found
    Source.TIKTOK -> R.string.tiktok_error_not_found
    Source.YOUTUBE -> R.string.yt_error_not_found
    Source.ECI -> R.string.eci_error_not_found
}

private fun Source.unavailableRes() = when (this) {
    Source.BLUESKY -> R.string.bsky_error_unavailable
    Source.MASTODON -> R.string.mastodon_error_unavailable
    Source.REDDIT -> R.string.reddit_error_unavailable
    Source.TIKTOK -> R.string.tiktok_error_unavailable
    Source.YOUTUBE -> R.string.yt_error_unavailable
    Source.ECI -> R.string.eci_error_unexpected_response
}

private fun Source.serverRes() = when (this) {
    Source.BLUESKY -> R.string.bsky_error_server
    Source.MASTODON -> R.string.mastodon_error_server
    Source.REDDIT -> R.string.reddit_error_server
    Source.TIKTOK -> R.string.tiktok_error_server
    Source.YOUTUBE -> R.string.yt_error_server
    Source.ECI -> R.string.eci_error_server
}

private fun Source.timeoutRes() = when (this) {
    Source.BLUESKY -> R.string.bsky_error_timeout
    Source.MASTODON -> R.string.mastodon_error_timeout
    Source.REDDIT -> R.string.reddit_error_timeout
    Source.TIKTOK -> R.string.tiktok_error_timeout
    Source.YOUTUBE -> R.string.yt_error_timeout
    Source.ECI -> R.string.eci_error_timeout
}

private fun Source.fetchFailedRes() = when (this) {
    Source.BLUESKY -> R.string.bsky_error_fetch_failed
    Source.MASTODON -> R.string.mastodon_error_fetch_failed
    Source.REDDIT -> R.string.reddit_error_fetch_failed
    Source.TIKTOK -> R.string.tiktok_error_fetch_failed
    Source.YOUTUBE -> R.string.yt_error_fetch_failed
    Source.ECI -> R.string.eci_error_load_failed
}
