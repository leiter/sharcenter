package cut.the.crap.intent

import android.net.Uri

private const val FACEBOOK_BASE_URL: String = "https://www.facebook.com/"
private const val FACEBOOK_SHARER_URL: String = "https://www.facebook.com/sharer/sharer.php"

// Regex pattern to extract URLs from text
private const val URL_PATTERN = "\\b((https?|ftp)://|www\\.)[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|]"
private val urlRegex = Regex(URL_PATTERN, RegexOption.IGNORE_CASE)

// Regex pattern to match Facebook post URLs and extract the post ID
private const val POST_ID_PATTERN = "facebook\\.com/[\\w.-]+/posts/(\\d+)"
private val postIdRegex = Regex(POST_ID_PATTERN, RegexOption.IGNORE_CASE)

/**
 * Sealed interface representing Facebook platform intents.
 * Each intent generates a specific Facebook URL for different actions.
 */
sealed interface FacebookIntent {
    val url: String

    /**
     * Share a new post with optional URL attachment.
     * @param text The post text content
     * @param prominentUrl Optional URL to share (will be shown as link preview)
     */
    data class SharePost(
        val text: String,
        val prominentUrl: String? = null,
    ) : FacebookIntent {
        override val url: String
            get() {
                var preparedText = text
                val attachmentUrl = if (!prominentUrl.isNullOrEmpty()) {
                    prominentUrl
                } else {
                    // Auto-extract first URL from text if no prominent URL provided
                    val urls = text.extractFacebookUrls()
                    if (urls.isNotEmpty()) {
                        val extractedUrl = urls.first()
                        preparedText = text.replace(extractedUrl, "")
                        extractedUrl
                    } else {
                        ""
                    }
                }

                return if (attachmentUrl.isEmpty()) {
                    // Facebook doesn't have a direct API for posting text-only via URL intent
                    // This opens the share dialog without a URL
                    "${FACEBOOK_BASE_URL}dialog/share?quote=${Uri.encode(preparedText)}"
                } else {
                    // Share with URL and optional quote
                    "$FACEBOOK_SHARER_URL?u=${Uri.encode(attachmentUrl)}&quote=${Uri.encode(preparedText)}"
                }
            }
    }

    /**
     * Share a URL (similar to reposting).
     * @param urlToShare The URL to share
     * @param quote Optional quote/comment to add to the share
     */
    data class ShareUrl(
        val urlToShare: String,
        val quote: String? = null,
    ) : FacebookIntent {
        override val url: String
            get() {
                return if (quote.isNullOrEmpty()) {
                    "$FACEBOOK_SHARER_URL?u=${Uri.encode(urlToShare)}"
                } else {
                    "$FACEBOOK_SHARER_URL?u=${Uri.encode(urlToShare)}&quote=${Uri.encode(quote)}"
                }
            }
    }

    /**
     * Share a Facebook post with a comment (similar to quote tweet).
     * @param postUrl The URL of the Facebook post to share
     * @param comment Your comment/quote text
     */
    data class ShareWithComment(
        val postUrl: String,
        val comment: String,
    ) : FacebookIntent {
        override val url: String
            get() = "$FACEBOOK_SHARER_URL?u=${Uri.encode(postUrl)}&quote=${Uri.encode(comment)}"
    }

    /**
     * Comment on a Facebook post.
     * Note: Facebook doesn't provide a direct URL intent for commenting.
     * This will open the post, but the user needs to manually add the comment.
     * @param postUrl The URL of the post to comment on
     */
    data class CommentOnPost(
        val postUrl: String,
    ) : FacebookIntent {
        override val url: String = postUrl
    }
}

/**
 * Extract all URLs from a string (Facebook-specific).
 * @return List of URLs found in the text
 */
private fun String.extractFacebookUrls(): List<String> {
    return urlRegex.findAll(this).map { it.value }.toList()
}

/**
 * Extract post ID from a Facebook URL or return the string if it's already a post ID.
 * @return The extracted post ID, or the original string if no ID found
 */
fun String.extractFacebookPostId(): String {
    // If the string is already all digits, return it as-is
    if (this.toCharArray().all { it.isDigit() }) return this

    // Try to extract from URL pattern
    val matchResult = postIdRegex.find(this)
    return matchResult?.groups?.get(1)?.value ?: this
}
