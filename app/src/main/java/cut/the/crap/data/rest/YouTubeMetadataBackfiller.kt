package cut.the.crap.data.rest

import android.util.Log
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.parseSocialMediaUrl
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time backfill of YouTube metadata for links that were saved before metadata
 * fetching existed (e.g. bulk-imported links, or shorts URLs that the parser used to
 * skip). Metadata is normally fetched only at insert time, so older rows stay blank
 * until this runs.
 *
 * Idempotent: it only touches YouTube links that have no thumbnail yet, so once a row
 * is filled it is skipped on subsequent runs. Links whose fetch fails (e.g. offline)
 * are simply retried next time.
 */
@Singleton
class YouTubeMetadataBackfiller @Inject constructor(
    private val contentRepository: ContentLinkRepository,
    private val youTubeRepository: YouTubeRepository,
) {

    companion object {
        private const val TAG = "YT_BACKFILL"
        // Be polite to the oEmbed endpoint when there are many links to fill.
        private const val DELAY_BETWEEN_FETCHES_MS = 250L
    }

    /**
     * Fetches and stores metadata for every YouTube link that is still missing it.
     * Safe to call on every app start; does nothing once all links are filled.
     */
    suspend fun backfillMissing() {
        val all = contentRepository.byTimeRange(start = 0L, end = Long.MAX_VALUE)
        val missing = all.filter { link ->
            YouTubeUrlParser.isYouTubeUrl(link.link) && LinkMetadata.getThumbnailUrl(link) == null
        }

        if (missing.isEmpty()) return
        Log.i(TAG, "Backfilling YouTube metadata for ${missing.size} link(s)")

        var filled = 0
        for (link in missing) {
            when (val result = youTubeRepository.getVideoMetadata(link.link)) {
                is Result.Success -> {
                    val type = parseSocialMediaUrl(link.link)?.contentType ?: "video"
                    val updated = LinkMetadata.setYouTubeMetadata(
                        link,
                        channelName = result.data.channelName,
                        videoTitle = result.data.title,
                        thumbnailUrl = result.data.thumbnailUrl,
                        contentType = type,
                    )
                    contentRepository.update(updated)
                    filled++
                }
                is Result.Error -> {
                    Log.w(TAG, "Skipping ${link.link}: ${result.message}")
                }
            }
            delay(DELAY_BETWEEN_FETCHES_MS)
        }

        Log.i(TAG, "Backfill complete: filled $filled of ${missing.size}")
    }
}
