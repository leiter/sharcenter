package cut.the.crap.data.rest.parser

import kotlinx.serialization.Serializable

/**
 * A declarative description of *what* to extract from a [RawSource] and *where* it lives —
 * the "rules and target" a [SourceParser] is parameterised with. It is intentionally
 * [Serializable] so a campaign can eventually be defined as data (a bundled asset or a
 * remotely fetched config) rather than Kotlin code; for now the ECI campaign builds one in
 * code (see `EciSchema`).
 *
 * @property format the source format these rules apply to (picks the parser).
 * @property fields top-level scalar values to pull out (e.g. a total, a deadline).
 * @property tables row collections to pull out (e.g. the per-country stats table).
 */
@Serializable
data class ParseSchema(
    val format: SourceFormat,
    val fields: List<FieldSpec> = emptyList(),
    val tables: List<TableSpec> = emptyList()
)

/**
 * How to extract one value and what to call it in the output.
 *
 * @property key the output key this value is stored under in [ParsedDocument.fields] or a table row.
 * @property path where to read it from. For JSON this is a dotted object path
 *   (e.g. `"sosReport.totalSignatures"`); inside a [TableSpec] it is relative to each row element.
 * @property type how to coerce the raw value into a [ParsedValue].
 * @property label optional human-facing column/field label for a future configurable UI.
 */
@Serializable
data class FieldSpec(
    val key: String,
    val path: String,
    val type: ValueType = ValueType.STRING,
    val label: String? = null
)

/**
 * How to extract a collection of rows.
 *
 * @property key the output key this table is stored under in [ParsedDocument.tables].
 * @property rowsPath dotted path to the array of row elements (e.g. `"sosReport.entry"`).
 * @property columns per-row field extraction, each [FieldSpec.path] relative to the row element.
 */
@Serializable
data class TableSpec(
    val key: String,
    val rowsPath: String,
    val columns: List<FieldSpec>
)

/** The target type a [FieldSpec] coerces its raw source value into. */
@Serializable
enum class ValueType { STRING, NUMBER, BOOLEAN }
