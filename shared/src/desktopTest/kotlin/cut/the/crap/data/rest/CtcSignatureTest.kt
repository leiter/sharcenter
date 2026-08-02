package cut.the.crap.data.rest

import cut.the.crap.fake.FakeIdentityKeyStore
import cut.the.crap.identity.IdentityManager
import cut.the.crap.platform.JvmCryptoProvider
import cut.the.crap.tools.decodeBase64Url
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The `CTC-Sig` request signing plugin.
 *
 * The fixed vectors are the important part: they are the *same constants* asserted by the server's
 * `test_identity.py`, computed once from the protocol definition. The canonical string is a
 * contract between two codebases in two languages, and a stray separator or a differently encoded
 * query is invisible until every request 401s in production. If one side's canonicalisation drifts,
 * one of the two suites goes red.
 */
class CtcSignatureTest {

    private val crypto = JvmCryptoProvider()

    private lateinit var captured: MutableList<HttpRequestData>

    private fun client(
        seed: ByteArray? = FIXED_SEED,
        signedHost: String = "cutthecrap.link",
    ): HttpClient {
        captured = mutableListOf()
        val manager = IdentityManager(FakeIdentityKeyStore(seed), crypto)
        return HttpClient(MockEngine { request ->
            captured += request
            respond(
                content = """{"user_id":"u1"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(CtcSignature) {
                signer = manager
                this.crypto = this@CtcSignatureTest.crypto
                this.signedHost = signedHost
                epochSeconds = { FIXED_TS }
                nonce = { FIXED_NONCE }
            }
        }
    }

    private fun authorization(): String? =
        captured.single().headers[HttpHeaders.Authorization]

    // --- Fixed vectors, shared with the server suite ------------------------

    @Test
    fun `signs a GET exactly as the server expects`() = runTest {
        client().get("https://cutthecrap.link/api/ping")

        assertEquals(
            "CTC-Sig keyid=\"$FIXED_PUBKEY\", ts=$FIXED_TS, nonce=\"$FIXED_NONCE\", " +
                "sig=\"p7iCa44Q87StU2fbaIhXGvB0yOVCeIbT7FbUfw5hqrZWU2xZ6-RfyYU7tPWqAgr3UugmnJuw7ETU6icSP9YLCw\"",
            authorization(),
        )
    }

    @Test
    fun `signs a POST body exactly as the server expects`() = runTest {
        client().post("https://cutthecrap.link/api/users") {
            contentType(ContentType.Application.Json)
            setBody(Register("Alice"))
        }

        val header = authorization()
        assertTrue(
            header!!.contains(
                "sig=\"xrHf68RLVjoXUu1qvbspDGfJD0_Q1Jp9I5CpMR_ZD8T1qfzRlXiwYvj_akt7zER9JXcf9uthEcEG6HPT901kCQ\""
            ),
            "body digest or canonicalisation drifted; header was $header",
        )
    }

    @Test
    fun `includes the query string, matching Flask's full_path`() = runTest {
        client().get("https://cutthecrap.link/api/campaigns/mine?since=7")

        assertTrue(
            authorization()!!.contains(
                "sig=\"igcJQmkoqVbDsFSgEEJdq8YcgbFeMNchCbF060Sn65cPY2pjctkiUe9chnZPBgF7T-ieEcMr-qyQK_oKwh2KDw\""
            ),
            "the query must be part of the signed path",
        )
    }

    @Test
    fun `the canonical string is five lines with no trailing newline`() {
        val message = signingString("get", "/api/ping", 1L, "n", "d")
        assertEquals("GET\n/api/ping\n1\nn\nd", message)
        assertEquals(5, message.split("\n").size)
    }

    // --- Behaviour ----------------------------------------------------------

    @Test
    fun `the signature verifies against this install's public key`() = runTest {
        client().get("https://cutthecrap.link/api/ping")

        val header = authorization()!!
        val keyId = header.substringAfter("keyid=\"").substringBefore('"')
        val signature = header.substringAfter("sig=\"").substringBefore('"')
        val message = signingString(
            "GET", "/api/ping", FIXED_TS, FIXED_NONCE, crypto.sha256(ByteArray(0)).toHex(),
        )

        assertEquals(FIXED_PUBKEY, keyId)
        assertTrue(
            crypto.ed25519Verify(
                publicKey = keyId.b64UrlToBytes(),
                message = message.toByteArray(),
                signature = signature.b64UrlToBytes(),
            )
        )
    }

    @Test
    fun `does not sign requests to another host`() = runTest {
        // The job-queue backend must never receive an identity header.
        client().get("https://api.example.test/jobs")
        assertNull(authorization())
    }

    @Test
    fun `does not sign when this install has no identity yet`() = runTest {
        client(seed = null).get("https://cutthecrap.link/api/ping")
        assertNull(authorization())
    }

    @Test
    fun `does not sign when no host is configured`() = runTest {
        client(signedHost = "").get("https://cutthecrap.link/api/ping")
        assertNull(authorization())
    }

    @Test
    fun `host matching ignores case`() = runTest {
        client(signedHost = "CutTheCrap.Link").get("https://cutthecrap.link/api/ping")
        assertTrue(authorization()!!.startsWith("CTC-Sig "))
    }

    @Serializable
    private data class Register(val display_name: String)

    private companion object {
        /** Seed 01..20 — the same one the server suite uses. */
        val FIXED_SEED = ByteArray(32) { (it + 1).toByte() }
        const val FIXED_PUBKEY = "ebVWLo_mVPlAeLES6KmLp5AfhTrmlb7X4OORC60ElmQ"
        const val FIXED_TS = 1753900000L
        const val FIXED_NONCE = "00000000-0000-4000-8000-000000000000"
    }
}

private fun ByteArray.toHex(): String = joinToString("") { byte ->
    val value = byte.toInt() and 0xFF
    "0123456789abcdef"[value shr 4].toString() + "0123456789abcdef"[value and 0x0F]
}

private fun String.b64UrlToBytes(): ByteArray = decodeBase64Url()
