package cut.the.crap.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        ContentLinkDB::class,
        KeywordDB::class,
        ContentItemDB::class,
        SubjectDB::class,
        PostSubjectCrossRef::class,
        LinkSubjectCrossRef::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentLinkDao(): ContentLinkDao
    abstract fun keywordDao(): KeywordDao
    abstract fun contentItemDao(): ContentItemDao
    abstract fun subjectDao(): SubjectDao
}

@Entity(tableName = "tweets_table")  // Keep old table name for backward compatibility
data class ContentLinkDB(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val link: String,
    val added: Long,
    val position: Int = 0,
    val description: String = "",
    val favourite: Boolean = false,
    val hideItem: Boolean = false,
)

@Entity(tableName = "prepared_tweets_table")  // Keep old table name for backward compatibility
data class DraftPostDB(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val added: Long,
    val position: Int = id,
    val message: String = "",
    val favourite: Boolean = false,
    val hideItem: Boolean = false,
)


@Dao
interface ContentLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contentLink: ContentLinkDB)

    @Update
    suspend fun update(contentLink: ContentLinkDB)

    @Delete
    suspend fun delete(contentLink: ContentLinkDB)

    @Query("SELECT * FROM tweets_table WHERE id = :contentLinkId")
    suspend fun getContentLinkById(contentLinkId: Int): ContentLinkDB?

    @Query("""
        SELECT * FROM tweets_table
        WHERE (:includeFavourite IS NULL OR favourite = :includeFavourite)
          AND (:includeHidden IS NULL OR hideItem = :includeHidden)
          AND (:linkSubstring IS NULL OR link LIKE '%' || :linkSubstring || '%')
          AND (:startTime IS NULL OR added >= :startTime)
          AND (:endTime IS NULL OR added <= :endTime)

        ORDER BY
          CASE WHEN :sortByListPosition THEN position END ASC,
          CASE WHEN :sortByDate THEN added END DESC
    """)  //

    fun getItems(
        includeFavourite: Boolean?,
        includeHidden: Boolean?,
        linkSubstring: String?,
        sortByListPosition: Boolean,
        sortByDate: Boolean,
        startTime: Long?,
        endTime: Long?
    ): Flow<List<ContentLinkDB>>

    @Query("""
        SELECT * FROM tweets_table
        WHERE added BETWEEN :start AND :end
        ORDER BY added DESC
    """)
    suspend fun getContentLinksByTimeRange(
        start: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000, // Default: one week ago
        end: Long = System.currentTimeMillis() // Default: current time
    ): List<ContentLinkDB>
}

@Entity(
    tableName = "handle_tag_table",
    indices = [
        Index(value = ["type"]),
        Index(value = ["lastUsed"]),
        Index(value = ["text", "type"], unique = true)
    ]
)
data class KeywordDB(
    @PrimaryKey(autoGenerate = true)
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
    val sortOrder: Int = 0
)

@Dao
interface KeywordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keyword: KeywordDB)

    @Update
    suspend fun update(keyword: KeywordDB)

    @Delete
    suspend fun delete(keyword: KeywordDB)

    @Query("SELECT * FROM handle_tag_table WHERE id = :keywordId")
    suspend fun getById(keywordId: Int): KeywordDB?

    @Query("""
        SELECT * FROM handle_tag_table
        WHERE type = :keywordType
          AND isArchived = 0
        ORDER BY
          CASE WHEN :sortByFavorite THEN isFavorite END DESC,
          CASE WHEN :sortByUsage THEN usageCount END DESC,
          CASE WHEN :sortByRecent THEN lastUsed END DESC,
          CASE WHEN :sortByManual THEN sortOrder END ASC
    """)
    fun getByType(
        keywordType: Int,
        sortByFavorite: Boolean = true,
        sortByUsage: Boolean = true,
        sortByRecent: Boolean = true,
        sortByManual: Boolean = false
    ): Flow<List<KeywordDB>>

    @Query("""
        SELECT * FROM handle_tag_table
        WHERE isArchived = 0
        ORDER BY
          isFavorite DESC,
          usageCount DESC,
          lastUsed DESC
    """)
    fun getAll(): Flow<List<KeywordDB>>

    @Query("""
        UPDATE handle_tag_table
        SET usageCount = usageCount + 1,
            lastUsed = :timestamp
        WHERE id = :keywordId
    """)
    suspend fun incrementUsage(keywordId: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE handle_tag_table SET isFavorite = :isFavorite WHERE id = :keywordId")
    suspend fun toggleFavorite(keywordId: Int, isFavorite: Boolean)

    @Query("UPDATE handle_tag_table SET isArchived = :isArchived WHERE id = :keywordId")
    suspend fun setArchived(keywordId: Int, isArchived: Boolean)

    @Query("DELETE FROM handle_tag_table WHERE type = :keywordType")
    suspend fun deleteAllByType(keywordType: Int)
}

@Entity(
    tableName = "content_items_table",
    indices = [
        Index(value = ["created"]),
        Index(value = ["lastModified"]),
        Index(value = ["sortOrder"])
    ]
)
data class ContentItemDB(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val created: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false,
    val category: String? = null,
    val isActive: Boolean = false  // true for the currently active/focused item
)

@Dao
interface ContentItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contentItem: ContentItemDB): Long

    @Update
    suspend fun update(contentItem: ContentItemDB)

    @Delete
    suspend fun delete(contentItem: ContentItemDB)

    @Query("SELECT * FROM content_items_table WHERE id = :contentItemId")
    suspend fun getById(contentItemId: Int): ContentItemDB?

    @Query("""
        SELECT * FROM content_items_table
        WHERE (:includeFavorite IS NULL OR isFavorite = :includeFavorite)
          AND (:startTime IS NULL OR created >= :startTime)
          AND (:endTime IS NULL OR created <= :endTime)
        ORDER BY
          CASE WHEN :sortByOrder THEN sortOrder END ASC,
          CASE WHEN :sortByDate THEN created END DESC
    """)
    fun getItems(
        includeFavorite: Boolean? = null,
        sortByOrder: Boolean = true,
        sortByDate: Boolean = false,
        startTime: Long? = null,
        endTime: Long? = null
    ): Flow<List<ContentItemDB>>

    @Query("SELECT * FROM content_items_table WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveItem(): ContentItemDB?

    @Query("UPDATE content_items_table SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActiveItem()

    @Query("UPDATE content_items_table SET isActive = 1 WHERE id = :contentItemId")
    suspend fun setActiveItem(contentItemId: Int)

    @Query("UPDATE content_items_table SET isFavorite = :isFavorite WHERE id = :contentItemId")
    suspend fun toggleFavorite(contentItemId: Int, isFavorite: Boolean)

    @Query("UPDATE content_items_table SET sortOrder = :sortOrder WHERE id = :contentItemId")
    suspend fun updateSortOrder(contentItemId: Int, sortOrder: Int)

    @Query("DELETE FROM content_items_table")
    suspend fun deleteAll()
}

// ---------------------------------------------------------------------------
// Subjects: a colour (optionally named) used to bundle posts and links into
// topics. Items relate to subjects many-to-many via the cross-ref tables below,
// so a single post or link can belong to several subjects at once.
// ---------------------------------------------------------------------------

@Entity(
    tableName = "subjects_table",
    indices = [Index(value = ["modifiedAt"])]
)
data class SubjectDB(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // Optional label. Null means the subject is identified by its colour alone.
    val name: String? = null,
    // Six-digit uppercase RRGGBB, matching ColorPicker's Color.toHexString().
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
)

/** Join row linking a post ([ContentItemDB]) to a [SubjectDB]. */
@Entity(
    tableName = "post_subject_cross_ref",
    primaryKeys = ["postId", "subjectId"],
    foreignKeys = [
        ForeignKey(
            entity = ContentItemDB::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SubjectDB::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["subjectId"])]
)
data class PostSubjectCrossRef(
    val postId: Int,
    val subjectId: Int,
)

/** Join row linking a link ([ContentLinkDB]) to a [SubjectDB]. */
@Entity(
    tableName = "link_subject_cross_ref",
    primaryKeys = ["linkId", "subjectId"],
    foreignKeys = [
        ForeignKey(
            entity = ContentLinkDB::class,
            parentColumns = ["id"],
            childColumns = ["linkId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SubjectDB::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["subjectId"])]
)
data class LinkSubjectCrossRef(
    val linkId: Int,
    val subjectId: Int,
)

@Dao
interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: SubjectDB): Long

    @Update
    suspend fun update(subject: SubjectDB)

    @Delete
    suspend fun delete(subject: SubjectDB)

    @Query("SELECT * FROM subjects_table WHERE id = :subjectId")
    suspend fun getById(subjectId: Int): SubjectDB?

    /** All subjects, most-recently-touched first (drives the colour-history preview order). */
    @Query("SELECT * FROM subjects_table ORDER BY modifiedAt DESC")
    fun getAllByRecency(): Flow<List<SubjectDB>>

    /** All subjects, alphabetically by name (falling back to colour) for management lists. */
    @Query("SELECT * FROM subjects_table ORDER BY COALESCE(name, colorHex) COLLATE NOCASE ASC")
    fun getAllByName(): Flow<List<SubjectDB>>

    // --- Post <-> subject links ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkPost(ref: PostSubjectCrossRef)

    @Delete
    suspend fun unlinkPost(ref: PostSubjectCrossRef)

    @Query("""
        SELECT s.* FROM subjects_table s
        INNER JOIN post_subject_cross_ref x ON s.id = x.subjectId
        WHERE x.postId = :postId
        ORDER BY s.modifiedAt DESC
    """)
    fun getSubjectsForPost(postId: Int): Flow<List<SubjectDB>>

    // --- Link <-> subject links ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkLink(ref: LinkSubjectCrossRef)

    @Delete
    suspend fun unlinkLink(ref: LinkSubjectCrossRef)

    @Query("""
        SELECT s.* FROM subjects_table s
        INNER JOIN link_subject_cross_ref x ON s.id = x.subjectId
        WHERE x.linkId = :linkId
        ORDER BY s.modifiedAt DESC
    """)
    fun getSubjectsForLink(linkId: Int): Flow<List<SubjectDB>>
}

