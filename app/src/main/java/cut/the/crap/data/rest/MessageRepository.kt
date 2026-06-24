package cut.the.crap.data.rest

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import java.io.IOException
import javax.inject.Inject

@Serializable
data class Message(val text: String)

interface MessageRepository {
    suspend fun postMessage(message: Message): Result<String>
}

class MessageRepositoryImpl @Inject constructor(
    private val client: HttpClient
) : MessageRepository {

    override suspend fun postMessage(message: Message): Result<String> {
        return try {
            val response = client.post("/api/messages") {
                contentType(ContentType.Application.Json)
                setBody(message)
            }.body<String>()

            Result.Success(response)
        } catch (e: ClientRequestException) {
            // 4xx errors (client errors like 400 Bad Request, 404 Not Found)
            Result.Error(
                message = "Client error: ${e.response.status.value} - ${e.response.status.description}",
                exception = e
            )
        } catch (e: ServerResponseException) {
            // 5xx errors (server errors like 500 Internal Server Error)
            Result.Error(
                message = "Server error: ${e.response.status.value} - ${e.response.status.description}",
                exception = e
            )
        } catch (e: SocketTimeoutException) {
            // Timeout errors
            Result.Error(
                message = "Request timed out. Please check your internet connection.",
                exception = e
            )
        } catch (e: IOException) {
            // Network errors (no internet, connection refused, etc.)
            Result.Error(
                message = "Network error: ${e.message ?: "Unable to connect to server"}",
                exception = e
            )
        } catch (e: Exception) {
            // Any other unexpected errors
            Result.Error(
                message = "Unexpected error: ${e.message ?: "Unknown error occurred"}",
                exception = e
            )
        }
    }
}
