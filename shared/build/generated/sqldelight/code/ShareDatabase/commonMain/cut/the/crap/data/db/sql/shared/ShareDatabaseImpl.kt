package cut.the.crap.`data`.db.sql.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import cut.the.crap.`data`.db.sql.ContentItemQueries
import cut.the.crap.`data`.db.sql.ContentLinkQueries
import cut.the.crap.`data`.db.sql.Content_items_table
import cut.the.crap.`data`.db.sql.Handle_tag_table
import cut.the.crap.`data`.db.sql.KeywordQueries
import cut.the.crap.`data`.db.sql.Link_subject_cross_ref
import cut.the.crap.`data`.db.sql.Post_subject_cross_ref
import cut.the.crap.`data`.db.sql.ShareDatabase
import cut.the.crap.`data`.db.sql.SubjectQueries
import cut.the.crap.`data`.db.sql.Subjects_table
import cut.the.crap.`data`.db.sql.Tweets_table
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<ShareDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = ShareDatabaseImpl.Schema

internal fun KClass<ShareDatabase>.newInstance(
  driver: SqlDriver,
  content_items_tableAdapter: Content_items_table.Adapter,
  handle_tag_tableAdapter: Handle_tag_table.Adapter,
  link_subject_cross_refAdapter: Link_subject_cross_ref.Adapter,
  post_subject_cross_refAdapter: Post_subject_cross_ref.Adapter,
  subjects_tableAdapter: Subjects_table.Adapter,
  tweets_tableAdapter: Tweets_table.Adapter,
): ShareDatabase = ShareDatabaseImpl(driver, content_items_tableAdapter, handle_tag_tableAdapter,
    link_subject_cross_refAdapter, post_subject_cross_refAdapter, subjects_tableAdapter,
    tweets_tableAdapter)

private class ShareDatabaseImpl(
  driver: SqlDriver,
  content_items_tableAdapter: Content_items_table.Adapter,
  handle_tag_tableAdapter: Handle_tag_table.Adapter,
  link_subject_cross_refAdapter: Link_subject_cross_ref.Adapter,
  post_subject_cross_refAdapter: Post_subject_cross_ref.Adapter,
  subjects_tableAdapter: Subjects_table.Adapter,
  tweets_tableAdapter: Tweets_table.Adapter,
) : TransacterImpl(driver), ShareDatabase {
  override val contentItemQueries: ContentItemQueries = ContentItemQueries(driver,
      content_items_tableAdapter)

  override val contentLinkQueries: ContentLinkQueries = ContentLinkQueries(driver,
      tweets_tableAdapter)

  override val keywordQueries: KeywordQueries = KeywordQueries(driver, handle_tag_tableAdapter)

  override val subjectQueries: SubjectQueries = SubjectQueries(driver, subjects_tableAdapter,
      post_subject_cross_refAdapter, link_subject_cross_refAdapter)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 5

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS content_items_table (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          |    text TEXT NOT NULL,
          |    created INTEGER NOT NULL,
          |    lastModified INTEGER NOT NULL,
          |    sortOrder INTEGER NOT NULL,
          |    isFavorite INTEGER NOT NULL,
          |    category TEXT,
          |    isActive INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS tweets_table (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          |    link TEXT NOT NULL,
          |    added INTEGER NOT NULL,
          |    position INTEGER NOT NULL DEFAULT 0,
          |    description TEXT NOT NULL DEFAULT '',
          |    favourite INTEGER NOT NULL DEFAULT 0,
          |    hideItem INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS handle_tag_table (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          |    text TEXT NOT NULL,
          |    type INTEGER NOT NULL,
          |    created INTEGER NOT NULL,
          |    lastUsed INTEGER NOT NULL,
          |    usageCount INTEGER NOT NULL,
          |    isFavorite INTEGER NOT NULL,
          |    category TEXT,
          |    color TEXT,
          |    isArchived INTEGER NOT NULL,
          |    sortOrder INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS subjects_table (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          |    name TEXT,
          |    colorHex TEXT NOT NULL,
          |    createdAt INTEGER NOT NULL,
          |    modifiedAt INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS post_subject_cross_ref (
          |    postId INTEGER NOT NULL,
          |    subjectId INTEGER NOT NULL,
          |    PRIMARY KEY(postId, subjectId),
          |    FOREIGN KEY(postId) REFERENCES content_items_table(id) ON DELETE CASCADE,
          |    FOREIGN KEY(subjectId) REFERENCES subjects_table(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS link_subject_cross_ref (
          |    linkId INTEGER NOT NULL,
          |    subjectId INTEGER NOT NULL,
          |    PRIMARY KEY(linkId, subjectId),
          |    FOREIGN KEY(linkId) REFERENCES tweets_table(id) ON DELETE CASCADE,
          |    FOREIGN KEY(subjectId) REFERENCES subjects_table(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null,
          "CREATE INDEX IF NOT EXISTS index_content_items_table_created ON content_items_table(created)",
          0)
      driver.execute(null,
          "CREATE INDEX IF NOT EXISTS index_content_items_table_lastModified ON content_items_table(lastModified)",
          0)
      driver.execute(null,
          "CREATE INDEX IF NOT EXISTS index_content_items_table_sortOrder ON content_items_table(sortOrder)",
          0)
      driver.execute(null,
          "CREATE INDEX IF NOT EXISTS index_handle_tag_table_type ON handle_tag_table(type)", 0)
      driver.execute(null,
          "CREATE INDEX IF NOT EXISTS index_handle_tag_table_lastUsed ON handle_tag_table(lastUsed)",
          0)
      driver.execute(null,
          "CREATE UNIQUE INDEX IF NOT EXISTS index_handle_tag_table_text_type ON handle_tag_table(text, type)",
          0)
      driver.execute(null,
          "CREATE INDEX IF NOT EXISTS index_subjects_table_modifiedAt ON subjects_table(modifiedAt)",
          0)
      driver.execute(null,
          "CREATE INDEX IF NOT EXISTS index_post_subject_cross_ref_subjectId ON post_subject_cross_ref(subjectId)",
          0)
      driver.execute(null,
          "CREATE INDEX IF NOT EXISTS index_link_subject_cross_ref_subjectId ON link_subject_cross_ref(subjectId)",
          0)
      return QueryResult.Unit
    }

    private fun migrateInternal(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
    ): QueryResult.Value<Unit> {
      if (oldVersion <= 1 && newVersion > 1) {
        driver.execute(null,
            "ALTER TABLE tweets_table ADD COLUMN position INTEGER NOT NULL DEFAULT 0", 0)
      }
      if (oldVersion <= 2 && newVersion > 2) {
        driver.execute(null, """
            |CREATE TABLE IF NOT EXISTS handle_tag_table (
            |    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            |    text TEXT NOT NULL,
            |    type INTEGER NOT NULL,
            |    created INTEGER NOT NULL,
            |    lastUsed INTEGER NOT NULL,
            |    usageCount INTEGER NOT NULL,
            |    isFavorite INTEGER NOT NULL,
            |    category TEXT,
            |    color TEXT,
            |    isArchived INTEGER NOT NULL,
            |    sortOrder INTEGER NOT NULL
            |)
            """.trimMargin(), 0)
        driver.execute(null,
            "CREATE INDEX IF NOT EXISTS index_handle_tag_table_type ON handle_tag_table(type)", 0)
        driver.execute(null,
            "CREATE INDEX IF NOT EXISTS index_handle_tag_table_lastUsed ON handle_tag_table(lastUsed)",
            0)
        driver.execute(null,
            "CREATE UNIQUE INDEX IF NOT EXISTS index_handle_tag_table_text_type ON handle_tag_table(text, type)",
            0)
      }
      if (oldVersion <= 3 && newVersion > 3) {
        driver.execute(null, """
            |CREATE TABLE IF NOT EXISTS content_items_table (
            |    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            |    text TEXT NOT NULL,
            |    created INTEGER NOT NULL,
            |    lastModified INTEGER NOT NULL,
            |    sortOrder INTEGER NOT NULL,
            |    isFavorite INTEGER NOT NULL,
            |    category TEXT,
            |    isActive INTEGER NOT NULL
            |)
            """.trimMargin(), 0)
        driver.execute(null,
            "CREATE INDEX IF NOT EXISTS index_content_items_table_created ON content_items_table(created)",
            0)
        driver.execute(null,
            "CREATE INDEX IF NOT EXISTS index_content_items_table_lastModified ON content_items_table(lastModified)",
            0)
        driver.execute(null,
            "CREATE INDEX IF NOT EXISTS index_content_items_table_sortOrder ON content_items_table(sortOrder)",
            0)
      }
      if (oldVersion <= 4 && newVersion > 4) {
        driver.execute(null, """
            |CREATE TABLE IF NOT EXISTS subjects_table (
            |    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            |    name TEXT,
            |    colorHex TEXT NOT NULL,
            |    createdAt INTEGER NOT NULL,
            |    modifiedAt INTEGER NOT NULL
            |)
            """.trimMargin(), 0)
        driver.execute(null,
            "CREATE INDEX IF NOT EXISTS index_subjects_table_modifiedAt ON subjects_table(modifiedAt)",
            0)
        driver.execute(null, """
            |CREATE TABLE IF NOT EXISTS post_subject_cross_ref (
            |    postId INTEGER NOT NULL,
            |    subjectId INTEGER NOT NULL,
            |    PRIMARY KEY(postId, subjectId),
            |    FOREIGN KEY(postId) REFERENCES content_items_table(id) ON DELETE CASCADE,
            |    FOREIGN KEY(subjectId) REFERENCES subjects_table(id) ON DELETE CASCADE
            |)
            """.trimMargin(), 0)
        driver.execute(null,
            "CREATE INDEX IF NOT EXISTS index_post_subject_cross_ref_subjectId ON post_subject_cross_ref(subjectId)",
            0)
        driver.execute(null, """
            |CREATE TABLE IF NOT EXISTS link_subject_cross_ref (
            |    linkId INTEGER NOT NULL,
            |    subjectId INTEGER NOT NULL,
            |    PRIMARY KEY(linkId, subjectId),
            |    FOREIGN KEY(linkId) REFERENCES tweets_table(id) ON DELETE CASCADE,
            |    FOREIGN KEY(subjectId) REFERENCES subjects_table(id) ON DELETE CASCADE
            |)
            """.trimMargin(), 0)
        driver.execute(null,
            "CREATE INDEX IF NOT EXISTS index_link_subject_cross_ref_subjectId ON link_subject_cross_ref(subjectId)",
            0)
      }
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> {
      var lastVersion = oldVersion

      callbacks.filter { it.afterVersion in oldVersion until newVersion }
      .sortedBy { it.afterVersion }
      .forEach { callback ->
        migrateInternal(driver, oldVersion = lastVersion, newVersion = callback.afterVersion + 1)
        callback.block(driver)
        lastVersion = callback.afterVersion + 1
      }

      if (lastVersion < newVersion) {
        migrateInternal(driver, lastVersion, newVersion)
      }
      return QueryResult.Unit
    }
  }
}
