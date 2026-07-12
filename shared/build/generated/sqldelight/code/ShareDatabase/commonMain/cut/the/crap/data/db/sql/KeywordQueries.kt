package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String

public class KeywordQueries(
  driver: SqlDriver,
  private val handle_tag_tableAdapter: Handle_tag_table.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getById(id: Int, mapper: (
    id: Int,
    text: String,
    type: Int,
    created: Long,
    lastUsed: Long,
    usageCount: Int,
    isFavorite: Boolean,
    category: String?,
    color: String?,
    isArchived: Boolean,
    sortOrder: Int,
  ) -> T): Query<T> = GetByIdQuery(id) { cursor ->
    mapper(
      handle_tag_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      handle_tag_tableAdapter.typeAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      handle_tag_tableAdapter.usageCountAdapter.decode(cursor.getLong(5)!!),
      cursor.getBoolean(6)!!,
      cursor.getString(7),
      cursor.getString(8),
      cursor.getBoolean(9)!!,
      handle_tag_tableAdapter.sortOrderAdapter.decode(cursor.getLong(10)!!)
    )
  }

  public fun getById(id: Int): Query<Handle_tag_table> = getById(id) { id_, text, type, created,
      lastUsed, usageCount, isFavorite, category, color, isArchived, sortOrder ->
    Handle_tag_table(
      id_,
      text,
      type,
      created,
      lastUsed,
      usageCount,
      isFavorite,
      category,
      color,
      isArchived,
      sortOrder
    )
  }

  public fun <T : Any> getByType(
    keywordType: Int,
    sortByFavorite: Boolean,
    sortByUsage: Boolean,
    sortByRecent: Boolean,
    sortByManual: Boolean,
    mapper: (
      id: Int,
      text: String,
      type: Int,
      created: Long,
      lastUsed: Long,
      usageCount: Int,
      isFavorite: Boolean,
      category: String?,
      color: String?,
      isArchived: Boolean,
      sortOrder: Int,
    ) -> T,
  ): Query<T> = GetByTypeQuery(keywordType, sortByFavorite, sortByUsage, sortByRecent,
      sortByManual) { cursor ->
    mapper(
      handle_tag_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      handle_tag_tableAdapter.typeAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      handle_tag_tableAdapter.usageCountAdapter.decode(cursor.getLong(5)!!),
      cursor.getBoolean(6)!!,
      cursor.getString(7),
      cursor.getString(8),
      cursor.getBoolean(9)!!,
      handle_tag_tableAdapter.sortOrderAdapter.decode(cursor.getLong(10)!!)
    )
  }

  public fun getByType(
    keywordType: Int,
    sortByFavorite: Boolean,
    sortByUsage: Boolean,
    sortByRecent: Boolean,
    sortByManual: Boolean,
  ): Query<Handle_tag_table> = getByType(keywordType, sortByFavorite, sortByUsage, sortByRecent,
      sortByManual) { id, text, type, created, lastUsed, usageCount, isFavorite, category, color,
      isArchived, sortOrder ->
    Handle_tag_table(
      id,
      text,
      type,
      created,
      lastUsed,
      usageCount,
      isFavorite,
      category,
      color,
      isArchived,
      sortOrder
    )
  }

  public fun <T : Any> getAll(mapper: (
    id: Int,
    text: String,
    type: Int,
    created: Long,
    lastUsed: Long,
    usageCount: Int,
    isFavorite: Boolean,
    category: String?,
    color: String?,
    isArchived: Boolean,
    sortOrder: Int,
  ) -> T): Query<T> = Query(-564_374_141, arrayOf("handle_tag_table"), driver, "Keyword.sq",
      "getAll", """
  |SELECT handle_tag_table.id, handle_tag_table.text, handle_tag_table.type, handle_tag_table.created, handle_tag_table.lastUsed, handle_tag_table.usageCount, handle_tag_table.isFavorite, handle_tag_table.category, handle_tag_table.color, handle_tag_table.isArchived, handle_tag_table.sortOrder FROM handle_tag_table
  |WHERE isArchived = 0
  |ORDER BY
  |  isFavorite DESC,
  |  usageCount DESC,
  |  lastUsed DESC
  """.trimMargin()) { cursor ->
    mapper(
      handle_tag_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      handle_tag_tableAdapter.typeAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      handle_tag_tableAdapter.usageCountAdapter.decode(cursor.getLong(5)!!),
      cursor.getBoolean(6)!!,
      cursor.getString(7),
      cursor.getString(8),
      cursor.getBoolean(9)!!,
      handle_tag_tableAdapter.sortOrderAdapter.decode(cursor.getLong(10)!!)
    )
  }

  public fun getAll(): Query<Handle_tag_table> = getAll { id, text, type, created, lastUsed,
      usageCount, isFavorite, category, color, isArchived, sortOrder ->
    Handle_tag_table(
      id,
      text,
      type,
      created,
      lastUsed,
      usageCount,
      isFavorite,
      category,
      color,
      isArchived,
      sortOrder
    )
  }

  public fun insert(
    text: String,
    type: Int,
    created: Long,
    lastUsed: Long,
    usageCount: Int,
    isFavorite: Boolean,
    category: String?,
    color: String?,
    isArchived: Boolean,
    sortOrder: Int,
  ) {
    driver.execute(-498_799_151, """
        |INSERT OR REPLACE INTO handle_tag_table(text, type, created, lastUsed, usageCount, isFavorite, category, color, isArchived, sortOrder)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 10) {
          bindString(0, text)
          bindLong(1, handle_tag_tableAdapter.typeAdapter.encode(type))
          bindLong(2, created)
          bindLong(3, lastUsed)
          bindLong(4, handle_tag_tableAdapter.usageCountAdapter.encode(usageCount))
          bindBoolean(5, isFavorite)
          bindString(6, category)
          bindString(7, color)
          bindBoolean(8, isArchived)
          bindLong(9, handle_tag_tableAdapter.sortOrderAdapter.encode(sortOrder))
        }
    notifyQueries(-498_799_151) { emit ->
      emit("handle_tag_table")
    }
  }

  public fun insertWithId(
    id: Int?,
    text: String,
    type: Int,
    created: Long,
    lastUsed: Long,
    usageCount: Int,
    isFavorite: Boolean,
    category: String?,
    color: String?,
    isArchived: Boolean,
    sortOrder: Int,
  ) {
    driver.execute(243_866_034, """
        |INSERT OR REPLACE INTO handle_tag_table(id, text, type, created, lastUsed, usageCount, isFavorite, category, color, isArchived, sortOrder)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 11) {
          bindLong(0, id?.let { handle_tag_tableAdapter.idAdapter.encode(it) })
          bindString(1, text)
          bindLong(2, handle_tag_tableAdapter.typeAdapter.encode(type))
          bindLong(3, created)
          bindLong(4, lastUsed)
          bindLong(5, handle_tag_tableAdapter.usageCountAdapter.encode(usageCount))
          bindBoolean(6, isFavorite)
          bindString(7, category)
          bindString(8, color)
          bindBoolean(9, isArchived)
          bindLong(10, handle_tag_tableAdapter.sortOrderAdapter.encode(sortOrder))
        }
    notifyQueries(243_866_034) { emit ->
      emit("handle_tag_table")
    }
  }

  public fun update(
    text: String,
    type: Int,
    created: Long,
    lastUsed: Long,
    usageCount: Int,
    isFavorite: Boolean,
    category: String?,
    color: String?,
    isArchived: Boolean,
    sortOrder: Int,
    id: Int,
  ) {
    driver.execute(-153_852_959, """
        |UPDATE handle_tag_table
        |SET text = ?, type = ?, created = ?, lastUsed = ?, usageCount = ?, isFavorite = ?,
        |    category = ?, color = ?, isArchived = ?, sortOrder = ?
        |WHERE id = ?
        """.trimMargin(), 11) {
          bindString(0, text)
          bindLong(1, handle_tag_tableAdapter.typeAdapter.encode(type))
          bindLong(2, created)
          bindLong(3, lastUsed)
          bindLong(4, handle_tag_tableAdapter.usageCountAdapter.encode(usageCount))
          bindBoolean(5, isFavorite)
          bindString(6, category)
          bindString(7, color)
          bindBoolean(8, isArchived)
          bindLong(9, handle_tag_tableAdapter.sortOrderAdapter.encode(sortOrder))
          bindLong(10, handle_tag_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(-153_852_959) { emit ->
      emit("handle_tag_table")
    }
  }

  public fun delete(id: Int) {
    driver.execute(-650_465_085, """DELETE FROM handle_tag_table WHERE id = ?""", 1) {
          bindLong(0, handle_tag_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(-650_465_085) { emit ->
      emit("handle_tag_table")
    }
  }

  public fun incrementUsage(timestamp: Long, keywordId: Int) {
    driver.execute(-446_078_326, """
        |UPDATE handle_tag_table
        |SET usageCount = usageCount + 1,
        |    lastUsed = ?
        |WHERE id = ?
        """.trimMargin(), 2) {
          bindLong(0, timestamp)
          bindLong(1, handle_tag_tableAdapter.idAdapter.encode(keywordId))
        }
    notifyQueries(-446_078_326) { emit ->
      emit("handle_tag_table")
    }
  }

  public fun toggleFavorite(isFavorite: Boolean, id: Int) {
    driver.execute(237_496_360, """UPDATE handle_tag_table SET isFavorite = ? WHERE id = ?""", 2) {
          bindBoolean(0, isFavorite)
          bindLong(1, handle_tag_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(237_496_360) { emit ->
      emit("handle_tag_table")
    }
  }

  public fun setArchived(isArchived: Boolean, id: Int) {
    driver.execute(-716_645_812, """UPDATE handle_tag_table SET isArchived = ? WHERE id = ?""", 2) {
          bindBoolean(0, isArchived)
          bindLong(1, handle_tag_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(-716_645_812) { emit ->
      emit("handle_tag_table")
    }
  }

  public fun deleteAllByType(type: Int) {
    driver.execute(-1_163_030_609, """DELETE FROM handle_tag_table WHERE type = ?""", 1) {
          bindLong(0, handle_tag_tableAdapter.typeAdapter.encode(type))
        }
    notifyQueries(-1_163_030_609) { emit ->
      emit("handle_tag_table")
    }
  }

  private inner class GetByIdQuery<out T : Any>(
    public val id: Int,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("handle_tag_table", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("handle_tag_table", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-315_687_888,
        """SELECT handle_tag_table.id, handle_tag_table.text, handle_tag_table.type, handle_tag_table.created, handle_tag_table.lastUsed, handle_tag_table.usageCount, handle_tag_table.isFavorite, handle_tag_table.category, handle_tag_table.color, handle_tag_table.isArchived, handle_tag_table.sortOrder FROM handle_tag_table WHERE id = ?""",
        mapper, 1) {
      bindLong(0, handle_tag_tableAdapter.idAdapter.encode(id))
    }

    override fun toString(): String = "Keyword.sq:getById"
  }

  private inner class GetByTypeQuery<out T : Any>(
    public val keywordType: Int,
    public val sortByFavorite: Boolean,
    public val sortByUsage: Boolean,
    public val sortByRecent: Boolean,
    public val sortByManual: Boolean,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("handle_tag_table", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("handle_tag_table", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_566_969_103, """
    |SELECT handle_tag_table.id, handle_tag_table.text, handle_tag_table.type, handle_tag_table.created, handle_tag_table.lastUsed, handle_tag_table.usageCount, handle_tag_table.isFavorite, handle_tag_table.category, handle_tag_table.color, handle_tag_table.isArchived, handle_tag_table.sortOrder FROM handle_tag_table
    |WHERE type = ?
    |  AND isArchived = 0
    |ORDER BY
    |  CASE WHEN ? THEN isFavorite END DESC,
    |  CASE WHEN ? THEN usageCount END DESC,
    |  CASE WHEN ? THEN lastUsed END DESC,
    |  CASE WHEN ? THEN sortOrder END ASC
    """.trimMargin(), mapper, 5) {
      bindLong(0, handle_tag_tableAdapter.typeAdapter.encode(keywordType))
      bindBoolean(1, sortByFavorite)
      bindBoolean(2, sortByUsage)
      bindBoolean(3, sortByRecent)
      bindBoolean(4, sortByManual)
    }

    override fun toString(): String = "Keyword.sq:getByType"
  }
}
