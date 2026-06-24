package cut.the.crap.data.rest

/**
 * Utility to parse YouTube URLs and extract video IDs
 */
object YouTubeUrlParser {

    // YouTube URL regex patterns - compiled once and reused
    private val WATCH_PATTERN = """(?:youtube\.com\/watch\?v=)([a-zA-Z0-9_-]{11})""".toRegex()
    private val SHORT_PATTERN = """(?:youtu\.be\/)([a-zA-Z0-9_-]{11})""".toRegex()
    private val EMBED_PATTERN = """(?:youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})""".toRegex()
    private val V_PATTERN = """(?:youtube\.com\/v\/)([a-zA-Z0-9_-]{11})""".toRegex()

    /**
     * Extracts the video ID from a YouTube URL
     *
     * Supports various YouTube URL formats:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://www.youtube.com/embed/VIDEO_ID
     * - https://www.youtube.com/v/VIDEO_ID
     * - https://m.youtube.com/watch?v=VIDEO_ID
     *
     * @param url The YouTube URL
     * @return The video ID if found, null otherwise
     */
    fun extractVideoId(url: String): String? {
        // Remove whitespace and convert to lowercase for matching
        val cleanUrl = url.trim()

        // Pattern 1: Standard watch URL (youtube.com/watch?v=VIDEO_ID)
        WATCH_PATTERN.find(cleanUrl)?.let { return it.groupValues[1] }

        // Pattern 2: Short URL (youtu.be/VIDEO_ID)
        SHORT_PATTERN.find(cleanUrl)?.let { return it.groupValues[1] }

        // Pattern 3: Embed URL (youtube.com/embed/VIDEO_ID)
        EMBED_PATTERN.find(cleanUrl)?.let { return it.groupValues[1] }

        // Pattern 4: Old style URL (youtube.com/v/VIDEO_ID)
        V_PATTERN.find(cleanUrl)?.let { return it.groupValues[1] }

        return null
    }

    /**
     * Checks if a URL is a valid YouTube URL
     *
     * @param url The URL to check
     * @return true if it's a YouTube URL, false otherwise
     */
    fun isYouTubeUrl(url: String): Boolean {
        val cleanUrl = url.trim().lowercase()
        return cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be")
    }

    /**
     * Constructs a standard YouTube watch URL from a video ID
     *
     * @param videoId The YouTube video ID
     * @return The standard YouTube watch URL
     */
    fun constructWatchUrl(videoId: String): String {
        return "https://www.youtube.com/watch?v=$videoId"
    }
}
