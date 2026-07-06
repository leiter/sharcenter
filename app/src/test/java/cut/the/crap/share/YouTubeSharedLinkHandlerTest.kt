package cut.the.crap.share

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.rest.Result
import cut.the.crap.fake.FakeYouTubeRepository
import cut.the.crap.testutils.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Test

class YouTubeSharedLinkHandlerTest {

    private val youTubeRepository = FakeYouTubeRepository()
    private val handler = YouTubeSharedLinkHandler(youTubeRepository)

    @Test
    fun `recognizes youtube urls and rejects others`() {
        assertThat(handler.recognizes("https://www.youtube.com/watch?v=abc")).isTrue()
        assertThat(handler.recognizes("https://youtu.be/abc")).isTrue()
        assertThat(handler.recognizes("https://x.com/jack/status/1")).isFalse()
    }

    @Test
    fun `enrich applies fetched metadata to the saved link`() = runTest {
        val metadata = TestData.youtubeMetadata(
            title = "Never Gonna Give You Up",
            channelName = "Rick Astley"
        )
        youTubeRepository.setSuccessResponse(metadata)
        val saved = TestData.youtubeLink(id = 7)

        val enriched = handler.enrich(saved)

        assertThat(enriched).isNotNull()
        assertThat(enriched!!.id).isEqualTo(saved.id)
        assertThat(enriched.description).contains("Rick Astley")
        assertThat(enriched.description).contains("Never Gonna Give You Up")
        assertThat(youTubeRepository.getFetchedUrls()).contains(saved.link)
    }

    @Test
    fun `enrich returns null when metadata fetch fails`() = runTest {
        youTubeRepository.setErrorResponse("boom")

        val enriched = handler.enrich(TestData.youtubeLink())

        assertThat(enriched).isNull()
    }

    @Test
    fun `enrich ignores non-youtube links without hitting the network`() = runTest {
        val enriched = handler.enrich(TestData.xLink())

        assertThat(enriched).isNull()
        assertThat(youTubeRepository.getFetchedUrls()).isEmpty()
    }
}
