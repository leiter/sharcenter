package cut.the.crap.intent

import cut.the.crap.tools.urlEncode

private const val TWITTER_BASE_URL: String = "https://twitter.com/intent/"

// Regex pattern to extract URLs from text
private const val URL_PATTERN = "\\b((https?|ftp)://|www\\.)[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|]"
private val urlRegex = Regex(URL_PATTERN, RegexOption.IGNORE_CASE)

// Regex pattern to match Twitter/X URLs and extract the tweet ID
private const val TWEET_ID_PATTERN = "x\\.com/\\w+/status/(\\d+)"
private val tweetIdRegex = Regex(TWEET_ID_PATTERN, RegexOption.IGNORE_CASE)

/**
 * Sealed interface representing Twitter/X platform intents.
 * Each intent generates a specific Twitter URL for different actions.
 */
sealed interface TwitterIntent {
    val url: String

    /**
     * Post a new tweet with optional URL attachment.
     * @param text The tweet text content
     * @param prominentUrl Optional URL to attach to the tweet (will be shown as link preview)
     */
    data class PostTweet(
        val text: String,
        val prominentUrl: String? = null,
    ) : TwitterIntent {
        override val url: String
            get() {
                var preparedText = text
                val attachmentUrl = if (!prominentUrl.isNullOrEmpty()) {
                    prominentUrl
                } else {
                    // Auto-extract first URL from text if no prominent URL provided
                    val urls = text.extractUrls()
                    if (urls.isNotEmpty()) {
                        val extractedUrl = urls.first()
                        preparedText = text.replace(extractedUrl, "")
                        extractedUrl
                    } else {
                        ""
                    }
                }

                val path = if (attachmentUrl.isEmpty()) {
                    "tweet?text=${preparedText.urlEncode()}"
                } else {
                    "tweet?text=${preparedText.urlEncode()}&url=${attachmentUrl.urlEncode()}"
                }
                return "$TWITTER_BASE_URL$path"
            }
    }

    /**
     * Retweet an existing tweet.
     * @param tweetId The ID of the tweet to retweet
     */
    data class Retweet(
        val tweetId: String,
    ) : TwitterIntent {
        override val url: String =
            "${TWITTER_BASE_URL}retweet?tweet_id=$tweetId"
    }

    /**
     * Quote tweet - retweet with a comment.
     * @param tweetId The ID of the tweet to quote
     * @param comment Your comment/quote text
     */
    data class QuoteTweet(
        val tweetId: String,
        val comment: String,
    ) : TwitterIntent {
        override val url: String
            get() {
                // Quote tweets use the tweet URL as an attachment to a new tweet
                val tweetUrl = "https://x.com/i/status/$tweetId"
                return "${TWITTER_BASE_URL}tweet?text=${comment.urlEncode()}&url=${tweetUrl.urlEncode()}"
            }
    }

    /**
     * Reply to an existing tweet (comment on it).
     * @param tweetId The ID of the tweet to reply to
     * @param replyText The reply/comment text
     */
    data class Reply(
        val tweetId: String,
        val replyText: String,
    ) : TwitterIntent {
        override val url: String =
            "${TWITTER_BASE_URL}tweet?in_reply_to=${tweetId.urlEncode()}&text=${replyText.urlEncode()}"
    }
}

/**
 * Extract all URLs from a string.
 * @return List of URLs found in the text
 */
fun String.extractUrls(): List<String> {
    return urlRegex.findAll(this).map { it.value }.toList()
}

/**
 * Extract tweet ID from a Twitter/X URL or return the string if it's already a tweet ID.
 * @return The extracted tweet ID, or the original string if no ID found
 */
fun String.extractTweetId(): String {
    // If the string is already all digits, return it as-is
    if (this.toCharArray().all { it.isDigit() }) return this

    // Try to extract from URL pattern
    val matchResult = tweetIdRegex.find(this)
    return matchResult?.groups?.get(1)?.value ?: this
}
