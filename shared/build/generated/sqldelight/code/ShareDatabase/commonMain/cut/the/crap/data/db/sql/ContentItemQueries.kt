package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.ExecutableQuery
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

public class ContentItemQueries(
  driver: SqlDriver,
  private val content_items_tableAdapter: Content_items_table.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getById(id: Int, mapper: (
    id: Int,
    text: String,
    created: Long,
    lastModified: Long,
    sortOrder: Int,
    isFavorite: Boolean,
    category: String?,
    isActive: Boolean,
  ) -> T): Query<T> = GetByIdQuery(id) { cursor ->
    mapper(
      content_items_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!,
      content_items_tableAdapter.sortOrderAdapter.decode(cursor.getLong(4)!!),
      cursor.getBoolean(5)!!,
      cursor.getString(6),
      cursor.getBoolean(7)!!
    )
  }

  public fun getById(id: Int): Query<Content_items_table> = getById(id) { id_, text, created,
      lastModified, sortOrder, isFavorite, category, isActive ->
    Content_items_table(
      id_,
      text,
      created,
      lastModified,
      sortOrder,
      isFavorite,
      category,
      isActive
    )
  }

  public fun <T : Any> getItems(
    includeFavorite: Boolean?,
    startTime: Long?,
    endTime: Long?,
    sortByOrder: Boolean,
    sortByDate: Boolean,
    mapper: (
      id: Int,
      text: String,
      created: Long,
      lastModified: Long,
      sortOrder: Int,
      isFavorite: Boolean,
      category: String?,
      isActive: Boolean,
    ) -> T,
  ): Query<T> = GetItemsQuery(includeFavorite, startTime, endTime, sortByOrder, sortByDate) {
      cursor ->
    mapper(
      content_items_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!,
      content_items_tableAdapter.sortOrderAdapter.decode(cursor.getLong(4)!!),
      cursor.getBoolean(5)!!,
      cursor.getString(6),
      cursor.getBoolean(7)!!
    )
  }

  public fun getItems(
    includeFavorite: Boolean?,
    startTime: Long?,
    endTime: Long?,
    sortByOrder: Boolean,
    sortByDate: Boolean,
  ): Query<Content_items_table> = getItems(includeFavorite, startTime, endTime, sortByOrder,
      sortByDate) { id, text, created, lastModified, sortOrder, isFavorite, category, isActive ->
    Content_items_table(
      id,
      text,
      created,
      lastModified,
      sortOrder,
      isFavorite,
      category,
      isActive
    )
  }

  public fun <T : Any> getActiveItem(mapper: (
    id: Int,
    text: String,
    created: Long,
    lastModified: Long,
    sortOrder: Int,
    isFavorite: Boolean,
    category: String?,
    isActive: Boolean,
  ) -> T): Query<T> = Query(-1_452_760_396, arrayOf("content_items_table"), driver,
      "ContentItem.sq", "getActiveItem",
      "SELECT content_items_table.id, content_items_table.text, content_items_table.created, content_items_table.lastModified, content_items_table.sortOrder, content_items_table.isFavorite, content_items_table.category, content_items_table.isActive FROM content_items_table WHERE isActive = 1 LIMIT 1") {
      cursor ->
    mapper(
      content_items_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!,
      content_items_tableAdapter.sortOrderAdapter.decode(cursor.getLong(4)!!),
      cursor.getBoolean(5)!!,
      cursor.getString(6),
      cursor.getBoolean(7)!!
    )
  }

  public fun getActiveItem(): Query<Content_items_table> = getActiveItem { id, text, created,
      lastModified, sortOrder, isFavorite, category, isActive ->
    Content_items_table(
      id,
      text,
      created,
      lastModified,
      sortOrder,
      isFavorite,
      category,
      isActive
    )
  }

  public fun lastInsertRowId(): ExecutableQuery<Long> = Query(-931_517_653, driver,
      "ContentItem.sq", "lastInsertRowId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  public fun insert(
    text: String,
    created: Long,
    lastModified: Long,
    sortOrder: Int,
    isFavorite: Boolean,
    category: String?,
    isActive: Boolean,
  ) {
    driver.execute(-1_871_016_236, """
        |INSERT OR REPLACE INTO content_items_table(text, created, lastModified, sortOrder, isFavorite, category, isActive)
        |VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 7) {
          bindString(0, text)
          bindLong(1, created)
          bindLong(2, lastModified)
          bindLong(3, content_items_tableAdapter.sortOrderAdapter.encode(sortOrder))
          bindBoolean(4, isFavorite)
          bindString(5, category)
          bindBoolean(6, isActive)
        }
    notifyQueries(-1_871_016_236) { emit ->
      emit("content_items_table")
    }
  }

  public fun insertWithId(
    id: Int?,
    text: String,
    created: Long,
    lastModified: Long,
    sortOrder: Int,
    isFavorite: Boolean,
    category: String?,
    isActive: Boolean,
  ) {
    driver.execute(-353_063_819, """
        |INSERT OR REPLACE INTO content_items_table(id, text, created, lastModified, sortOrder, isFavorite, category, isActive)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindLong(0, id?.let { content_items_tableAdapter.idAdapter.encode(it) })
          bindString(1, text)
          bindLong(2, created)
          bindLong(3, lastModified)
          bindLong(4, content_items_tableAdapter.sortOrderAdapter.encode(sortOrder))
          bindBoolean(5, isFavorite)
          bindString(6, category)
          bindBoolean(7, isActive)
        }
    notifyQueries(-353_063_819) { emit ->
      emit("content_items_table")
    }
  }

  public fun update(
    text: String,
    created: Long,
    lastModified: Long,
    sortOrder: Int,
    isFavorite: Boolean,
    category: String?,
    isActive: Boolean,
    id: Int,
  ) {
    driver.execute(-1_526_070_044, """
        |UPDATE content_items_table
        |SET text = ?, created = ?, lastModified = ?, sortOrder = ?, isFavorite = ?, category = ?, isActive = ?
        |WHERE id = ?
        """.trimMargin(), 8) {
          bindString(0, text)
          bindLong(1, created)
          bindLong(2, lastModified)
          bindLong(3, content_items_tableAdapter.sortOrderAdapter.encode(sortOrder))
          bindBoolean(4, isFavorite)
          bindString(5, category)
          bindBoolean(6, isActive)
          bindLong(7, content_items_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(-1_526_070_044) { emit ->
      emit("content_items_table")
    }
  }

  public fun delete(id: Int) {
    driver.execute(-2_022_682_170, """DELETE FROM content_items_table WHERE id = ?""", 1) {
          bindLong(0, content_items_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(-2_022_682_170) { emit ->
      emit("content_items_table")
      emit("post_subject_cross_ref")
    }
  }

  public fun clearActiveItem() {
    driver.execute(-1_730_153_557,
        """UPDATE content_items_table SET isActive = 0 WHERE isActive = 1""", 0)
    notifyQueries(-1_730_153_557) { emit ->
      emit("content_items_table")
    }
  }

  public fun setActiveItem(id: Int) {
    driver.execute(-678_629_184, """UPDATE content_items_table SET isActive = 1 WHERE id = ?""", 1)
        {
          bindLong(0, content_items_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(-678_629_184) { emit ->
      emit("content_items_table")
    }
  }

  public fun toggleFavorite(isFavorite: Boolean, id: Int) {
    driver.execute(2_113_525_291, """UPDATE content_items_table SET isFavorite = ? WHERE id = ?""",
        2) {
          bindBoolean(0, isFavorite)
          bindLong(1, content_items_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(2_113_525_291) { emit ->
      emit("content_items_table")
    }
  }

  public fun updateSortOrder(sortOrder: Int, id: Int) {
    driver.execute(-1_824_731_188, """UPDATE content_items_table SET sortOrder = ? WHERE id = ?""",
        2) {
          bindLong(0, content_items_tableAdapter.sortOrderAdapter.encode(sortOrder))
          bindLong(1, content_items_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(-1_824_731_188) { emit ->
      emit("content_items_table")
    }
  }

  public fun deleteAll() {
    driver.execute(666_702_331, """DELETE FROM content_items_table""", 0)
    notifyQueries(666_702_331) { emit ->
      emit("content_items_table")
      emit("post_subject_cross_ref")
    }
  }

  private inner class GetByIdQuery<out T : Any>(
    public val id: Int,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("content_items_table", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("content_items_table", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(95_255_437,
        """SELECT content_items_table.id, content_items_table.text, content_items_table.created, content_items_table.lastModified, content_items_table.sortOrder, content_items_table.isFavorite, content_items_table.category, content_items_table.isActive FROM content_items_table WHERE id = ?""",
        mapper, 1) {
      bindLong(0, content_items_tableAdapter.idAdapter.encode(id))
    }

    override fun toString(): String = "ContentItem.sq:getById"
  }

  private inner class GetItemsQuery<out T : Any>(
    public val includeFavorite: Boolean?,
    public val startTime: Long?,
    public val endTime: Long?,
    public val sortByOrder: Boolean,
    public val sortByDate: Boolean,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("content_items_table", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("content_items_table", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT content_items_table.id, content_items_table.text, content_items_table.created, content_items_table.lastModified, content_items_table.sortOrder, content_items_table.isFavorite, content_items_table.category, content_items_table.isActive FROM content_items_table
    |WHERE (? IS NULL OR isFavorite ${ if (includeFavorite == null) "IS" else "=" } ?)
    |  AND (? IS NULL OR created >= ?)
    |  AND (? IS NULL OR created <= ?)
    |ORDER BY
    |  CASE WHEN ? THEN sortOrder END ASC,
    |  CASE WHEN ? THEN created END DESC
    """.trimMargin(), mapper, 8) {
      bindBoolean(0, includeFavorite)
      bindBoolean(1, includeFavorite)
      bindLong(2, startTime)
      bindLong(3, startTime)
      bindLong(4, endTime)
      bindLong(5, endTime)
      bindBoolean(6, sortByOrder)
      bindBoolean(7, sortByDate)
    }

    override fun toString(): String = "ContentItem.sq:getItems"
  }
}
