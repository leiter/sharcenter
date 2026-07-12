package cut.the.crap.data.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import cut.the.crap.data.db.sql.ContentItemQueries
import cut.the.crap.data.db.sql.ContentLinkQueries
import cut.the.crap.data.db.sql.KeywordQueries
import cut.the.crap.data.db.sql.SubjectQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementations of the DAO contracts in [Entities].
 *
 * Room generated these; SQLDelight generates typed queries instead, so the mapping
 * from row columns to the `*DB` models is explicit here. Behaviour is kept identical
 * to the Room DAOs, including `@Insert(REPLACE)` + `autoGenerate` semantics: an id of
 * 0 means "new row" (the PK is omitted so SQLite assigns it), a non-zero id replaces
 * the row with that id.
 */

class SqlDelightContentLinkDao(
    private val queries: ContentLinkQueries,
    private val dispatcher: CoroutineDispatcher,
) : ContentLinkDao {

    override suspend fun insert(contentLink: ContentLinkDB) = withContext(dispatcher) {
        with(contentLink) {
            if (id == 0) {
                queries.insert(link, added, position, description, favourite, hideItem)
            } else {
                queries.insertWithId(id, link, added, position, description, favourite, hideItem)
            }
        }
    }

    override suspend fun update(contentLink: ContentLinkDB) = withContext(dispatcher) {
        with(contentLink) {
            queries.update(link, added, position, description, favourite, hideItem, id)
        }
    }

    override suspend fun delete(contentLink: ContentLinkDB) = withContext(dispatcher) {
        queries.delete(contentLink.id)
    }

    override suspend fun getContentLinkById(contentLinkId: Int): ContentLinkDB? =
        withContext(dispatcher) {
            queries.getById(contentLinkId, ::ContentLinkDB).executeAsOneOrNull()
        }

    override fun getItems(
        includeFavourite: Boolean?,
        includeHidden: Boolean?,
        linkSubstring: String?,
        sortByListPosition: Boolean,
        sortByDate: Boolean,
        startTime: Long?,
        endTime: Long?,
    ): Flow<List<ContentLinkDB>> =
        queries.getItems(
            includeFavourite = includeFavourite,
            includeHidden = includeHidden,
            linkSubstring = linkSubstring,
            startTime = startTime,
            endTime = endTime,
            sortByListPosition = sortByListPosition,
            sortByDate = sortByDate,
            mapper = ::ContentLinkDB,
        ).asFlow().mapToList(dispatcher)

    override suspend fun getContentLinksByTimeRange(start: Long, end: Long): List<ContentLinkDB> =
        withContext(dispatcher) {
            queries.getByTimeRange(start, end, ::ContentLinkDB).executeAsList()
        }
}

class SqlDelightKeywordDao(
    private val queries: KeywordQueries,
    private val dispatcher: CoroutineDispatcher,
) : KeywordDao {

    override suspend fun insert(keyword: KeywordDB) = withContext(dispatcher) {
        with(keyword) {
            if (id == 0) {
                queries.insert(
                    text, type, created, lastUsed, usageCount,
                    isFavorite, category, color, isArchived, sortOrder,
                )
            } else {
                queries.insertWithId(
                    id, text, type, created, lastUsed, usageCount,
                    isFavorite, category, color, isArchived, sortOrder,
                )
            }
        }
    }

    override suspend fun update(keyword: KeywordDB) = withContext(dispatcher) {
        with(keyword) {
            queries.update(
                text, type, created, lastUsed, usageCount,
                isFavorite, category, color, isArchived, sortOrder, id,
            )
        }
    }

    override suspend fun delete(keyword: KeywordDB) = withContext(dispatcher) {
        queries.delete(keyword.id)
    }

    override suspend fun getById(keywordId: Int): KeywordDB? = withContext(dispatcher) {
        queries.getById(keywordId, ::KeywordDB).executeAsOneOrNull()
    }

    override fun getByType(
        keywordType: Int,
        sortByFavorite: Boolean,
        sortByUsage: Boolean,
        sortByRecent: Boolean,
        sortByManual: Boolean,
    ): Flow<List<KeywordDB>> =
        queries.getByType(
            keywordType = keywordType,
            sortByFavorite = sortByFavorite,
            sortByUsage = sortByUsage,
            sortByRecent = sortByRecent,
            sortByManual = sortByManual,
            mapper = ::KeywordDB,
        ).asFlow().mapToList(dispatcher)

    override fun getAll(): Flow<List<KeywordDB>> =
        queries.getAll(::KeywordDB).asFlow().mapToList(dispatcher)

    override suspend fun incrementUsage(keywordId: Int, timestamp: Long) = withContext(dispatcher) {
        queries.incrementUsage(timestamp, keywordId)
    }

    override suspend fun toggleFavorite(keywordId: Int, isFavorite: Boolean) =
        withContext(dispatcher) {
            queries.toggleFavorite(isFavorite, keywordId)
        }

    override suspend fun setArchived(keywordId: Int, isArchived: Boolean) =
        withContext(dispatcher) {
            queries.setArchived(isArchived, keywordId)
        }

    override suspend fun deleteAllByType(keywordType: Int) = withContext(dispatcher) {
        queries.deleteAllByType(keywordType)
    }
}

class SqlDelightContentItemDao(
    private val queries: ContentItemQueries,
    private val dispatcher: CoroutineDispatcher,
) : ContentItemDao {

    override suspend fun insert(contentItem: ContentItemDB): Long = withContext(dispatcher) {
        with(contentItem) {
            if (id == 0) {
                queries.transactionWithResult {
                    queries.insert(
                        text, created, lastModified, sortOrder, isFavorite, category, isActive,
                    )
                    queries.lastInsertRowId().executeAsOne()
                }
            } else {
                queries.insertWithId(
                    id, text, created, lastModified, sortOrder, isFavorite, category, isActive,
                )
                id.toLong()
            }
        }
    }

    override suspend fun update(contentItem: ContentItemDB) = withContext(dispatcher) {
        with(contentItem) {
            queries.update(
                text, created, lastModified, sortOrder, isFavorite, category, isActive, id,
            )
        }
    }

    override suspend fun delete(contentItem: ContentItemDB) = withContext(dispatcher) {
        queries.delete(contentItem.id)
    }

    override suspend fun getById(contentItemId: Int): ContentItemDB? = withContext(dispatcher) {
        queries.getById(contentItemId, ::ContentItemDB).executeAsOneOrNull()
    }

    override fun getItems(
        includeFavorite: Boolean?,
        sortByOrder: Boolean,
        sortByDate: Boolean,
        startTime: Long?,
        endTime: Long?,
    ): Flow<List<ContentItemDB>> =
        queries.getItems(
            includeFavorite = includeFavorite,
            startTime = startTime,
            endTime = endTime,
            sortByOrder = sortByOrder,
            sortByDate = sortByDate,
            mapper = ::ContentItemDB,
        ).asFlow().mapToList(dispatcher)

    override suspend fun getActiveItem(): ContentItemDB? = withContext(dispatcher) {
        queries.getActiveItem(::ContentItemDB).executeAsOneOrNull()
    }

    override suspend fun clearActiveItem() = withContext(dispatcher) {
        queries.clearActiveItem()
    }

    override suspend fun setActiveItem(contentItemId: Int) = withContext(dispatcher) {
        queries.setActiveItem(contentItemId)
    }

    override suspend fun toggleFavorite(contentItemId: Int, isFavorite: Boolean) =
        withContext(dispatcher) {
            queries.toggleFavorite(isFavorite, contentItemId)
        }

    override suspend fun updateSortOrder(contentItemId: Int, sortOrder: Int) =
        withContext(dispatcher) {
            queries.updateSortOrder(sortOrder, contentItemId)
        }

    override suspend fun deleteAll() = withContext(dispatcher) {
        queries.deleteAll()
    }
}

class SqlDelightSubjectDao(
    private val queries: SubjectQueries,
    private val dispatcher: CoroutineDispatcher,
) : SubjectDao {

    override suspend fun insert(subject: SubjectDB): Long = withContext(dispatcher) {
        with(subject) {
            if (id == 0) {
                queries.transactionWithResult {
                    queries.insert(name, colorHex, createdAt, modifiedAt)
                    queries.lastInsertRowId().executeAsOne()
                }
            } else {
                queries.insertWithId(id, name, colorHex, createdAt, modifiedAt)
                id.toLong()
            }
        }
    }

    override suspend fun update(subject: SubjectDB) = withContext(dispatcher) {
        with(subject) {
            queries.update(name, colorHex, createdAt, modifiedAt, id)
        }
    }

    override suspend fun delete(subject: SubjectDB) = withContext(dispatcher) {
        queries.delete(subject.id)
    }

    override suspend fun getById(subjectId: Int): SubjectDB? = withContext(dispatcher) {
        queries.getById(subjectId, ::SubjectDB).executeAsOneOrNull()
    }

    override fun getAllByRecency(): Flow<List<SubjectDB>> =
        queries.getAllByRecency(::SubjectDB).asFlow().mapToList(dispatcher)

    override fun getAllByName(): Flow<List<SubjectDB>> =
        queries.getAllByName(::SubjectDB).asFlow().mapToList(dispatcher)

    override suspend fun linkPost(ref: PostSubjectCrossRef) = withContext(dispatcher) {
        queries.linkPost(ref.postId, ref.subjectId)
    }

    override suspend fun unlinkPost(ref: PostSubjectCrossRef) = withContext(dispatcher) {
        queries.unlinkPost(ref.postId, ref.subjectId)
    }

    override fun getSubjectsForPost(postId: Int): Flow<List<SubjectDB>> =
        queries.getSubjectsForPost(postId, ::SubjectDB).asFlow().mapToList(dispatcher)

    override suspend fun linkLink(ref: LinkSubjectCrossRef) = withContext(dispatcher) {
        queries.linkLink(ref.linkId, ref.subjectId)
    }

    override suspend fun unlinkLink(ref: LinkSubjectCrossRef) = withContext(dispatcher) {
        queries.unlinkLink(ref.linkId, ref.subjectId)
    }

    override fun getSubjectsForLink(linkId: Int): Flow<List<SubjectDB>> =
        queries.getSubjectsForLink(linkId, ::SubjectDB).asFlow().mapToList(dispatcher)
}
