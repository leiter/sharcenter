package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.ColumnAdapter
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class Tweets_table(
  public val id: Int,
  public val link: String,
  public val added: Long,
  public val position: Int,
  public val description: String,
  public val favourite: Boolean,
  public val hideItem: Boolean,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<Int, Long>,
    public val positionAdapter: ColumnAdapter<Int, Long>,
  )
}
