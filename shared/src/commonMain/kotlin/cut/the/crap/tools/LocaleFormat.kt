package cut.the.crap.tools

/**
 * Formatting that depends on the user's locale, and therefore cannot be done in common code.
 *
 * kotlinx-datetime deliberately has no locale-aware formatting, and there is no multiplatform
 * number formatter — so this is a seam rather than a shared implementation. It matters more than
 * it looks: on a German device `NumberFormat` renders 5154 as **"5.154"** and 0.3848 as
 * **"38,48 %"**. A naive common implementation would silently produce "5,154" and "38.48%", which
 * compiles, passes every test, and is wrong on screen — so the platform keeps doing this.
 *
 * The dates here are the *locale-sensitive* ones (month names). Everything with an all-numeric
 * pattern is locale-independent and lives in `TimeUtils` on kotlinx-datetime instead.
 */

/** An integer with the locale's grouping separators, e.g. `5.154` (de) or `5,154` (en). */
expect fun formatInteger(value: Long): String

/** A fraction as a locale-formatted percentage with two decimals, e.g. `38,48 %` (de). */
expect fun formatPercent(fraction: Double): String

/** A number to one decimal place, locale-aware — `1,5` (de) vs `1.5` (en). `String.format` is JVM-only. */
expect fun formatOneDecimal(value: Double): String

/** A medium date with a localised month name plus time, e.g. `Jul 14, 2026 13:45`. */
expect fun formatMediumDateTime(timestamp: Long): String
