package cut.the.crap.data.rest.bluesky

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for [BlueskyPostMetadata.fromThread]: field mapping, the displayName→handle fallback, and
 * the thumbnail preference order (embed image → author avatar).
 */
class BlueskyPostMetadataMapperTest {

    private fun response(
        handle: String = "alice.bsky.social",
        displayName: String? = "Alice",
        avatar: String? = "https://cdn.bsky.app/avatar.jpg",
        text: String = "hello world",
        embed: BlueskyEmbed? = null
    ) = BlueskyThreadResponse(
        thread = BlueskyThreadView(
            post = BlueskyPostView(
                author = BlueskyAuthor(handle = handle, displayName = displayName, avatar = avatar),
                record = BlueskyRecord(text = text),
                embed = embed
            )
        )
    )

    @Test
    fun `maps author text and prefers embed image thumbnail`() {
        val embed = BlueskyEmbed(images = listOf(BlueskyImage(thumb = "https://cdn.bsky.app/img.jpg")))

        val metadata = BlueskyPostMetadata.fromThread(response(embed = embed))

        assertThat(metadata).isNotNull()
        assertThat(metadata!!.authorName).isEqualTo("Alice")
        assertThat(metadata.authorHandle).isEqualTo("alice.bsky.social")
        assertThat(metadata.text).isEqualTo("hello world")
        assertThat(metadata.thumbnailUrl).isEqualTo("https://cdn.bsky.app/img.jpg")
    }

    @Test
    fun `falls back to author avatar when the post has no image`() {
        val metadata = BlueskyPostMetadata.fromThread(response(embed = null))

        assertThat(metadata!!.thumbnailUrl).isEqualTo("https://cdn.bsky.app/avatar.jpg")
    }

    @Test
    fun `falls back to handle when display name is blank`() {
        val metadata = BlueskyPostMetadata.fromThread(response(displayName = ""))

        assertThat(metadata!!.authorName).isEqualTo("alice.bsky.social")
    }

    @Test
    fun `recordWithMedia embed image is used for the thumbnail`() {
        val embed = BlueskyEmbed(
            media = BlueskyEmbedMedia(images = listOf(BlueskyImage(thumb = "https://cdn.bsky.app/media.jpg")))
        )

        val metadata = BlueskyPostMetadata.fromThread(response(avatar = null, embed = embed))

        assertThat(metadata!!.thumbnailUrl).isEqualTo("https://cdn.bsky.app/media.jpg")
    }

    @Test
    fun `returns null when the response carries no post`() {
        assertThat(BlueskyPostMetadata.fromThread(BlueskyThreadResponse(thread = null))).isNull()
    }
}
