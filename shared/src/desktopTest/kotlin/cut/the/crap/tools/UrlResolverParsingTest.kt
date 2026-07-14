package cut.the.crap.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * WP5b: pins the GraphQL response parsing that moved from `org.json` to kotlinx-serialization.
 *
 * This is the part that fails *soft* — a wrong path returns the original URL, which compiles and
 * "works", so the only guard is asserting the extraction against realistic response shapes. Both
 * shapes the old code handled are covered, plus the regex fallback it dropped to when navigation
 * missed.
 */
class UrlResolverParsingTest {

    @Test
    fun `extracts screen_name from the direct tweetResult shape`() {
        val json = """
            {"data":{"tweetResult":{"result":{"core":{"user_results":{"result":{
              "legacy":{"screen_name":"jack"}
            }}}}}}}
        """.trimIndent()

        assertEquals("jack", UrlResolver.extractScreenName(json))
    }

    @Test
    fun `extracts screen_name from the threaded timeline shape`() {
        // The TweetDetail query's actual response: instructions -> entries -> ... -> legacy.
        val json = """
            {"data":{"threaded_conversation_with_injections_v2":{"instructions":[
              {"entries":[
                {"content":{"itemContent":{"tweet_results":{"result":{
                  "core":{"user_results":{"result":{"legacy":{"screen_name":"nasa"}}}}
                }}}}}
              ]}
            ]}}}
        """.trimIndent()

        assertEquals("nasa", UrlResolver.extractScreenName(json))
    }

    @Test
    fun `falls back to regex when the structure is unfamiliar`() {
        // Navigation finds nothing, but the field is present somewhere in the text.
        val json = """{"data":{"something_new":{"nested":{"screen_name":"fallback_user"}}}}"""

        assertEquals("fallback_user", UrlResolver.extractScreenName(json))
    }

    @Test
    fun `returns null when there is no screen_name anywhere`() {
        assertNull(UrlResolver.extractScreenName("""{"data":{"tweetResult":{"result":{}}}}"""))
    }

    @Test
    fun `returns null rather than throwing on malformed json`() {
        assertNull(UrlResolver.extractScreenName("not json at all {"))
    }

    @Test
    fun `handles an empty object`() {
        assertNull(UrlResolver.extractScreenName("{}"))
    }

    @Test
    fun `tweetIdOf accepts a bare status url and rejects non-numeric`() {
        assertEquals("12345", UrlResolver.tweetIdOf("https://x.com/i/status/12345"))
        assertEquals("12345", UrlResolver.tweetIdOf("https://x.com/i/status/12345?s=20"))
        assertNull(UrlResolver.tweetIdOf("https://x.com/i/status/not-a-number"))
    }

    @Test
    fun `usernameFromStatusUrl extracts the author and rejects the i placeholder`() {
        assertEquals("realuser", UrlResolver.usernameFromStatusUrl("https://x.com/realuser/status/9"))
        assertNull(UrlResolver.usernameFromStatusUrl("https://x.com/i/status/9"))
    }

    @Test
    fun `isXRedirectUrl recognises only the i-status form`() {
        assert(UrlResolver.isXRedirectUrl("https://x.com/i/status/12345"))
        assert(UrlResolver.isXRedirectUrl("https://twitter.com/i/status/12345"))
        assert(!UrlResolver.isXRedirectUrl("https://x.com/nasa/status/12345"))
        assert(!UrlResolver.isXRedirectUrl("https://example.com/whatever"))
    }
}
