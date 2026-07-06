package cut.the.crap.share

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for [xProfileHandle], the pure profile-only rule behind [XSharedLinkHandler]'s
 * "save the @handle instead of the link" behaviour. Only X/Twitter *profile* URLs yield a
 * handle; post links, the /i/ share form, and non-X URLs must not.
 */
class XSharedLinkHandlerTest {

    @Test
    fun `x profile url yields normalised handle`() {
        assertThat(xProfileHandle("https://x.com/jack")).isEqualTo("@jack")
    }

    @Test
    fun `twitter profile url yields normalised handle`() {
        assertThat(xProfileHandle("https://twitter.com/jack")).isEqualTo("@jack")
    }

    @Test
    fun `x post url is saved as a link not a handle`() {
        assertThat(xProfileHandle("https://x.com/jack/status/123")).isNull()
    }

    @Test
    fun `i-status share form yields no handle`() {
        assertThat(xProfileHandle("https://x.com/i/status/123")).isNull()
    }

    @Test
    fun `non-x url yields no handle`() {
        assertThat(xProfileHandle("https://www.youtube.com/watch?v=abc")).isNull()
    }
}
