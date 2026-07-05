package cut.the.crap.ui.content.posts

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.tools.DescriptionParser
import org.junit.Test

/**
 * Tests for [toComposedPostText], the pure builder behind the Links → Posts
 * "Compose post from this link" bridge. Covers marker prefix reconstruction and
 * the empty / legacy description fallbacks.
 */
class ComposePostFromLinkTest {

    @Test
    fun `link with no description yields just the url`() {
        val link = ContentLink(id = 1, link = "https://x.com/user/status/1")

        assertThat(link.toComposedPostText()).isEqualTo("\nhttps://x.com/user/status/1")
    }

    @Test
    fun `handles hashtags and keywords are re-prefixed and space joined after the url`() {
        val description = DescriptionParser.serialize(
            metadata = emptyList(),
            handles = listOf("alice", "bob"),
            hashtags = listOf("news"),
            keywords = listOf("launch")
        )
        val link = ContentLink(id = 1, link = "https://x.com/post/1", description = description)

        assertThat(link.toComposedPostText())
            .isEqualTo("\nhttps://x.com/post/1\n\n@alice @bob #news launch")
    }

    @Test
    fun `youtube metadata in description is not included as markers`() {
        // Metadata (channel, title, thumbnail) must not leak into the composed post — only
        // the user's saved handles/hashtags/keywords do.
        val description = DescriptionParser.serialize(
            metadata = listOf("Some Channel", "A Video", "https://i.ytimg.com/vi/abc/hq.jpg"),
            handles = emptyList(),
            hashtags = listOf("music"),
            keywords = emptyList()
        )
        val link = ContentLink(id = 1, link = "https://youtube.com/watch?v=abc", description = description)

        assertThat(link.toComposedPostText())
            .isEqualTo("\nhttps://youtube.com/watch?v=abc\n\n#music")
    }

    @Test
    fun `legacy comma separated description is treated as keywords`() {
        val link = ContentLink(id = 1, link = "https://x.com/post/1", description = "one, two")

        assertThat(link.toComposedPostText()).isEqualTo("\nhttps://x.com/post/1\n\none two")
    }
}
