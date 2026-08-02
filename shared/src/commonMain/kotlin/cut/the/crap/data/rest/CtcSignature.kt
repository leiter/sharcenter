package cut.the.crap.data.rest

import cut.the.crap.identity.RequestSigner
import cut.the.crap.platform.CryptoProvider
import cut.the.crap.tools.encodeBase64Url
import cut.the.crap.tools.currentTimeMillis
import cut.the.crap.tools.randomUuid
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent

/**
 * Signs outgoing requests with this install's identity, per `doc/IDENTITY_SPEC.md` §4.
 *
 *     Authorization: CTC-Sig keyid="<b64url>", ts=<unix>, nonce="<uuid>", sig="<b64url>"
 *
 * Installed on the shared [io.ktor.client.HttpClient], which is why no repository has to know that
 * authentication exists at all.
 *
 * Two things make it safe to install unconditionally:
 * - it signs **only** requests to [CtcSignatureConfig.signedHost], so the job-queue backend never
 *   receives an identity header;
 * - with no [CtcSignatureConfig.signer] bound, or with no identity created yet, requests go out
 *   unsigned rather than failing. That is what lets iOS run before it has a [CryptoProvider].
 */
class CtcSignatureConfig {

    /** Supplies key id and signature; null leaves every request unsigned. */
    var signer: RequestSigner? = null

    /** Hashes the canonical string. Same seam as the signer so the plugin needs no crypto of its own. */
    var crypto: CryptoProvider? = null

    /** Only requests to this host are signed. Empty disables signing entirely. */
    var signedHost: String = ""

    /** Overridable so tests can pin the timestamp. */
    var epochSeconds: () -> Long = { currentTimeMillis() / 1000 }

    /** Overridable so tests can force a nonce collision. */
    var nonce: () -> String = ::randomUuid
}

val CtcSignature = createClientPlugin("CtcSignature", ::CtcSignatureConfig) {
    val signer = pluginConfig.signer
    val crypto = pluginConfig.crypto
    val signedHost = pluginConfig.signedHost
    val epochSeconds = pluginConfig.epochSeconds
    val nonceSource = pluginConfig.nonce

    on(Send) { request ->
        // Snapshot the URL: the builder exposes only segment/parameter lists, and the signed path
        // has to be the exact encoded string the server sees.
        val url = request.url.build()
        if (signer == null || crypto == null || signedHost.isEmpty() ||
            !url.host.equals(signedHost, ignoreCase = true)
        ) {
            return@on proceed(request)
        }

        val body = requestBodyBytes(request)
        if (body == null) {
            // A streaming body cannot be hashed without consuming it. Nothing signed sends one
            // today; failing loudly beats shipping a request the server will reject as a bad
            // signature for reasons nobody can see from the outside.
            error("CTC-Sig cannot sign a streaming body (${request.body::class.simpleName}).")
        }

        val timestamp = epochSeconds()
        val nonce = nonceSource()
        val message = signingString(
            method = request.method.value,
            pathAndQuery = url.pathAndQuery(),
            timestamp = timestamp,
            nonce = nonce,
            bodyDigest = crypto.sha256(body).toHex(),
        )

        val signed = signer.signRequest(message.encodeToByteArray())
        if (signed != null) {
            request.headers.append(
                HttpHeaders.Authorization,
                "CTC-Sig keyid=\"${signed.keyId}\", ts=$timestamp, nonce=\"$nonce\", " +
                    "sig=\"${signed.signature.encodeBase64Url()}\"",
            )
        }

        proceed(request)
    }
}

/**
 * The canonical signing string: five lines joined by `\n`, no trailing newline.
 *
 * Internal rather than private so a test can assert the exact bytes — this string is the contract
 * with the server's `utils/ctc_sig.py`, and a stray separator here is invisible until every
 * request 401s.
 */
internal fun signingString(
    method: String,
    pathAndQuery: String,
    timestamp: Long,
    nonce: String,
    bodyDigest: String,
): String = listOf(method.uppercase(), pathAndQuery, timestamp.toString(), nonce, bodyDigest)
    .joinToString("\n")

/**
 * Path plus query, matching what Flask's `request.full_path` / `request.path` produce: the query
 * is appended with `?` only when there is one.
 */
internal fun Url.pathAndQuery(): String =
    if (encodedQuery.isEmpty()) encodedPath else "$encodedPath?$encodedQuery"

/**
 * The already-rendered body, or null when it is a stream that cannot be read without consuming it.
 *
 * The `Send` hook runs after ContentNegotiation, so a JSON body has become `TextContent` — a
 * [OutgoingContent.ByteArrayContent] — by the time it gets here, and a bodyless request is
 * `EmptyContent`, a [OutgoingContent.NoContent].
 */
private fun requestBodyBytes(request: HttpRequestBuilder): ByteArray? =
    when (val body = request.body) {
        is OutgoingContent.ByteArrayContent -> body.bytes()
        is OutgoingContent.NoContent -> ByteArray(0)
        else -> null
    }

private fun ByteArray.toHex(): String = joinToString("") { byte ->
    val value = byte.toInt() and 0xFF
    HEX[value shr 4].toString() + HEX[value and 0x0F]
}

private const val HEX = "0123456789abcdef"
