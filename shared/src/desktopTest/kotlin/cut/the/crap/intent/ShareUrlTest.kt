package cut.the.crap.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the URLs actually handed to X and Facebook.
 *
 * The intent builders moved from `:app` to `commonMain` and swapped `android.net.Uri.encode` for
 * the common encoder. That swap is invisible to the compiler and invisible to a smoke test that
 * only checks "a browser opened" — a wrong encoding produces a *valid* URL with the wrong text
 * in it. So the full strings are asserted here.
 */
class ShareUrlTest {

    @Test
    fun `a plain tweet encodes its text into the query`() {
        val url = TwitterIntent.PostTweet(text = "hello world").url
        assertEquals("https://twitter.com/intent/tweet?text=hello%20world", url)
    }

    @Test
    fun `a tweet with a url attaches it and strips it from the text`() {
        val url = TwitterIntent.PostTweet(text = "look at this https://example.com/a").url
        assertEquals(
            "https://twitter.com/intent/tweet" +
                "?text=look%20at%20this%20" +
                "&url=https%3A%2F%2Fexample.com%2Fa",
            url,
        )
    }

    @Test
    fun `an explicit prominent url wins over one found in the text`() {
        val url = TwitterIntent.PostTweet(
            text = "see https://example.com/ignored",
            prominentUrl = "https://example.com/chosen",
        ).url
        assertTrue(url.endsWith("&url=https%3A%2F%2Fexample.com%2Fchosen"))
        // The in-text URL is left alone when it was not the one promoted.
        assertTrue(url.contains("example.com%2Fignored"))
    }

    @Test
    fun `tweet text containing query syntax cannot forge extra parameters`() {
        // The regression this whole test file exists for: an unescaped & or = here would let the
        // tweet body invent a `url=` parameter of its own.
        val url = TwitterIntent.PostTweet(text = "a&url=evil").url
        assertEquals("https://twitter.com/intent/tweet?text=a%26url%3Devil", url)
    }

    @Test
    fun `a quote tweet points at the quoted status`() {
        val url = TwitterIntent.QuoteTweet(tweetId = "12345", comment = "well said").url
        assertEquals(
            "https://twitter.com/intent/tweet" +
                "?text=well%20said" +
                "&url=https%3A%2F%2Fx.com%2Fi%2Fstatus%2F12345",
            url,
        )
    }

    @Test
    fun `a retweet needs no encoding`() {
        assertEquals(
            "https://twitter.com/intent/retweet?tweet_id=12345",
            TwitterIntent.Retweet("12345").url,
        )
    }

    @Test
    fun `a reply targets the tweet being replied to`() {
        assertEquals(
            "https://twitter.com/intent/tweet?in_reply_to=12345&text=my%20reply",
            TwitterIntent.Reply(tweetId = "12345", replyText = "my reply").url,
        )
    }

    @Test
    fun `a facebook share with a url uses the sharer endpoint`() {
        val url = FacebookIntent.SharePost(text = "read this https://example.com/a").url
        assertEquals(
            "https://www.facebook.com/sharer/sharer.php" +
                "?u=https%3A%2F%2Fexample.com%2Fa" +
                "&quote=read%20this%20",
            url,
        )
    }

    @Test
    fun `a text-only facebook share falls back to the share dialog`() {
        assertEquals(
            "https://www.facebook.com/dialog/share?quote=just%20text",
            FacebookIntent.SharePost(text = "just text").url,
        )
    }

    @Test
    fun `emoji survive the round trip into a tweet`() {
        val url = TwitterIntent.PostTweet(text = "ship it 🚀").url
        assertEquals("https://twitter.com/intent/tweet?text=ship%20it%20%F0%9F%9A%80", url)
    }

    @Test
    fun `extractTweetId pulls the id out of an x dot com status url`() {
        assertEquals("123", "https://x.com/someone/status/123".extractTweetId())
    }

    @Test
    fun `extractTweetId passes a bare id straight through`() {
        assertEquals("123", "123".extractTweetId())
    }

    @Test
    fun `extractTweetId returns the input unchanged when it is not a status url`() {
        assertEquals("https://example.com/a", "https://example.com/a".extractTweetId())
    }
}
