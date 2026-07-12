package cut.the.crap.data.rest.task

import android.util.Log
import cut.the.crap.R
import cut.the.crap.data.rest.Result
import cut.the.crap.tools.StringProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.IOException
import java.util.UUID

/**
 * Request payload for the job queue server.
 */
@Serializable
data class JobQueueRequest(
    val job_type: String,
    val job_id: String,
    val data: JobData
)

@Serializable
data class JobData(
    val task_data: JsonElement,
    val source: String = "android_app"
)

/**
 * Data class for file upload information.
 */
data class FileUploadData(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileUploadData
        return fileName == other.fileName && mimeType == other.mimeType && bytes.contentEquals(other.bytes)
    }
    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

interface JobQueueRepository {
    suspend fun submitTask(task: Task): Result<String>
    suspend fun uploadFiles(files: List<FileUploadData>): Result<String>
}

class JobQueueRepositoryImpl constructor(
    private val strings: StringProvider
) : JobQueueRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    // Dedicated HttpClient for job queue server (different base URL than main API)
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }

    companion object {
        private const val TAG = "JobQueueRepository"
        // 10.0.2.2 is the emulator's alias for host localhost
        private const val BASE_URL = "http://10.0.2.2:5000"
    }

    override suspend fun submitTask(task: Task): Result<String> {
        return try {
            val taskJson = json.encodeToJsonElement(TaskSerializer, task)

            val request = JobQueueRequest(
                job_type = "BackgroundJob",
                job_id = UUID.randomUUID().toString(),
                data = JobData(task_data = taskJson)
            )

            Log.d(TAG, "Submitting task: ${json.encodeToString(JobQueueRequest.serializer(), request)}")

            val response = client.post("$BASE_URL/jobs") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<String>()

            Log.d(TAG, "Job submitted successfully: $response")
            Result.Success(response)
        } catch (e: ClientRequestException) {
            Log.e(TAG, "Client error: ${e.response.status}", e)
            Result.Error(
                message = strings.get(R.string.error_client, e.response.status.value, e.response.status.description),
                exception = e
            )
        } catch (e: ServerResponseException) {
            Log.e(TAG, "Server error: ${e.response.status}", e)
            Result.Error(
                message = strings.get(R.string.error_server, e.response.status.value, e.response.status.description),
                exception = e
            )
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timed out", e)
            Result.Error(
                message = strings.get(R.string.error_timeout),
                exception = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            Result.Error(
                message = strings.get(R.string.error_network, e.message ?: strings.get(R.string.error_network_fallback)),
                exception = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Result.Error(
                message = strings.get(R.string.error_unexpected, e.message ?: strings.get(R.string.error_unknown)),
                exception = e
            )
        }
    }

    override suspend fun uploadFiles(files: List<FileUploadData>): Result<String> {
        return try {
            val jobId = UUID.randomUUID().toString()
            val taskData = UploadFilesTask(
                fileNames = files.map { it.fileName },
                mimeTypes = files.map { it.mimeType },
                totalSize = files.sumOf { it.bytes.size.toLong() }
            )
            val taskJson = json.encodeToString(TaskSerializer, taskData)

            Log.d(TAG, "Uploading ${files.size} files with job_id: $jobId")

            val response = client.submitFormWithBinaryData(
                url = "$BASE_URL/jobs/upload",
                formData = formData {
                    append("job_type", "BackgroundJob")
                    append("job_id", jobId)
                    append("task_data", taskJson)
                    files.forEach { file ->
                        append("files", file.bytes, Headers.build {
                            append(HttpHeaders.ContentType, file.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                        })
                    }
                }
            ).body<String>()

            Log.d(TAG, "Files uploaded successfully: $response")
            Result.Success(response)
        } catch (e: ClientRequestException) {
            Log.e(TAG, "Client error: ${e.response.status}", e)
            Result.Error(
                message = strings.get(R.string.error_client, e.response.status.value, e.response.status.description),
                exception = e
            )
        } catch (e: ServerResponseException) {
            Log.e(TAG, "Server error: ${e.response.status}", e)
            Result.Error(
                message = strings.get(R.string.error_server, e.response.status.value, e.response.status.description),
                exception = e
            )
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timed out", e)
            Result.Error(
                message = strings.get(R.string.error_timeout),
                exception = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            Result.Error(
                message = strings.get(R.string.error_network, e.message ?: strings.get(R.string.error_network_fallback)),
                exception = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Result.Error(
                message = strings.get(R.string.error_unexpected, e.message ?: strings.get(R.string.error_unknown)),
                exception = e
            )
        }
    }
}
