package cut.the.crap.share

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.domain.ContentLink
import org.junit.Test

/**
 * Verifies the handler-selection contract used by the share receiver:
 * `handlers.first { it.recognizes(url) }` over an ordered chain that ends in a catch-all.
 * A platform handler wins over the generic fallback, and unrecognized URLs fall through to it.
 */
class SharedLinkHandlerSelectionTest {

    /** Minimal handler that claims URLs containing [needle]. */
    private class NeedleHandler(private val needle: String) : SharedLinkHandler {
        override fun recognizes(url: String): Boolean = url.contains(needle)
    }

    private val x = NeedleHandler("x.com")
    private val youTube = NeedleHandler("youtube.com")
    private val generic = GenericSharedLinkHandler()

    // Same order ShareModule wires: platform handlers first, generic catch-all last.
    private val handlers: List<SharedLinkHandler> = listOf(x, youTube, generic)

    private fun select(url: String) = handlers.first { it.recognizes(url) }

    @Test
    fun `platform handler is selected over the generic fallback`() {
        assertThat(select("https://x.com/jack/status/1")).isSameInstanceAs(x)
        assertThat(select("https://www.youtube.com/watch?v=abc")).isSameInstanceAs(youTube)
    }

    @Test
    fun `unrecognized url falls through to the generic handler`() {
        assertThat(select("https://example.com/article")).isSameInstanceAs(generic)
    }

    @Test
    fun `generic handler recognizes everything and stays inert`() = kotlinx.coroutines.test.runTest {
        val url = "https://example.com/article"
        assertThat(generic.recognizes(url)).isTrue()
        assertThat(generic.handleToSaveInstead(url)).isNull()
        assertThat(generic.enrich(ContentLink(link = url))).isNull()
        assertThat(generic.resolve(url)).isEqualTo(UrlResolution.Resolved(url, changed = false))
    }
}
