package cut.the.crap.data.rest.parser

/**
 * A single extracted value, type-tagged so a consumer (a domain mapper today, a
 * configurable UI later) can sort, format, and colour without re-sniffing the raw text.
 *
 * Numbers are held as [Double] regardless of the source's integer/decimal distinction;
 * use [asLongOrNull] when a whole number is expected. [Null] represents an absent or
 * unconvertible value, so callers never deal with a Kotlin `null` [ParsedValue].
 */
sealed interface ParsedValue {

    data class Str(val value: String) : ParsedValue
    data class Num(val value: Double) : ParsedValue
    data class Bool(val value: Boolean) : ParsedValue
    data class Lst(val values: List<ParsedValue>) : ParsedValue
    data object Null : ParsedValue

    /** The string content, or `null` for non-strings and [Null]. */
    fun asStringOrNull(): String? = (this as? Str)?.value

    /** The numeric value as a [Double], or `null` for non-numbers and [Null]. */
    fun asDoubleOrNull(): Double? = (this as? Num)?.value

    /** The numeric value truncated to a [Long], or `null` for non-numbers and [Null]. */
    fun asLongOrNull(): Long? = (this as? Num)?.value?.toLong()

    /** The boolean value, or `null` for non-booleans and [Null]. */
    fun asBooleanOrNull(): Boolean? = (this as? Bool)?.value
}
