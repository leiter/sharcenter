package cut.the.crap.data.rest.identity

import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.fake.FakeIdentityKeyStore
import cut.the.crap.identity.IdentityManager
import cut.the.crap.platform.JvmCryptoProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The profile and device-management calls of `doc/IDENTITY_SPEC.md` §5.2–§5.5.
 *
 * Signing is `CtcSignatureTest`'s job and the round trip is `IdentityIntegrationTest`'s; what is
 * pinned here is the shape of each request and, in particular, that the server's machine-readable
 * `error` code survives into [AppError.Client]. Without it a 409 is just "409", and the UI cannot
 * tell "that key belongs to someone else" from "that is your last device".
 */
class IdentityRepositoryTest {

    private val captured = mutableListOf<HttpRequestData>()

    private fun repository(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "{}",
    ): IdentityRepositoryImpl {
        captured.clear()
        val client = HttpClient(MockEngine { request ->
            captured += request
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return IdentityRepositoryImpl(
            client = client,
            config = AppConfig(
                apiBaseUrl = "https://api.example.test",
                campaignBaseUrl = "https://cutthecrap.link/",
                isDebug = false,
            ),
            identityManager = IdentityManager(FakeIdentityKeyStore(), JvmCryptoProvider()),
        )
    }

    private suspend fun bodyText(): String =
        captured.single().body.let { content ->
            (content as io.ktor.http.content.TextContent).text
        }

    @Test
    fun `me returns the user and its devices`() = runTest {
        val result = repository(
            body = """
                {"user_id":"u1","display_name":"Alice","created_at":1753900000,
                 "keys":[{"pubkey":"k1","label":"Pixel","added_at":10,"revoked_at":null},
                         {"pubkey":"k2","label":null,"added_at":20,"revoked_at":30}]}
            """.trimIndent(),
        ).me()

        val profile = assertIs<Result.Success<UserProfile>>(result).data
        assertEquals("https://cutthecrap.link/api/users/me", captured.single().url.toString())
        assertEquals("u1", profile.userId)
        assertEquals(listOf("k1", "k2"), profile.keys.map { it.pubkey })
        assertTrue(profile.keys[0].isActive)
        assertTrue(!profile.keys[1].isActive, "a revoked key must not read as active")
        assertEquals(30L, profile.keys[1].revokedAt)
        assertNull(profile.keys[1].label)
    }

    @Test
    fun `setDisplayName PATCHes the name`() = runTest {
        repository(body = """{"user_id":"u1","display_name":"Bob","created_at":1,"keys":[]}""")
            .setDisplayName("Bob")

        assertEquals(HttpMethod.Patch, captured.single().method)
        assertEquals("https://cutthecrap.link/api/users/me", captured.single().url.toString())
        assertEquals("""{"display_name":"Bob"}""", bodyText())
    }

    @Test
    fun `clearing the name sends an empty string, not null`() = runTest {
        // The server distinguishes "field absent" (400) from "empty" (clear the name); sending
        // null would serialise the field away and read as a malformed request.
        repository(body = """{"user_id":"u1","display_name":null,"created_at":1,"keys":[]}""")
            .setDisplayName(null)

        assertEquals("""{"display_name":""}""", bodyText())
    }

    @Test
    fun `addKey posts the new key and its proof`() = runTest {
        val result = repository(
            status = HttpStatusCode.Created,
            body = """{"pubkey":"k2","label":"Desktop","added_at":20,"revoked_at":null}""",
        ).addKey(pubkey = "k2", proof = "sig", label = "Desktop")

        val key = assertIs<Result.Success<DeviceKey>>(result).data
        assertEquals(HttpMethod.Post, captured.single().method)
        assertEquals("https://cutthecrap.link/api/users/keys", captured.single().url.toString())
        assertEquals("""{"pubkey":"k2","proof":"sig","label":"Desktop"}""", bodyText())
        assertEquals("k2", key.pubkey)
        assertTrue(key.isActive)
    }

    @Test
    fun `revokeKey deletes the key by path`() = runTest {
        val result = repository(
            body = """{"pubkey":"k2","label":"Desktop","added_at":20,"revoked_at":99}""",
        ).revokeKey("k2")

        assertEquals(HttpMethod.Delete, captured.single().method)
        assertEquals("https://cutthecrap.link/api/users/keys/k2", captured.single().url.toString())
        assertEquals(99L, assertIs<Result.Success<DeviceKey>>(result).data.revokedAt)
    }

    @Test
    fun `a base64url key id passes through the path unchanged`() = runTest {
        // base64url is entirely unreserved characters, so percent-encoding must be a no-op —
        // otherwise the signed path and the path the server sees would differ and every device
        // call would 401.
        val keyId = "ebVWLo_mVPlAeLES6KmLp5AfhTrmlb7X4OORC60ElmQ"
        repository(body = """{"pubkey":"$keyId","added_at":1}""").revokeKey(keyId)

        assertEquals("https://cutthecrap.link/api/users/keys/$keyId", captured.single().url.toString())
    }

    @Test
    fun `a conflict surfaces the server's error code`() = runTest {
        val result = repository(
            status = HttpStatusCode.Conflict,
            body = """{"error":"last_key","detail":"..."}""",
        ).revokeKey("k1")

        val error = assertIs<Result.Error>(result)
        assertEquals(AppError.Client(409, "last_key"), error.error)
        assertTrue(!error.retryable, "a 409 will not fix itself on retry")
    }

    @Test
    fun `a rejected proof surfaces as invalid_proof`() = runTest {
        val result = repository(
            status = HttpStatusCode.BadRequest,
            body = """{"error":"invalid_proof","detail":"..."}""",
        ).addKey(pubkey = "k2", proof = "wrong")

        assertEquals(AppError.Client(400, "invalid_proof"), assertIs<Result.Error>(result).error)
    }

    @Test
    fun `an error body that is not ours falls back to the status`() = runTest {
        val result = repository(status = HttpStatusCode.NotFound, body = "<html>nope</html>").me()

        val error = assertIs<Result.Error>(result)
        assertEquals(AppError.Client(404, HttpStatusCode.NotFound.description), error.error)
    }

    @Test
    fun `a revoked key on this device is a 401 that must not be retried`() = runTest {
        val result = repository(
            status = HttpStatusCode.Unauthorized,
            body = """{"error":"key_revoked","detail":"..."}""",
        ).me()

        val error = assertIs<Result.Error>(result)
        assertEquals(AppError.Client(401, "key_revoked"), error.error)
        assertTrue(!error.retryable)
    }
}
