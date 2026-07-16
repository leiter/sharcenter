package cut.the.crap.data.domain
import cut.the.crap.tools.currentTimeMillis

import cut.the.crap.data.db.ContentLinkDB
import cut.the.crap.data.db.ContentLinkDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

interface ContentLinkRepository {

    val focusedItem: Flow<ContentLink>

    suspend fun insert(contentLink: ContentLink)

    suspend fun update(contentLink: ContentLink)

    suspend fun delete(contentLink: ContentLink)

    suspend fun getContentLinkById(userId: Int): ContentLink?

    fun getItems(
        includeFavourite: Boolean? = null,
        includeHidden: Boolean? = null,
        linkSubstring: String? = null,
        sortByListPosition: Boolean = false,
        sortByDate: Boolean = true,
        startTime: Long?, // Default: one week ago
        endTime: Long? // Default: current time
    ): Flow<List<ContentLink>>

    suspend fun byTimeRange(
        start: Long = currentTimeMillis() - 7 * 24 * 60 * 60 * 1000, // Default: one week ago
        end: Long = currentTimeMillis() // Default: current time
    ): List<ContentLink>
}
class ContentLinkRepositoryImpl constructor(
    private val contentLinkDao: ContentLinkDao
) : ContentLinkRepository {

    private val internalFocusedItem =  MutableSharedFlow<ContentLink>()
    override val focusedItem: Flow<ContentLink>
        get() = internalFocusedItem

    override suspend fun insert(contentLink: ContentLink) {
        contentLinkDao.insert(contentLink.toDbItem())
    }

    override suspend fun update(contentLink: ContentLink) {
        contentLinkDao.update(contentLink.toDbItem())
    }

    override suspend fun delete(contentLink: ContentLink) {
        contentLinkDao.delete(contentLink.toDbItem())
    }

    override suspend fun getContentLinkById(userId: Int): ContentLink? {
        return contentLinkDao.getContentLinkById(userId)?.toDomain()
    }

    override fun getItems(
        includeFavourite: Boolean?,
        includeHidden: Boolean?,
        linkSubstring: String?,
        sortByListPosition: Boolean,
        sortByDate: Boolean,
        startTime: Long?, // Default: one week ago
        endTime: Long?,// Default: current time
    ): Flow<List<ContentLink>> {
        return contentLinkDao.getItems(
            includeFavourite,
            includeHidden,
            linkSubstring,
            sortByListPosition,
            sortByDate,
            startTime,
            endTime

        ).map { it.map { entity -> entity.toDomain() } }
    }

    override suspend fun byTimeRange(start: Long, end: Long): List<ContentLink> {
        return contentLinkDao.getContentLinksByTimeRange(start, end).map { it.toDomain() }
    }

}