package cut.the.crap.data.domain
import cut.the.crap.tools.currentTimeMillis

import cut.the.crap.data.db.KeywordDB

enum class KeywordType(val value: Int) {
    ACCOUNT(0),
    HASHTAG(1),
    TAG(2);

    companion object {
        fun fromInt(value: Int) = entries.first { it.value == value }
    }
}

data class KeyWord(
    val id: Int = 0,
    val text: String,
    val type: KeywordType,
    val created: Long = currentTimeMillis(),
    val lastUsed: Long = currentTimeMillis(),
    val usageCount: Int = 0,
    val isFavorite: Boolean = false,
    val category: String? = null,
    val color: String? = null,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)

// Mapper functions
fun KeyWord.toDbItem(): KeywordDB {
    return KeywordDB(
        id = id,
        text = text,
        type = type.value,
        created = created,
        lastUsed = lastUsed,
        usageCount = usageCount,
        isFavorite = isFavorite,
        category = category,
        color = color,
        isArchived = isArchived,
        sortOrder = sortOrder
    )
}

fun KeywordDB.toDomain(): KeyWord {
    return KeyWord(
        id = id,
        text = text,
        type = KeywordType.fromInt(type),
        created = created,
        lastUsed = lastUsed,
        usageCount = usageCount,
        isFavorite = isFavorite,
        category = category,
        color = color,
        isArchived = isArchived,
        sortOrder = sortOrder
    )
}
