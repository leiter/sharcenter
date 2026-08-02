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

    // --- The list and the per-campaign endpoint (step 2) ---------------------

    @Test
    fun `list maps the summaries`() = runTest {
        var requested = ""
        val result = jsonRepository(
            """
            {"campaigns":[
              {"id":"abu-safiya","title":"Abu Safiya","description":null,"state":"active",
               "version":3,"role":null,"featured":true,
               "countryCount":42,"postCount":120,"contactCount":1},
              {"id":"mine","title":"Mine","state":"draft","version":1,"role":"owner",
               "featured":false,"countryCount":0,"postCount":0,"contactCount":0}
            ]}
            """.trimIndent()
        ) { requested = it }.list()

        assertEquals("https://campaign.invalid/api/campaigns/mine", requested)
        val campaigns = (result as Result.Success).data
        assertEquals(listOf("abu-safiya", "mine"), campaigns.map { it.id })
        assertEquals(42, campaigns[0].countryCount)
        assertTrue(campaigns[0].featured && !campaigns[0].isMine)
        assertTrue(campaigns[1].isMine)
    }

    @Test
    fun `get requests the campaign by id and keeps the new fields`() = runTest {
        var requested = ""
        val result = jsonRepository(
            payload.replace(
                "\"campaign\": \"abu-safiya\",",
                "\"campaign\": \"abu-safiya\", \"id\": \"abu-safiya\", " +
                    "\"title\": \"Freiheit\", \"role\": \"member\",",
            )
        ) { requested = it }.get("abu-safiya")

        assertEquals("https://campaign.invalid/api/campaigns/abu-safiya", requested)
        val campaign = (result as Result.Success).data
        assertEquals("abu-safiya", campaign.id)
        assertEquals("Freiheit", campaign.displayTitle)
        assertEquals("member", campaign.role)
    }

    @Test
    fun `contacts are parsed and blank urls dropped`() = runTest {
        val campaign = (jsonRepository(
            """
            {"campaign":"c","id":"c","version":1,"countries":[
              {"code":"de","name":"Deutschland","flag":"DE","defaultLang":"de","langs":["de"],
               "url":"https://x.test/de","parliament":true,
               "posts":[{"id":"A","lang":"de","text":"Text"}],
               "contacts":[{"id":"c1","url":"https://x.test/mdb","label":"MP","note":"n"},
                           {"id":"c2","url":""}]},
              {"code":"it","name":"Italia","flag":"IT","defaultLang":"it","langs":["it"],
               "url":"https://x.test/it","parliament":false,
               "posts":[{"id":"IT","lang":"it","text":"Testo"}]}
            ]}
            """.trimIndent()
        ).get("c") as Result.Success).data

        val de = campaign.countries.single { it.countryCode == "de" }
        assertEquals("a contact with no url has nothing to open", 1, de.contacts.size)
        assertEquals("https://x.test/mdb", de.contacts.single().url)
        assertEquals("MP", de.contacts.single().label)
        assertEquals(listOf("de"), campaign.countriesWithContacts.map { it.countryCode })
    }

    @Test
    fun `a second get is served from memory`() = runTest {
        var calls = 0
        val repository = jsonRepository(payload) { calls++ }

        repository.get("abu-safiya")
        repository.get("abu-safiya")

        // Moving from the detail screen to the composer must not re-download ~47 kB.
        assertEquals(1, calls)

        repository.get("abu-safiya", forceRefresh = true)
        assertEquals("an explicit refresh must actually go to the server", 2, calls)
    }

    @Test
    fun `a disabled campaign is permanent, not something to retry`() = runTest {
        val result = repository(
            engine = MockEngine { respondError(HttpStatusCode(451, "Unavailable For Legal Reasons")) }
        ).get("secret")

        val error = result as Result.Error
        assertEquals("the operator switched it off; retrying cannot help", false, error.retryable)
        assertEquals(451, (error.error as AppError.Client).status)
    }

    private fun jsonRepository(body: String, onRequest: (String) -> Unit = {}) = repository(
        engine = MockEngine { request ->
            onRequest(request.url.toString())
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
    )

    // --- Step 3: authoring, invites, join -------------------------------------

    private val summaryBody = """
        {"id":"c1","title":"New campaign","description":null,"state":"active","version":1,
         "role":"owner","featured":false,"countryCount":1,"postCount":1,"contactCount":0}
    """.trimIndent()

    @Test
    fun `create posts the raw json as-is and maps the returned summary`() = runTest {
        var requestUrl = ""
        var requestMethod = ""
        var requestBody = ""
        val result = repository(
            engine = MockEngine { request ->
                requestUrl = request.url.toString()
                requestMethod = request.method.value
                requestBody = (request.body as io.ktor.http.content.TextContent).text
                respond(
                    content = summaryBody,
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        ).create("""{"title":"New campaign","items":[]}""")

        assertEquals("https://campaign.invalid/api/campaigns", requestUrl)
        assertEquals("POST", requestMethod)
        // Sent verbatim: no client-side re-encoding of the pasted JSON.
        assertEquals("""{"title":"New campaign","items":[]}""", requestBody)
        assertTrue(result is Result.Success)
        assertEquals("c1", (result as Result.Success).data.id)
        assertEquals("owner", result.data.role)
    }

    @Test
    fun `create surfaces a validation failure as a non-retryable client error`() = runTest {
        val result = repository(
            engine = MockEngine { respondError(HttpStatusCode.BadRequest) }
        ).create("""{"title":""}""")

        assertTrue(result is Result.Error)
        assertEquals(false, (result as Result.Error).retryable)
    }

    @Test
    fun `replaceItems posts to the campaign's items path`() = runTest {
        var requestUrl = ""
        val result = repository(
            engine = MockEngine { request ->
                requestUrl = request.url.toString()
                respond(content = """{"version":2}""", status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            }
        ).replaceItems("c1", """{"items":[]}""")

        assertEquals("https://campaign.invalid/api/campaigns/c1/items", requestUrl)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `invite posts the role and returns the code`() = runTest {
        val result = repository(
            engine = MockEngine {
                respond(content = """{"code":"ABCD2345"}""", status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            }
        ).invite("c1", "member")

        assertTrue(result is Result.Success)
        assertEquals("ABCD2345", (result as Result.Success).data)
    }

    @Test
    fun `join posts the code and maps the returned summary`() = runTest {
        var requestUrl = ""
        val result = repository(
            engine = MockEngine { request ->
                requestUrl = request.url.toString()
                respond(
                    content = summaryBody.replace("\"role\":\"owner\"", "\"role\":\"member\""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        ).join("ABCD2345")

        assertEquals("https://campaign.invalid/api/campaigns/join", requestUrl)
        assertTrue(result is Result.Success)
        assertEquals("member", (result as Result.Success).data.role)
    }

    @Test
    fun `join surfaces an unknown code as a not-found error`() = runTest {
        val result = repository(
            engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        ).join("NOPENOPE")

        assertEquals(AppError.NotFound(Source.CAMPAIGN), (result as Result.Error).error)
    }

    @Test
    fun `leave issues a DELETE to the campaign's own membership`() = runTest {
        var requestUrl = ""
        var requestMethod = ""
        val result = repository(
            engine = MockEngine { request ->
                requestUrl = request.url.toString()
                requestMethod = request.method.value
                respond(content = "", status = HttpStatusCode.NoContent)
            }
        ).leave("c1")

        assertEquals("https://campaign.invalid/api/campaigns/c1/members/me", requestUrl)
        assertEquals("DELETE", requestMethod)
        assertTrue(result is Result.Success)
    }
}
