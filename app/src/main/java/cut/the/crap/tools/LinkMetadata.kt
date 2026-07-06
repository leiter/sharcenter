package cut.the.crap.tools

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.YouTubeUrlParser
import cut.the.crap.ui.components.api.ChipsType

/**
 * Typed accessors for platform-specific metadata stored in ContentLink.description.
 *
 * Platform is determined from the URL via SocialMediaParser / YouTubeUrlParser,
 * metadata positions are platform-dependent:
 *
 * YouTube: [channelName, videoTitle, thumbnailUrl, contentType]
 * Bluesky: [authorName, postText, thumbnailUrl, contentType]
 * X/Twitter: [username]
 * Generic: [rawInfo]
 */
object LinkMetadata {

    // --- YouTube accessors (positions 0-3) ---

    fun getChannelName(contentLink: ContentLink): String? {
        if (!YouTubeUrlParser.isYouTubeUrl(contentLink.link)) return null
        return getParsed(contentLink).metadata.getOrNull(0)?.takeIf { it.isNotBlank() }
    }

    fun getVideoTitle(contentLink: ContentLink): String? {
        if (!YouTubeUrlParser.isYouTubeUrl(contentLink.link)) return null
        return getParsed(contentLink).metadata.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    fun getThumbnailUrl(contentLink: ContentLink): String? {
        if (!YouTubeUrlParser.isYouTubeUrl(contentLink.link)) return null
        return getParsed(contentLink).metadata.getOrNull(2)?.takeIf { it.isNotBlank() }
    }

    fun getContentType(contentLink: ContentLink): String? {
        if (!YouTubeUrlParser.isYouTubeUrl(contentLink.link)) return null
        return getParsed(contentLink).metadata.getOrNull(3)?.takeIf { it.isNotBlank() }
    }

    // --- X/Twitter accessors (position 0) ---

    fun getUsername(contentLink: ContentLink): String? {
        val url = contentLink.link.lowercase()
        if (!url.contains("x.com") && !url.contains("twitter.com")) return null
        return getParsed(contentLink).metadata.getOrNull(0)?.takeIf { it.isNotBlank() }
    }

    // --- Bluesky accessors (positions 0-3) ---

    fun getBlueskyAuthor(contentLink: ContentLink): String? {
        if (!isBlueskyUrl(contentLink.link)) return null
        return getParsed(contentLink).metadata.getOrNull(0)?.takeIf { it.isNotBlank() }
    }

    fun getBlueskyText(contentLink: ContentLink): String? {
        if (!isBlueskyUrl(contentLink.link)) return null
        return getParsed(contentLink).metadata.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    fun getBlueskyThumbnailUrl(contentLink: ContentLink): String? {
        if (!isBlueskyUrl(contentLink.link)) return null
        return getParsed(contentLink).metadata.getOrNull(2)?.takeIf { it.isNotBlank() }
    }

    // --- Setter helpers ---

    fun setYouTubeMetadata(
        contentLink: ContentLink,
        channelName: String,
        videoTitle: String,
        thumbnailUrl: String,
        contentType: String,
    ): ContentLink {
        val parsed = getParsed(contentLink)
        val metadataFields = listOf(channelName, videoTitle, thumbnailUrl, contentType)
        val delimiters = DescriptionParser.chooseDelimiters(
            metadataFields, parsed.handles, parsed.hashtags, parsed.keywords
        )
        val safeMetadata = metadataFields.map { DescriptionParser.sanitize(it, delimiters) }
        val newDescription = DescriptionParser.serialize(
            metadata = safeMetadata,
            handles = parsed.handles,
            hashtags = parsed.hashtags,
            keywords = parsed.keywords,
            delimiters = delimiters,
        )
        return contentLink.copy(description = newDescription)
    }

    fun setBlueskyMetadata(
        contentLink: ContentLink,
        authorName: String,
        postText: String,
        thumbnailUrl: String,
        contentType: String,
    ): ContentLink {
        val parsed = getParsed(contentLink)
        val metadataFields = listOf(authorName, postText, thumbnailUrl, contentType)
        val delimiters = DescriptionParser.chooseDelimiters(
            metadataFields, parsed.handles, parsed.hashtags, parsed.keywords
        )
        val safeMetadata = metadataFields.map { DescriptionParser.sanitize(it, delimiters) }
        val newDescription = DescriptionParser.serialize(
            metadata = safeMetadata,
            handles = parsed.handles,
            hashtags = parsed.hashtags,
            keywords = parsed.keywords,
            delimiters = delimiters,
        )
        return contentLink.copy(description = newDescription)
    }

    fun setXMetadata(contentLink: ContentLink, username: String): ContentLink {
        val parsed = getParsed(contentLink)
        val metadataFields = listOf(username)
        val delimiters = DescriptionParser.chooseDelimiters(
            metadataFields, parsed.handles, parsed.hashtags, parsed.keywords
        )
        val safeMetadata = metadataFields.map { DescriptionParser.sanitize(it, delimiters) }
        val newDescription = DescriptionParser.serialize(
            metadata = safeMetadata,
            handles = parsed.handles,
            hashtags = parsed.hashtags,
            keywords = parsed.keywords,
            delimiters = delimiters,
        )
        return contentLink.copy(description = newDescription)
    }

    // --- Tag category accessors ---

    fun getHandles(contentLink: ContentLink): List<String> = getParsed(contentLink).handles
    fun getHashtags(contentLink: ContentLink): List<String> = getParsed(contentLink).hashtags
    fun getKeywords(contentLink: ContentLink): List<String> = getParsed(contentLink).keywords

    fun addTag(contentLink: ContentLink, tag: String, type: ChipsType): ContentLink {
        val parsed = getParsed(contentLink)
        val handles = parsed.handles.toMutableList()
        val hashtags = parsed.hashtags.toMutableList()
        val keywords = parsed.keywords.toMutableList()

        when (type) {
            ChipsType.Handle -> if (tag !in handles) handles.add(tag)
            ChipsType.Tag -> if (tag !in hashtags) hashtags.add(tag)
            ChipsType.KeyWords -> if (tag !in keywords) keywords.add(tag)
            else -> keywords.add(tag)
        }

        val delimiters = DescriptionParser.chooseDelimiters(
            parsed.metadata, handles, hashtags, keywords
        )
        val newDescription = DescriptionParser.serialize(
            metadata = parsed.metadata,
            handles = handles,
            hashtags = hashtags,
            keywords = keywords,
            delimiters = delimiters,
        )
        return contentLink.copy(description = newDescription)
    }

    /**
     * Replaces the entire set of tags of the given [type] with [tags], preserving the
     * link's metadata and the other two tag categories. Entries are trimmed, blanks
     * dropped, and duplicates removed. Returns the link with an updated description
     * (empty string if nothing remains).
     */
    fun setTags(contentLink: ContentLink, tags: List<String>, type: ChipsType): ContentLink {
        val parsed = getParsed(contentLink)
        var handles = parsed.handles
        var hashtags = parsed.hashtags
        var keywords = parsed.keywords

        val cleaned = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        when (type) {
            ChipsType.Handle -> handles = cleaned
            ChipsType.Tag -> hashtags = cleaned
            ChipsType.KeyWords -> keywords = cleaned
            else -> keywords = cleaned
        }

        if (parsed.metadata.isEmpty() && handles.isEmpty() && hashtags.isEmpty() && keywords.isEmpty()) {
            return contentLink.copy(description = "")
        }

        val delimiters = DescriptionParser.chooseDelimiters(
            parsed.metadata, handles, hashtags, keywords
        )
        val newDescription = DescriptionParser.serialize(
            metadata = parsed.metadata,
            handles = handles,
            hashtags = hashtags,
            keywords = keywords,
            delimiters = delimiters,
        )
        return contentLink.copy(description = newDescription)
    }

    fun removeTag(contentLink: ContentLink, tag: String, type: ChipsType): ContentLink {
        val parsed = getParsed(contentLink)
        val handles = parsed.handles.toMutableList()
        val hashtags = parsed.hashtags.toMutableList()
        val keywords = parsed.keywords.toMutableList()

        when (type) {
            ChipsType.Handle -> handles.remove(tag)
            ChipsType.Tag -> hashtags.remove(tag)
            ChipsType.KeyWords -> keywords.remove(tag)
            else -> keywords.remove(tag)
        }

        // If everything is empty, return empty description
        if (parsed.metadata.isEmpty() && handles.isEmpty() && hashtags.isEmpty() && keywords.isEmpty()) {
            return contentLink.copy(description = "")
        }

        val delimiters = DescriptionParser.chooseDelimiters(
            parsed.metadata, handles, hashtags, keywords
        )
        val newDescription = DescriptionParser.serialize(
            metadata = parsed.metadata,
            handles = handles,
            hashtags = hashtags,
            keywords = keywords,
            delimiters = delimiters,
        )
        return contentLink.copy(description = newDescription)
    }

    private fun getParsed(contentLink: ContentLink): DescriptionParser.ParsedDescription {
        return DescriptionParser.parse(contentLink.description)
    }
}
