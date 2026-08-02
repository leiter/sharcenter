package cut.the.crap.ui

import androidx.compose.runtime.Composable
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Source
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.bsky_error_fetch_failed
import cut.the.crap.shared.resources.bsky_error_invalid_url
import cut.the.crap.shared.resources.bsky_error_not_found
import cut.the.crap.shared.resources.bsky_error_server
import cut.the.crap.shared.resources.bsky_error_timeout
import cut.the.crap.shared.resources.bsky_error_unavailable
import cut.the.crap.shared.resources.campaign_error_load_failed
import cut.the.crap.shared.resources.campaign_error_not_found
import cut.the.crap.shared.resources.campaign_error_server
import cut.the.crap.shared.resources.campaign_error_timeout
import cut.the.crap.shared.resources.campaign_error_unexpected_response
import cut.the.crap.shared.resources.campaign_error_unrecognised_url
import cut.the.crap.shared.resources.eci_error_invalid_number
import cut.the.crap.shared.resources.eci_error_load_failed
import cut.the.crap.shared.resources.eci_error_not_found
import cut.the.crap.shared.resources.eci_error_parse
import cut.the.crap.shared.resources.eci_error_server
import cut.the.crap.shared.resources.eci_error_timeout
import cut.the.crap.shared.resources.eci_error_unexpected_response
import cut.the.crap.shared.resources.eci_error_unrecognised_url
import cut.the.crap.shared.resources.error_client
import cut.the.crap.shared.resources.error_network
import cut.the.crap.shared.resources.error_network_fallback
import cut.the.crap.shared.resources.error_server
import cut.the.crap.shared.resources.error_timeout
import cut.the.crap.shared.resources.error_unexpected
import cut.the.crap.shared.resources.error_unknown
import cut.the.crap.shared.resources.mastodon_error_fetch_failed
import cut.the.crap.shared.resources.mastodon_error_invalid_url
import cut.the.crap.shared.resources.mastodon_error_not_found
import cut.the.crap.shared.resources.mastodon_error_server
import cut.the.crap.shared.resources.mastodon_error_timeout
import cut.the.crap.shared.resources.mastodon_error_unavailable
import cut.the.crap.shared.resources.reddit_error_fetch_failed
import cut.the.crap.shared.resources.reddit_error_invalid_url
import cut.the.crap.shared.resources.reddit_error_not_found
import cut.the.crap.shared.resources.reddit_error_server
import cut.the.crap.shared.resources.reddit_error_timeout
import cut.the.crap.shared.resources.reddit_error_unavailable
import cut.the.crap.shared.resources.tiktok_error_fetch_failed
import cut.the.crap.shared.resources.tiktok_error_invalid_url
import cut.the.crap.shared.resources.tiktok_error_not_found
import cut.the.crap.shared.resources.tiktok_error_server
import cut.the.crap.shared.resources.tiktok_error_timeout
import cut.the.crap.shared.resources.tiktok_error_unavailable
import cut.the.crap.shared.resources.yt_error_fetch_failed
import cut.the.crap.shared.resources.yt_error_invalid_url
import cut.the.crap.shared.resources.yt_error_invalid_video_id
import cut.the.crap.shared.resources.yt_error_no_video_id
import cut.the.crap.shared.resources.yt_error_not_accessible
import cut.the.crap.shared.resources.yt_error_not_found
import cut.the.crap.shared.resources.yt_error_server
import cut.the.crap.shared.resources.yt_error_timeout
import cut.the.crap.shared.resources.yt_error_unavailable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Turns a typed [AppError] into localised, user-facing text.
 *
 * This is the UI half of the split: the data layer (in `:shared/commonMain`) says *what* went
 * wrong, and this — the only place that knows the string catalogue — says *how to phrase it*.
 * That's what lets the repositories stay pure Kotlin.
 *
 * There are two accessors because Compose resources are read differently inside and outside
 * composition: [localized] for composables, and the suspending [localizedText] for coroutine
 * callers such as Toasts and snackbars. Both read the same catalogue entries, so the wording
 * cannot drift between them.
 */
@Composable
fun AppError.localized(): String = when (this) {
    // Transport-level
    is AppError.Client -> stringResource(Res.string.error_client, status, description)
    is AppError.Server -> stringResource(Res.string.error_server, status, description)
    AppError.Timeout -> stringResource(Res.string.error_timeout)
    is AppError.Network -> stringResource(
        Res.string.error_network,
        detail ?: stringResource(Res.string.error_network_fallback),
    )
    is AppError.Unexpected -> stringResource(
        Res.string.error_unexpected,
        detail ?: stringResource(Res.string.error_unknown),
    )

    // Source-scoped
    is AppError.InvalidUrl -> stringResource(source.invalidUrlRes(), url)
    is AppError.NotFound -> stringResource(source.notFoundRes())
    is AppError.Unavailable -> stringResource(source.unavailableRes())
    is AppError.SourceServer -> stringResource(source.serverRes(), status)
    is AppError.SourceTimeout -> stringResource(source.timeoutRes())
    is AppError.FetchFailed -> stringResource(
        source.fetchFailedRes(),
        detail ?: stringResource(Res.string.error_unknown),
    )

    // YouTube
    is AppError.NoVideoId -> stringResource(Res.string.yt_error_no_video_id, url)
    is AppError.InvalidVideoId -> stringResource(Res.string.yt_error_invalid_video_id, id)
    AppError.NotAccessible -> stringResource(Res.string.yt_error_not_accessible)

    // ECI
    is AppError.UnrecognisedUrl -> stringResource(Res.string.eci_error_unrecognised_url, url)
    AppError.UnexpectedResponse -> stringResource(Res.string.eci_error_unexpected_response)
    AppError.ParseError -> stringResource(Res.string.eci_error_parse)
    is AppError.InvalidNumber -> stringResource(Res.string.eci_error_invalid_number, number)
    is AppError.LoadFailed -> stringResource(
        Res.string.eci_error_load_failed,
        detail ?: stringResource(Res.string.error_unknown),
    )
    is AppError.EciNotFound -> stringResource(Res.string.eci_error_not_found, year, number)
}

/** Non-composable equivalent of [localized], for coroutine callers (Toasts, snackbars). */
suspend fun AppError.localizedText(): String = when (this) {
    // Transport-level
    is AppError.Client -> getString(Res.string.error_client, status, description)
    is AppError.Server -> getString(Res.string.error_server, status, description)
    AppError.Timeout -> getString(Res.string.error_timeout)
    is AppError.Network -> getString(
        Res.string.error_network,
        detail ?: getString(Res.string.error_network_fallback),
    )
    is AppError.Unexpected -> getString(
        Res.string.error_unexpected,
        detail ?: getString(Res.string.error_unknown),
    )

    // Source-scoped
    is AppError.InvalidUrl -> getString(source.invalidUrlRes(), url)
    is AppError.NotFound -> getString(source.notFoundRes())
    is AppError.Unavailable -> getString(source.unavailableRes())
    is AppError.SourceServer -> getString(source.serverRes(), status)
    is AppError.SourceTimeout -> getString(source.timeoutRes())
    is AppError.FetchFailed -> getString(
        source.fetchFailedRes(),
        detail ?: getString(Res.string.error_unknown),
    )

    // YouTube
    is AppError.NoVideoId -> getString(Res.string.yt_error_no_video_id, url)
    is AppError.InvalidVideoId -> getString(Res.string.yt_error_invalid_video_id, id)
    AppError.NotAccessible -> getString(Res.string.yt_error_not_accessible)

    // ECI
    is AppError.UnrecognisedUrl -> getString(Res.string.eci_error_unrecognised_url, url)
    AppError.UnexpectedResponse -> getString(Res.string.eci_error_unexpected_response)
    AppError.ParseError -> getString(Res.string.eci_error_parse)
    is AppError.InvalidNumber -> getString(Res.string.eci_error_invalid_number, number)
    is AppError.LoadFailed -> getString(
        Res.string.eci_error_load_failed,
        detail ?: getString(Res.string.error_unknown),
    )
    is AppError.EciNotFound -> getString(Res.string.eci_error_not_found, year, number)
}

private fun Source.invalidUrlRes(): StringResource = when (this) {
    Source.BLUESKY -> Res.string.bsky_error_invalid_url
    Source.MASTODON -> Res.string.mastodon_error_invalid_url
    Source.REDDIT -> Res.string.reddit_error_invalid_url
    Source.TIKTOK -> Res.string.tiktok_error_invalid_url
    Source.YOUTUBE -> Res.string.yt_error_invalid_url
    Source.ECI -> Res.string.eci_error_unrecognised_url
    Source.CAMPAIGN -> Res.string.campaign_error_unrecognised_url
}

private fun Source.notFoundRes(): StringResource = when (this) {
    Source.BLUESKY -> Res.string.bsky_error_not_found
    Source.MASTODON -> Res.string.mastodon_error_not_found
    Source.REDDIT -> Res.string.reddit_error_not_found
    Source.TIKTOK -> Res.string.tiktok_error_not_found
    Source.YOUTUBE -> Res.string.yt_error_not_found
    Source.ECI -> Res.string.eci_error_not_found
    Source.CAMPAIGN -> Res.string.campaign_error_not_found
}

private fun Source.unavailableRes(): StringResource = when (this) {
    Source.BLUESKY -> Res.string.bsky_error_unavailable
    Source.MASTODON -> Res.string.mastodon_error_unavailable
    Source.REDDIT -> Res.string.reddit_error_unavailable
    Source.TIKTOK -> Res.string.tiktok_error_unavailable
    Source.YOUTUBE -> Res.string.yt_error_unavailable
    Source.ECI -> Res.string.eci_error_unexpected_response
    Source.CAMPAIGN -> Res.string.campaign_error_unexpected_response
}

private fun Source.serverRes(): StringResource = when (this) {
    Source.BLUESKY -> Res.string.bsky_error_server
    Source.MASTODON -> Res.string.mastodon_error_server
    Source.REDDIT -> Res.string.reddit_error_server
    Source.TIKTOK -> Res.string.tiktok_error_server
    Source.YOUTUBE -> Res.string.yt_error_server
    Source.ECI -> Res.string.eci_error_server
    Source.CAMPAIGN -> Res.string.campaign_error_server
}

private fun Source.timeoutRes(): StringResource = when (this) {
    Source.BLUESKY -> Res.string.bsky_error_timeout
    Source.MASTODON -> Res.string.mastodon_error_timeout
    Source.REDDIT -> Res.string.reddit_error_timeout
    Source.TIKTOK -> Res.string.tiktok_error_timeout
    Source.YOUTUBE -> Res.string.yt_error_timeout
    Source.ECI -> Res.string.eci_error_timeout
    Source.CAMPAIGN -> Res.string.campaign_error_timeout
}

private fun Source.fetchFailedRes(): StringResource = when (this) {
    Source.BLUESKY -> Res.string.bsky_error_fetch_failed
    Source.MASTODON -> Res.string.mastodon_error_fetch_failed
    Source.REDDIT -> Res.string.reddit_error_fetch_failed
    Source.TIKTOK -> Res.string.tiktok_error_fetch_failed
    Source.YOUTUBE -> Res.string.yt_error_fetch_failed
    Source.ECI -> Res.string.eci_error_load_failed
    Source.CAMPAIGN -> Res.string.campaign_error_load_failed
}
