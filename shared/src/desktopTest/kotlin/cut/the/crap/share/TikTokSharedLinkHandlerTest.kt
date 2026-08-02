package cut.the.crap.share

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.tiktok.TikTokPostMetadata
import cut.the.crap.fake.FakeTikTokRepository
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.UrlResolver
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TikTokSharedLinkHandlerTest {

    private val repository = FakeTikTokRepository()
    private val handler = TikTokSharedLinkHandler(repository, mockk<UrlResolver>())

    @Test
    fun `recognizes tiktok urls and rejects others`() {
        assertThat(handler.recognizes("https://www.tiktok.com/@scout2015/video/1")).isTrue()
        assertThat(handler.recognizes("https://vm.tiktok.com/ZMabc/")).isTrue()
        assertThat(handler.recognizes("https://x.com/jack/status/1")).isFalse()
    }

    // --- resolve (short links only) ---

    @Test
    fun `canonical url resolves to itself without network`() = runTest {
        val url = "https://www.tiktok.com/@scout2015/video/6718335390845095173"

        val resolution = handler.resolve(url)

        assertThat(resolution).isEqualTo(UrlResolution.Resolved(url, changed = false))
    }

    // --- handleToSaveInstead (profile -> handle) ---

    @Test
    fun `profile url is saved as a handle`() {
        assertThat(handler.handleToSaveInstead("https://www.tiktok.com/@scout2015"))
            .isEqualTo("@scout2015")
    }

    @Test
    fun `video url is saved as a link not a handle`() {
        assertThat(handler.handleToSaveInstead("https://www.tiktok.com/@scout2015/video/1")).isNull()
    }

    // --- enrich ---

    @Test
    fun `enrich applies fetched post metadata to the saved link`() = runTest {
        repository.setSuccessResponse(
            TikTokPostMetadata(
                authorName = "Scout",
                authorHandle = "scout2015",
                text = "gm from tiktok",
                thumbnailUrl = "https://cdn.tiktok.com/cover.jpg"
            )
        )
        val saved = ContentLink(id = 9, link = "https://www.tiktok.com/@scout2015/video/6718335390845095173")

        val enriched = handler.enrich(saved)

        assertThat(enriched).isNotNull()
        assertThat(enriched!!.id).isEqualTo(saved.id)
        assertThat(LinkMetadata.getTikTokAuthor(enriched)).isEqualTo("Scout")
        assertThat(LinkMetadata.getTikTokText(enriched)).isEqualTo("gm from tiktok")
        assertThat(LinkMetadata.getTikTokThumbnailUrl(enriched)).isEqualTo("https://cdn.tiktok.com/cover.jpg")
        assertThat(repository.getFetchedUrls()).contains(saved.link)
    }

    @Test
    fun `enrich returns null when metadata fetch fails`() = runTest {
        repository.setErrorResponse("boom")

        val enriched = handler.enrich(
            ContentLink(link = "https://www.tiktok.com/@scout2015/video/1")
        )

        assertThat(enriched).isNull()
    }

    @Test
    fun `enrich ignores non-tiktok links without hitting the network`() = runTest {
        val enriched = handler.enrich(ContentLink(link = "https://x.com/jack/status/1"))

        assertThat(enriched).isNull()
        assertThat(repository.getFetchedUrls()).isEmpty()
    }
}
