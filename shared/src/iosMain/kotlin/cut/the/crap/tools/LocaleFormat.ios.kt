package cut.the.crap.tools

import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterPercentStyle

/**
 * iOS locale-aware formatting via `NSNumberFormatter` / `NSDateFormatter`, mirroring the Android
 * `java.text` actual: grouping separators, a two-decimal percent, and localised month names all
 * come from the OS locale (or an explicitly named one), which no common code can reproduce.
 */

actual fun formatInteger(value: Long): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        maximumFractionDigits = 0.toULong()
    }.stringFromNumber(NSNumber(long = value)) ?: value.toString()

actual fun formatPercent(fraction: Double): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterPercentStyle
        minimumFractionDigits = 2.toULong()
        maximumFractionDigits = 2.toULong()
    }.stringFromNumber(NSNumber(double = fraction)) ?: ""

actual fun formatOneDecimal(value: Double): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        minimumFractionDigits = 1.toULong()
        maximumFractionDigits = 1.toULong()
    }.stringFromNumber(NSNumber(double = value)) ?: value.toString()

actual fun formatMediumDateTime(timestamp: Long): String =
    // No explicit locale: NSDateFormatter defaults to the current locale, matching the Android actual.
    NSDateFormatter().apply {
        dateFormat = "MMM d, yyyy HH:mm"
        // NSDate only exposes a `timeIntervalSinceReferenceDate` (2001-01-01) constructor in K/N;
        // 978307200 is the fixed offset from the 1970 epoch (NSTimeIntervalSince1970).
    }.stringFromDate(NSDate(timeIntervalSinceReferenceDate = timestamp / 1000.0 - 978_307_200.0))

actual fun formatIntegerForLanguage(value: Long, languageTag: String): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        maximumFractionDigits = 0.toULong()
        locale = NSLocale(localeIdentifier = languageTag)
    }.stringFromNumber(NSNumber(long = value)) ?: value.toString()

actual fun formatMediumDateForLanguage(date: LocalDate, languageTag: String): String {
    val components = NSDateComponents().apply {
        year = date.year.toLong()
        month = (date.month.ordinal + 1).toLong() // Month is 1-based for NSDateComponents; ordinal is 0-based
        day = date.day.toLong()
    }
    val nsDate = NSCalendar.currentCalendar.dateFromComponents(components) ?: return ""
    return NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        locale = NSLocale(localeIdentifier = languageTag)
    }.stringFromDate(nsDate)
}
