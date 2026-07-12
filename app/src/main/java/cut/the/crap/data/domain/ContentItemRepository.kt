package cut.the.crap.data.domain

import cut.the.crap.data.db.ContentItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ContentItemRepository {
    suspend fun insert(contentItem: ContentItem): Long
    suspend fun update(contentItem: ContentItem)
    suspend fun delete(contentItem: ContentItem)
    suspend fun getById(id: Int): ContentItem?
    fun getItems(
        includeFavorite: Boolean? = null,
        sortByOrder: Boolean = true,
        sortByDate: Boolean = false,
        startTime: Long? = null,
        endTime: Long? = null
    ): Flow<List<ContentItem>>
    suspend fun getActiveItem(): ContentItem?
    suspend fun setActiveItem(id: Int)
    suspend fun clearActiveItem()
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)
    suspend fun updateSortOrder(id: Int, sortOrder: Int)
    suspend fun updateSortOrders(items: List<ContentItem>)
    suspend fun deleteAll()
}

class ContentItemRepositoryImpl constructor(
    private val contentItemDao: ContentItemDao
) : ContentItemRepository {

    override suspend fun insert(contentItem: ContentItem): Long {
        return contentItemDao.insert(contentItem.toDbItem())
    }

    override suspend fun update(contentItem: ContentItem) {
        contentItemDao.update(contentItem.toDbItem())
    }

    override suspend fun delete(contentItem: ContentItem) {
        contentItemDao.delete(contentItem.toDbItem())
    }

    override suspend fun getById(id: Int): ContentItem? {
        return contentItemDao.getById(id)?.toDomain()
    }

    override fun getItems(
        includeFavorite: Boolean?,
        sortByOrder: Boolean,
        sortByDate: Boolean,
        startTime: Long?,
        endTime: Long?
    ): Flow<List<ContentItem>> {
        return contentItemDao.getItems(
            includeFavorite = includeFavorite,
            sortByOrder = sortByOrder,
            sortByDate = sortByDate,
            startTime = startTime,
            endTime = endTime
        ).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveItem(): ContentItem? {
        return contentItemDao.getActiveItem()?.toDomain()
    }

    override suspend fun setActiveItem(id: Int) {
        contentItemDao.clearActiveItem()
        contentItemDao.setActiveItem(id)
    }

    override suspend fun clearActiveItem() {
        contentItemDao.clearActiveItem()
    }

    override suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        contentItemDao.toggleFavorite(id, isFavorite)
    }

    override suspend fun updateSortOrder(id: Int, sortOrder: Int) {
        contentItemDao.updateSortOrder(id, sortOrder)
    }

    override suspend fun updateSortOrders(items: List<ContentItem>) {
        items.forEachIndexed { index, item ->
            contentItemDao.updateSortOrder(item.id, index)
        }
    }

    override suspend fun deleteAll() {
        contentItemDao.deleteAll()
    }
}
