package cut.the.crap.data.rest

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class YouTubeUrlParserTest {

    @Test
    fun `extracts id from standard watch url`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://www.youtube.com/watch?v=OZgWRKm0hwI"))
            .isEqualTo("OZgWRKm0hwI")
    }

    @Test
    fun `extracts id from watch url with extra query params`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://youtube.com/watch?v=OZgWRKm0hwI&si=abc123"))
            .isEqualTo("OZgWRKm0hwI")
    }

    @Test
    fun `extracts id from shorts url`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://youtube.com/shorts/OZgWRKm0hwI"))
            .isEqualTo("OZgWRKm0hwI")
    }

    @Test
    fun `extracts id from shorts url with query params`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://youtube.com/shorts/OZgWRKm0hwI?is=EkTJ-c1g"))
            .isEqualTo("OZgWRKm0hwI")
    }

    @Test
    fun `extracts id from short youtu_be url`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://youtu.be/OZgWRKm0hwI"))
            .isEqualTo("OZgWRKm0hwI")
    }

    @Test
    fun `extracts id from live url`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://youtube.com/live/aG2dwHVD0BA?is=BnLD6XUGMdtj"))
            .isEqualTo("aG2dwHVD0BA")
    }

    @Test
    fun `returns null for community post url`() {
        assertThat(YouTubeUrlParser.extractVideoId("http://youtube.com/post/Ugkxu8JEOx7xfjqoJ6WtoA")).isNull()
    }

    @Test
    fun `extracts id from embed url`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://www.youtube.com/embed/OZgWRKm0hwI"))
            .isEqualTo("OZgWRKm0hwI")
    }

    @Test
    fun `extracts id from old v url`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://www.youtube.com/v/OZgWRKm0hwI"))
            .isEqualTo("OZgWRKm0hwI")
    }

    @Test
    fun `returns null for non youtube url`() {
        assertThat(YouTubeUrlParser.extractVideoId("https://x.com/user/status/123")).isNull()
    }

    @Test
    fun `isYouTubeUrl recognises shorts and watch and short links`() {
        assertThat(YouTubeUrlParser.isYouTubeUrl("https://youtube.com/shorts/OZgWRKm0hwI")).isTrue()
        assertThat(YouTubeUrlParser.isYouTubeUrl("https://www.youtube.com/watch?v=OZgWRKm0hwI")).isTrue()
        assertThat(YouTubeUrlParser.isYouTubeUrl("https://youtu.be/OZgWRKm0hwI")).isTrue()
        assertThat(YouTubeUrlParser.isYouTubeUrl("https://x.com/user")).isFalse()
    }

    @Test
    fun `constructWatchUrl builds canonical watch url`() {
        assertThat(YouTubeUrlParser.constructWatchUrl("OZgWRKm0hwI"))
            .isEqualTo("https://www.youtube.com/watch?v=OZgWRKm0hwI")
    }
}
