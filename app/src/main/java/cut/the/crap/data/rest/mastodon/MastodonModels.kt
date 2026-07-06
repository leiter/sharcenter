package cut.the.crap.data.rest.mastodon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Partial models for the public Mastodon status API (`GET /api/v1/statuses/{id}`), which needs no
 * authentication for public posts. Only the fields the app uses are declared; the Ktor client is
 * configured with `ignoreUnknownKeys`, so every other field is dropped safely.
 */
@Serializable
data class MastodonStatus(
    /** Post body as sanitised HTML (paragraphs, links, mentions). */
    val content: String? = null,
    val account: MastodonAccount? = null,
    @SerialName("media_attachments")
    val mediaAttachments: List<MastodonMediaAttachment>? = null
)

@Serializable
data class MastodonAccount(
    val username: String? = null,
    /** Federated handle: "user" for a local account, "user@remote.tld" for a remote one. */
    val acct: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val avatar: String? = null
)

@Serializable
data class MastodonMediaAttachment(
    val type: String? = null,          // "image", "video", "gifv", "audio", …
    @SerialName("preview_url")
    val previewUrl: String? = null,
    val url: String? = null
)

/**
 * Simplified Mastodon post metadata for app usage. [thumbnailUrl] is the first visual attachment's
 * preview and falls back to the author's avatar so a card always has something to show. [text] is
 * the post body converted from HTML to plain text.
 */
data class MastodonPostMetadata(
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val thumbnailUrl: String?
) {
    companion object {
        /** Maps a status response to [MastodonPostMetadata], or null if it carries no account. */
        fun fromStatus(status: MastodonStatus): MastodonPostMetadata? {
            val account = status.account ?: return null
            val handle = account.acct?.takeIf { it.isNotBlank() } ?: account.username.orEmpty()
            val name = account.displayName?.takeIf { it.isNotBlank() } ?: handle
            val mediaThumb = status.mediaAttachments
                ?.firstOrNull { it.type == "image" || it.type == "gifv" || it.type == "video" }
                ?.let { it.previewUrl?.takeIf { url -> url.isNotBlank() } ?: it.url }
            val thumbnail = mediaThumb ?: account.avatar
            return MastodonPostMetadata(
                authorName = name,
                authorHandle = handle,
                text = htmlToPlainText(status.content.orEmpty()),
                thumbnailUrl = thumbnail?.takeIf { it.isNotBlank() }
            )
        }
    }
}

/**
 * Converts Mastodon's sanitised post HTML to plain text: paragraph breaks become blank lines,
 * `<br>` becomes a newline, remaining tags are stripped, and the common HTML entities are decoded.
 * Kept dependency-free (no android.text.Html) so it is unit-testable on the JVM.
 */
internal fun htmlToPlainText(html: String): String {
    if (html.isBlank()) return ""
    var text = html
        .replace(Regex("(?i)</p>\\s*<p>"), "\n\n")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]+>"), "")
    // Decode entities; &amp; is decoded last so it never double-decodes an encoded entity.
    text = text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
    text = Regex("&#(\\d+);").replace(text) { m ->
        m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
    }
    text = text.replace("&amp;", "&")
    return text.trim()
}
