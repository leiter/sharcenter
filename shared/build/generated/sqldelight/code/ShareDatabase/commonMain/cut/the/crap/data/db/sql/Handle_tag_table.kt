package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.ColumnAdapter
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class Handle_tag_table(
  public val id: Int,
  public val text: String,
  public val type: Int,
  public val created: Long,
  public val lastUsed: Long,
  public val usageCount: Int,
  public val isFavorite: Boolean,
  public val category: String?,
  public val color: String?,
  public val isArchived: Boolean,
  public val sortOrder: Int,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<Int, Long>,
    public val typeAdapter: ColumnAdapter<Int, Long>,
    public val usageCountAdapter: ColumnAdapter<Int, Long>,
    public val sortOrderAdapter: ColumnAdapter<Int, Long>,
  )
}
