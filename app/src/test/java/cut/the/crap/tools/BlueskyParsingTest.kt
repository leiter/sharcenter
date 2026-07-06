package cut.the.crap.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for Bluesky URL recognition in [SocialMediaParser]: post/profile parsing, at:// URI
 * construction for the public API, and profile-URL reconstruction.
 */
class BlueskyParsingTest {

    @Test
    fun `post url parses actor and rkey`() {
        val info = parseBlueskyUrl("https://bsky.app/profile/alice.bsky.social/post/3k2abcd")

        assertThat(info).isNotNull()
        assertThat(info!!.platform).isEqualTo("bluesky")
        assertThat(info.contentType).isEqualTo("post")
        assertThat(info.username).isEqualTo("alice.bsky.social")
        assertThat(info.identifier).isEqualTo("3k2abcd")
    }

    @Test
    fun `profile url parses actor and no rkey`() {
        val info = parseBlueskyUrl("https://bsky.app/profile/alice.bsky.social")

        assertThat(info).isNotNull()
        assertThat(info!!.contentType).isEqualTo("profile")
        assertThat(info.username).isEqualTo("alice.bsky.social")
        assertThat(info.identifier).isNull()
    }

    @Test
    fun `did based profile url is recognised`() {
        val info = parseBlueskyUrl("https://bsky.app/profile/did:plc:abc123")

        assertThat(info).isNotNull()
        assertThat(info!!.contentType).isEqualTo("profile")
        assertThat(info.username).isEqualTo("did:plc:abc123")
    }

    @Test
    fun `non-bluesky url is not parsed`() {
        assertThat(parseBlueskyUrl("https://x.com/jack/status/1")).isNull()
    }

    @Test
    fun `post at-uri is built from actor and rkey`() {
        val atUri = blueskyPostAtUri("https://bsky.app/profile/alice.bsky.social/post/3k2abcd")

        assertThat(atUri).isEqualTo("at://alice.bsky.social/app.bsky.feed.post/3k2abcd")
    }

    @Test
    fun `profile url has no post at-uri`() {
        assertThat(blueskyPostAtUri("https://bsky.app/profile/alice.bsky.social")).isNull()
    }

    @Test
    fun `profile url is reconstructed from parsed info`() {
        val info = parseBlueskyUrl("https://bsky.app/profile/alice.bsky.social/post/3k2abcd")

        assertThat(info!!.profileUrl()).isEqualTo("https://bsky.app/profile/alice.bsky.social")
    }

    @Test
    fun `isBlueskyUrl recognises bsky app links only`() {
        assertThat(isBlueskyUrl("https://bsky.app/profile/alice.bsky.social")).isTrue()
        assertThat(isBlueskyUrl("https://x.com/jack")).isFalse()
    }
}
