package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.ColumnAdapter
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class Subjects_table(
  public val id: Int,
  public val name: String?,
  public val colorHex: String,
  public val createdAt: Long,
  public val modifiedAt: Long,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<Int, Long>,
  )
}
