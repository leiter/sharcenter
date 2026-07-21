package cut.the.crap.data.rest.campaign

import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.Source
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the campaign payload → domain mapping against a captured slice of the real
 * `/api/abu-safiya` response, plus the failure mapping the UI's error messages depend on.
 */
class CampaignRepositoryTest {

    private val config = AppConfig(
        apiBaseUrl = "http://jobs.invalid",
        campaignBaseUrl = "https://campaign.invalid/",
        isDebug = false,
    )

    /** Trimmed from the live endpoint: a single-language country, a bilingual one, and an empty one. */
    private val payload = """
        {
          "campaign": "abu-safiya",
          "version": 1,
          "locateUrl": "https://cutthecrap.link/abu-safiya/locate",
          "countries": [
            {
              "code": "de", "name": "Deutschland", "flag": "🇩🇪",
              "defaultLang": "de", "langs": ["de"],
              "url": "https://cutthecrap.link/de/abu-safiya",
              "parliament": true,
              "posts": [
                {"id": "A — Der Aufruf", "lang": "de", "text": "Aufruf-Text"},
                {"id": "B — Niedrige Hürde", "lang": "de", "text": "Huerden-Text"}
              ]
            },
            {
              "code": "lu", "name": "Luxembourg", "flag": "🇱🇺",
              "defaultLang": "fr", "langs": ["fr", "de"],
              "url": "https://cutthecrap.link/lu/abu-safiya",
              "parliament": false,
              "posts": [
                {"id": "LU (français)", "lang": "fr", "text": "Texte FR"},
                {"id": "LU (Deutsch)", "lang": "de", "text": "Text DE"}
              ]
            },
            {
              "code": "xx", "name": "", "flag": "",
              "defaultLang": "en", "langs": ["en"],
              "url": "https://cutthecrap.link/xx/abu-safiya",
              "parliament": false,
              "posts": []
            }
          ]
        }
    """.trimIndent()

    private fun repository(handler: MockEngine.() -> Unit = {}, engine: MockEngine) =
        CampaignRepositoryImpl(
            HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            config,
        )

    private fun okRepository(onRequest: (String) -> Unit = {}) = repository(
        engine = MockEngine { request ->
            onRequest(request.url.toString())
            respond(
                content = payload,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
    )

    @Test
    fun `requests the campaign endpoint on the campaign host, not the api host`() = runTest {
        var requested = ""
        okRepository { requested = it }.getCampaign()

        // The trailing slash in campaignBaseUrl must not produce a double slash.
        assertEquals("https://campaign.invalid/api/abu-safiya", requested)
    }

    @Test
    fun `maps countries and posts, keeping the payload order`() = runTest {
        val result = okRepository().getCampaign()

        assertTrue(result is Result.Success)
        val campaign = (result as Result.Success).data
        assertEquals("abu-safiya", campaign.name)
        assertEquals("https://cutthecrap.link/abu-safiya/locate", campaign.locateUrl)
        assertEquals(listOf("de", "lu", "xx"), campaign.countries.map { it.countryCode })

        val de = campaign.countries.first()
        assertEquals("Deutschland", de.countryName)
        assertTrue(de.hasParliamentAction)
        assertEquals(listOf("A — Der Aufruf", "B — Niedrige Hürde"), de.posts.map { it.id })
    }

    @Test
    fun `a country contributes one draft per language, not per post`() = runTest {
        val campaign = (okRepository().getCampaign() as Result.Success).data
        val de = campaign.countries.single { it.countryCode == "de" }
        val lu = campaign.countries.single { it.countryCode == "lu" }

        // Germany has two posts but only one language — selecting it yields one draft.
        assertEquals(1, de.postCount)
        assertEquals(2, de.variantCount)
        // Luxembourg has one post in each of two languages — two drafts, no variant choice.
        assertEquals(2, lu.postCount)
        assertEquals(1, lu.variantCount)
        assertEquals(listOf("Texte FR", "Text DE"), lu.postsForVariant(0).map { it.text })
    }

    @Test
    fun `variant index is clamped per language`() = runTest {
        val campaign = (okRepository().getCampaign() as Result.Success).data
        val de = campaign.countries.single { it.countryCode == "de" }
        val lu = campaign.countries.single { it.countryCode == "lu" }

        assertEquals(listOf("Huerden-Text"), de.postsForVariant(1).map { it.text })
        // Luxembourg has no second variant, so each language falls back to its only post.
        assertEquals(listOf("Texte FR", "Text DE"), lu.postsForVariant(1).map { it.text })
    }

    @Test
    fun `a country without posts is kept but not selectable`() = runTest {
        val campaign = (okRepository().getCampaign() as Result.Success).data
        val xx = campaign.countries.single { it.countryCode == "xx" }

        assertEquals(false, xx.hasPosts)
        // A blank name falls back to the code so the row is never empty.
        assertEquals("XX", xx.countryName)
        assertEquals(listOf("de", "lu"), campaign.countriesWithPosts.map { it.countryCode })
    }

    /**
     * Contract test against bytes captured verbatim from the running campaign server
     * (`/api/abu-safiya`, countries DE/IT/LU). Guards the field names in [CampaignDto] — a
     * synthetic payload would happily agree with a typo on both sides.
     */
    @Test
    fun `parses a slice captured from the live campaign server`() = runTest {
        val captured = checkNotNull(
            javaClass.getResource("/campaign_abu_safiya_slice.json")
        ).readText()

        val result = repository(
            engine = MockEngine {
                respond(
                    content = captured,
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    ),
                )
            }
        ).getCampaign()

        assertTrue(result is Result.Success)
        val campaign = (result as Result.Success).data
        assertEquals("abu-safiya", campaign.name)
        assertEquals(listOf("de", "it", "lu"), campaign.countries.map { it.countryCode })

        val de = campaign.countries.single { it.countryCode == "de" }
        assertEquals("Deutschland", de.countryName)
        assertEquals("🇩🇪", de.flag)
        assertTrue(de.hasParliamentAction)
        // Every post carries a language and finished text — nothing fell back to a blank.
        assertTrue(campaign.countries.all { c -> c.posts.all { it.language.isNotBlank() } })
        assertTrue(campaign.countries.all { c -> c.posts.all { it.text.isNotBlank() } })

        // Luxembourg is the bilingual case: one post per language, so two drafts.
        val lu = campaign.countries.single { it.countryCode == "lu" }
        assertEquals(listOf("fr", "de"), lu.postsForVariant(0).map { it.language })
        // Italy carries three variants in one language, so it still yields a single draft.
        val it = campaign.countries.single { it.countryCode == "it" }
        assertEquals(1, it.postCount)
        assertEquals(3, it.variantCount)
        assertTrue(it.postsForVariant(0).single().text.contains("cutthecrap.link/it/abu-safiya"))
    }

    @Test
    fun `a server error is retryable and attributed to the campaign source`() = runTest {
        val result = repository(
            engine = MockEngine { respondError(HttpStatusCode.BadGateway) }
        ).getCampaign()

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.retryable)
        assertEquals(AppError.SourceServer(Source.CAMPAIGN, 502), error.error)
    }

    @Test
    fun `a missing campaign is a permanent not-found`() = runTest {
        val result = repository(
            engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        ).getCampaign()

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals(false, error.retryable)
        assertEquals(AppError.NotFound(Source.CAMPAIGN), error.error)
    }
}
