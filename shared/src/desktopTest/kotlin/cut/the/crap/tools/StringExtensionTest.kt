package cut.the.crap.tools

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins `prepareUrlInformation` to the exact output of the original `java.net.URL` implementation.
 *
 * These goldens were captured by running the *old* code, then asserted against the rewrite — not
 * written from what the new code happens to do. That matters: the three tokens feed keyword
 * derivation (a bsky.app link must keep yielding the "bsky" handle), so a "tidier" parser that
 * returned better answers would still be a regression.
 *
 * The quirks below are therefore deliberate, bugs and all:
 *  - `example.co.uk` yields "co", not "example"
 *  - `192.168.0.1` yields "0"
 *  - `localhost:8080` yields "n/a"
 *  - a trailing slash yields an empty third token
 *
 * Ktor's `Url` was the obvious substitute for `java.net.URL` and the wrong one: it is lenient
 * where `java.net.URL` throws, and the fallback branch exists precisely *because* it throws.
 */
class StringExtensionTest {

    private fun assertTokens(url: String, vararg expected: String) =
        assertEquals(expected.toList(), prepareUrlInformation(url), "for <$url>")

    @Test
    fun `social links yield domain, first path token and id`() {
        assertTokens(
            "https://bsky.app/profile/someone.bsky.social/post/3abcd",
            "bsky", "profile", "3abcd",
        )
        assertTokens("https://x.com/jack/status/1234567890", "x", "jack", "1234567890")
        assertTokens("https://mastodon.social/@user/109876543210", "mastodon", "@user", "109876543210")
        assertTokens(
            "https://www.tiktok.com/@user/video/7300000000000000000",
            "tiktok", "@user", "7300000000000000000",
        )
    }

    @Test
    fun `www prefix is dropped and the TLD trimmed`() {
        assertTokens("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s", "youtube", "watch", "watch")
        assertTokens("https://youtu.be/dQw4w9WgXcQ", "youtu", "dQw4w9WgXcQ", "dQw4w9WgXcQ")
        assertTokens("https://sub.domain.example.com/a/b/c/d/e/f/g", "example", "a", "g")
    }

    @Test
    fun `the query string and fragment are excluded from the path`() {
        assertTokens("https://example.com/path?a=1&b=2", "example", "path", "path")
        assertTokens("https://example.com/a/b?x=1#frag", "example", "a", "b")
    }

    @Test
    fun `malformed input falls back to naive slash splitting`() {
        assertTokens("not a url at all", "n/a", "n/a", "n/a")
        assertTokens("", "n/a", "n/a", "n/a")
        // No scheme, so java.net.URL rejected it — and so must we.
        assertTokens("www.example.com/no-scheme", "n/a", "n/a", "n/a")
    }

    @Test
    fun `known quirks are preserved, not fixed`() {
        // A multi-part TLD defeats the "drop the last label" heuristic.
        assertTokens("https://example.co.uk/path/to/thing?q=1", "co", "path", "thing")
        // An IP address is split on dots like a hostname.
        assertTokens("https://192.168.0.1/admin", "0", "admin", "admin")
        // A single-label host leaves nothing after dropping the last label.
        assertTokens("http://localhost:8080/api/v1/items", "n/a", "api", "items")
        // A trailing slash makes the final segment empty.
        assertTokens(
            "https://www.reddit.com/r/kotlin/comments/abc123/some_title/",
            "reddit", "r", "",
        )
    }

    @Test
    fun `a bare host yields empty path tokens`() {
        assertTokens("https://example.com", "example", "", "")
        assertTokens("https://example.com/", "example", "", "")
        assertTokens("https://example.com/single", "example", "single", "single")
    }

    @Test
    fun `non-http schemes still parse`() {
        assertTokens("ftp://files.example.org/pub/file.txt", "example", "pub", "file.txt")
    }

    @Test
    fun `ensureTrailingSpace appends only when needed`() {
        assertEquals("", "".ensureTrailingSpace())
        assertEquals("a ", "a".ensureTrailingSpace())
        assertEquals("a ", "a ".ensureTrailingSpace())
    }

    // ---- urlSchemeAndHost ------------------------------------------------------------------
    // Pins the replacement for the two UI callers that used java.net.URI / java.net.URL — both
    // *fully qualified*, so no import ever revealed them (the same blind spot as before).
    // Behaviour that matters: these callers treat "doesn't parse" as "not a link", so anything
    // java.net rejected must still yield null here. A lenient parser would silently make
    // LinkVisualTransformation highlight junk as a clickable link.

    @Test
    fun `scheme and host are extracted, userinfo and port stripped like URL#getHost`() {
        assertEquals(UrlSchemeHost("https", "example.com"), urlSchemeAndHost("https://example.com/a/b?q=1#f"))
        assertEquals(UrlSchemeHost("http", "example.com"), urlSchemeAndHost("http://example.com"))
        assertEquals(UrlSchemeHost("https", "example.com"), urlSchemeAndHost("https://user:pw@example.com:8443/x"))
        assertEquals(UrlSchemeHost("https", "localhost"), urlSchemeAndHost("https://localhost:8080/api"))
        assertEquals(UrlSchemeHost("ftp", "files.example.org"), urlSchemeAndHost("ftp://files.example.org/pub"))
    }

    @Test
    fun `input java-net rejected still yields null`() {
        assertEquals(null, urlSchemeAndHost("https://"))          // no host at all
        assertEquals(null, urlSchemeAndHost("not a url at all"))  // no scheme
        assertEquals(null, urlSchemeAndHost("www.example.com"))   // no scheme
        assertEquals(null, urlSchemeAndHost(""))
        // Illegal host characters: java.net.URI throws, so a match here would be a false positive.
        assertEquals(null, urlSchemeAndHost("https://ex ample.com"))
        assertEquals(null, urlSchemeAndHost("https://ex<ample>.com"))
    }

    @Test
    fun `isValidUrl semantics - http-s scheme plus a dotted host`() {
        // What LinkVisualTransformation.isValidUrl asks of it.
        fun valid(c: String): Boolean = urlSchemeAndHost(c)
            ?.let { (it.scheme == "http" || it.scheme == "https") && it.host.contains(".") } ?: false

        assertEquals(true, valid("https://example.com/x"))
        assertEquals(true, valid("http://a.b.co/x"))
        assertEquals(false, valid("https://localhost/x"))   // host without a dot
        assertEquals(false, valid("https://"))              // bare scheme
        assertEquals(false, valid("ftp://files.example.org")) // not http(s)
    }
}
