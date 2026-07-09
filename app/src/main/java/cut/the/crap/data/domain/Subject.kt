package cut.the.crap.data.domain

import cut.the.crap.data.db.SubjectDB

/**
 * A subject bundles posts and links under a shared colour (and optional name). Items relate to
 * subjects many-to-many, so a single post or link can belong to several subjects at once.
 *
 * [colorHex] is a six-digit uppercase RRGGBB string, matching the colour picker's
 * `Color.toHexString()`. Kept as a plain string here so the domain stays free of Compose/Android
 * types for the planned KMP migration.
 */
data class Subject(
    val id: Int = 0,
    val name: String? = null,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
)

fun Subject.toDbItem(): SubjectDB = SubjectDB(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
)

fun SubjectDB.toDomain(): Subject = Subject(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
)
