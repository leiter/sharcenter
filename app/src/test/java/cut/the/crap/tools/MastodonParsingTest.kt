package cut.the.crap.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for Mastodon URL recognition in [SocialMediaParser]: structural post/profile parsing across
 * arbitrary instances, the non-Mastodon `@`-path denylist, origin-host status API construction, and
 * profile-URL reconstruction.
 */
class MastodonParsingTest {

    @Test
    fun `post url parses host user and id`() {
        val info = parseMastodonUrl("https://mstdn.nrkn.fr/@vl/116874614315059898")

        assertThat(info).isNotNull()
        assertThat(info!!.platform).isEqualTo("mastodon")
        assertThat(info.contentType).isEqualTo("post")
        assertThat(info.username).isEqualTo("vl")
        assertThat(info.identifier).isEqualTo("116874614315059898")
        assertThat(info.additionalInfo["host"]).isEqualTo("https://mstdn.nrkn.fr")
    }

    @Test
    fun `profile url parses host and user with no id`() {
        val info = parseMastodonUrl("https://mastodon.social/@Gargron")

        assertThat(info).isNotNull()
        assertThat(info!!.contentType).isEqualTo("profile")
        assertThat(info.username).isEqualTo("Gargron")
        assertThat(info.identifier).isNull()
        assertThat(info.additionalInfo["host"]).isEqualTo("https://mastodon.social")
    }

    @Test
    fun `federated profile url keeps the remote handle`() {
        val info = parseMastodonUrl("https://mastodon.social/@vl@mstdn.nrkn.fr")

        assertThat(info!!.contentType).isEqualTo("profile")
        assertThat(info.username).isEqualTo("vl@mstdn.nrkn.fr")
    }

    @Test
    fun `known non-mastodon at-path hosts are not treated as mastodon`() {
        // YouTube channels and Medium profiles use the same /@user form.
        assertThat(parseMastodonUrl("https://www.youtube.com/@MrBeast")).isNull()
        assertThat(parseMastodonUrl("https://medium.com/@kev")).isNull()
        assertThat(parseMastodonUrl("https://x.com/jack")).isNull()
        assertThat(parseMastodonUrl("https://bsky.app/profile/alice.bsky.social")).isNull()
    }

    @Test
    fun `platforms without an at-user path are not matched`() {
        // TikTok (/@user/video/id) and Threads (/@user/post/id) don't put a numeric id right
        // after the user, so their posts never look like Mastodon posts.
        assertThat(isMastodonUrl("https://www.tiktok.com/@user/video/12345")).isFalse()
        assertThat(isMastodonUrl("https://www.threads.net/@user/post/ABC123")).isFalse()
    }

    @Test
    fun `status api url targets the origin instance`() {
        val api = mastodonStatusApiUrl("https://mstdn.nrkn.fr/@vl/116874614315059898")

        assertThat(api).isEqualTo("https://mstdn.nrkn.fr/api/v1/statuses/116874614315059898")
    }

    @Test
    fun `profile url has no status api url`() {
        assertThat(mastodonStatusApiUrl("https://mastodon.social/@Gargron")).isNull()
    }

    @Test
    fun `profile url is reconstructed from parsed info`() {
        val info = parseMastodonUrl("https://mstdn.nrkn.fr/@vl/116874614315059898")

        assertThat(info!!.profileUrl()).isEqualTo("https://mstdn.nrkn.fr/@vl")
    }

    @Test
    fun `isMastodonUrl recognises federated instances only`() {
        assertThat(isMastodonUrl("https://fosstodon.org/@kev/109332057798209446")).isTrue()
        assertThat(isMastodonUrl("https://www.youtube.com/@MrBeast")).isFalse()
    }

    @Test
    fun `parseSocialMediaUrl dispatches mastodon links`() {
        val info = parseSocialMediaUrl("https://hachyderm.io/@user/110000000000000000")

        assertThat(info?.platform).isEqualTo("mastodon")
        assertThat(info?.contentType).isEqualTo("post")
    }
}
