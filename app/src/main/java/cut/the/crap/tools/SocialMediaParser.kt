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
