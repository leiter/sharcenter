package cut.the.crap.tools

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val TAG = "UrlResolver"

/**
 * Resolves shortened or redirect URLs to their final destination.
 * Useful for X/Twitter /i/status/ URLs that redirect to the full URL with username.
 */
object UrlResolver {

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    // Bearer token for X API (public, used by web client)
    private const val X_BEARER_TOKEN = "AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA"

    // GraphQL endpoint for tweet details
    private const val GRAPHQL_ENDPOINT = "https://x.com/i/api/graphql/xOhkmRac04YFZmOzU9PJHg/TweetDetail"

    // OkHttp client for API calls
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * X API credentials for resolving /i/status/ URLs
     */
    data class XCredentials(
        val authToken: String,
        val ct0Token: String
    )

    /**
     * Result of URL resolution
     */
    sealed class ResolveResult {
        data class Success(val url: String) : ResolveResult()
        data class AuthRequired(val originalUrl: String) : ResolveResult()
        data class Failed(val originalUrl: String, val reason: String) : ResolveResult()
    }

    /**
     * Resolves a URL by following redirects and returning the final URL.
     * For X/Twitter /i/status/ URLs, uses the GraphQL API with credentials.
     *
     * @param url The URL to resolve
     * @param credentials Optional X credentials for GraphQL API
     * @return The final URL after resolution, or the original URL if resolution fails
     */
    suspend fun resolveRedirect(url: String, credentials: XCredentials? = null): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Resolving URL: $url")

            // For X /i/status/ URLs, use GraphQL API if credentials are available
            if (isXRedirectUrl(url)) {
                if (credentials != null) {
                    val resolved = resolveViaGraphQL(url, credentials)
                    if (resolved != url) {
                        return@withContext resolved
                    }
                }
                // Fallback to proxy method if GraphQL fails or no credentials
                return@withContext resolveViaProxy(url)
            }

            // For other URLs, follow redirects
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                Log.d(TAG, "Response code: ${response.code}, Final URL: $finalUrl")

                if (finalUrl != url) {
                    Log.d(TAG, "Resolved via redirect: $url -> $finalUrl")
                    return@withContext finalUrl
                }

                url
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving URL: $url", e)
            url
        }
    }

    /**
     * Resolves X/Twitter /i/status/ URL via GraphQL API to get the real username.
     */
    private fun resolveViaGraphQL(url: String, credentials: XCredentials): String {
        try {
            // Extract tweet ID from URL
            val tweetId = url.substringAfter("/status/").substringBefore("?").substringBefore("/")
            if (tweetId.isEmpty() || !tweetId.all { it.isDigit() }) {
                Log.w(TAG, "Invalid tweet ID: $tweetId")
                return url
            }

            Log.d(TAG, "Resolving tweet ID via GraphQL: $tweetId")

            // Build variables JSON
            val variables = JSONObject().apply {
                put("focalTweetId", tweetId)
                put("includePromotedContent", false)
                put("withCommunity", true)
                put("withVoice", true)
                put("withBirdwatchNotes", true)
            }.toString()

            // Build features JSON (required by API)
            val features = JSONObject().apply {
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

            // Build the URL with encoded parameters
            val encodedVariables = URLEncoder.encode(variables, "UTF-8")
            val encodedFeatures = URLEncoder.encode(features, "UTF-8")
            val requestUrl = "$GRAPHQL_ENDPOINT?variables=$encodedVariables&features=$encodedFeatures"

            val request = Request.Builder()
                .url(requestUrl)
                .header("Authorization", "Bearer $X_BEARER_TOKEN")
                .header("Cookie", "auth_token=${credentials.authToken}; ct0=${credentials.ct0Token}")
                .header("x-csrf-token", credentials.ct0Token)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "GraphQL response code: ${response.code}")

                if (!response.isSuccessful) {
                    Log.w(TAG, "GraphQL request failed: ${response.code}")
                    return url
                }

                val body = response.body?.string() ?: return url

                // Parse JSON to extract screen_name
                val screenName = extractScreenName(body, tweetId)
                if (screenName != null && screenName != "i") {
                    val resolvedUrl = "https://x.com/$screenName/status/$tweetId"
                    Log.d(TAG, "Resolved via GraphQL: $url -> $resolvedUrl")
                    return resolvedUrl
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "GraphQL resolution error", e)
        }
        return url
    }

    /**
     * Extract screen_name from GraphQL response JSON
     */
    private fun extractScreenName(json: String, tweetId: String): String? {
        try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data")
            if (data == null) {
                Log.d(TAG, "No 'data' in response. Keys: ${root.keys().asSequence().toList()}")
                return tryRegexExtraction(json)
            }

            // Try the timeline entries path first (TweetDetail returns this structure)
            val threadedConversation = data.optJSONObject("threaded_conversation_with_injections_v2")
            if (threadedConversation != null) {
                Log.d(TAG, "Found threaded_conversation_with_injections_v2")
                val instructions = threadedConversation.optJSONArray("instructions")
                if (instructions != null && instructions.length() > 0) {
                    for (i in 0 until instructions.length()) {
                        val instruction = instructions.optJSONObject(i)
                        val entries = instruction?.optJSONArray("entries")
                        if (entries != null) {
                            for (j in 0 until entries.length()) {
                                val entry = entries.optJSONObject(j)
                                val content = entry?.optJSONObject("content")
                                val itemContent = content?.optJSONObject("itemContent")
                                val tweetResults = itemContent?.optJSONObject("tweet_results")
                                val result = tweetResults?.optJSONObject("result")
                                val core = result?.optJSONObject("core")
                                val userResults = core?.optJSONObject("user_results")
                                val userResult = userResults?.optJSONObject("result")
                                val legacy = userResult?.optJSONObject("legacy")
                                val screenName = legacy?.optString("screen_name")
                                if (!screenName.isNullOrEmpty()) {
                                    Log.d(TAG, "Found screen_name in timeline: $screenName")
                                    return screenName
                                }
                            }
                        }
                    }
                }
            }

            // Try direct tweetResult path
            val tweetResult = data.optJSONObject("tweetResult")
            if (tweetResult == null) {
                Log.d(TAG, "No 'tweetResult' in data. Keys: ${data.keys().asSequence().toList()}")
                return tryRegexExtraction(json)
            }

            val result = tweetResult.optJSONObject("result")
            if (result == null) {
                Log.d(TAG, "No 'result' in tweetResult")
                return tryRegexExtraction(json)
            }

            // Navigate to core -> user_results -> result -> legacy -> screen_name
            val core = result.optJSONObject("core")
            if (core == null) {
                Log.d(TAG, "No 'core' in result. Keys: ${result.keys().asSequence().toList()}")
                return tryRegexExtraction(json)
            }

            val userResults = core.optJSONObject("user_results")
            val userResult = userResults?.optJSONObject("result")
            val legacy = userResult?.optJSONObject("legacy")

            return legacy?.optString("screen_name")?.takeIf { it.isNotEmpty() } ?: tryRegexExtraction(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GraphQL response", e)
            return tryRegexExtraction(json)
        }
    }

    private fun tryRegexExtraction(json: String): String? {
        // Fallback: try regex pattern matching
        val screenNameRegex = """"screen_name"\s*:\s*"([A-Za-z0-9_]+)"""".toRegex()
        val match = screenNameRegex.find(json)
        val result = match?.groupValues?.get(1)
        Log.d(TAG, "Regex extraction result: $result")
        return result
    }

    // Client that doesn't follow redirects - to capture redirect Location header
    private val noRedirectClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves X/Twitter URL via vxtwitter.com proxy which resolves /i/status/ URLs.
     * Fallback when GraphQL API is not available.
     */
    private fun resolveViaProxy(url: String): String {
        // Try multiple proxy services
        val proxies = listOf("vxtwitter.com", "fxtwitter.com")

        for (proxyDomain in proxies) {
            try {
                val proxyUrl = when {
                    url.contains("x.com") -> url.replace("x.com", proxyDomain)
                    url.contains("twitter.com") -> url.replace("twitter.com", proxyDomain)
                    else -> continue
                }
                Log.d(TAG, "Trying proxy: $proxyUrl")

                val request = Request.Builder()
                    .url(proxyUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html")
                    .build()

                // Use no-redirect client to capture the response before redirect
                noRedirectClient.newCall(request).execute().use { response ->
                    Log.d(TAG, "Proxy response code: ${response.code}")

                    // Check Location header for redirect
                    val location = response.header("Location")
                    if (location != null) {
                        Log.d(TAG, "Redirect location: $location")
                        val usernameMatch = """/([A-Za-z0-9_]+)/status/""".toRegex().find(location)
                        if (usernameMatch != null) {
                            val username = usernameMatch.groupValues[1]
                            if (username.isNotEmpty() && username != "i") {
                                val statusId = url.substringAfter("/status/").substringBefore("?")
                                val resolvedUrl = "https://x.com/$username/status/$statusId"
                                Log.d(TAG, "Resolved via redirect: $url -> $resolvedUrl")
                                return resolvedUrl
                            }
                        }
                    }

                    // Check HTML body for og:url or other patterns
                    val body = response.body?.string() ?: ""
                    Log.d(TAG, "Body length: ${body.length}")
                    if (body.length < 1000) {
                        Log.d(TAG, "Body content: $body")
                    }

                    // Try multiple patterns
                    val patterns = listOf(
                        """og:url"[^>]*content="[^"]*(?:twitter|x)\.com/([A-Za-z0-9_]+)/status/""".toRegex(),
                        """twitter\.com/([A-Za-z0-9_]+)/status/\d+""".toRegex(),
                        """x\.com/([A-Za-z0-9_]+)/status/\d+""".toRegex()
                    )

                    for (pattern in patterns) {
                        val match = pattern.find(body)
                        if (match != null) {
                            val username = match.groupValues[1]
                            if (username.isNotEmpty() && username != "i") {
                                val statusId = url.substringAfter("/status/").substringBefore("?")
                                val resolvedUrl = "https://x.com/$username/status/$statusId"
                                Log.d(TAG, "Resolved via HTML pattern: $url -> $resolvedUrl")
                                return resolvedUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Proxy $proxyDomain error: ${e.message}")
            }
        }

        Log.w(TAG, "Could not resolve via any proxy")
        return url
    }

    /**
     * Checks if a URL is an X/Twitter /i/status/ URL that needs resolution.
     */
    fun isXRedirectUrl(url: String): Boolean {
        return (url.contains("x.com/i/status/") || url.contains("twitter.com/i/status/"))
    }

    /**
     * Resolves an X/Twitter /i/status/ URL to get the real username.
     * Returns the original URL if it's not a redirect URL or if resolution fails.
     *
     * @param url The URL to resolve
     * @param credentials Optional X credentials for GraphQL API
     */
    suspend fun resolveXUrl(url: String, credentials: XCredentials? = null): String {
        return if (isXRedirectUrl(url)) {
            resolveRedirect(url, credentials)
        } else {
            url
        }
    }

    /**
     * Resolves an X/Twitter /i/status/ URL with detailed result status.
     * Use this when you need to know if authentication is required.
     *
     * @param url The URL to resolve
     * @param credentials Optional X credentials for GraphQL API
     * @return ResolveResult indicating success, auth required, or failure
     */
    suspend fun resolveXUrlWithStatus(url: String, credentials: XCredentials? = null): ResolveResult = withContext(Dispatchers.IO) {
        if (!isXRedirectUrl(url)) {
            return@withContext ResolveResult.Success(url)
        }

        try {
            Log.d(TAG, "Resolving URL with status: $url")

            // Try GraphQL first if credentials are available
            if (credentials != null) {
                val graphqlResult = resolveViaGraphQLWithStatus(url, credentials)
                when (graphqlResult) {
                    is ResolveResult.Success -> return@withContext graphqlResult
                    is ResolveResult.AuthRequired -> return@withContext graphqlResult
                    is ResolveResult.Failed -> {
                        // GraphQL failed for non-auth reasons, try proxy fallback
                        Log.d(TAG, "GraphQL failed: ${graphqlResult.reason}, trying proxy")
                    }
                }
            } else {
                Log.d(TAG, "No credentials provided, trying proxy")
            }

            // Fallback to proxy method
            val proxyResult = resolveViaProxy(url)
            if (proxyResult != url) {
                return@withContext ResolveResult.Success(proxyResult)
            }

            // Proxy failed too - if we had credentials that failed, report auth required
            // If no credentials were provided, suggest auth is needed for better resolution
            if (credentials != null) {
                ResolveResult.Failed(url, "Could not resolve URL")
            } else {
                ResolveResult.AuthRequired(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving URL with status", e)
            ResolveResult.Failed(url, e.message ?: "Unknown error")
        }
    }

    /**
     * GraphQL resolution with detailed status
     */
    private fun resolveViaGraphQLWithStatus(url: String, credentials: XCredentials): ResolveResult {
        try {
            // Extract tweet ID from URL
            val tweetId = url.substringAfter("/status/").substringBefore("?").substringBefore("/")
            if (tweetId.isEmpty() || !tweetId.all { it.isDigit() }) {
                Log.w(TAG, "Invalid tweet ID: $tweetId")
                return ResolveResult.Failed(url, "Invalid tweet ID")
            }

            Log.d(TAG, "Resolving tweet ID via GraphQL: $tweetId")

            // Build variables JSON
            val variables = JSONObject().apply {
                put("focalTweetId", tweetId)
                put("includePromotedContent", false)
                put("withCommunity", true)
                put("withVoice", true)
                put("withBirdwatchNotes", true)
            }.toString()

            // Build features JSON (required by API)
            val features = JSONObject().apply {
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

            // Build the URL with encoded parameters
            val encodedVariables = URLEncoder.encode(variables, "UTF-8")
            val encodedFeatures = URLEncoder.encode(features, "UTF-8")
            val requestUrl = "$GRAPHQL_ENDPOINT?variables=$encodedVariables&features=$encodedFeatures"

            val request = Request.Builder()
                .url(requestUrl)
                .header("Authorization", "Bearer $X_BEARER_TOKEN")
                .header("Cookie", "auth_token=${credentials.authToken}; ct0=${credentials.ct0Token}")
                .header("x-csrf-token", credentials.ct0Token)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "GraphQL response code: ${response.code}")

                // Check for auth errors
                if (response.code == 401 || response.code == 403) {
                    Log.w(TAG, "GraphQL auth failed: ${response.code}")
                    return ResolveResult.AuthRequired(url)
                }

                if (!response.isSuccessful) {
                    Log.w(TAG, "GraphQL request failed: ${response.code}")
                    return ResolveResult.Failed(url, "API error: ${response.code}")
                }

                val body = response.body?.string()
                if (body == null) {
                    return ResolveResult.Failed(url, "Empty response")
                }

                // Log first part of response for debugging
                Log.d(TAG, "GraphQL response (first 500 chars): ${body.take(500)}")

                // Check for auth errors in response body
                if (body.contains("\"code\":32") || body.contains("\"code\":63") ||
                    body.contains("Could not authenticate") || body.contains("Bad guest token")) {
                    Log.w(TAG, "GraphQL auth error in response body")
                    return ResolveResult.AuthRequired(url)
                }

                // Parse JSON to extract screen_name
                val screenName = extractScreenName(body, tweetId)
                Log.d(TAG, "Extracted screen_name: $screenName")
                if (screenName != null && screenName != "i") {
                    val resolvedUrl = "https://x.com/$screenName/status/$tweetId"
                    Log.d(TAG, "Resolved via GraphQL: $url -> $resolvedUrl")
                    return ResolveResult.Success(resolvedUrl)
                }

                return ResolveResult.Failed(url, "Could not extract username from response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "GraphQL resolution error", e)
            return ResolveResult.Failed(url, e.message ?: "Unknown error")
        }
    }
}
