package cut.the.crap.data.rest.tiktok

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for [TikTokPostMetadata.fromOEmbed]: field mapping, the handle source (author_unique_id),
 * the displayName→handle fallback, and the null result when the payload carries no author.
 */
class TikTokPostMetadataMapperTest {

    private fun response(
        type: String? = "video",
        title: String? = "a great clip #fyp",
        authorName: String? = "Scout & Suki",
        authorUniqueId: String? = "scout2015",
        thumbnailUrl: String? = "https://cdn.tiktok.com/cover.jpg"
    ) = TikTokOEmbedResponse(
        type = type,
        title = title,
        authorName = authorName,
        authorUniqueId = authorUniqueId,
        thumbnailUrl = thumbnailUrl
    )

    @Test
    fun `maps author caption handle and thumbnail`() {
        val metadata = TikTokPostMetadata.fromOEmbed(response())

        assertThat(metadata).isNotNull()
        assertThat(metadata!!.authorName).isEqualTo("Scout & Suki")
        assertThat(metadata.authorHandle).isEqualTo("scout2015")
        assertThat(metadata.text).isEqualTo("a great clip #fyp")
        assertThat(metadata.thumbnailUrl).isEqualTo("https://cdn.tiktok.com/cover.jpg")
    }

    @Test
    fun `falls back to handle when author name is blank`() {
        val metadata = TikTokPostMetadata.fromOEmbed(response(authorName = ""))

        assertThat(metadata!!.authorName).isEqualTo("scout2015")
    }

    @Test
    fun `blank thumbnail becomes null`() {
        val metadata = TikTokPostMetadata.fromOEmbed(response(thumbnailUrl = ""))

        assertThat(metadata!!.thumbnailUrl).isNull()
    }

    @Test
    fun `returns null when the payload has no author`() {
        val metadata = TikTokPostMetadata.fromOEmbed(
            TikTokOEmbedResponse(type = null, title = null, authorName = null, authorUniqueId = null)
        )

        assertThat(metadata).isNull()
    }
}
