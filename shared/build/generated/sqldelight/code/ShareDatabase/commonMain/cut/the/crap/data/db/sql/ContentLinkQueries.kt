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

public class ContentLinkQueries(
  driver: SqlDriver,
  private val tweets_tableAdapter: Tweets_table.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getById(id: Int, mapper: (
    id: Int,
    link: String,
    added: Long,
    position: Int,
    description: String,
    favourite: Boolean,
    hideItem: Boolean,
  ) -> T): Query<T> = GetByIdQuery(id) { cursor ->
    mapper(
      tweets_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      tweets_tableAdapter.positionAdapter.decode(cursor.getLong(3)!!),
      cursor.getString(4)!!,
      cursor.getBoolean(5)!!,
      cursor.getBoolean(6)!!
    )
  }

  public fun getById(id: Int): Query<Tweets_table> = getById(id) { id_, link, added, position,
      description, favourite, hideItem ->
    Tweets_table(
      id_,
      link,
      added,
      position,
      description,
      favourite,
      hideItem
    )
  }

  public fun <T : Any> getItems(
    includeFavourite: Boolean?,
    includeHidden: Boolean?,
    linkSubstring: String?,
    startTime: Long?,
    endTime: Long?,
    sortByListPosition: Boolean,
    sortByDate: Boolean,
    mapper: (
      id: Int,
      link: String,
      added: Long,
      position: Int,
      description: String,
      favourite: Boolean,
      hideItem: Boolean,
    ) -> T,
  ): Query<T> = GetItemsQuery(includeFavourite, includeHidden, linkSubstring, startTime, endTime,
      sortByListPosition, sortByDate) { cursor ->
    mapper(
      tweets_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      tweets_tableAdapter.positionAdapter.decode(cursor.getLong(3)!!),
      cursor.getString(4)!!,
      cursor.getBoolean(5)!!,
      cursor.getBoolean(6)!!
    )
  }

  public fun getItems(
    includeFavourite: Boolean?,
    includeHidden: Boolean?,
    linkSubstring: String?,
    startTime: Long?,
    endTime: Long?,
    sortByListPosition: Boolean,
    sortByDate: Boolean,
  ): Query<Tweets_table> = getItems(includeFavourite, includeHidden, linkSubstring, startTime,
      endTime, sortByListPosition, sortByDate) { id, link, added, position, description, favourite,
      hideItem ->
    Tweets_table(
      id,
      link,
      added,
      position,
      description,
      favourite,
      hideItem
    )
  }

  public fun <T : Any> getByTimeRange(
    start: Long,
    end: Long,
    mapper: (
      id: Int,
      link: String,
      added: Long,
      position: Int,
      description: String,
      favourite: Boolean,
      hideItem: Boolean,
    ) -> T,
  ): Query<T> = GetByTimeRangeQuery(start, end) { cursor ->
    mapper(
      tweets_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      tweets_tableAdapter.positionAdapter.decode(cursor.getLong(3)!!),
      cursor.getString(4)!!,
      cursor.getBoolean(5)!!,
      cursor.getBoolean(6)!!
    )
  }

  public fun getByTimeRange(start: Long, end: Long): Query<Tweets_table> = getByTimeRange(start,
      end) { id, link, added, position, description, favourite, hideItem ->
    Tweets_table(
      id,
      link,
      added,
      position,
      description,
      favourite,
      hideItem
    )
  }

  public fun insert(
    link: String,
    added: Long,
    position: Int,
    description: String,
    favourite: Boolean,
    hideItem: Boolean,
  ) {
    driver.execute(300_420_859, """
        |INSERT OR REPLACE INTO tweets_table(link, added, position, description, favourite, hideItem)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindString(0, link)
          bindLong(1, added)
          bindLong(2, tweets_tableAdapter.positionAdapter.encode(position))
          bindString(3, description)
          bindBoolean(4, favourite)
          bindBoolean(5, hideItem)
        }
    notifyQueries(300_420_859) { emit ->
      emit("tweets_table")
    }
  }

  public fun insertWithId(
    id: Int?,
    link: String,
    added: Long,
    position: Int,
    description: String,
    favourite: Boolean,
    hideItem: Boolean,
  ) {
    driver.execute(324_350_812, """
        |INSERT OR REPLACE INTO tweets_table(id, link, added, position, description, favourite, hideItem)
        |VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 7) {
          bindLong(0, id?.let { tweets_tableAdapter.idAdapter.encode(it) })
          bindString(1, link)
          bindLong(2, added)
          bindLong(3, tweets_tableAdapter.positionAdapter.encode(position))
          bindString(4, description)
          bindBoolean(5, favourite)
          bindBoolean(6, hideItem)
        }
    notifyQueries(324_350_812) { emit ->
      emit("tweets_table")
    }
  }

  public fun update(
    link: String,
    added: Long,
    position: Int,
    description: String,
    favourite: Boolean,
    hideItem: Boolean,
    id: Int,
  ) {
    driver.execute(645_367_051, """
        |UPDATE tweets_table
        |SET link = ?, added = ?, position = ?, description = ?, favourite = ?, hideItem = ?
        |WHERE id = ?
        """.trimMargin(), 7) {
          bindString(0, link)
          bindLong(1, added)
          bindLong(2, tweets_tableAdapter.positionAdapter.encode(position))
          bindString(3, description)
          bindBoolean(4, favourite)
          bindBoolean(5, hideItem)
          bindLong(6, tweets_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(645_367_051) { emit ->
      emit("tweets_table")
    }
  }

  public fun delete(id: Int) {
    driver.execute(148_754_925, """DELETE FROM tweets_table WHERE id = ?""", 1) {
          bindLong(0, tweets_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(148_754_925) { emit ->
      emit("link_subject_cross_ref")
      emit("tweets_table")
    }
  }

  private inner class GetByIdQuery<out T : Any>(
    public val id: Int,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("tweets_table", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("tweets_table", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_309_671_354,
        """SELECT tweets_table.id, tweets_table.link, tweets_table.added, tweets_table.position, tweets_table.description, tweets_table.favourite, tweets_table.hideItem FROM tweets_table WHERE id = ?""",
        mapper, 1) {
      bindLong(0, tweets_tableAdapter.idAdapter.encode(id))
    }

    override fun toString(): String = "ContentLink.sq:getById"
  }

  private inner class GetItemsQuery<out T : Any>(
    public val includeFavourite: Boolean?,
    public val includeHidden: Boolean?,
    public val linkSubstring: String?,
    public val startTime: Long?,
    public val endTime: Long?,
    public val sortByListPosition: Boolean,
    public val sortByDate: Boolean,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("tweets_table", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("tweets_table", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT tweets_table.id, tweets_table.link, tweets_table.added, tweets_table.position, tweets_table.description, tweets_table.favourite, tweets_table.hideItem FROM tweets_table
    |WHERE (? IS NULL OR favourite ${ if (includeFavourite == null) "IS" else "=" } ?)
    |  AND (? IS NULL OR hideItem ${ if (includeHidden == null) "IS" else "=" } ?)
    |  AND (? IS NULL OR link LIKE '%' || ? || '%')
    |  AND (? IS NULL OR added >= ?)
    |  AND (? IS NULL OR added <= ?)
    |ORDER BY
    |  CASE WHEN ? THEN position END ASC,
    |  CASE WHEN ? THEN added END DESC
    """.trimMargin(), mapper, 12) {
      bindBoolean(0, includeFavourite)
      bindBoolean(1, includeFavourite)
      bindBoolean(2, includeHidden)
      bindBoolean(3, includeHidden)
      bindString(4, linkSubstring)
      bindString(5, linkSubstring)
      bindLong(6, startTime)
      bindLong(7, startTime)
      bindLong(8, endTime)
      bindLong(9, endTime)
      bindBoolean(10, sortByListPosition)
      bindBoolean(11, sortByDate)
    }

    override fun toString(): String = "ContentLink.sq:getItems"
  }

  private inner class GetByTimeRangeQuery<out T : Any>(
    public val start: Long,
    public val end: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("tweets_table", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("tweets_table", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_158_341_861, """
    |SELECT tweets_table.id, tweets_table.link, tweets_table.added, tweets_table.position, tweets_table.description, tweets_table.favourite, tweets_table.hideItem FROM tweets_table
    |WHERE added BETWEEN ? AND ?
    |ORDER BY added DESC
    """.trimMargin(), mapper, 2) {
      bindLong(0, start)
      bindLong(1, end)
    }

    override fun toString(): String = "ContentLink.sq:getByTimeRange"
  }
}
