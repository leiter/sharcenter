package cut.the.crap.data.rest.campaign

import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.CtcSignature
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.identity.IdentityRepositoryImpl
import cut.the.crap.fake.FakeIdentityKeyStore
import cut.the.crap.identity.IdentityManager
import cut.the.crap.platform.JvmCryptoProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The real client against a real server, for campaigns.
 *
 * The unit tests use hand-written payloads; this is the only check that the shapes the server
 * actually emits are the shapes the client actually parses. It caught nothing on the way in, which
 * is the point of writing it before believing the unit tests.
 *
 * **Skipped unless `CTC_SERVER` is set.** The server must have the Abu-Safiya campaign imported:
 *
 * ```
 * python3 run_server.py &
 * python3 scripts/import_abu_safiya.py --owner <user_id>
 * CTC_SERVER=http://127.0.0.1:5099 ./gradlew :shared:desktopTest --tests '*CampaignIntegrationTest'
 * ```
 */
class CampaignIntegrationTest {

    private val baseUrl: String? = System.getenv("CTC_SERVER")

    private fun repositories(): Pair<CampaignRepositoryImpl, IdentityRepositoryImpl> {
        val config = AppConfig(
            apiBaseUrl = "http://127.0.0.1:1",
            campaignBaseUrl = baseUrl!!,
            isDebug = false,
        )
        val manager = IdentityManager(FakeIdentityKeyStore(), JvmCryptoProvider())
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(CtcSignature) {
                signer = manager
                crypto = JvmCryptoProvider()
                signedHost = Url(config.campaignBaseUrl).host
            }
        }
        return CampaignRepositoryImpl(client, config) to
            IdentityRepositoryImpl(client, config, manager)
    }

    @Test
    fun `a fresh install sees the bundled campaign in its list`() = runTest {
        if (baseUrl == null) {
            println("skipped: set CTC_SERVER to run the campaign integration test")
            return@runTest
        }

        val (campaigns, identity) = repositories()
        identity.register(displayName = "Integration")

        // The point of `featured`: someone who has joined nothing still has something to act on.
        val list = assertIs<Result.Success<List<CampaignSummary>>>(campaigns.list()).data
        val bundled = list.single { it.id == "abu-safiya" }
        assertTrue(bundled.featured)
        assertTrue(!bundled.isMine, "a fresh install has joined nothing")
        assertTrue(bundled.countryCount > 1, "counts come from the server, not from the payload")
        assertTrue(bundled.postCount > bundled.countryCount)
    }

    @Test
    fun `the detail payload carries the three fields that used to be dead`() = runTest {
        if (baseUrl == null) return@runTest

        val (campaigns, identity) = repositories()
        identity.register()

        val campaign = assertIs<Result.Success<Campaign>>(campaigns.get("abu-safiya")).data

        assertEquals("abu-safiya", campaign.id)
        assertTrue(campaign.displayTitle.isNotBlank())
        // locateUrl, country.url and a contact url: parsed since the first version, never opened.
        assertTrue(campaign.locateUrl!!.endsWith("/abu-safiya/locate"))
        assertTrue(campaign.countries.all { it.url.isNotBlank() })
        val withContacts = campaign.countriesWithContacts
        assertTrue(withContacts.isNotEmpty(), "the parliament flag must now have a destination")
        assertTrue(withContacts.all { it.hasParliamentAction })
        assertTrue(withContacts.first().contacts.first().url.startsWith("http"))
    }

    @Test
    fun `the legacy alias still parses, unsigned and unchanged`() = runTest {
        if (baseUrl == null) return@runTest

        // No register() on this one: a client with no identity at all must still get the alias.
        // That is C7 — its path is hardcoded in shipped builds that predate identity entirely.
        val (unidentified, _) = repositories()
        val legacy = assertIs<Result.Success<Campaign>>(unidentified.getCampaign()).data

        val (campaigns, identity) = repositories()
        identity.register()
        val byId = assertIs<Result.Success<Campaign>>(campaigns.get("abu-safiya")).data

        // Same campaign, two routes: the alias carries no id or title, and everything else matches.
        assertEquals("", legacy.id, "the alias predates ids and must not grow one")
        assertEquals(byId.countries.map { it.countryCode }, legacy.countries.map { it.countryCode })
        assertEquals(byId.countries.sumOf { it.posts.size }, legacy.countries.sumOf { it.posts.size })
    }

    @Test
    fun `an unknown campaign is a permanent not-found`() = runTest {
        if (baseUrl == null) return@runTest

        val (campaigns, identity) = repositories()
        identity.register()

        val error = assertIs<Result.Error>(campaigns.get("no-such-campaign"))
        assertTrue(!error.retryable)
    }
}
