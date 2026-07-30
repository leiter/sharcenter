package cut.the.crap.data.rest.identity

import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.CtcSignature
import cut.the.crap.data.rest.Result
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
 * The real client against a real server — the one thing the unit tests cannot prove.
 *
 * The fixed contract vectors in `CtcSignatureTest` show that both sides canonicalise identically,
 * and the server's `test_identity.py` shows the server accepts a correctly signed request. What
 * neither covers is the whole stack meeting: Ktor's engine, its header encoding and its JSON
 * content type against WSGI's view of the same request.
 *
 * **Skipped unless `CTC_SERVER` is set**, so the ordinary suite needs no server:
 *
 * ```
 * python3 run_server.py &                       # the Flask app on :5099
 * CTC_SERVER=http://127.0.0.1:5099 ./gradlew :shared:desktopTest --tests '*IdentityIntegrationTest'
 * ```
 */
class IdentityIntegrationTest {

    private val baseUrl: String? = System.getenv("CTC_SERVER")

    private fun repository(seed: ByteArray? = null): Pair<IdentityRepositoryImpl, IdentityManager> {
        val config = AppConfig(
            apiBaseUrl = "http://127.0.0.1:1",
            campaignBaseUrl = baseUrl!!,
            isDebug = false,
        )
        val manager = IdentityManager(FakeIdentityKeyStore(seed), JvmCryptoProvider())
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(CtcSignature) {
                signer = manager
                crypto = JvmCryptoProvider()
                signedHost = Url(config.campaignBaseUrl).host
            }
        }
        return IdentityRepositoryImpl(client, config, manager) to manager
    }

    @Test
    fun `registers and then pings as the same user`() = runTest {
        if (baseUrl == null) {
            println("skipped: set CTC_SERVER to run the identity integration test")
            return@runTest
        }

        val (repository, _) = repository()

        val registered = repository.register(displayName = "Integration", keyLabel = "desktopTest")
        assertIs<Result.Success<RegisteredUser>>(registered)
        assertTrue(registered.data.userId.isNotEmpty())

        val ping = repository.ping()
        assertIs<Result.Success<String>>(ping)
        assertEquals(registered.data.userId, ping.data, "ping must resolve to the registered user")
    }

    @Test
    fun `a second install is a different user, and the same seed is the same user`() = runTest {
        if (baseUrl == null) return@runTest

        val (first, firstManager) = repository()
        val firstUser = assertIs<Result.Success<RegisteredUser>>(first.register()).data
        val seed = requireNotNull(firstManager.recoveryPhrase())

        val (second, _) = repository()
        val secondUser = assertIs<Result.Success<RegisteredUser>>(second.register()).data
        assertTrue(firstUser.userId != secondUser.userId, "two installs must not share an identity")

        // Restore the first install's seed into a fresh key store: same key, same user.
        val (restored, restoredManager) = repository()
        restoredManager.restore(seed.joinToString(" "))
        val ping = restored.ping()
        assertEquals(firstUser.userId, assertIs<Result.Success<String>>(ping).data)
    }

    @Test
    fun `an unregistered key cannot ping`() = runTest {
        if (baseUrl == null) return@runTest

        val (repository, manager) = repository()
        manager.getOrCreate()

        val ping = repository.ping()
        val error = assertIs<Result.Error>(ping)
        assertTrue(error.error.debugText.contains("401"), "expected 401, was ${error.error.debugText}")
        assertTrue(!error.retryable, "a 401 must not be marked retryable")
    }
}
