package cut.the.crap.ui.content.posts

/**
 * Calculates character count for Twitter/X post formatting.
 * Links are counted as 23 characters regardless of their actual length.
 */
fun calculateCharCount(text: String): Int {
    // Regex pattern to match URLs (http, https, www)
    val urlPattern = Regex(
        pattern = """https?://\S+|www\.\S+""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    var charCount = text.length
    val links = urlPattern.findAll(text)

    // For each link found, subtract its actual length and add 23
    links.forEach { matchResult ->
        val linkLength = matchResult.value.length
        charCount = charCount - linkLength + 23
    }

    return charCount
}
