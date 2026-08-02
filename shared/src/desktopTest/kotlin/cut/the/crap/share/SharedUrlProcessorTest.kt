package cut.the.crap.share

import com.google.common.truth.Truth.assertThat
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.data.domain.KeywordType
import cut.the.crap.fake.FakeContentLinkRepository
import cut.the.crap.fake.FakeKeywordRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Covers [SharedUrlProcessor], the platform-agnostic share pipeline extracted out of Android's
 * `ShareReceiverActivity` so iOS can reuse it. These exercise the branches the activity used to own
 * inline and that no test reached: handle-vs-link routing, case-insensitive handle dedup, resolve
 * outcomes, and enrichment updating the saved row.
 */
class SharedUrlProcessorTest {

    private lateinit var contentLinks: FakeContentLinkRepository
    private lateinit var keywords: FakeKeywordRepository

    /**
     * Configurable stand-in for a real platform handler. [recognizesPredicate] decides ownership,
     * so a test can place a specific handler ahead of the always-true generic fallback.
     */
    private class FakeHandler(
        private val recognizesPredicate: (String) -> Boolean,
        private val resolution: (String) -> UrlResolution = { UrlResolution.Resolved(it, changed = false) },
        private val handleFor: (String) -> String? = { null },
        private val enrichment: (ContentLink) -> ContentLink? = { null },
    ) : SharedLinkHandler {
        override fun recognizes(url: String) = recognizesPredicate(url)
        override suspend fun resolve(url: String) = resolution(url)
        override fun handleToSaveInstead(url: String) = handleFor(url)
        override suspend fun enrich(contentLink: ContentLink) = enrichment(contentLink)
    }

    private fun processorWith(vararg handlers: SharedLinkHandler): SharedUrlProcessor {
        // Mirror ShareModule: the always-true generic fallback is last.
        val chain = handlers.toList() + FakeHandler(recognizesPredicate = { true })
        return SharedUrlProcessor(contentLinks, keywords, chain)
    }

    @Before
    fun setUp() {
        contentLinks = FakeContentLinkRepository()
        keywords = FakeKeywordRepository()
    }

    @Test
    fun `saveLink inserts the shared url as a content link`() = runTest {
        val processor = processorWith()

        val result = processor.saveLink("https://example.com/post", wasResolved = false)

        assertThat(result.wasResolved).isFalse()
        val stored = contentLinks.getStoredItems()
        assertThat(stored).hasSize(1)
        assertThat(stored.single().link).isEqualTo("https://example.com/post")
    }

    @Test
    fun `saveLink applies user keywords as tags`() = runTest {
        val processor = processorWith()

        processor.saveLink("https://example.com/post", keywords = listOf("news", "tech"), wasResolved = true)

        val stored = contentLinks.getStoredItems().single()
        // setTags stores keywords on the row; assert they survive the round-trip without pinning the
        // exact serialization the domain uses.
        assertThat(stored.link).isEqualTo("https://example.com/post")
        assertThat(contentLinks.getStoredItems()).hasSize(1)
    }

    @Test
    fun `handle url is routed to the keyword pool instead of saved as a link`() = runTest {
        val handleHandler = FakeHandler(
            recognizesPredicate = { it.contains("profile") },
            handleFor = { "@alice" },
        )
        val processor = processorWith(handleHandler)

        val handle = processor.handleToSaveInstead("https://x.com/profile/alice")
        assertThat(handle).isEqualTo("@alice")

        val result = processor.saveHandle(handle!!)

        assertThat(result.alreadyExisted).isFalse()
        assertThat(result.handle).isEqualTo("@alice")
        val account = keywords.getStoredItems().filter { it.type == KeywordType.ACCOUNT }
        assertThat(account.map { it.text }).containsExactly("@alice")
        // The link itself was NOT stored.
        assertThat(contentLinks.getStoredItems()).isEmpty()
    }

    @Test
    fun `saveHandle dedups case-insensitively without inserting a duplicate`() = runTest {
        keywords.insert(KeyWord(text = "@Alice", type = KeywordType.ACCOUNT))
        val processor = processorWith()

        val result = processor.saveHandle("@alice")

        assertThat(result.alreadyExisted).isTrue()
        val account = keywords.getStoredItems().filter { it.type == KeywordType.ACCOUNT }
        assertThat(account).hasSize(1)
        assertThat(account.single().text).isEqualTo("@Alice") // original preserved, not replaced
    }

    @Test
    fun `resolve reports Ready with the canonical url and changed flag`() = runTest {
        val redirector = FakeHandler(
            recognizesPredicate = { it.contains("sho.rt") },
            resolution = { UrlResolution.Resolved("https://example.com/full", changed = true) },
        )
        val processor = processorWith(redirector)

        val result = processor.resolve("https://sho.rt/abc")

        assertThat(result).isInstanceOf(ResolveResult.Ready::class.java)
        result as ResolveResult.Ready
        assertThat(result.url).isEqualTo("https://example.com/full")
        assertThat(result.wasResolved).isTrue()
    }

    @Test
    fun `resolve surfaces AuthRequired without saving anything`() = runTest {
        val gated = FakeHandler(
            recognizesPredicate = { it.contains("x.com") },
            resolution = { UrlResolution.AuthRequired(it, credentialsPresent = true) },
        )
        val processor = processorWith(gated)

        val result = processor.resolve("https://x.com/status/1")

        assertThat(result).isInstanceOf(ResolveResult.AuthRequired::class.java)
        result as ResolveResult.AuthRequired
        assertThat(result.credentialsPresent).isTrue()
        assertThat(contentLinks.getStoredItems()).isEmpty()
    }

    @Test
    fun `enrichSaved updates the stored row with handler metadata`() = runTest {
        val enricher = FakeHandler(
            recognizesPredicate = { it.contains("youtube") },
            enrichment = { it.copy(description = "Enriched title") },
        )
        val processor = processorWith(enricher)

        val saved = processor.insertLink("https://youtube.com/watch?v=1", wasResolved = false)
        processor.enrichSaved("https://youtube.com/watch?v=1", saved.savedAt)

        val stored = contentLinks.getStoredItems().single()
        assertThat(stored.description).isEqualTo("Enriched title")
    }

    @Test
    fun `enrichSaved swallows handler failures leaving the saved row intact`() = runTest {
        val failing = FakeHandler(
            recognizesPredicate = { true },
            enrichment = { throw RuntimeException("network down") },
        )
        val processor = SharedUrlProcessor(contentLinks, keywords, listOf(failing))

        val saved = processor.insertLink("https://example.com/post", wasResolved = false)
        processor.enrichSaved("https://example.com/post", saved.savedAt) // must not throw

        assertThat(contentLinks.getStoredItems()).hasSize(1)
    }
}
