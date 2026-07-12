package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Int
import kotlin.Long
import kotlin.String

public class SubjectQueries(
  driver: SqlDriver,
  private val subjects_tableAdapter: Subjects_table.Adapter,
  private val post_subject_cross_refAdapter: Post_subject_cross_ref.Adapter,
  private val link_subject_cross_refAdapter: Link_subject_cross_ref.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getById(id: Int, mapper: (
    id: Int,
    name: String?,
    colorHex: String,
    createdAt: Long,
    modifiedAt: Long,
  ) -> T): Query<T> = GetByIdQuery(id) { cursor ->
    mapper(
      subjects_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1),
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getById(id: Int): Query<Subjects_table> = getById(id) { id_, name, colorHex, createdAt,
      modifiedAt ->
    Subjects_table(
      id_,
      name,
      colorHex,
      createdAt,
      modifiedAt
    )
  }

  public fun <T : Any> getAllByRecency(mapper: (
    id: Int,
    name: String?,
    colorHex: String,
    createdAt: Long,
    modifiedAt: Long,
  ) -> T): Query<T> = Query(1_227_530_418, arrayOf("subjects_table"), driver, "Subject.sq",
      "getAllByRecency",
      "SELECT subjects_table.id, subjects_table.name, subjects_table.colorHex, subjects_table.createdAt, subjects_table.modifiedAt FROM subjects_table ORDER BY modifiedAt DESC") {
      cursor ->
    mapper(
      subjects_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1),
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getAllByRecency(): Query<Subjects_table> = getAllByRecency { id, name, colorHex,
      createdAt, modifiedAt ->
    Subjects_table(
      id,
      name,
      colorHex,
      createdAt,
      modifiedAt
    )
  }

  public fun <T : Any> getAllByName(mapper: (
    id: Int,
    name: String?,
    colorHex: String,
    createdAt: Long,
    modifiedAt: Long,
  ) -> T): Query<T> = Query(-157_082_584, arrayOf("subjects_table"), driver, "Subject.sq",
      "getAllByName",
      "SELECT subjects_table.id, subjects_table.name, subjects_table.colorHex, subjects_table.createdAt, subjects_table.modifiedAt FROM subjects_table ORDER BY COALESCE(name, colorHex) COLLATE NOCASE ASC") {
      cursor ->
    mapper(
      subjects_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1),
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getAllByName(): Query<Subjects_table> = getAllByName { id, name, colorHex, createdAt,
      modifiedAt ->
    Subjects_table(
      id,
      name,
      colorHex,
      createdAt,
      modifiedAt
    )
  }

  public fun <T : Any> getSubjectsForPost(postId: Int, mapper: (
    id: Int,
    name: String?,
    colorHex: String,
    createdAt: Long,
    modifiedAt: Long,
  ) -> T): Query<T> = GetSubjectsForPostQuery(postId) { cursor ->
    mapper(
      subjects_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1),
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getSubjectsForPost(postId: Int): Query<Subjects_table> = getSubjectsForPost(postId) {
      id, name, colorHex, createdAt, modifiedAt ->
    Subjects_table(
      id,
      name,
      colorHex,
      createdAt,
      modifiedAt
    )
  }

  public fun <T : Any> getSubjectsForLink(linkId: Int, mapper: (
    id: Int,
    name: String?,
    colorHex: String,
    createdAt: Long,
    modifiedAt: Long,
  ) -> T): Query<T> = GetSubjectsForLinkQuery(linkId) { cursor ->
    mapper(
      subjects_tableAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1),
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getSubjectsForLink(linkId: Int): Query<Subjects_table> = getSubjectsForLink(linkId) {
      id, name, colorHex, createdAt, modifiedAt ->
    Subjects_table(
      id,
      name,
      colorHex,
      createdAt,
      modifiedAt
    )
  }

  public fun lastInsertRowId(): ExecutableQuery<Long> = Query(-1_557_996_373, driver, "Subject.sq",
      "lastInsertRowId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  public fun insert(
    name: String?,
    colorHex: String,
    createdAt: Long,
    modifiedAt: Long,
  ) {
    driver.execute(2_138_844_500, """
        |INSERT OR REPLACE INTO subjects_table(name, colorHex, createdAt, modifiedAt)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, name)
          bindString(1, colorHex)
          bindLong(2, createdAt)
          bindLong(3, modifiedAt)
        }
    notifyQueries(2_138_844_500) { emit ->
      emit("subjects_table")
    }
  }

  public fun insertWithId(
    id: Int?,
    name: String?,
    colorHex: String,
    createdAt: Long,
    modifiedAt: Long,
  ) {
    driver.execute(1_625_647_861, """
        |INSERT OR REPLACE INTO subjects_table(id, name, colorHex, createdAt, modifiedAt)
        |VALUES (?, ?, ?, ?, ?)
        """.trimMargin(), 5) {
          bindLong(0, id?.let { subjects_tableAdapter.idAdapter.encode(it) })
          bindString(1, name)
          bindString(2, colorHex)
          bindLong(3, createdAt)
          bindLong(4, modifiedAt)
        }
    notifyQueries(1_625_647_861) { emit ->
      emit("subjects_table")
    }
  }

  public fun update(
    name: String?,
    colorHex: String,
    createdAt: Long,
    modifiedAt: Long,
    id: Int,
  ) {
    driver.execute(-1_811_176_604, """
        |UPDATE subjects_table
        |SET name = ?, colorHex = ?, createdAt = ?, modifiedAt = ?
        |WHERE id = ?
        """.trimMargin(), 5) {
          bindString(0, name)
          bindString(1, colorHex)
          bindLong(2, createdAt)
          bindLong(3, modifiedAt)
          bindLong(4, subjects_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(-1_811_176_604) { emit ->
      emit("subjects_table")
    }
  }

  public fun delete(id: Int) {
    driver.execute(1_987_178_566, """DELETE FROM subjects_table WHERE id = ?""", 1) {
          bindLong(0, subjects_tableAdapter.idAdapter.encode(id))
        }
    notifyQueries(1_987_178_566) { emit ->
      emit("link_subject_cross_ref")
      emit("post_subject_cross_ref")
      emit("subjects_table")
    }
  }

  public fun linkPost(postId: Int, subjectId: Int) {
    driver.execute(-1_207_476_331,
        """INSERT OR IGNORE INTO post_subject_cross_ref(postId, subjectId) VALUES (?, ?)""", 2) {
          bindLong(0, post_subject_cross_refAdapter.postIdAdapter.encode(postId))
          bindLong(1, post_subject_cross_refAdapter.subjectIdAdapter.encode(subjectId))
        }
    notifyQueries(-1_207_476_331) { emit ->
      emit("post_subject_cross_ref")
    }
  }

  public fun unlinkPost(postId: Int, subjectId: Int) {
    driver.execute(1_223_899_758,
        """DELETE FROM post_subject_cross_ref WHERE postId = ? AND subjectId = ?""", 2) {
          bindLong(0, post_subject_cross_refAdapter.postIdAdapter.encode(postId))
          bindLong(1, post_subject_cross_refAdapter.subjectIdAdapter.encode(subjectId))
        }
    notifyQueries(1_223_899_758) { emit ->
      emit("post_subject_cross_ref")
    }
  }

  public fun linkLink(linkId: Int, subjectId: Int) {
    driver.execute(-1_207_601_425,
        """INSERT OR IGNORE INTO link_subject_cross_ref(linkId, subjectId) VALUES (?, ?)""", 2) {
          bindLong(0, link_subject_cross_refAdapter.linkIdAdapter.encode(linkId))
          bindLong(1, link_subject_cross_refAdapter.subjectIdAdapter.encode(subjectId))
        }
    notifyQueries(-1_207_601_425) { emit ->
      emit("link_subject_cross_ref")
    }
  }

  public fun unlinkLink(linkId: Int, subjectId: Int) {
    driver.execute(1_223_774_664,
        """DELETE FROM link_subject_cross_ref WHERE linkId = ? AND subjectId = ?""", 2) {
          bindLong(0, link_subject_cross_refAdapter.linkIdAdapter.encode(linkId))
          bindLong(1, link_subject_cross_refAdapter.subjectIdAdapter.encode(subjectId))
        }
    notifyQueries(1_223_774_664) { emit ->
      emit("link_subject_cross_ref")
    }
  }

  private inner class GetByIdQuery<out T : Any>(
    public val id: Int,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("subjects_table", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("subjects_table", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-153_113_331,
        """SELECT subjects_table.id, subjects_table.name, subjects_table.colorHex, subjects_table.createdAt, subjects_table.modifiedAt FROM subjects_table WHERE id = ?""",
        mapper, 1) {
      bindLong(0, subjects_tableAdapter.idAdapter.encode(id))
    }

    override fun toString(): String = "Subject.sq:getById"
  }

  private inner class GetSubjectsForPostQuery<out T : Any>(
    public val postId: Int,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("subjects_table", "post_subject_cross_ref", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("subjects_table", "post_subject_cross_ref", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-64_134_617, """
    |SELECT s.id, s.name, s.colorHex, s.createdAt, s.modifiedAt FROM subjects_table s
    |INNER JOIN post_subject_cross_ref x ON s.id = x.subjectId
    |WHERE x.postId = ?
    |ORDER BY s.modifiedAt DESC
    """.trimMargin(), mapper, 1) {
      bindLong(0, post_subject_cross_refAdapter.postIdAdapter.encode(postId))
    }

    override fun toString(): String = "Subject.sq:getSubjectsForPost"
  }

  private inner class GetSubjectsForLinkQuery<out T : Any>(
    public val linkId: Int,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("subjects_table", "link_subject_cross_ref", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("subjects_table", "link_subject_cross_ref", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-64_259_711, """
    |SELECT s.id, s.name, s.colorHex, s.createdAt, s.modifiedAt FROM subjects_table s
    |INNER JOIN link_subject_cross_ref x ON s.id = x.subjectId
    |WHERE x.linkId = ?
    |ORDER BY s.modifiedAt DESC
    """.trimMargin(), mapper, 1) {
      bindLong(0, link_subject_cross_refAdapter.linkIdAdapter.encode(linkId))
    }

    override fun toString(): String = "Subject.sq:getSubjectsForLink"
  }
}
