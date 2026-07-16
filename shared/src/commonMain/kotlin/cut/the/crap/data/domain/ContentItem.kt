package cut.the.crap.data.domain
import cut.the.crap.tools.currentTimeMillis

import cut.the.crap.data.db.ContentItemDB

data class ContentItem(
    val id: Int = 0,
    val text: String,
    val created: Long = currentTimeMillis(),
    val lastModified: Long = currentTimeMillis(),
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false,
    val category: String? = null,
    val isActive: Boolean = false
)

// Mapper functions
fun ContentItem.toDbItem(): ContentItemDB {
    return ContentItemDB(
        id = id,
        text = text,
        created = created,
        lastModified = lastModified,
        sortOrder = sortOrder,
        isFavorite = isFavorite,
        category = category,
        isActive = isActive
    )
}

fun ContentItemDB.toDomain(): ContentItem {
    return ContentItem(
        id = id,
        text = text,
        created = created,
        lastModified = lastModified,
        sortOrder = sortOrder,
        isFavorite = isFavorite,
        category = category,
        isActive = isActive
    )
}
