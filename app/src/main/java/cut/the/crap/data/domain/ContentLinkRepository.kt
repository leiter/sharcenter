package cut.the.crap.data.domain

import cut.the.crap.data.db.ContentLinkDB
import cut.the.crap.data.db.ContentLinkDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

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

    suspend fun importFromFile(file: File)

    suspend fun byTimeRange(
        start: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000, // Default: one week ago
        end: Long = System.currentTimeMillis() // Default: current time
    ): List<ContentLink>
}
class ContentLinkRepositoryImpl @Inject constructor(
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

    override suspend fun importFromFile(file: File) {
        var start = System.currentTimeMillis()
        val content = file.readText().split("\n")
        for (i in content) {
            val item = ContentLinkDB(
                link = i,
                added = start,
            )
            contentLinkDao.insert(item)
            start += 120000L
        }
    }

    override suspend fun byTimeRange(start: Long, end: Long): List<ContentLink> {
        return contentLinkDao.getContentLinksByTimeRange(start, end).map { it.toDomain() }
    }

}