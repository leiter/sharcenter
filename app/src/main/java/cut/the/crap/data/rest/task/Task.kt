package cut.the.crap.data.rest.task

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Marker interface for all job queue tasks.
 * Each subtype defines its own fields and is serialized with a "type" discriminator.
 */
@Serializable(with = TaskSerializer::class)
sealed interface Task

/**
 * Task to share a list of links to the job queue server.
 */
@Serializable
data class ShareLinksTask(
    val links: List<String>
) : Task

/**
 * Task to upload files to the job queue server.
 */
@Serializable
data class UploadFilesTask(
    val fileNames: List<String>,
    val mimeTypes: List<String>,
    val totalSize: Long
) : Task

// Future task types can have different fields:
// data class ProcessVideoTask(val videoId: String, val quality: String) : Task
// data class NotificationTask(val title: String, val body: String) : Task

/**
 * Custom serializer that outputs task type as "type" field alongside task-specific fields.
 * Example: ShareLinksTask(links=["a","b"]) -> {"type":"ShareLinksTask","links":["a","b"]}
 */
object TaskSerializer : KSerializer<Task> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Task")

    override fun serialize(encoder: Encoder, value: Task) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalStateException("TaskSerializer only supports JSON encoding")

        val jsonObject = when (value) {
            is ShareLinksTask -> buildJsonObject {
                put("type", "ShareLinksTask")
                putJsonArray("links") {
                    value.links.forEach { add(JsonPrimitive(it)) }
                }
            }
            is UploadFilesTask -> buildJsonObject {
                put("type", "UploadFilesTask")
                putJsonArray("fileNames") {
                    value.fileNames.forEach { add(JsonPrimitive(it)) }
                }
                putJsonArray("mimeTypes") {
                    value.mimeTypes.forEach { add(JsonPrimitive(it)) }
                }
                put("totalSize", value.totalSize)
            }
        }
        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): Task {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("TaskSerializer only supports JSON decoding")

        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val type = jsonObject["type"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Missing 'type' field in Task JSON")

        return when (type) {
            "ShareLinksTask" -> {
                val linksArray = jsonObject["links"]?.jsonArray ?: JsonArray(emptyList())
                val links = linksArray.map { it.jsonPrimitive.content }
                ShareLinksTask(links)
            }
            "UploadFilesTask" -> {
                val fileNamesArray = jsonObject["fileNames"]?.jsonArray ?: JsonArray(emptyList())
                val fileNames = fileNamesArray.map { it.jsonPrimitive.content }
                val mimeTypesArray = jsonObject["mimeTypes"]?.jsonArray ?: JsonArray(emptyList())
                val mimeTypes = mimeTypesArray.map { it.jsonPrimitive.content }
                val totalSize = jsonObject["totalSize"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                UploadFilesTask(fileNames, mimeTypes, totalSize)
            }
            else -> throw IllegalStateException("Unknown task type: $type")
        }
    }
}
