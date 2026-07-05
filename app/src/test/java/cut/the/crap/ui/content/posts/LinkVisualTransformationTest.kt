package cut.the.crap.ui.content.posts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for URL detection behind the editor's link highlighting / tap-to-open: only valid
 * http(s) URLs are recognized, and trailing punctuation is excluded from the link.
 */
class LinkVisualTransformationTest {

    @Test
    fun `valid url is detected`() {
        val text = "see https://example.com/path here"
        val spans = findUrlSpans(text)

        assertThat(spans).hasSize(1)
        assertThat(spans.first().url).isEqualTo("https://example.com/path")
        assertThat(text.substring(spans.first().range)).isEqualTo("https://example.com/path")
    }

    @Test
    fun `trailing punctuation is not part of the link`() {
        val spans = findUrlSpans("check (https://example.com).")

        assertThat(spans).hasSize(1)
        assertThat(spans.first().url).isEqualTo("https://example.com")
    }

    @Test
    fun `scheme without a host is rejected`() {
        assertThat(findUrlSpans("https:// nope")).isEmpty()
    }

    @Test
    fun `host without a dot is rejected`() {
        assertThat(findUrlSpans("visit https://localhost now")).isEmpty()
    }

    @Test
    fun `non-http scheme is not matched`() {
        assertThat(findUrlSpans("ftp://example.com/file")).isEmpty()
    }

    @Test
    fun `findUrlAt returns the url only when the offset is on it`() {
        val text = "a https://example.com b"
        val urlStart = text.indexOf("https")

        assertThat(findUrlAt(text, urlStart + 2)).isEqualTo("https://example.com")
        assertThat(findUrlAt(text, 0)).isNull()
    }

    private fun String.substring(range: IntRange): String = substring(range.first, range.last + 1)
}
