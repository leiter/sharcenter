package cut.the.crap.tools

/**
 * Data class representing parsed information from social media URLs
 */
data class SocialMediaInfo(
    val platform: String,           // "instagram", "facebook", "x", "youtube", etc.
    val contentType: String,        // "post", "reel", "video", "photo", "story", "profile", etc.
    val identifier: String?,        // Post ID, video ID, etc.
    val username: String? = null,   // Username if available in URL
    val additionalInfo: Map<String, String> = emptyMap()  // Any extra parsed data
)

// Instagram regex patterns
private val INSTAGRAM_POST_REGEX = """/p/([A-Za-z0-9_-]+)""".toRegex()
private val INSTAGRAM_REEL_REGEX = """/reel/([A-Za-z0-9_-]+)""".toRegex()
private val INSTAGRAM_TV_REGEX = """/tv/([A-Za-z0-9_-]+)""".toRegex()
private val INSTAGRAM_STORY_REGEX = """/stories/([A-Za-z0-9_.]+)""".toRegex()
private val INSTAGRAM_PROFILE_REGEX = """instagram\.com/([A-Za-z0-9_.]+)/?$""".toRegex()

// Facebook regex patterns
private val FACEBOOK_POST_ID_REGEX = """/posts/(\d+)""".toRegex()
private val FACEBOOK_GROUP_ID_REGEX = """/groups/(\d+)""".toRegex()
private val FACEBOOK_POST_USERNAME_REGEX = """facebook\.com/([A-Za-z0-9.]+)/posts""".toRegex()
private val FACEBOOK_VIDEO_ID_REGEX = """/videos/(\d+)""".toRegex()
private val FACEBOOK_VIDEO_USERNAME_REGEX = """facebook\.com/([A-Za-z0-9.]+)/videos""".toRegex()
private val FACEBOOK_PROFILE_REGEX = """facebook\.com/([A-Za-z0-9.]+)/?$""".toRegex()

// X/Twitter regex patterns
private val TWITTER_TWEET_ID_REGEX = """/status/(\d+)""".toRegex()
private val TWITTER_USERNAME_REGEX = """(?:x\.com|twitter\.com)/([A-Za-z0-9_]+)""".toRegex()

// Bluesky regex patterns. The actor segment is a handle (e.g. "alice.bsky.social", a custom
// domain) or a DID ("did:plc:…"), so it is matched loosely up to the next path separator.
private val BLUESKY_POST_REGEX = """bsky\.app/profile/([^/?#]+)/post/([A-Za-z0-9]+)""".toRegex()
private val BLUESKY_PROFILE_REGEX = """bsky\.app/profile/([^/?#]+)""".toRegex()

// Mastodon regex patterns. Mastodon is federated with no fixed domain, so recognition is
// structural: a post is `https://{instance}/@{user}/{numericId}` and a profile is
// `https://{instance}/@{user}`. The captured host (with scheme) is kept in additionalInfo["host"]
// so the origin instance can be re-derived for the public status API and profile links.
private val MASTODON_POST_REGEX = """^(https?://[^/]+)/@([^/]+)/(\d{6,})""".toRegex()
private val MASTODON_PROFILE_REGEX = """^(https?://[^/]+)/@([^/]+?)/?(?:[?#].*)?$""".toRegex()

// Hosts that use the same `/@user` path form but are not Mastodon instances. Checked without a
// leading "www." so both bare and www hosts are covered. The already-handled platforms are also
// listed so `isMastodonUrl` is safe to call on any URL, independent of handler ordering.
private val NON_MASTODON_HOSTS = setOf(
    "youtube.com", "youtu.be", "medium.com", "x.com", "twitter.com",
    "instagram.com", "facebook.com", "fb.com", "fb.watch", "threads.net", "tiktok.com", "bsky.app"
)

private fun hostWithScheme(url: String): String? =
    """^(https?://[^/]+)""".toRegex().find(url)?.groupValues?.get(1)

private fun isMastodonHost(url: String): Boolean {
    val host = hostWithScheme(url)?.substringAfter("://")?.lowercase() ?: return false
    return host.removePrefix("www.") !in NON_MASTODON_HOSTS
}

/**
 * Parse Instagram URLs to extract content information
 *
 * Supported patterns:
 * - https://www.instagram.com/p/ABC123/           -> Post
 * - https://www.instagram.com/reel/XYZ456/        -> Reel
 * - https://www.instagram.com/tv/DEF789/          -> IGTV
 * - https://www.instagram.com/stories/username/   -> Story
 * - https://www.instagram.com/username/           -> Profile
 */
fun parseInstagramUrl(url: String): SocialMediaInfo? {
    if (!url.contains("instagram.com")) return null

    return when {
        // Instagram Post: /p/{post-id}/
        url.contains("/p/") -> {
            val match = INSTAGRAM_POST_REGEX.find(url)
            match?.let {
                SocialMediaInfo(
                    platform = "instagram",
                    contentType = "post",
                    identifier = it.groupValues[1]
                )
            }
        }

        // Instagram Reel: /reel/{reel-id}/
        url.contains("/reel/") -> {
            val match = INSTAGRAM_REEL_REGEX.find(url)
            match?.let {
                SocialMediaInfo(
                    platform = "instagram",
                    contentType = "reel",
                    identifier = it.groupValues[1]
                )
            }
        }

        // Instagram TV: /tv/{video-id}/
        url.contains("/tv/") -> {
            val match = INSTAGRAM_TV_REGEX.find(url)
            match?.let {
                SocialMediaInfo(
                    platform = "instagram",
                    contentType = "igtv",
                    identifier = it.groupValues[1]
                )
            }
        }

        // Instagram Story: /stories/{username}/
        url.contains("/stories/") -> {
            val match = INSTAGRAM_STORY_REGEX.find(url)
            match?.let {
                SocialMediaInfo(
                    platform = "instagram",
                    contentType = "story",
                    identifier = null,
                    username = it.groupValues[1]
                )
            }
        }

        // Instagram Profile: /{username}/
        else -> {
            // Extract username from URL path
            val match = INSTAGRAM_PROFILE_REGEX.find(url)
            match?.let {
                SocialMediaInfo(
                    platform = "instagram",
                    contentType = "profile",
                    identifier = null,
                    username = it.groupValues[1]
                )
            }
        }
    }
}

/**
 * Parse Facebook URLs to extract content information
 *
 * Supported patterns:
 * - https://www.facebook.com/username/posts/123    -> Post
 * - https://www.facebook.com/photo/?fbid=456       -> Photo
 * - https://www.facebook.com/watch/?v=789          -> Video
 * - https://www.facebook.com/reel/123              -> Reel
 * - https://www.facebook.com/share/r/ABC123/       -> Reel (share link)
 * - https://www.facebook.com/groups/123/posts/456  -> Group Post
 * - https://www.facebook.com/username/videos/123   -> Video
 * - https://www.facebook.com/username/             -> Profile
 */
fun parseFacebookUrl(url: String): SocialMediaInfo? {
    if (!url.contains("facebook.com") && !url.contains("fb.com") && !url.contains("fb.watch")) return null

    return when {
        // Facebook Watch/Video: /watch/?v={video-id} or fb.watch/{video-id}
        url.contains("/watch/") || url.contains("fb.watch") -> {
            val videoId = when {
                url.contains("?v=") -> url.substringAfter("?v=").substringBefore("&")
                url.contains("fb.watch/") -> url.substringAfter("fb.watch/").substringBefore("?")
                else -> null
            }
            videoId?.let {
                SocialMediaInfo(
                    platform = "facebook",
                    contentType = "video",
                    identifier = it
                )
            }
        }

        // Facebook Photo: /photo/?fbid={photo-id}
        url.contains("/photo/") && url.contains("fbid=") -> {
            val photoId = url.substringAfter("fbid=").substringBefore("&")
            SocialMediaInfo(
                platform = "facebook",
                contentType = "photo",
                identifier = photoId
            )
        }

        // Facebook Reel: /reel/{reel-id} or /share/r/{share-id}
        url.contains("/reel/") || (url.contains("/share/") && url.contains("/r/")) -> {
            val reelId = when {
                url.contains("/reel/") -> url.substringAfter("/reel/").substringBefore("/").substringBefore("?")
                url.contains("/share/r/") -> url.substringAfter("/share/r/").substringBefore("/").substringBefore("?")
                else -> null
            }
            reelId?.let {
                SocialMediaInfo(
                    platform = "facebook",
                    contentType = "reel",
                    identifier = it
                )
            }
        }

        // Facebook Group Post: /groups/{group-id}/posts/{post-id}
        url.contains("/groups/") && url.contains("/posts/") -> {
            val postMatch = FACEBOOK_POST_ID_REGEX.find(url)
            val groupMatch = FACEBOOK_GROUP_ID_REGEX.find(url)

            if (postMatch != null && groupMatch != null) {
                SocialMediaInfo(
                    platform = "facebook",
                    contentType = "group_post",
                    identifier = postMatch.groupValues[1],
                    additionalInfo = mapOf("group_id" to groupMatch.groupValues[1])
                )
            } else {
                null
            }
        }

        // Facebook Post: /{username}/posts/{post-id}
        url.contains("/posts/") -> {
            val postMatch = FACEBOOK_POST_ID_REGEX.find(url)
            val userMatch = FACEBOOK_POST_USERNAME_REGEX.find(url)

            SocialMediaInfo(
                platform = "facebook",
                contentType = "post",
                identifier = postMatch?.groupValues?.get(1),
                username = userMatch?.groupValues?.get(1)
            )
        }

        // Facebook Video: /{username}/videos/{video-id}
        url.contains("/videos/") -> {
            val videoMatch = FACEBOOK_VIDEO_ID_REGEX.find(url)
            val userMatch = FACEBOOK_VIDEO_USERNAME_REGEX.find(url)

            SocialMediaInfo(
                platform = "facebook",
                contentType = "video",
                identifier = videoMatch?.groupValues?.get(1),
                username = userMatch?.groupValues?.get(1)
            )
        }

        // Facebook Profile: /{username}/
        else -> {
            val match = FACEBOOK_PROFILE_REGEX.find(url)
            match?.let {
                SocialMediaInfo(
                    platform = "facebook",
                    contentType = "profile",
                    identifier = null,
                    username = it.groupValues[1]
                )
            }
        }
    }
}

/**
 * Parse YouTube URLs to extract content information
 *
 * Supported patterns:
 * - https://www.youtube.com/watch?v=ABC123           -> Video
 * - https://youtu.be/ABC123                          -> Video (short URL)
 * - https://www.youtube.com/shorts/XYZ456            -> Short
 * - https://www.youtube.com/playlist?list=PLxyz      -> Playlist
 * - https://www.youtube.com/c/ChannelName            -> Channel
 * - https://www.youtube.com/@username                -> Channel
 */
fun parseYoutubeUrl(url: String): SocialMediaInfo? {
    if (!url.contains("youtube.com") && !url.contains("youtu.be")) return null

    return when {
        // YouTube Short URL: youtu.be/{video-id}
        url.contains("youtu.be/") -> {
            val videoId = url.substringAfter("youtu.be/").substringBefore("?")
            SocialMediaInfo(
                platform = "youtube",
                contentType = "video",
                identifier = videoId
            )
        }

        // YouTube Shorts: /shorts/{video-id}
        url.contains("/shorts/") -> {
            val videoId = url.substringAfter("/shorts/").substringBefore("?")
            SocialMediaInfo(
                platform = "youtube",
                contentType = "short",
                identifier = videoId
            )
        }

        // YouTube Playlist: ?list={playlist-id}
        url.contains("?list=") || url.contains("&list=") -> {
            val playlistId = url.substringAfter("list=").substringBefore("&")
            SocialMediaInfo(
                platform = "youtube",
                contentType = "playlist",
                identifier = playlistId
            )
        }

        // YouTube Video: ?v={video-id}
        url.contains("?v=") || url.contains("&v=") -> {
            val videoId = url.substringAfter("v=").substringBefore("&")
            SocialMediaInfo(
                platform = "youtube",
                contentType = "video",
                identifier = videoId
            )
        }

        // YouTube Channel: /c/{channel-name} or /@{username}
        url.contains("/c/") || url.contains("/@") -> {
            val channelName = when {
                url.contains("/c/") -> url.substringAfter("/c/").substringBefore("/").substringBefore("?")
                url.contains("/@") -> url.substringAfter("/@").substringBefore("/").substringBefore("?")
                else -> null
            }
            channelName?.let {
                SocialMediaInfo(
                    platform = "youtube",
                    contentType = "channel",
                    identifier = null,
                    username = it
                )
            }
        }

        else -> null
    }
}

/**
 * Parse X/Twitter URLs to extract content information
 *
 * Supported patterns:
 * - https://x.com/username/status/123456    -> Tweet/Post
 * - https://twitter.com/username/status/123 -> Tweet/Post
 * - https://x.com/i/status/123456           -> Tweet/Post (shared from app, no username)
 * - https://x.com/username                  -> Profile
 */
fun parseXUrl(url: String): SocialMediaInfo? {
    if (!url.contains("x.com") && !url.contains("twitter.com")) return null

    return when {
        // Tweet/Post: /username/status/{tweet-id} or /i/status/{tweet-id}
        url.contains("/status/") -> {
            val tweetMatch = TWITTER_TWEET_ID_REGEX.find(url)
            val userMatch = TWITTER_USERNAME_REGEX.find(url)

            // Check if this is the /i/status/ format (shared from app, no real username)
            val extractedUsername = userMatch?.groupValues?.get(1)
            val username = if (extractedUsername == "i") null else extractedUsername

            SocialMediaInfo(
                platform = "x",
                contentType = "post",
                identifier = tweetMatch?.groupValues?.get(1),
                username = username
            )
        }

        // Profile: /username
        else -> {
            val match = TWITTER_USERNAME_REGEX.find(url)
            match?.let {
                val extractedUsername = it.groupValues[1]
                // Skip if it's just "i" (not a real profile)
                if (extractedUsername == "i") return null
                SocialMediaInfo(
                    platform = "x",
                    contentType = "profile",
                    identifier = null,
                    username = extractedUsername
                )
            }
        }
    }
}

/**
 * Parse Bluesky URLs to extract content information
 *
 * Supported patterns:
 * - https://bsky.app/profile/{actor}/post/{rkey}   -> Post
 * - https://bsky.app/profile/{actor}               -> Profile
 *
 * where {actor} is a handle ("alice.bsky.social", a custom domain) or a DID ("did:plc:…").
 */
fun parseBlueskyUrl(url: String): SocialMediaInfo? {
    if (!url.contains("bsky.app")) return null

    BLUESKY_POST_REGEX.find(url)?.let { match ->
        return SocialMediaInfo(
            platform = "bluesky",
            contentType = "post",
            identifier = match.groupValues[2],
            username = match.groupValues[1]
        )
    }

    BLUESKY_PROFILE_REGEX.find(url)?.let { match ->
        return SocialMediaInfo(
            platform = "bluesky",
            contentType = "profile",
            identifier = null,
            username = match.groupValues[1]
        )
    }

    return null
}

/** Whether [url] points at Bluesky (bsky.app). */
fun isBlueskyUrl(url: String): Boolean = url.contains("bsky.app")

/**
 * Builds the `at://` URI for the post [url] points at, resolvable by the public Bluesky API
 * (`app.bsky.feed.getPostThread`). Returns null for profile links and non-Bluesky URLs. The actor
 * segment (handle or DID) is used as the URI authority — the public AppView resolves handles.
 */
fun blueskyPostAtUri(url: String): String? {
    val info = parseBlueskyUrl(url) ?: return null
    if (info.contentType != "post") return null
    val actor = info.username?.takeIf { it.isNotBlank() } ?: return null
    val rkey = info.identifier?.takeIf { it.isNotBlank() } ?: return null
    return "at://$actor/app.bsky.feed.post/$rkey"
}

/**
 * Parse Mastodon URLs to extract content information.
 *
 * Supported patterns (on any instance host that is not a known non-Mastodon `@`-path site):
 * - https://{instance}/@{user}/{numericId}   -> Post
 * - https://{instance}/@{user}               -> Profile  ({user} may be federated, "user@remote")
 *
 * The instance host (with scheme) is stored in additionalInfo["host"] because, unlike the other
 * platforms, the API and profile links depend on which instance the content lives on.
 */
fun parseMastodonUrl(url: String): SocialMediaInfo? {
    if (!isMastodonHost(url)) return null

    MASTODON_POST_REGEX.find(url)?.let { match ->
        return SocialMediaInfo(
            platform = "mastodon",
            contentType = "post",
            identifier = match.groupValues[3],
            username = match.groupValues[2],
            additionalInfo = mapOf("host" to match.groupValues[1])
        )
    }

    MASTODON_PROFILE_REGEX.find(url)?.let { match ->
        val user = match.groupValues[2].takeIf { it.isNotBlank() } ?: return null
        return SocialMediaInfo(
            platform = "mastodon",
            contentType = "profile",
            identifier = null,
            username = user,
            additionalInfo = mapOf("host" to match.groupValues[1])
        )
    }

    return null
}

/** Whether [url] structurally looks like a Mastodon post/profile on a federated instance. */
fun isMastodonUrl(url: String): Boolean = parseMastodonUrl(url) != null

/**
 * Builds the public status API URL (`/api/v1/statuses/{id}` on the post's *origin* instance) for
 * the Mastodon post [url] points at. Returns null for profile links and non-Mastodon URLs. The
 * status endpoint is unauthenticated for public posts.
 */
fun mastodonStatusApiUrl(url: String): String? {
    val info = parseMastodonUrl(url) ?: return null
    if (info.contentType != "post") return null
    val host = info.additionalInfo["host"] ?: return null
    val id = info.identifier ?: return null
    return "$host/api/v1/statuses/$id"
}

/**
 * Parse any social media URL and return extracted information
 *
 * This is the main entry point - it automatically detects the platform
 * and calls the appropriate parser.
 *
 * @param url The social media URL to parse
 * @return SocialMediaInfo if URL is recognized, null otherwise
 */
fun parseSocialMediaUrl(url: String): SocialMediaInfo? {
    return when {
        url.contains("instagram.com") -> parseInstagramUrl(url)
        url.contains("facebook.com") || url.contains("fb.com") || url.contains("fb.watch") -> parseFacebookUrl(url)
        url.contains("youtube.com") || url.contains("youtu.be") -> parseYoutubeUrl(url)
        url.contains("x.com") || url.contains("twitter.com") -> parseXUrl(url)
        url.contains("bsky.app") -> parseBlueskyUrl(url)
        isMastodonUrl(url) -> parseMastodonUrl(url)
        else -> null
    }
}

/**
 * Get a human-readable display name for the content type
 */
fun SocialMediaInfo.getDisplayName(): String {
    return when (contentType.lowercase()) {
        "post" -> "Post"
        "reel" -> "Reel"
        "igtv" -> "IGTV"
        "story" -> "Story"
        "video" -> "Video"
        "short" -> "Short"
        "photo" -> "Photo"
        "profile" -> "Profile"
        "channel" -> "Channel"
        "playlist" -> "Playlist"
        "group_post" -> "Group Post"
        else -> contentType.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Build a URL that points at the channel / user profile behind this content, when one can be
 * derived from the parsed [username]. Returns null when there is no username to link to (e.g. a
 * YouTube video URL that carries no channel handle), so callers can omit the profile action.
 */
fun SocialMediaInfo.profileUrl(): String? {
    val user = username?.takeIf { it.isNotBlank() } ?: return null
    return when (platform.lowercase()) {
        "instagram" -> "https://www.instagram.com/$user/"
        "facebook" -> "https://www.facebook.com/$user/"
        "x" -> "https://x.com/$user"
        "youtube" -> "https://www.youtube.com/@$user"
        "bluesky" -> "https://bsky.app/profile/$user"
        "mastodon" -> additionalInfo["host"]?.let { "$it/@$user" }
        else -> null
    }
}

/**
 * Get a formatted description for the social media content
 */
fun SocialMediaInfo.getDescription(): String {
    val platformName = platform.replaceFirstChar { it.uppercase() }
    val typeName = getDisplayName()

    return when {
        username != null && identifier != null -> "$platformName $typeName by @$username"
        username != null -> "$platformName $typeName: @$username"
        identifier != null -> "$platformName $typeName: $identifier"
        else -> "$platformName $typeName"
    }
}
