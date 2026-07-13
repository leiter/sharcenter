package cut.the.crap.intent

// Instagram web URLs automatically open in the app if installed, otherwise open in browser
private const val INSTAGRAM_BASE_URL: String = "https://www.instagram.com/"

// Regex pattern to extract URLs from text
private const val URL_PATTERN = "\\b((https?|ftp)://|www\\.)[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|]"
private val urlRegex = Regex(URL_PATTERN, RegexOption.IGNORE_CASE)

// Regex pattern to match Instagram post URLs and extract the post ID
private const val POST_ID_PATTERN = "instagram\\.com/p/([A-Za-z0-9_-]+)"
private val postIdRegex = Regex(POST_ID_PATTERN, RegexOption.IGNORE_CASE)

// Regex pattern to match Instagram reel URLs and extract the reel ID
private const val REEL_ID_PATTERN = "instagram\\.com/reel/([A-Za-z0-9_-]+)"
private val reelIdRegex = Regex(REEL_ID_PATTERN, RegexOption.IGNORE_CASE)

/**
 * Sealed interface representing Instagram platform intents.
 * Each intent generates a specific Instagram URL for different actions.
 *
 * Note: Instagram web URLs will automatically open in the app if installed,
 * otherwise they open in a web browser. However, Instagram has limited web functionality
 * and some actions (like creating posts) are only available through the app.
 */
sealed interface InstagramIntent {
    val url: String

    /**
     * Open a user's profile.
     * @param username The Instagram username (without @ symbol)
     */
    data class OpenProfile(
        val username: String,
    ) : InstagramIntent {
        override val url: String
            get() {
                val cleanUsername = username.removePrefix("@")
                return "$INSTAGRAM_BASE_URL$cleanUsername/"
            }
    }

    /**
     * Open a specific post.
     * @param postId The Instagram post ID (short code from URL)
     */
    data class OpenPost(
        val postId: String,
    ) : InstagramIntent {
        override val url: String = "${INSTAGRAM_BASE_URL}p/$postId/"
    }

    /**
     * Open a specific reel.
     * @param reelId The Instagram reel ID (short code from URL)
     */
    data class OpenReel(
        val reelId: String,
    ) : InstagramIntent {
        override val url: String = "${INSTAGRAM_BASE_URL}reel/$reelId/"
    }

    /**
     * Open Instagram Direct Messages.
     * Will open in app if installed, otherwise opens Instagram homepage in browser.
     */
    data object OpenDirectMessages : InstagramIntent {
        override val url: String = "${INSTAGRAM_BASE_URL}direct/inbox/"
    }

    /**
     * Open a user's profile to message them.
     * Will open in app if installed, otherwise opens profile in browser.
     * @param username The Instagram username to view (without @ symbol)
     */
    data class MessageUser(
        val username: String,
    ) : InstagramIntent {
        override val url: String
            get() {
                val cleanUsername = username.removePrefix("@")
                return "$INSTAGRAM_BASE_URL$cleanUsername/"
            }
    }

    /**
     * Open Instagram explore/search page.
     * Will open in app if installed, otherwise opens in browser.
     */
    data object OpenExplore : InstagramIntent {
        override val url: String = "${INSTAGRAM_BASE_URL}explore/"
    }

    /**
     * Search for a hashtag.
     * @param hashtag The hashtag to search (without # symbol)
     */
    data class SearchHashtag(
        val hashtag: String,
    ) : InstagramIntent {
        override val url: String
            get() {
                val cleanHashtag = hashtag.removePrefix("#")
                return "${INSTAGRAM_BASE_URL}explore/tags/$cleanHashtag/"
            }
    }

    /**
     * Open Instagram to a specific location.
     * @param locationId The Instagram location ID
     */
    data class OpenLocation(
        val locationId: String,
    ) : InstagramIntent {
        override val url: String = "${INSTAGRAM_BASE_URL}explore/locations/$locationId/"
    }
}

/**
 * Extract all URLs from a string (Instagram-specific).
 * @return List of URLs found in the text
 */
private fun String.extractInstagramUrls(): List<String> {
    return urlRegex.findAll(this).map { it.value }.toList()
}

/**
 * Extract post ID from an Instagram post URL or return the string if it's already a post ID.
 * @return The extracted post ID, or the original string if no ID found
 */
fun String.extractInstagramPostId(): String {
    // If the string is a valid short code (alphanumeric, underscore, hyphen), return it as-is
    if (this.matches(Regex("[A-Za-z0-9_-]+"))) return this

    // Try to extract from URL pattern
    val matchResult = postIdRegex.find(this)
    return matchResult?.groups?.get(1)?.value ?: this
}

/**
 * Extract reel ID from an Instagram reel URL or return the string if it's already a reel ID.
 * @return The extracted reel ID, or the original string if no ID found
 */
fun String.extractInstagramReelId(): String {
    // If the string is a valid short code (alphanumeric, underscore, hyphen), return it as-is
    if (this.matches(Regex("[A-Za-z0-9_-]+"))) return this

    // Try to extract from URL pattern
    val matchResult = reelIdRegex.find(this)
    return matchResult?.groups?.get(1)?.value ?: this
}

/**
 * Extract username from an Instagram profile URL or return the string if it's already a username.
 * @return The extracted username, or the original string if no username found
 */
fun String.extractInstagramUsername(): String {
    // Remove @ prefix if present
    val cleaned = this.removePrefix("@")

    // If it's already a valid username format, return it
    if (cleaned.matches(Regex("[a-zA-Z0-9._]+"))) return cleaned

    // Try to extract from Instagram URL
    val usernamePattern = Regex("instagram\\.com/([a-zA-Z0-9._]+)/?")
    val matchResult = usernamePattern.find(this)
    return matchResult?.groups?.get(1)?.value ?: cleaned
}
