package cut.the.crap.data.rest.reddit

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for [RedditPostMetadata.fromOEmbed]: title→text mapping, the `u/` author normalisation, and
 * the null result when the payload carries no title.
 */
class RedditPostMetadataMapperTest {

    @Test
    fun `maps title and prefixes the author with u slash`() {
        val metadata = RedditPostMetadata.fromOEmbed(
            RedditOEmbedResponse(title = "My cool post", authorName = "lonaangreen")
        )

        assertThat(metadata).isNotNull()
        assertThat(metadata!!.text).isEqualTo("My cool post")
        assertThat(metadata.authorName).isEqualTo("u/lonaangreen")
        assertThat(metadata.thumbnailUrl).isNull()
    }

    @Test
    fun `does not double prefix an author that already has u slash`() {
        val metadata = RedditPostMetadata.fromOEmbed(
            RedditOEmbedResponse(title = "t", authorName = "u/spez")
        )

        assertThat(metadata!!.authorName).isEqualTo("u/spez")
    }

    @Test
    fun `keeps a thumbnail when one is present`() {
        val metadata = RedditPostMetadata.fromOEmbed(
            RedditOEmbedResponse(title = "t", authorName = "spez", thumbnailUrl = "https://i.redd.it/x.jpg")
        )

        assertThat(metadata!!.thumbnailUrl).isEqualTo("https://i.redd.it/x.jpg")
    }

    @Test
    fun `returns null when the payload has no title`() {
        assertThat(RedditPostMetadata.fromOEmbed(RedditOEmbedResponse(title = null, authorName = "spez")))
            .isNull()
    }
}
