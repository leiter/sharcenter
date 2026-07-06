package cut.the.crap.data.rest.mastodon

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for [MastodonPostMetadata.fromStatus]: field mapping, the displayName→handle fallback, the
 * media→avatar thumbnail preference, and the [htmlToPlainText] content conversion.
 */
class MastodonPostMetadataMapperTest {

    private fun status(
        displayName: String? = "Alice",
        acct: String? = "alice@mastodon.social",
        username: String? = "alice",
        avatar: String? = "https://cdn.example/avatar.jpg",
        content: String = "<p>hello world</p>",
        media: List<MastodonMediaAttachment>? = null
    ) = MastodonStatus(
        content = content,
        account = MastodonAccount(
            username = username,
            acct = acct,
            displayName = displayName,
            avatar = avatar
        ),
        mediaAttachments = media
    )

    @Test
    fun `maps author text and prefers media preview thumbnail`() {
        val media = listOf(MastodonMediaAttachment(type = "image", previewUrl = "https://cdn.example/img.jpg"))

        val metadata = MastodonPostMetadata.fromStatus(status(media = media))

        assertThat(metadata).isNotNull()
        assertThat(metadata!!.authorName).isEqualTo("Alice")
        assertThat(metadata.authorHandle).isEqualTo("alice@mastodon.social")
        assertThat(metadata.text).isEqualTo("hello world")
        assertThat(metadata.thumbnailUrl).isEqualTo("https://cdn.example/img.jpg")
    }

    @Test
    fun `falls back to author avatar when the post has no visual media`() {
        val audioOnly = listOf(MastodonMediaAttachment(type = "audio", url = "https://cdn.example/a.mp3"))

        val metadata = MastodonPostMetadata.fromStatus(status(media = audioOnly))

        assertThat(metadata!!.thumbnailUrl).isEqualTo("https://cdn.example/avatar.jpg")
    }

    @Test
    fun `falls back to handle when display name is blank`() {
        val metadata = MastodonPostMetadata.fromStatus(status(displayName = ""))

        assertThat(metadata!!.authorName).isEqualTo("alice@mastodon.social")
    }

    @Test
    fun `returns null when the status carries no account`() {
        assertThat(MastodonPostMetadata.fromStatus(MastodonStatus(account = null))).isNull()
    }

    @Test
    fun `htmlToPlainText strips tags and decodes entities`() {
        val html = "<p>first &amp; second</p><p>third &lt;tag&gt; line<br />next</p>"

        assertThat(htmlToPlainText(html)).isEqualTo("first & second\n\nthird <tag> line\nnext")
    }

    @Test
    fun `htmlToPlainText decodes numeric entities`() {
        assertThat(htmlToPlainText("<p>caf&#233;</p>")).isEqualTo("café")
    }
}
