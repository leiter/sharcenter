package cut.the.crap.tools

import cut.the.crap.platform.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

private const val TAG = "UrlResolver"

/**
 * Resolves shortened or redirect URLs to their final destination.
 *
 * The interesting case is an X/Twitter `/i/status/…` link, which hides the author: with
 * credentials it asks the GraphQL API for the real `screen_name`; without them (or on failure) it
 * falls back to the public vxtwitter/fxtwitter proxies, then to scraping `og:url` out of the HTML.
 * Every path degrades to returning the original URL rather than throwing — a share must never fail
 * just because enrichment did.
 *
 * Was an `object` holding two raw OkHttp clients; now an injected class over a Ktor [HttpClient],
 * so it compiles in `commonMain`. The no-redirect client the proxy path needs is derived with
 * `client.config { }`, which shares the same engine rather than opening a second one.
 */
class UrlResolver(private val client: HttpClient) {

    // Same engine, redirects off — so the proxy path can read the Location header itself.
    private val noRedirectClient: HttpClient by lazy {
        client.config { followRedirects = false }
    }

    /** X API credentials for resolving `/i/status/` URLs. */
    data class XCredentials(
        val authToken: String,
        val ct0Token: String,
    )

    /** Result of URL resolution. */
    sealed class ResolveResult {
        data class Success(val url: String) : ResolveResult()
        data class AuthRequired(val originalUrl: String) : ResolveResult()
        data class Failed(val originalUrl: String, val reason: String) : ResolveResult()
    }

    /**
     * Follows redirects and returns the final URL. For X `/i/status/` URLs, uses the GraphQL API
     * when [credentials] are present, otherwise the proxy fallback. Returns [url] unchanged on any
     * failure.
     */
    suspend fun resolveRedirect(url: String, credentials: XCredentials? = null): String {
        return try {
            Log.d(TAG, "Resolving URL: $url")

            if (isXRedirectUrl(url)) {
                if (credentials != null) {
                    val resolved = resolveViaGraphQL(url, credentials)
                    if (resolved != url) return resolved
                }
                return resolveViaProxy(url)
            }

            // Everything else: let the client follow redirects, then report where it landed.
            val response = client.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            }
            val finalUrl = response.request.url.toString()
            if (finalUrl != url) {
                Log.d(TAG, "Resolved via redirect: $url -> $finalUrl")
                finalUrl
            } else {
                url
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving URL: $url", e)
            url
        }
    }

    /** Resolves an X `/i/status/` URL to its real username, or returns [url] unchanged. */
    suspend fun resolveXUrl(url: String, credentials: XCredentials? = null): String =
        if (isXRedirectUrl(url)) resolveRedirect(url, credentials) else url

    /**
     * Like [resolveXUrl], but distinguishes "couldn't resolve" from "needs authentication" so the
     * caller can prompt for login.
     */
    suspend fun resolveXUrlWithStatus(
        url: String,
        credentials: XCredentials? = null,
    ): ResolveResult {
        if (!isXRedirectUrl(url)) return ResolveResult.Success(url)

        return try {
            Log.d(TAG, "Resolving URL with status: $url")

            if (credentials != null) {
                when (val graphql = resolveViaGraphQLWithStatus(url, credentials)) {
                    is ResolveResult.Success -> return graphql
                    is ResolveResult.AuthRequired -> return graphql
                    // Non-auth GraphQL failure: fall through to the proxy.
                    is ResolveResult.Failed -> Log.d(TAG, "GraphQL failed: ${graphql.reason}, trying proxy")
                }
            } else {
                Log.d(TAG, "No credentials provided, trying proxy")
            }

            val proxyResult = resolveViaProxy(url)
            if (proxyResult != url) {
                ResolveResult.Success(proxyResult)
            } else if (credentials != null) {
                ResolveResult.Failed(url, "Could not resolve URL")
            } else {
                // No creds and the proxy couldn't help — auth would likely do better.
                ResolveResult.AuthRequired(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving URL with status", e)
            ResolveResult.Failed(url, e.message ?: "Unknown error")
        }
    }

    /** GraphQL resolution returning only the resolved URL (or [url] on any failure). */
    private suspend fun resolveViaGraphQL(url: String, credentials: XCredentials): String =
        when (val result = resolveViaGraphQLWithStatus(url, credentials)) {
            is ResolveResult.Success -> result.url
            else -> url
        }

    /** GraphQL resolution with detailed status. */
    private suspend fun resolveViaGraphQLWithStatus(
        url: String,
        credentials: XCredentials,
    ): ResolveResult {
        val tweetId = tweetIdOf(url)
        if (tweetId == null) {
            Log.w(TAG, "Invalid tweet ID in: $url")
            return ResolveResult.Failed(url, "Invalid tweet ID")
        }

        return try {
            Log.d(TAG, "Resolving tweet ID via GraphQL: $tweetId")

            val response = client.get(GRAPHQL_ENDPOINT) {
                // Ktor percent-encodes these, replacing the manual URLEncoder step.
                parameter("variables", graphQlVariables(tweetId))
                parameter("features", GRAPHQL_FEATURES)
                header(HttpHeaders.Authorization, "Bearer $X_BEARER_TOKEN")
                header(HttpHeaders.Cookie, "auth_token=${credentials.authToken}; ct0=${credentials.ct0Token}")
                header("x-csrf-token", credentials.ct0Token)
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "application/json")
            }

            Log.d(TAG, "GraphQL response code: ${response.status.value}")
            if (response.status.value == 401 || response.status.value == 403) {
                Log.w(TAG, "GraphQL auth failed: ${response.status.value}")
                return ResolveResult.AuthRequired(url)
            }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "GraphQL request failed: ${response.status.value}")
                return ResolveResult.Failed(url, "API error: ${response.status.value}")
            }

            val body = response.bodyAsText()
            if (body.isEmpty()) return ResolveResult.Failed(url, "Empty response")

            if (isAuthErrorBody(body)) {
                Log.w(TAG, "GraphQL auth error in response body")
                return ResolveResult.AuthRequired(url)
            }

            val screenName = extractScreenName(body)
            Log.d(TAG, "Extracted screen_name: $screenName")
            if (screenName != null && screenName != "i") {
                val resolved = "https://x.com/$screenName/status/$tweetId"
                Log.d(TAG, "Resolved via GraphQL: $url -> $resolved")
                ResolveResult.Success(resolved)
            } else {
                ResolveResult.Failed(url, "Could not extract username from response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "GraphQL resolution error", e)
            ResolveResult.Failed(url, e.message ?: "Unknown error")
        }
    }

    /** Resolves via the vxtwitter / fxtwitter proxies, which 3xx to the canonical URL. */
    private suspend fun resolveViaProxy(url: String): String {
        for (proxyDomain in PROXIES) {
            try {
                val proxyUrl = when {
                    url.contains("x.com") -> url.replace("x.com", proxyDomain)
                    url.contains("twitter.com") -> url.replace("twitter.com", proxyDomain)
                    else -> continue
                }
                Log.d(TAG, "Trying proxy: $proxyUrl")

                val response = noRedirectClient.get(proxyUrl) {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    header(HttpHeaders.Accept, "text/html")
                }
                Log.d(TAG, "Proxy response code: ${response.status.value}")

                // Preferred: the redirect Location header names the author directly.
                val location = response.headers[HttpHeaders.Location]
                if (location != null) {
                    usernameFromStatusUrl(location)?.let { username ->
                        val statusId = url.substringAfter("/status/").substringBefore("?")
                        val resolved = "https://x.com/$username/status/$statusId"
                        Log.d(TAG, "Resolved via redirect: $url -> $resolved")
                        return resolved
                    }
                }

                // Fallback: scrape og:url (and friends) out of the HTML body.
                val body = response.bodyAsText()
                Log.d(TAG, "Body length: ${body.length}")
                for (pattern in HTML_USERNAME_PATTERNS) {
                    val username = pattern.find(body)?.groupValues?.getOrNull(1)
                    if (!username.isNullOrEmpty() && username != "i") {
                        val statusId = url.substringAfter("/status/").substringBefore("?")
                        val resolved = "https://x.com/$username/status/$statusId"
                        Log.d(TAG, "Resolved via HTML pattern: $url -> $resolved")
                        return resolved
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Proxy $proxyDomain error: ${e.message}")
            }
        }

        Log.w(TAG, "Could not resolve via any proxy")
        return url
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // Public bearer token used by X's own web client.
        private const val X_BEARER_TOKEN =
            "AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA"

        private const val GRAPHQL_ENDPOINT =
            "https://x.com/i/api/graphql/xOhkmRac04YFZmOzU9PJHg/TweetDetail"

        private val PROXIES = listOf("vxtwitter.com", "fxtwitter.com")

        private val HTML_USERNAME_PATTERNS = listOf(
            """og:url"[^>]*content="[^"]*(?:twitter|x)\.com/([A-Za-z0-9_]+)/status/""".toRegex(),
            """twitter\.com/([A-Za-z0-9_]+)/status/\d+""".toRegex(),
            """x\.com/([A-Za-z0-9_]+)/status/\d+""".toRegex(),
        )

        private val screenNameRegex =
            """"screen_name"\s*:\s*"([A-Za-z0-9_]+)"""".toRegex()

        private val json = Json { ignoreUnknownKeys = true }

        /** Whether [url] is an X `/i/status/` URL that hides its author and needs resolving. */
        fun isXRedirectUrl(url: String): Boolean =
            url.contains("x.com/i/status/") || url.contains("twitter.com/i/status/")

        /** The tweet ID from a `/status/{id}` URL, or null if it is not all digits. */
        internal fun tweetIdOf(url: String): String? {
            val id = url.substringAfter("/status/").substringBefore("?").substringBefore("/")
            return id.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        }

        /** The `username` from a `…/{username}/status/…` URL, excluding the placeholder `i`. */
        internal fun usernameFromStatusUrl(url: String): String? {
            val name = """/([A-Za-z0-9_]+)/status/""".toRegex().find(url)?.groupValues?.get(1)
            return name?.takeIf { it.isNotEmpty() && it != "i" }
        }

        private fun isAuthErrorBody(body: String): Boolean =
            body.contains("\"code\":32") || body.contains("\"code\":63") ||
                body.contains("Could not authenticate") || body.contains("Bad guest token")

        /**
         * Pulls `screen_name` out of a TweetDetail GraphQL response.
         *
         * The structure is deep and varies between response shapes, so this walks it defensively
         * and falls back to a regex over the raw text — exactly as the OkHttp/`org.json` version
         * did. `internal` so it can be unit-tested against a captured response without any HTTP.
         */
        internal fun extractScreenName(body: String): String? {
            val root = runCatching { json.parseToJsonElement(body) }.getOrNull()
                ?: return regexScreenName(body)
            val data = root.obj()?.get("data")?.obj() ?: return regexScreenName(body)

            // Path 1: the timeline entries a TweetDetail query returns.
            data["threaded_conversation_with_injections_v2"].obj()
                ?.get("instructions")?.arr()
                ?.forEach { instruction ->
                    instruction.obj()?.get("entries")?.arr()?.forEach { entry ->
                        entry.obj()
                            ?.get("content").obj()
                            ?.get("itemContent").obj()
                            ?.get("tweet_results").obj()
                            ?.get("result").obj()
                            ?.screenName()
                            ?.let { return it }
                    }
                }

            // Path 2: a direct tweetResult, the simpler response shape.
            data["tweetResult"].obj()?.get("result").obj()?.screenName()?.let { return it }

            return regexScreenName(body)
        }

        private fun regexScreenName(body: String): String? =
            screenNameRegex.find(body)?.groupValues?.get(1)

        /** Navigates `result → core → user_results → result → legacy → screen_name`. */
        private fun JsonObject.screenName(): String? =
            this["core"].obj()
                ?.get("user_results").obj()
                ?.get("result").obj()
                ?.get("legacy").obj()
                ?.get("screen_name")?.stringOrNull()
                ?.takeIf { it.isNotEmpty() }

        private fun graphQlVariables(tweetId: String): String = buildJsonObject {
            put("focalTweetId", tweetId)
            put("includePromotedContent", false)
            put("withCommunity", true)
            put("withVoice", true)
            put("withBirdwatchNotes", true)
        }.toString()

        // Required by the endpoint; the exact flag set X's web client sends.
        private val GRAPHQL_FEATURES: String = buildJsonObject {
            put("creator_subscriptions_tweet_preview_api_enabled", true)
            put("premium_content_api_read_enabled", false)
            put("communities_web_enable_tweet_community_results_fetch", true)
            put("c9s_tweet_anatomy_moderator_badge_enabled", true)
            put("responsive_web_grok_analyze_button_fetch_trends_enabled", false)
            put("responsive_web_grok_analyze_post_followups_enabled", false)
            put("responsive_web_jetfuel_frame", false)
            put("responsive_web_grok_share_attachment_enabled", false)
            put("articles_preview_enabled", true)
            put("responsive_web_edit_tweet_api_enabled", true)
            put("graphql_is_translatable_rweb_tweet_is_translatable_enabled", true)
            put("view_counts_everywhere_api_enabled", true)
            put("longform_notetweets_consumption_enabled", true)
            put("responsive_web_twitter_article_tweet_consumption_enabled", true)
            put("tweet_awards_web_tipping_enabled", false)
            put("responsive_web_grok_show_grok_translated_post", false)
            put("responsive_web_grok_analysis_button_from_backend", false)
            put("longform_notetweets_rich_text_read_enabled", true)
            put("longform_notetweets_inline_media_enabled", true)
            put("responsive_web_enhance_cards_enabled", false)
            put("rweb_tipjar_consumption_enabled", true)
            put("responsive_web_graphql_exclude_directive_enabled", true)
            put("verified_phone_label_enabled", false)
            put("freedom_of_speech_not_reach_fetch_enabled", true)
            put("standardized_nudges_misinfo", true)
            put("tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled", true)
            put("responsive_web_graphql_skip_user_profile_image_extensions_enabled", false)
            put("responsive_web_graphql_timeline_navigation_enabled", true)
            put("responsive_web_media_download_video_enabled", true)
            put("tweetypie_unmention_optimization_enabled", true)
            put("responsive_web_text_conversations_enabled", false)
            put("vibe_api_enabled", true)
        }.toString()

        // --- small JsonElement navigation helpers (null-safe, never throw) ---

        private fun JsonElement?.obj(): JsonObject? = this as? JsonObject

        private fun JsonElement?.arr(): JsonArray? = this as? JsonArray

        private fun JsonElement?.stringOrNull(): String? =
            (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }
}
