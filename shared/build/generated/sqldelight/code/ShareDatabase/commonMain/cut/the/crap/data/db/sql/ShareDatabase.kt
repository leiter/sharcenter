package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import cut.the.crap.`data`.db.sql.shared.newInstance
import cut.the.crap.`data`.db.sql.shared.schema
import kotlin.Unit

public interface ShareDatabase : Transacter {
  public val contentItemQueries: ContentItemQueries

  public val contentLinkQueries: ContentLinkQueries

  public val keywordQueries: KeywordQueries

  public val subjectQueries: SubjectQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = ShareDatabase::class.schema

    public operator fun invoke(
      driver: SqlDriver,
      content_items_tableAdapter: Content_items_table.Adapter,
      handle_tag_tableAdapter: Handle_tag_table.Adapter,
      link_subject_cross_refAdapter: Link_subject_cross_ref.Adapter,
      post_subject_cross_refAdapter: Post_subject_cross_ref.Adapter,
      subjects_tableAdapter: Subjects_table.Adapter,
      tweets_tableAdapter: Tweets_table.Adapter,
    ): ShareDatabase = ShareDatabase::class.newInstance(driver, content_items_tableAdapter,
        handle_tag_tableAdapter, link_subject_cross_refAdapter, post_subject_cross_refAdapter,
        subjects_tableAdapter, tweets_tableAdapter)
  }
}
