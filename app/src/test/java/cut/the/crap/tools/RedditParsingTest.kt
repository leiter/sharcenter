package cut.the.crap.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for Reddit URL recognition in [SocialMediaParser]: post/user/subreddit parsing, short-link
 * detection, and profile/subreddit URL reconstruction.
 */
class RedditParsingTest {

    @Test
    fun `post url parses subreddit and id`() {
        val info = parseRedditUrl("https://www.reddit.com/r/pics/comments/1uosb5v/my_parents/")

        assertThat(info).isNotNull()
        assertThat(info!!.platform).isEqualTo("reddit")
        assertThat(info.contentType).isEqualTo("post")
        assertThat(info.identifier).isEqualTo("1uosb5v")
        assertThat(info.additionalInfo["subreddit"]).isEqualTo("pics")
    }

    @Test
    fun `user url parses as profile`() {
        val info = parseRedditUrl("https://www.reddit.com/user/lonaangreen")

        assertThat(info!!.contentType).isEqualTo("profile")
        assertThat(info.username).isEqualTo("lonaangreen")
    }

    @Test
    fun `short u prefix user url is recognised`() {
        val info = parseRedditUrl("https://www.reddit.com/u/spez")

        assertThat(info!!.contentType).isEqualTo("profile")
        assertThat(info.username).isEqualTo("spez")
    }

    @Test
    fun `subreddit url parses as subreddit`() {
        val info = parseRedditUrl("https://www.reddit.com/r/androiddev")

        assertThat(info!!.contentType).isEqualTo("subreddit")
        assertThat(info.username).isEqualTo("androiddev")
    }

    @Test
    fun `short link has no parseable content until resolved`() {
        assertThat(parseRedditUrl("https://redd.it/1uosb5v")).isNull()
    }

    @Test
    fun `non-reddit url is not parsed`() {
        assertThat(parseRedditUrl("https://x.com/jack/status/1")).isNull()
    }

    @Test
    fun `isRedditShortLink recognises redd_it and s share links only`() {
        assertThat(isRedditShortLink("https://redd.it/1uosb5v")).isTrue()
        assertThat(isRedditShortLink("https://www.reddit.com/r/pics/s/AbCdEf123")).isTrue()
        assertThat(isRedditShortLink("https://www.reddit.com/r/pics/comments/1uosb5v/x/")).isFalse()
    }

    @Test
    fun `isRedditUrl recognises reddit and redd_it hosts`() {
        assertThat(isRedditUrl("https://www.reddit.com/r/pics/comments/1/x/")).isTrue()
        assertThat(isRedditUrl("https://redd.it/abc")).isTrue()
        assertThat(isRedditUrl("https://x.com/jack")).isFalse()
    }

    @Test
    fun `profile and subreddit urls are reconstructed from parsed info`() {
        val user = parseRedditUrl("https://www.reddit.com/user/spez")
        val sub = parseRedditUrl("https://www.reddit.com/r/androiddev")

        assertThat(user!!.profileUrl()).isEqualTo("https://www.reddit.com/user/spez")
        assertThat(sub!!.profileUrl()).isEqualTo("https://www.reddit.com/r/androiddev")
    }

    @Test
    fun `parseSocialMediaUrl dispatches reddit links`() {
        val info = parseSocialMediaUrl("https://www.reddit.com/r/pics/comments/1uosb5v/x/")

        assertThat(info?.platform).isEqualTo("reddit")
        assertThat(info?.contentType).isEqualTo("post")
    }
}
