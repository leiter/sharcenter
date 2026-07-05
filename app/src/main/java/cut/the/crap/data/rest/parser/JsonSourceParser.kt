package cut.the.crap.data.rest.parser

import cut.the.crap.R
import cut.the.crap.data.rest.Result
import cut.the.crap.tools.StringProvider
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import javax.inject.Inject

/**
 * A [SourceParser] for [SourceFormat.JSON]. Walks the parsed JSON tree following the
 * dotted [FieldSpec.path] / [TableSpec.rowsPath] declared in the [ParseSchema], with no
 * knowledge of any particular campaign — the ECI campaign is just the first schema fed to it.
 *
 * Shares the app-wide [Json] instance so lenient/unknown-key handling matches the HTTP client.
 */
class JsonSourceParser @Inject constructor(
    private val json: Json,
    private val strings: StringProvider,
) : SourceParser {

    override fun supports(format: SourceFormat): Boolean = format == SourceFormat.JSON

    override fun parse(source: RawSource, schema: ParseSchema): Result<ParsedDocument> {
        val root = try {
            json.parseToJsonElement(source.content)
        } catch (e: SerializationException) {
            return Result.Error(strings.get(R.string.parser_error_parse), e, retryable = false)
        } catch (e: IllegalArgumentException) {
            return Result.Error(strings.get(R.string.parser_error_parse), e, retryable = false)
        }

        val fields = schema.fields.associate { spec ->
            spec.key to convert(root.resolve(spec.path), spec.type)
        }

        val tables = schema.tables.associate { table ->
            val elements = (root.resolve(table.rowsPath) as? JsonArray).orEmpty()
            val rows = elements.map { element ->
                table.columns.associate { column ->
                    column.key to convert(element.resolve(column.path), column.type)
                }
            }
            table.key to ParsedTable(key = table.key, columns = table.columns, rows = rows)
        }

        return Result.Success(ParsedDocument(fields = fields, tables = tables))
    }

    /** Follows a dotted object path from this element; returns `null` if any segment is missing. */
    private fun JsonElement.resolve(path: String): JsonElement? {
        var current: JsonElement? = this
        for (segment in path.split('.')) {
            val obj = current as? JsonObject ?: return null
            current = obj[segment]
        }
        return current
    }

    /** Coerces a JSON element into a typed [ParsedValue]; absent/incompatible values become [ParsedValue.Null]. */
    private fun convert(element: JsonElement?, type: ValueType): ParsedValue {
        val primitive = element as? JsonPrimitive ?: return ParsedValue.Null
        return when (type) {
            ValueType.STRING -> primitive.contentOrNull?.let(ParsedValue::Str) ?: ParsedValue.Null
            ValueType.NUMBER -> primitive.doubleOrNull?.let(ParsedValue::Num) ?: ParsedValue.Null
            ValueType.BOOLEAN -> primitive.booleanOrNull?.let(ParsedValue::Bool) ?: ParsedValue.Null
        }
    }

    private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()
}
