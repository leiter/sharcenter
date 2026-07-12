package cut.the.crap.data.db

import kotlinx.coroutines.flow.Flow

/**
 * Database row models and DAO contracts.
 *
 * These were Room `@Entity`/`@Dao` declarations; the schema now lives in the SQLDelight
 * `.sq` files under `src/main/sqldelight`, and these are plain Kotlin so they can move to
 * `commonMain` unchanged. Signatures are kept identical to the Room DAOs so the
 * repositories and domain mappers are untouched. Implementations: `SqlDelightDaos.kt`.
 */

// --- tweets_table (legacy name kept for backward compatibility) ---
data class ContentLinkDB(
    val id: Int = 0,
    val link: String,
    val added: Long,
    val position: Int = 0,
    val description: String = "",
    val favourite: Boolean = false,
    val hideItem: Boolean = false,
)

interface ContentLinkDao {
    suspend fun insert(contentLink: ContentLinkDB)

    suspend fun update(contentLink: ContentLinkDB)

    suspend fun delete(contentLink: ContentLinkDB)

    suspend fun getContentLinkById(contentLinkId: Int): ContentLinkDB?

    fun getItems(
        includeFavourite: Boolean?,
        includeHidden: Boolean?,
        linkSubstring: String?,
        sortByListPosition: Boolean,
        sortByDate: Boolean,
        startTime: Long?,
        endTime: Long?,
    ): Flow<List<ContentLinkDB>>

    suspend fun getContentLinksByTimeRange(
        start: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
        end: Long = System.currentTimeMillis(),
    ): List<ContentLinkDB>
}

// --- handle_tag_table ---
data class KeywordDB(
    val id: Int = 0,
    val text: String,
    val type: Int, // 0 = account, 1 = hashtag, 2 = tag/word
    val created: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
    val usageCount: Int = 0,
    val isFavorite: Boolean = false,
    val category: String? = null,
    val color: String? = null,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
)

interface KeywordDao {
    suspend fun insert(keyword: KeywordDB)

    suspend fun update(keyword: KeywordDB)

    suspend fun delete(keyword: KeywordDB)

    suspend fun getById(keywordId: Int): KeywordDB?

    fun getByType(
        keywordType: Int,
        sortByFavorite: Boolean = true,
        sortByUsage: Boolean = true,
        sortByRecent: Boolean = true,
        sortByManual: Boolean = false,
    ): Flow<List<KeywordDB>>

    fun getAll(): Flow<List<KeywordDB>>

    suspend fun incrementUsage(keywordId: Int, timestamp: Long = System.currentTimeMillis())

    suspend fun toggleFavorite(keywordId: Int, isFavorite: Boolean)

    suspend fun setArchived(keywordId: Int, isArchived: Boolean)

    suspend fun deleteAllByType(keywordType: Int)
}

// --- content_items_table ---
data class ContentItemDB(
    val id: Int = 0,
    val text: String,
    val created: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false,
    val category: String? = null,
    val isActive: Boolean = false, // true for the currently active/focused item
)

interface ContentItemDao {
    suspend fun insert(contentItem: ContentItemDB): Long

    suspend fun update(contentItem: ContentItemDB)

    suspend fun delete(contentItem: ContentItemDB)

    suspend fun getById(contentItemId: Int): ContentItemDB?

    fun getItems(
        includeFavorite: Boolean? = null,
        sortByOrder: Boolean = true,
        sortByDate: Boolean = false,
        startTime: Long? = null,
        endTime: Long? = null,
    ): Flow<List<ContentItemDB>>

    suspend fun getActiveItem(): ContentItemDB?

    suspend fun clearActiveItem()

    suspend fun setActiveItem(contentItemId: Int)

    suspend fun toggleFavorite(contentItemId: Int, isFavorite: Boolean)

    suspend fun updateSortOrder(contentItemId: Int, sortOrder: Int)

    suspend fun deleteAll()
}

// ---------------------------------------------------------------------------
// Subjects: a colour (optionally named) used to bundle posts and links into
// topics. Items relate to subjects many-to-many via the cross-ref tables below,
// so a single post or link can belong to several subjects at once.
// ---------------------------------------------------------------------------

data class SubjectDB(
    val id: Int = 0,
    // Optional label. Null means the subject is identified by its colour alone.
    val name: String? = null,
    // Six-digit uppercase RRGGBB, matching ColorPicker's Color.toHexString().
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
)

/** Join row linking a post ([ContentItemDB]) to a [SubjectDB]. */
data class PostSubjectCrossRef(
    val postId: Int,
    val subjectId: Int,
)

/** Join row linking a link ([ContentLinkDB]) to a [SubjectDB]. */
data class LinkSubjectCrossRef(
    val linkId: Int,
    val subjectId: Int,
)

interface SubjectDao {
    suspend fun insert(subject: SubjectDB): Long

    suspend fun update(subject: SubjectDB)

    suspend fun delete(subject: SubjectDB)

    suspend fun getById(subjectId: Int): SubjectDB?

    /** All subjects, most-recently-touched first (drives the colour-history preview order). */
    fun getAllByRecency(): Flow<List<SubjectDB>>

    /** All subjects, alphabetically by name (falling back to colour) for management lists. */
    fun getAllByName(): Flow<List<SubjectDB>>

    // --- Post <-> subject links ---

    suspend fun linkPost(ref: PostSubjectCrossRef)

    suspend fun unlinkPost(ref: PostSubjectCrossRef)

    fun getSubjectsForPost(postId: Int): Flow<List<SubjectDB>>

    // --- Link <-> subject links ---

    suspend fun linkLink(ref: LinkSubjectCrossRef)

    suspend fun unlinkLink(ref: LinkSubjectCrossRef)

    fun getSubjectsForLink(linkId: Int): Flow<List<SubjectDB>>
}
