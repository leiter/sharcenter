package cut.the.crap.share

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.mastodon.MastodonPostMetadata
import cut.the.crap.fake.FakeMastodonRepository
import cut.the.crap.tools.LinkMetadata
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MastodonSharedLinkHandlerTest {

    private val repository = FakeMastodonRepository()
    private val handler = MastodonSharedLinkHandler(repository)

    @Test
    fun `recognizes mastodon urls and rejects others`() {
        assertThat(handler.recognizes("https://mastodon.social/@Gargron/109252175198888644")).isTrue()
        assertThat(handler.recognizes("https://www.youtube.com/@MrBeast")).isFalse()
    }

    // --- handleToSaveInstead (profile -> handle) ---

    @Test
    fun `local profile url is saved as a fully qualified handle`() {
        assertThat(handler.handleToSaveInstead("https://mastodon.social/@Gargron"))
            .isEqualTo("@Gargron@mastodon.social")
    }

    @Test
    fun `federated profile url keeps its remote handle`() {
        assertThat(handler.handleToSaveInstead("https://mastodon.social/@vl@mstdn.nrkn.fr"))
            .isEqualTo("@vl@mstdn.nrkn.fr")
    }

    @Test
    fun `post url is saved as a link not a handle`() {
        assertThat(handler.handleToSaveInstead("https://mastodon.social/@Gargron/109252175198888644"))
            .isNull()
    }

    // --- enrich ---

    @Test
    fun `enrich applies fetched post metadata to the saved link`() = runTest {
        repository.setSuccessResponse(
            MastodonPostMetadata(
                authorName = "Eugen",
                authorHandle = "Gargron@mastodon.social",
                text = "gm from mastodon",
                thumbnailUrl = "https://cdn.example/img.jpg"
            )
        )
        val saved = ContentLink(id = 7, link = "https://mastodon.social/@Gargron/109252175198888644")

        val enriched = handler.enrich(saved)

        assertThat(enriched).isNotNull()
        assertThat(enriched!!.id).isEqualTo(saved.id)
        assertThat(LinkMetadata.getMastodonAuthor(enriched)).isEqualTo("Eugen")
        assertThat(LinkMetadata.getMastodonText(enriched)).isEqualTo("gm from mastodon")
        assertThat(LinkMetadata.getMastodonThumbnailUrl(enriched)).isEqualTo("https://cdn.example/img.jpg")
        assertThat(repository.getFetchedUrls()).contains(saved.link)
    }

    @Test
    fun `enrich returns null when metadata fetch fails`() = runTest {
        repository.setErrorResponse("boom")

        val enriched = handler.enrich(
            ContentLink(link = "https://mastodon.social/@Gargron/109252175198888644")
        )

        assertThat(enriched).isNull()
    }

    @Test
    fun `enrich ignores non-mastodon links without hitting the network`() = runTest {
        val enriched = handler.enrich(ContentLink(link = "https://www.youtube.com/@MrBeast"))

        assertThat(enriched).isNull()
        assertThat(repository.getFetchedUrls()).isEmpty()
    }
}
