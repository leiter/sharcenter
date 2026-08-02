package cut.the.crap.data.domain
import cut.the.crap.tools.currentTimeMillis

import cut.the.crap.data.db.ContentLinkDB


const val DELIMITER = "@@x@@"


data class ContentLink(
    val id: Int = -1,
    val link: String = "",
    val description: String = "",
    val added: Long = currentTimeMillis(),
    val position: Int = id,
    val favourite: Boolean = false,
    val hideItem: Boolean = false,
    /** An optional comment/quote the user wrote (or picked from a campaign post) about this link. */
    val comment: String? = null,
)

fun String.toContentLink(): ContentLink {
    val parts = this.split(DELIMITER)
    return ContentLink(
        id = parts[0].toInt(),
        link = parts[1],
        description = parts[2],
        added = parts[3].toLong(),
        position = parts[4].toInt(),
        favourite = parts[5].toInt() > 0,
        hideItem = parts[6].toInt() > 0,
        // Absent in backups exported before the comment field existed.
        comment = parts.getOrNull(7)?.takeIf { it.isNotEmpty() },
    )
}
// Integrate with db version and casting map
fun ContentLink.toLine() : String {
    return "$id$DELIMITER" +
        "$link$DELIMITER" +
        "$description$DELIMITER" +
        "$added$DELIMITER" +
        "$position$DELIMITER" +
        "${if (favourite) 1 else 0}$DELIMITER" +
        "${if (hideItem) 1 else 0}$DELIMITER" +
        comment.orEmpty()
}

// Validate that delimiter doesn't appear in any of the item's fields
fun ContentLink.validateDelimiter(): Boolean {
    return !link.contains(DELIMITER) &&
           !description.contains(DELIMITER) &&
           (comment == null || !comment.contains(DELIMITER))
}

// Validate all items in a list
fun List<ContentLink>.validateDelimiter(): Boolean {
    return all { it.validateDelimiter() }
}

fun ContentLinkDB.toDomain() : ContentLink {
    return ContentLink(
        id,
        link,
        description,
        added,
        position,
        favourite,
        hideItem,
        comment
    )
}
fun ContentLink.toDbItem() : ContentLinkDB {
    return if(id == -1 ) ContentLinkDB(
        link = link,
        added = added,
        description = description,
        favourite = favourite,
        hideItem = hideItem,
        comment = comment
    ) else {
        ContentLinkDB(
            id,
            link,
            added,
            position,
            description,
            favourite,
            hideItem,
            comment
        )
    }
}