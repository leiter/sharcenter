package cut.the.crap.tools

import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

/**
 * Normalizes a timestamp to the start of the day (00:00:00).
 * Sets hours, minutes, seconds, and milliseconds to 0.
 *
 * @param timestamp The timestamp to normalize
 * @return The normalized timestamp at midnight of the same day
 */
fun normalizeToStartOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Converts a timestamp to the start of that day (00:00:00.000)
 */
fun toStartOfDay(timestamp: Long): Long {
    val localDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return localDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

/**
 * Converts a timestamp to the end of that day (23:59:59.999)
 */
fun toEndOfDay(timestamp: Long): Long {
    val localDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return localDate
        .atTime(23, 59, 59, 999_999_999)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

