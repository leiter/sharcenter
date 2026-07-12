package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.ColumnAdapter
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class Content_items_table(
  public val id: Int,
  public val text: String,
  public val created: Long,
  public val lastModified: Long,
  public val sortOrder: Int,
  public val isFavorite: Boolean,
  public val category: String?,
  public val isActive: Boolean,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<Int, Long>,
    public val sortOrderAdapter: ColumnAdapter<Int, Long>,
  )
}
