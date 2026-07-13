package cut.the.crap.tools

import java.net.MalformedURLException
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

fun prepareUrlInformation(url: String): List<String> {

    return try {
        val parsedUrl = URL(url)

        // Extract and clean domain
        val domainWithSubdomain = parsedUrl.host
        val domain = domainWithSubdomain.split(".")
            .dropLastWhile { it.matches(tldRegex) }.dropLast(1) // Remove known TLDs
            .lastOrNull() ?: domain(url)

        // Extract first path token
        val path = parsedUrl.path.trimStart('/')
        val firstPathToken = path.split("/").getOrNull(0)
            ?: firstPathSequence(url)

        // Extract path sequence up to `?`
        val pathUpToQuery = path.substringBefore('?')
            .split("/").lastOrNull() ?: idSequence(url)
        listOf(domain, firstPathToken, pathUpToQuery)

    } catch (e: MalformedURLException) {
        listOf(domain(url), firstPathSequence(url), idSequence(url))
    }
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

fun formatTimestampWithLocalizedFormatter(
    timestamp: Long,
    locale: Locale = Locale.getDefault()
): String {

    val localDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )
    val formatter = DateTimeFormatter
        .ofPattern("dd.MM.yy, HH:mm" )//ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)

    return localDateTime.format(formatter)
}

fun formatDateOnly(
    timestamp: Long,
    locale: Locale = Locale.getDefault()
): String {

    val localDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )
    val formatter = DateTimeFormatter
        .ofPattern("dd.MM.yy")
        .withLocale(locale)

    return localDateTime.format(formatter)
}

