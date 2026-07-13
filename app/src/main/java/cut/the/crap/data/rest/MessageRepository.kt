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

@Serializable
data class Message(val text: String)

interface MessageRepository {
    suspend fun postMessage(message: Message): Result<String>
}

class MessageRepositoryImpl constructor(
    private val client: HttpClient,
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
                error = AppError.Client(e.response.status.value, e.response.status.description),
                exception = e
            )
        } catch (e: ServerResponseException) {
            // 5xx errors (server errors like 500 Internal Server Error)
            Result.Error(
                error = AppError.Server(e.response.status.value, e.response.status.description),
                exception = e
            )
        } catch (e: SocketTimeoutException) {
            // Timeout errors
            Result.Error(
                error = AppError.Timeout,
                exception = e
            )
        } catch (e: IOException) {
            // Network errors (no internet, connection refused, etc.)
            Result.Error(
                error = AppError.Network(e.message),
                exception = e
            )
        } catch (e: Exception) {
            // Any other unexpected errors
            Result.Error(
                error = AppError.Unexpected(e.message),
                exception = e
            )
        }
    }
}
