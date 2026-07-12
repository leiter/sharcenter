package cut.the.crap.`data`.db.sql

import app.cash.sqldelight.ColumnAdapter
import kotlin.Int
import kotlin.Long

public data class Link_subject_cross_ref(
  public val linkId: Int,
  public val subjectId: Int,
) {
  public class Adapter(
    public val linkIdAdapter: ColumnAdapter<Int, Long>,
    public val subjectIdAdapter: ColumnAdapter<Int, Long>,
  )
}
