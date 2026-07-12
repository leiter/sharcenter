package cut.the.crap.data.rest

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.parseSocialMediaUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private val Context.youTubeBackfillDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "youtube_backfill_preferences"
)

/**
 * One-time backfill of YouTube metadata for links that were saved before metadata
 * fetching existed (e.g. bulk-imported links, or shorts/live URLs that the parser used
 * to skip). Metadata is normally fetched only at insert time, so older rows stay blank
 * until this runs.
 *
 * Idempotent: it only touches YouTube links that have no thumbnail yet, so once a row
 * is filled it is skipped on subsequent runs. A link whose fetch fails transiently
 * (e.g. offline) is retried next time; one that fails permanently (deleted/private
 * video, or a non-video URL like a community post) is recorded so it is never retried.
 */
class YouTubeMetadataBackfiller constructor(
    private val context: Context,
    private val contentRepository: ContentLinkRepository,
    private val youTubeRepository: YouTubeRepository,
) {

    companion object {
        private const val TAG = "YT_BACKFILL"
        // Be polite to the oEmbed endpoint when there are many links to fill.
        private const val DELAY_BETWEEN_FETCHES_MS = 250L
    }

    private object PreferencesKeys {
        // Links that failed permanently — skipped on future runs so we don't retry forever.
        val FAILED_LINKS = stringSetPreferencesKey("failed_links")
    }

    /**
     * Fetches and stores metadata for every YouTube link that is still missing it,
     * skipping links previously marked as permanently failed. Safe to call on every
     * app start; does nothing once all reachable links are filled.
     */
    suspend fun backfillMissing() {
        val failedLinks = context.youTubeBackfillDataStore.data.first()[PreferencesKeys.FAILED_LINKS]
            ?: emptySet()

        val all = contentRepository.byTimeRange(start = 0L, end = Long.MAX_VALUE)
        val missing = all.filter { link ->
            YouTubeUrlParser.isYouTubeUrl(link.link) &&
                LinkMetadata.getThumbnailUrl(link) == null &&
                link.link !in failedLinks
        }

        if (missing.isEmpty()) return
        Log.i(TAG, "Backfilling YouTube metadata for ${missing.size} link(s)")

        var filled = 0
        val newlyFailed = mutableSetOf<String>()
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
                    if (!result.retryable) {
                        newlyFailed.add(link.link)
                    }
                    Log.w(TAG, "Skipping ${link.link} (retryable=${result.retryable}): ${result.message}")
                }
            }
            delay(DELAY_BETWEEN_FETCHES_MS)
        }

        if (newlyFailed.isNotEmpty()) {
            context.youTubeBackfillDataStore.edit { prefs ->
                prefs[PreferencesKeys.FAILED_LINKS] = failedLinks + newlyFailed
            }
        }

        Log.i(
            TAG,
            "Backfill complete: filled $filled, marked ${newlyFailed.size} permanently failed, " +
                "of ${missing.size} attempted"
        )
    }
}
