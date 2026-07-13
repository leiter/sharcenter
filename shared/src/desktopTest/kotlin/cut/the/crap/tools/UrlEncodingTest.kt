package cut.the.crap.tools

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins [urlEncode] to `android.net.Uri.encode`'s exact behaviour.
 *
 * These expectations are not arbitrary: they are what `Uri.encode` produced before the intent
 * builders moved off Android. The encoder cannot be checked against the real thing in a JVM test
 * (`android.net.Uri` is a stub off-device), so its contract is pinned here instead — otherwise a
 * divergence would surface only as a mangled tweet, which compiles and passes silently.
 */
class UrlEncodingTest {

    @Test
    fun `leaves the unreserved set untouched`() {
        // Android's safe set: alphanumerics plus _-!.~'()*  — note the apostrophe, which a
        // stricter RFC 3986 encoder would escape, changing every tweet containing "don't".
        val safe = "abcXYZ0189_-!.~'()*"
        assertEquals(safe, safe.urlEncode())
    }

    @Test
    fun `escapes the characters that would otherwise break the query string`() {
        // These are the ones that actually matter: unescaped, they would terminate the value
        // or invent a new query parameter.
        assertEquals("%26", "&".urlEncode())
        assertEquals("%3D", "=".urlEncode())
        assertEquals("%3F", "?".urlEncode())
        assertEquals("%23", "#".urlEncode())
        assertEquals("%2F", "/".urlEncode())
        assertEquals("%3A", ":".urlEncode())
    }

    @Test
    fun `encodes space as a percent triplet rather than plus`() {
        assertEquals("hello%20world", "hello world".urlEncode())
    }

    @Test
    fun `escapes a literal plus, so it cannot be read back as a space`() {
        assertEquals("a%2Bb", "a+b".urlEncode())
    }

    @Test
    fun `encodes non-ascii as utf-8 byte triplets`() {
        assertEquals("caf%C3%A9", "café".urlEncode())
        assertEquals("%E2%82%AC", "€".urlEncode())
    }

    @Test
    fun `encodes characters outside the basic multilingual plane`() {
        // A surrogate pair must come out as four bytes, not two mangled ones — emoji in a tweet
        // are the common case, not an edge case.
        assertEquals("%F0%9F%9A%80", "🚀".urlEncode())
    }

    @Test
    fun `encodes newlines`() {
        assertEquals("a%0Ab", "a\nb".urlEncode())
    }

    @Test
    fun `uses uppercase hex`() {
        assertEquals("%7B", "{".urlEncode())
    }

    @Test
    fun `handles the empty string`() {
        assertEquals("", "".urlEncode())
    }
}
