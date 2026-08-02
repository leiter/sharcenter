package cut.the.crap.share

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.data.domain.KeywordRepository
import cut.the.crap.data.domain.KeywordType
import cut.the.crap.platform.Log
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.ui.components.api.ChipsType
import kotlinx.coroutines.flow.first

/**
 * The platform-agnostic core of "a URL was shared into the app": pick the owning handler, resolve
 * any redirect, then either save an `@handle` to the keyword pool or persist the link and enrich
 * it. Extracted out of Android's `ShareReceiverActivity` so a future iOS Share Extension can drive
 * the exact same logic instead of a second, drifting copy.
 *
 * What deliberately stays in the platform layer, because it is interactive UI this class must not
 * own: parsing the platform's share intent, the optional "edit before save" dialog (gated on a
 * user setting), the interactive X sign-in prompt + retry, and surfacing the [ShareOutcome] as a
 * toast/snackbar. This class performs no UI and no interactive auth — [resolve] merely *reports*
 * [ResolveResult.AuthRequired] and lets the caller decide whether to prompt or save unresolved.
 */
class SharedUrlProcessor(
    private val contentLinkRepository: ContentLinkRepository,
    private val keywordRepository: KeywordRepository,
    private val sharedLinkHandlers: List<SharedLinkHandler>,
) {

    /**
     * The first handler that recognizes [url] owns it. Never throws: [GenericSharedLinkHandler]
     * recognizes everything and is guaranteed last in the chain (see [ShareModule]).
     */
    private fun handlerFor(url: String): SharedLinkHandler =
        sharedLinkHandlers.first { it.recognizes(url) }

    /**
     * Phase 1 — choose the handler and resolve a redirect/short URL to its canonical form. Performs
     * network I/O but never writes. [ResolveResult.AuthRequired] means the handler needs interactive
     * auth (X): the caller either prompts and, on success, calls [resolve] again, or gives up and
     * persists the raw URL via [saveLink]/[saveHandle] using [ResolveResult.AuthRequired.url].
     */
    suspend fun resolve(url: String): ResolveResult =
        when (val resolution = handlerFor(url).resolve(url)) {
            is UrlResolution.Resolved -> {
                Log.d(TAG, "URL resolved: ${resolution.url} (changed=${resolution.changed})")
                ResolveResult.Ready(resolution.url, wasResolved = resolution.changed)
            }

            is UrlResolution.AuthRequired -> {
                Log.d(TAG, "Auth required for URL resolution")
                ResolveResult.AuthRequired(resolution.url, resolution.credentialsPresent)
            }
        }

    /**
     * If [url] denotes an account/profile whose `@handle` belongs in the keyword pool *instead of*
     * being saved as a link, returns that handle; otherwise null. Pure — the caller checks this
     * before deciding between the (interactive) edit dialog and [saveHandle], exactly as the
     * original activity did.
     */
    fun handleToSaveInstead(url: String): String? = handlerFor(url).handleToSaveInstead(url)

    /**
     * Insert [url] as a link (optionally tagged with user-supplied [keywords]). Does *not* enrich —
     * call [enrichSaved] afterwards. The two are split so a caller can show "saved" feedback
     * immediately and run the (possibly slow, networked) enrichment after, which is exactly what
     * the Android share flow does. [wasResolved] only selects the feedback the caller shows.
     */
    suspend fun insertLink(
        url: String,
        keywords: List<String> = emptyList(),
        wasResolved: Boolean,
        comment: String? = null,
    ): SaveResult {
        var contentLink = ContentLink(link = url, comment = comment)
        if (keywords.isNotEmpty()) {
            contentLink = LinkMetadata.setTags(contentLink, keywords, ChipsType.KeyWords)
        }
        contentLinkRepository.insert(contentLink)
        return SaveResult(wasResolved, savedAt = contentLink.added)
    }

    /**
     * Let the owning handler enrich the row just saved for [url] (e.g. YouTube title/thumbnail),
     * updating it in place. Enrichment failures are swallowed — the link is already saved and a
     * missing thumbnail must not fail the share. No-op if the row can't be found.
     *
     * [savedAt] is the [SaveResult.savedAt] from the [insertLink] that stored the row: insert()
     * returns no id, so the row is recovered by matching [url] within a small window around that
     * timestamp.
     */
    suspend fun enrichSaved(url: String, savedAt: Long) {
        val saved = contentLinkRepository
            .byTimeRange(start = savedAt - ENRICH_WINDOW_MS, end = savedAt + ENRICH_WINDOW_MS)
            .firstOrNull { it.link == url }
            ?: return
        try {
            handlerFor(url).enrich(saved)?.let { enriched ->
                contentLinkRepository.update(enriched)
                Log.d(TAG, "Metadata enriched for: $url")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Metadata enrichment failed for: $url", e)
        }
    }

    /**
     * Convenience for callers with no need to interleave feedback (iOS share, tests): insert then
     * enrich in one call.
     */
    suspend fun saveLink(
        url: String,
        keywords: List<String> = emptyList(),
        wasResolved: Boolean,
        comment: String? = null,
    ): SaveResult {
        val result = insertLink(url, keywords, wasResolved, comment)
        enrichSaved(url, result.savedAt)
        return result
    }

    /**
     * Save [handle] to the `ACCOUNT` keyword pool — the pool the handles dialog reads from. Deduped
     * case-insensitively: skipping an existing handle avoids the unique-index REPLACE resetting its
     * favorite/usage stats. Returns whether it already existed so the caller shows the right
     * "added" vs "already there" feedback.
     */
    suspend fun saveHandle(handle: String): HandleResult {
        val existing = keywordRepository.getByType(KeywordType.ACCOUNT).first()
        return if (existing.any { it.text.equals(handle, ignoreCase = true) }) {
            Log.d(TAG, "Handle already in pool, skipping: $handle")
            HandleResult(handle, alreadyExisted = true)
        } else {
            keywordRepository.insert(KeyWord(text = handle, type = KeywordType.ACCOUNT))
            Log.d(TAG, "Added handle to pool: $handle")
            HandleResult(handle, alreadyExisted = false)
        }
    }

    private companion object {
        const val TAG = "SharedUrlProcessor"

        /** Half-width of the timestamp window used to recover a just-inserted row (insert returns no id). */
        const val ENRICH_WINDOW_MS = 1000L
    }
}

/** Outcome of [SharedUrlProcessor.resolve]. */
sealed interface ResolveResult {
    /** Resolution succeeded; [url] is the canonical link to save. [wasResolved] reports whether it changed. */
    data class Ready(val url: String, val wasResolved: Boolean) : ResolveResult

    /**
     * The handler needs interactive auth before it can resolve. [url] is the original input and
     * [credentialsPresent] reports whether stale credentials existed (an expired session vs a
     * first-time setup), so the caller can pick the right prompt.
     */
    data class AuthRequired(val url: String, val credentialsPresent: Boolean) : ResolveResult
}

/**
 * Outcome of [SharedUrlProcessor.insertLink]/[SharedUrlProcessor.saveLink]. [wasResolved] drives
 * "resolved and saved" vs "saved" feedback; [savedAt] is the stored row's timestamp, threaded into
 * [SharedUrlProcessor.enrichSaved] to recover the row for enrichment.
 */
data class SaveResult(val wasResolved: Boolean, val savedAt: Long)

/** Outcome of [SharedUrlProcessor.saveHandle]. */
data class HandleResult(val handle: String, val alreadyExisted: Boolean)
