package cut.the.crap.data.rest.bluesky

import kotlinx.serialization.Serializable

/**
 * Partial models for the public Bluesky AppView response of
 * `app.bsky.feed.getPostThread` (https://public.api.bsky.app). Only the fields the app needs are
 * declared; the Ktor client is configured with `ignoreUnknownKeys`, so the many other fields and
 * the union `$type` variants are dropped safely.
 */
@Serializable
data class BlueskyThreadResponse(
    val thread: BlueskyThreadView? = null
)

@Serializable
data class BlueskyThreadView(
    val post: BlueskyPostView? = null
)

@Serializable
data class BlueskyPostView(
    val author: BlueskyAuthor? = null,
    val record: BlueskyRecord? = null,
    val embed: BlueskyEmbed? = null
)

@Serializable
data class BlueskyAuthor(
    val handle: String? = null,
    val displayName: String? = null,
    val avatar: String? = null
)

@Serializable
data class BlueskyRecord(
    val text: String? = null
)

/**
 * The post's view embed. Shapes covered: `app.bsky.embed.images#view` ([images]),
 * `app.bsky.embed.recordWithMedia#view` ([media]), and `app.bsky.embed.external#view` ([external]).
 */
@Serializable
data class BlueskyEmbed(
    val images: List<BlueskyImage>? = null,
    val media: BlueskyEmbedMedia? = null,
    val external: BlueskyExternal? = null
)

@Serializable
data class BlueskyEmbedMedia(
    val images: List<BlueskyImage>? = null
)

@Serializable
data class BlueskyImage(
    val thumb: String? = null
)

@Serializable
data class BlueskyExternal(
    val thumb: String? = null
)

/** First available image/preview thumbnail across the supported embed shapes, or null. */
fun BlueskyEmbed.firstThumbnail(): String? =
    images?.firstOrNull()?.thumb
        ?: media?.images?.firstOrNull()?.thumb
        ?: external?.thumb

/**
 * Simplified Bluesky post metadata for app usage. [thumbnailUrl] is the post's first image (or link
 * preview) and falls back to the author's avatar so a card always has something to show.
 */
data class BlueskyPostMetadata(
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val thumbnailUrl: String?
) {
    companion object {
        /** Maps a getPostThread response to [BlueskyPostMetadata], or null if it carries no post. */
        fun fromThread(response: BlueskyThreadResponse): BlueskyPostMetadata? {
            val post = response.thread?.post ?: return null
            val author = post.author
            val handle = author?.handle.orEmpty()
            val name = author?.displayName?.takeIf { it.isNotBlank() } ?: handle
            val thumbnail = post.embed?.firstThumbnail() ?: author?.avatar
            return BlueskyPostMetadata(
                authorName = name,
                authorHandle = handle,
                text = post.record?.text.orEmpty(),
                thumbnailUrl = thumbnail?.takeIf { it.isNotBlank() }
            )
        }
    }
}
