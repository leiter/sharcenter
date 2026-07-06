package cut.the.crap.share

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.bluesky.BlueskyPostMetadata
import cut.the.crap.fake.FakeBlueskyRepository
import cut.the.crap.tools.LinkMetadata
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BlueskySharedLinkHandlerTest {

    private val repository = FakeBlueskyRepository()
    private val handler = BlueskySharedLinkHandler(repository)

    @Test
    fun `recognizes bluesky urls and rejects others`() {
        assertThat(handler.recognizes("https://bsky.app/profile/alice.bsky.social/post/1")).isTrue()
        assertThat(handler.recognizes("https://x.com/jack/status/1")).isFalse()
    }

    // --- handleToSaveInstead (profile -> handle) ---

    @Test
    fun `profile url with handle is saved as a handle`() {
        assertThat(handler.handleToSaveInstead("https://bsky.app/profile/alice.bsky.social"))
            .isEqualTo("@alice.bsky.social")
    }

    @Test
    fun `did based profile url is saved as a link not a handle`() {
        assertThat(handler.handleToSaveInstead("https://bsky.app/profile/did:plc:abc123")).isNull()
    }

    @Test
    fun `post url is saved as a link not a handle`() {
        assertThat(handler.handleToSaveInstead("https://bsky.app/profile/alice.bsky.social/post/1"))
            .isNull()
    }

    // --- enrich ---

    @Test
    fun `enrich applies fetched post metadata to the saved link`() = runTest {
        repository.setSuccessResponse(
            BlueskyPostMetadata(
                authorName = "Alice",
                authorHandle = "alice.bsky.social",
                text = "gm from bluesky",
                thumbnailUrl = "https://cdn.bsky.app/img.jpg"
            )
        )
        val saved = ContentLink(id = 3, link = "https://bsky.app/profile/alice.bsky.social/post/abc")

        val enriched = handler.enrich(saved)

        assertThat(enriched).isNotNull()
        assertThat(enriched!!.id).isEqualTo(saved.id)
        assertThat(LinkMetadata.getBlueskyAuthor(enriched)).isEqualTo("Alice")
        assertThat(LinkMetadata.getBlueskyText(enriched)).isEqualTo("gm from bluesky")
        assertThat(LinkMetadata.getBlueskyThumbnailUrl(enriched)).isEqualTo("https://cdn.bsky.app/img.jpg")
        assertThat(repository.getFetchedUrls()).contains(saved.link)
    }

    @Test
    fun `enrich returns null when metadata fetch fails`() = runTest {
        repository.setErrorResponse("boom")

        val enriched = handler.enrich(
            ContentLink(link = "https://bsky.app/profile/alice.bsky.social/post/abc")
        )

        assertThat(enriched).isNull()
    }

    @Test
    fun `enrich ignores non-bluesky links without hitting the network`() = runTest {
        val enriched = handler.enrich(ContentLink(link = "https://x.com/jack/status/1"))

        assertThat(enriched).isNull()
        assertThat(repository.getFetchedUrls()).isEmpty()
    }
}
