package cut.the.crap.data.domain

import cut.the.crap.data.db.KeywordDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface KeywordRepository {
    suspend fun insert(keyWord: KeyWord)
    suspend fun update(keyWord: KeyWord)
    suspend fun delete(keyWord: KeyWord)
    suspend fun getById(id: Int): KeyWord?
    fun getByType(
        type: KeywordType,
        sortByFavorite: Boolean = true,
        sortByUsage: Boolean = true,
        sortByRecent: Boolean = true,
        sortByManual: Boolean = false
    ): Flow<List<KeyWord>>
    fun getAll(): Flow<List<KeyWord>>
    suspend fun incrementUsage(id: Int, timestamp: Long = System.currentTimeMillis())
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)
    suspend fun setArchived(id: Int, isArchived: Boolean)
    suspend fun deleteAllByType(type: KeywordType)
    suspend fun importFromList(items: List<String>, type: KeywordType)
}

class KeywordRepositoryImpl constructor(
    private val keywordDao: KeywordDao
) : KeywordRepository {

    override suspend fun insert(keyWord: KeyWord) {
        keywordDao.insert(keyWord.toDbItem())
    }

    override suspend fun update(keyWord: KeyWord) {
        keywordDao.update(keyWord.toDbItem())
    }

    override suspend fun delete(keyWord: KeyWord) {
        keywordDao.delete(keyWord.toDbItem())
    }

    override suspend fun getById(id: Int): KeyWord? {
        return keywordDao.getById(id)?.toDomain()
    }

    override fun getByType(
        type: KeywordType,
        sortByFavorite: Boolean,
        sortByUsage: Boolean,
        sortByRecent: Boolean,
        sortByManual: Boolean
    ): Flow<List<KeyWord>> {
        return keywordDao.getByType(
            keywordType = type.value,
            sortByFavorite = sortByFavorite,
            sortByUsage = sortByUsage,
            sortByRecent = sortByRecent,
            sortByManual = sortByManual
        ).map { list -> list.map { it.toDomain() } }
    }

    override fun getAll(): Flow<List<KeyWord>> {
        return keywordDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun incrementUsage(id: Int, timestamp: Long) {
        keywordDao.incrementUsage(id, timestamp)
    }

    override suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        keywordDao.toggleFavorite(id, isFavorite)
    }

    override suspend fun setArchived(id: Int, isArchived: Boolean) {
        keywordDao.setArchived(id, isArchived)
    }

    override suspend fun deleteAllByType(type: KeywordType) {
        keywordDao.deleteAllByType(type.value)
    }

    override suspend fun importFromList(items: List<String>, type: KeywordType) {
        var sortOrder = 0
        items.forEach { text ->
            val keyWord = KeyWord(
                text = text,
                type = type,
                sortOrder = sortOrder++
            )
            keywordDao.insert(keyWord.toDbItem())
        }
    }
}
