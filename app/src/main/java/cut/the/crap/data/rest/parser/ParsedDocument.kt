package cut.the.crap.data.rest.parser

/**
 * The generic, map-like result of parsing a [RawSource] against a [ParseSchema].
 *
 * This is deliberately *not* a concrete campaign type: it is a bag of named scalar
 * [fields] plus named [tables], which a domain mapper (e.g. `EciStatisticsMapper`) turns
 * into a concrete model today, and which a configurable UI can render directly later.
 *
 * @property fields top-level scalars keyed by [FieldSpec.key]; absent paths map to [ParsedValue.Null].
 * @property tables row collections keyed by [TableSpec.key].
 */
data class ParsedDocument(
    val fields: Map<String, ParsedValue> = emptyMap(),
    val tables: Map<String, ParsedTable> = emptyMap()
) {
    /** The scalar stored under [key], or [ParsedValue.Null] if the schema didn't declare it. */
    fun field(key: String): ParsedValue = fields[key] ?: ParsedValue.Null

    /** The table stored under [key], or `null` if the schema didn't declare it. */
    fun table(key: String): ParsedTable? = tables[key]
}

/**
 * One extracted collection of rows.
 *
 * @property key the schema's [TableSpec.key].
 * @property columns the schema's column specs, carried forward so a configurable UI can
 *   render headers, alignment, and per-type formatting without seeing the schema.
 * @property rows each a map of [FieldSpec.key] to the extracted [ParsedValue].
 */
data class ParsedTable(
    val key: String,
    val columns: List<FieldSpec>,
    val rows: List<Map<String, ParsedValue>>
)
