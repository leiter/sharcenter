package cut.the.crap.tools

fun String.ensureTrailingSpace(): String {
    return if (this.isEmpty() || this.endsWith(" ")) {
        this
    } else {
        "$this "
    }
}

const val CONTENT = "content_2025.txt"

// private val domainRegex = Regex("^(?:www\\.)?([^.]+)")
private val tldRegex = Regex("\\.(com|org|net|gov|edu|io|co|uk|de|fr|es|it|us|ru|jp|cn)$")

/**
 * Splits a URL into `scheme`, `authority` and `path`, or returns null if it has no `scheme://`.
 *
 * This replaces `java.net.URL`, which is JVM-only. Ktor's [io.ktor.http.Url] is the obvious
 * substitute and the wrong one: it is deliberately lenient, so it parses the junk that
 * `java.net.URL` rejects — and the whole fallback branch below exists *because* of that
 * rejection. Swapping it in would silently stop the fallback from ever firing.
 *
 * The path deliberately stops at `?` or `#`, which is what `URL.getPath()` does.
 */
private val urlRegex = Regex("^([a-zA-Z][a-zA-Z0-9+.\\-]*)://([^/?#]*)([^?#]*)")

/** A URL's scheme and host, as returned by [urlSchemeAndHost]. */
data class UrlSchemeHost(val scheme: String, val host: String)

/**
 * The `scheme` and `host` of [url], or null when it has no `scheme://host` or the host is not a
 * legal host. [host] is stripped of userinfo and port, exactly as `java.net.URL.getHost()` does.
 *
 * Replaces `java.net.URI`/`java.net.URL` for the two UI callers that need an origin. Same reasoning
 * as [prepareUrlInformation] — Ktor's `Url` is lenient where `java.net.URI` *throws*, and both
 * callers depend on that rejection (they treat "doesn't parse" as "not a link"). Hence [hostRegex]:
 * without it a host containing a space or `<` would be accepted here but rejected by `java.net.URI`,
 * which is exactly the kind of silent divergence a green build would not show.
 */
private val hostRegex = Regex("^[A-Za-z0-9\\-._~%]+$")

fun urlSchemeAndHost(url: String): UrlSchemeHost? {
    val match = urlRegex.find(url) ?: return null
    val host = match.groupValues[2].substringAfterLast('@').substringBefore(':')
    if (!hostRegex.matches(host)) return null
    return UrlSchemeHost(scheme = match.groupValues[1], host = host)
}

fun prepareUrlInformation(url: String): List<String> {

    val match = urlRegex.find(url)
        ?: return listOf(domain(url), firstPathSequence(url), idSequence(url))

    // Strip any userinfo and port, as URL.getHost() does.
    val host = match.groupValues[2].substringAfterLast('@').substringBefore(':')

    // Extract and clean domain
    val domain = host.split(".")
        .dropLastWhile { it.matches(tldRegex) }.dropLast(1) // Remove known TLDs
        .lastOrNull() ?: domain(url)

    // Extract first path token
    val path = match.groupValues[3].trimStart('/')
    val firstPathToken = path.split("/").getOrNull(0)
        ?: firstPathSequence(url)

    // Extract path sequence up to `?`
    val pathUpToQuery = path.substringBefore('?')
        .split("/").lastOrNull() ?: idSequence(url)

    return listOf(domain, firstPathToken, pathUpToQuery)
}

private fun domain(url: String): String {
    val parts = url.split("/")
    if (parts.size>3 && parts[1].isEmpty()){
        val domainSeq = parts[3].split(".")
        if (domainSeq.size>1){
            return domainSeq.dropLast(1).last()
        }
    }
    return "n/a"
}

private fun firstPathSequence(url: String) : String{
    val parts = url.split("/")
    if (parts.size>4 && parts[1].isEmpty()){
        val domainSeq = parts[4]
        if (domainSeq.isNotEmpty()){
            return domainSeq
        }
    }
    return "n/a"
}
private fun idSequence(url: String) : String{
    val parts = url.split("/")
    if (parts.size>6 && parts[1].isEmpty()){
        val idSeq = parts[6]
        if (idSeq.isNotEmpty()){
            return idSeq
        }
    }
    return "n/a"
}



