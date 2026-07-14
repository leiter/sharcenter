package cut.the.crap.fake

import cut.the.crap.data.domain.KeyWord
import cut.the.crap.data.domain.KeywordRepository
import cut.the.crap.data.domain.KeywordType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of KeywordRepository for testing.
 * Uses in-memory storage with MutableStateFlow for reactive updates.
 */
class FakeKeywordRepository : KeywordRepository {

    private val items = MutableStateFlow<List<KeyWord>>(emptyList())
    private var nextId = 1

    override suspend fun insert(keyWord: KeyWord) {
        val newItem = if (keyWord.id == 0) {
            keyWord.copy(id = nextId++)
        } else {
            nextId = maxOf(nextId, keyWord.id + 1)
            keyWord
        }
        items.update { it + newItem }
    }

    override suspend fun update(keyWord: KeyWord) {
        items.update { list ->
            list.map { if (it.id == keyWord.id) keyWord else it }
        }
    }

    override suspend fun delete(keyWord: KeyWord) {
        items.update { list ->
            list.filter { it.id != keyWord.id }
        }
    }

    override suspend fun getById(id: Int): KeyWord? {
        return items.value.find { it.id == id }
    }

    override fun getByType(
        type: KeywordType,
        sortByFavorite: Boolean,
        sortByUsage: Boolean,
        sortByRecent: Boolean,
        sortByManual: Boolean
    ): Flow<List<KeyWord>> {
        return items.map { list ->
            var filtered = list.filter { it.type == type && !it.isArchived }

            // Apply sorting
            filtered = when {
                sortByManual -> filtered.sortedBy { it.sortOrder }
                else -> {
                    filtered.sortedWith(
                        compareBy<KeyWord> { !it.isFavorite || !sortByFavorite }
                            .thenByDescending { if (sortByUsage) it.usageCount else 0 }
                            .thenByDescending { if (sortByRecent) it.lastUsed else 0L }
                    )
                }
            }

            filtered
        }
    }

    override fun getAll(): Flow<List<KeyWord>> {
        return items
    }

    override suspend fun incrementUsage(id: Int, timestamp: Long) {
        items.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(usageCount = it.usageCount + 1, lastUsed = timestamp)
                } else it
            }
        }
    }

    override suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        items.update { list ->
            list.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
        }
    }

    override suspend fun setArchived(id: Int, isArchived: Boolean) {
        items.update { list ->
            list.map { if (it.id == id) it.copy(isArchived = isArchived) else it }
        }
    }

    override suspend fun deleteAllByType(type: KeywordType) {
        items.update { list ->
            list.filter { it.type != type }
        }
    }

    override suspend fun importFromList(itemsList: List<String>, type: KeywordType) {
        var sortOrder = 0
        itemsList.forEach { text ->
            insert(KeyWord(text = text, type = type, sortOrder = sortOrder++))
        }
    }

    // Test helpers
    fun setItems(newItems: List<KeyWord>) {
        nextId = (newItems.maxOfOrNull { it.id } ?: 0) + 1
        items.value = newItems
    }

    fun getStoredItems(): List<KeyWord> = items.value

    fun reset() {
        items.value = emptyList()
        nextId = 1
    }
}
