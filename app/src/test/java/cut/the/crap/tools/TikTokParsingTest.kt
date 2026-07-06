package cut.the.crap.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for TikTok URL recognition in [SocialMediaParser]: video/photo/profile parsing, short-link
 * detection, and profile-URL reconstruction.
 */
class TikTokParsingTest {

    @Test
    fun `video url parses user and id`() {
        val info = parseTikTokUrl("https://www.tiktok.com/@scout2015/video/6718335390845095173")

        assertThat(info).isNotNull()
        assertThat(info!!.platform).isEqualTo("tiktok")
        assertThat(info.contentType).isEqualTo("video")
        assertThat(info.username).isEqualTo("scout2015")
        assertThat(info.identifier).isEqualTo("6718335390845095173")
    }

    @Test
    fun `photo url parses as photo content`() {
        val info = parseTikTokUrl("https://www.tiktok.com/@someone/photo/7300000000000000000")

        assertThat(info!!.contentType).isEqualTo("photo")
        assertThat(info.username).isEqualTo("someone")
        assertThat(info.identifier).isEqualTo("7300000000000000000")
    }

    @Test
    fun `profile url parses user and no id`() {
        val info = parseTikTokUrl("https://www.tiktok.com/@scout2015")

        assertThat(info!!.contentType).isEqualTo("profile")
        assertThat(info.username).isEqualTo("scout2015")
        assertThat(info.identifier).isNull()
    }

    @Test
    fun `profile url tolerates a query string`() {
        val info = parseTikTokUrl("https://www.tiktok.com/@scout2015?lang=en")

        assertThat(info!!.contentType).isEqualTo("profile")
        assertThat(info.username).isEqualTo("scout2015")
    }

    @Test
    fun `short link has no parseable content until resolved`() {
        assertThat(parseTikTokUrl("https://vm.tiktok.com/ZMabc123/")).isNull()
    }

    @Test
    fun `non-tiktok url is not parsed`() {
        assertThat(parseTikTokUrl("https://x.com/jack/status/1")).isNull()
    }

    @Test
    fun `isTikTokShortLink recognises vm vt and t links only`() {
        assertThat(isTikTokShortLink("https://vm.tiktok.com/ZMabc123/")).isTrue()
        assertThat(isTikTokShortLink("https://vt.tiktok.com/ZMabc123/")).isTrue()
        assertThat(isTikTokShortLink("https://www.tiktok.com/t/ZMabc123/")).isTrue()
        assertThat(isTikTokShortLink("https://www.tiktok.com/@scout2015/video/1")).isFalse()
    }

    @Test
    fun `isTikTokUrl recognises tiktok hosts`() {
        assertThat(isTikTokUrl("https://www.tiktok.com/@scout2015/video/1")).isTrue()
        assertThat(isTikTokUrl("https://vm.tiktok.com/ZMabc/")).isTrue()
        assertThat(isTikTokUrl("https://x.com/jack")).isFalse()
    }

    @Test
    fun `profile url is reconstructed from parsed info`() {
        val info = parseTikTokUrl("https://www.tiktok.com/@scout2015/video/6718335390845095173")

        assertThat(info!!.profileUrl()).isEqualTo("https://www.tiktok.com/@scout2015")
    }

    @Test
    fun `parseSocialMediaUrl dispatches tiktok links`() {
        val info = parseSocialMediaUrl("https://www.tiktok.com/@scout2015/video/6718335390845095173")

        assertThat(info?.platform).isEqualTo("tiktok")
        assertThat(info?.contentType).isEqualTo("video")
    }
}
