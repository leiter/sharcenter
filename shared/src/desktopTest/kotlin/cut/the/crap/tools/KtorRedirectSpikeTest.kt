package cut.the.crap.tools

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * WP5 spike: can Ktor replace the two OkHttp clients in `UrlResolver`?
 *
 * `UrlResolver` is the riskiest thing left in the migration because it does not just *make* HTTP
 * calls — it drives redirects by hand. It needs exactly two capabilities, and an engine
 * abstraction is precisely the sort of thing that hides them:
 *
 *  1. Follow redirects, then tell me the **final** URL (`response.request.url` on OkHttp).
 *  2. **Don't** follow redirects, so I can read the `Location` header myself
 *     (OkHttp's second, `followRedirects(false)` client).
 *
 * These tests answer both against the real Ktor client (MockEngine only supplies the responses;
 * redirect handling is the `HttpRedirect` plugin under test).
 */
class KtorRedirectSpikeTest {

    private val shortened = "https://x.com/i/status/12345"
    private val canonical = "https://x.com/realuser/status/12345"

    /** Answers capability (1): the final URL is reachable after Ktor follows the redirect. */
    @Test
    fun `following redirects exposes the final url`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.toString()) {
                shortened -> respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, canonical),
                )
                else -> respondOk("landed")
            }
        }
        // followRedirects defaults to true; stated explicitly because it is the point of the test.
        val client = HttpClient(engine) { followRedirects = true }

        val response: HttpResponse = client.get(shortened)

        assertEquals(HttpStatusCode.OK, response.status)
        // This is the OkHttp `response.request.url` equivalent, and it is what UrlResolver needs.
        assertEquals(canonical, response.request.url.toString())
    }

    /** Answers capability (2): the 3xx and its Location header survive when redirects are off. */
    @Test
    fun `disabling redirects surfaces the location header`() = runTest {
        val engine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, canonical),
            )
        }
        val client = HttpClient(engine) { followRedirects = false }

        val response = client.get(shortened)

        // The redirect is handed back intact rather than swallowed — the vxtwitter/fxtwitter
        // proxy path depends on exactly this.
        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals(canonical, response.headers[HttpHeaders.Location])
    }

    @Test
    fun `a non-redirecting url reports itself as the final url`() = runTest {
        val engine = MockEngine { respondOk("fine") }
        val client = HttpClient(engine) { followRedirects = true }

        val response = client.get(canonical)

        assertEquals(canonical, response.request.url.toString())
    }

    @Test
    fun `redirect chains are followed to the end`() = runTest {
        // UrlResolver assumes a chain can be more than one hop deep.
        val hop = "https://t.co/abc"
        val engine = MockEngine { request ->
            when (request.url.toString()) {
                shortened -> respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, hop))
                hop -> respond("", HttpStatusCode.MovedPermanently, headersOf(HttpHeaders.Location, canonical))
                else -> respondOk("landed")
            }
        }
        val client = HttpClient(engine) { followRedirects = true }

        val response = client.get(shortened)

        assertEquals(canonical, response.request.url.toString())
    }

    @Test
    fun `a response with no location header yields null rather than throwing`() = runTest {
        val engine = MockEngine { respondOk("no redirect here") }
        val client = HttpClient(engine) { followRedirects = false }

        assertNull(client.get(canonical).headers[HttpHeaders.Location])
    }
}
