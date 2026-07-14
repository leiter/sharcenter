package cut.the.crap.tools

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Identical to the Android actual — both targets are JVM, so both get `java.text` for free.
 *
 * They are still written as separate actuals rather than shared through an intermediate JVM source
 * set, because the seam is what keeps a future non-JVM target (iOS) addable: that actual would use
 * `NSNumberFormatter`, and nothing else would change.
 */

private val integerFormat: NumberFormat get() = NumberFormat.getIntegerInstance()

private val percentFormat: NumberFormat
    get() = NumberFormat.getPercentInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

actual fun formatInteger(value: Long): String = integerFormat.format(value)

actual fun formatPercent(fraction: Double): String = percentFormat.format(fraction)

actual fun formatOneDecimal(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

actual fun formatMediumDateTime(timestamp: Long): String =
    SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
