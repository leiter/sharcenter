@file:OptIn(kotlin.time.ExperimentalTime::class)

package cut.the.crap.tools

import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Timestamp helpers, on kotlinx-datetime rather than `java.time`/`Calendar`.
 *
 * Everything here is deliberately **locale-independent**: the patterns are all-numeric, so they
 * render identically in every locale and need no `Locale` argument. Anything that *is*
 * locale-sensitive — thousands separators, month names — cannot be done in common code and lives
 * behind a seam instead (see `LocaleFormat`).
 */

private val zone: TimeZone get() = TimeZone.currentSystemDefault()

/** Epoch milliseconds now. `System.currentTimeMillis()` is JVM-only. */
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(zone)

/** The timestamp normalised to 00:00:00.000 of the same local day. */
fun toStartOfDay(timestamp: Long): Long =
    Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(zone)
        .date
        .atStartOfDayIn(zone)
        .toEpochMilliseconds()

/**
 * The timestamp moved to the last instant of the same local day, 23:59:59.999.
 *
 * (The old version asked for 999_999_999 nanoseconds and then truncated to milliseconds, which is
 * the same thing — stated directly here.)
 */
fun toEndOfDay(timestamp: Long): Long =
    Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(zone)
        .date
        .atTime(hour = 23, minute = 59, second = 59, nanosecond = 999_000_000)
        .toInstant(zone)
        .toEpochMilliseconds()

/**
 * Kept as a separate name because callers use it, but it is [toStartOfDay] — the old
 * `Calendar`-based implementation did exactly the same thing by a different route.
 */
fun normalizeToStartOfDay(timestamp: Long): Long = toStartOfDay(timestamp)

// `dd.MM.yy, HH:mm` — e.g. "13.07.26, 21:27". Shown on every link and post card.
private val dateTimeFormat = LocalDateTime.Format {
    dayOfMonth(Padding.ZERO)
    char('.')
    monthNumber(Padding.ZERO)
    char('.')
    yearTwoDigits(baseYear = 2000)
    chars(", ")
    hour(Padding.ZERO)
    char(':')
    minute(Padding.ZERO)
}

// `dd.MM.yy` — e.g. "13.07.26".
private val dateOnlyFormat = LocalDateTime.Format {
    dayOfMonth(Padding.ZERO)
    char('.')
    monthNumber(Padding.ZERO)
    char('.')
    yearTwoDigits(baseYear = 2000)
}

// `yyyy-MM-dd_HHmmss` — used for export and backup filenames, so it must stay sortable.
private val fileNameFormat = LocalDateTime.Format {
    year()
    char('-')
    monthNumber(Padding.ZERO)
    char('-')
    dayOfMonth(Padding.ZERO)
    char('_')
    hour(Padding.ZERO)
    minute(Padding.ZERO)
    second(Padding.ZERO)
}

// `yyyy-MM-dd HH:mm:ss`
private val isoLikeFormat = LocalDateTime.Format {
    year()
    char('-')
    monthNumber(Padding.ZERO)
    char('-')
    dayOfMonth(Padding.ZERO)
    char(' ')
    hour(Padding.ZERO)
    char(':')
    minute(Padding.ZERO)
    char(':')
    second(Padding.ZERO)
}

/** Date and time, `dd.MM.yy, HH:mm`. */
fun formatTimestampWithLocalizedFormatter(timestamp: Long): String =
    timestamp.toLocalDateTime().format(dateTimeFormat)

/** Date only, `dd.MM.yy`. */
fun formatDateOnly(timestamp: Long): String =
    timestamp.toLocalDateTime().format(dateOnlyFormat)

/** A filename-safe, sortable stamp: `yyyy-MM-dd_HHmmss`. */
fun formatTimestampForFileName(timestamp: Long): String =
    timestamp.toLocalDateTime().format(fileNameFormat)

/** `yyyy-MM-dd HH:mm:ss`. */
fun formatTimestampIsoLike(timestamp: Long): String =
    timestamp.toLocalDateTime().format(isoLikeFormat)
