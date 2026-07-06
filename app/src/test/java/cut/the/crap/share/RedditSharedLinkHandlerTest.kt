package cut.the.crap.share

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.reddit.RedditPostMetadata
import cut.the.crap.fake.FakeRedditRepository
import cut.the.crap.tools.LinkMetadata
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RedditSharedLinkHandlerTest {

    private val repository = FakeRedditRepository()
    private val handler = RedditSharedLinkHandler(repository)

    @Test
    fun `recognizes reddit urls and rejects others`() {
        assertThat(handler.recognizes("https://www.reddit.com/r/pics/comments/1/x/")).isTrue()
        assertThat(handler.recognizes("https://redd.it/abc")).isTrue()
        assertThat(handler.recognizes("https://x.com/jack/status/1")).isFalse()
    }

    // --- resolve (short links only) ---

    @Test
    fun `canonical url resolves to itself without network`() = runTest {
        val url = "https://www.reddit.com/r/pics/comments/1uosb5v/x/"

        val resolution = handler.resolve(url)

        assertThat(resolution).isEqualTo(UrlResolution.Resolved(url, changed = false))
    }

    // --- handleToSaveInstead (user/subreddit -> handle) ---

    @Test
    fun `user profile is saved as a u-slash handle`() {
        assertThat(handler.handleToSaveInstead("https://www.reddit.com/user/spez")).isEqualTo("u/spez")
    }

    @Test
    fun `subreddit is saved as an r-slash handle`() {
        assertThat(handler.handleToSaveInstead("https://www.reddit.com/r/androiddev")).isEqualTo("r/androiddev")
    }

    @Test
    fun `post url is saved as a link not a handle`() {
        assertThat(handler.handleToSaveInstead("https://www.reddit.com/r/pics/comments/1/x/")).isNull()
    }

    // --- enrich ---

    @Test
    fun `enrich applies fetched post metadata to the saved link`() = runTest {
        repository.setSuccessResponse(
            RedditPostMetadata(authorName = "u/lonaangreen", text = "My cool post", thumbnailUrl = null)
        )
        val saved = ContentLink(id = 11, link = "https://www.reddit.com/r/pics/comments/1uosb5v/x/")

        val enriched = handler.enrich(saved)

        assertThat(enriched).isNotNull()
        assertThat(enriched!!.id).isEqualTo(saved.id)
        assertThat(LinkMetadata.getRedditAuthor(enriched)).isEqualTo("u/lonaangreen")
        assertThat(LinkMetadata.getRedditText(enriched)).isEqualTo("My cool post")
        assertThat(repository.getFetchedUrls()).contains(saved.link)
    }

    @Test
    fun `enrich returns null when metadata fetch fails`() = runTest {
        repository.setErrorResponse("boom")

        val enriched = handler.enrich(
            ContentLink(link = "https://www.reddit.com/r/pics/comments/1/x/")
        )

        assertThat(enriched).isNull()
    }

    @Test
    fun `enrich ignores non-reddit links without hitting the network`() = runTest {
        val enriched = handler.enrich(ContentLink(link = "https://x.com/jack/status/1"))

        assertThat(enriched).isNull()
        assertThat(repository.getFetchedUrls()).isEmpty()
    }
}
