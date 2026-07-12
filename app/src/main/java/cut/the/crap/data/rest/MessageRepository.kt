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
import cut.the.crap.R
import cut.the.crap.tools.StringProvider
import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
data class Message(val text: String)

interface MessageRepository {
    suspend fun postMessage(message: Message): Result<String>
}

class MessageRepositoryImpl constructor(
    private val client: HttpClient,
    private val strings: StringProvider
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
                message = strings.get(R.string.error_client, e.response.status.value, e.response.status.description),
                exception = e
            )
        } catch (e: ServerResponseException) {
            // 5xx errors (server errors like 500 Internal Server Error)
            Result.Error(
                message = strings.get(R.string.error_server, e.response.status.value, e.response.status.description),
                exception = e
            )
        } catch (e: SocketTimeoutException) {
            // Timeout errors
            Result.Error(
                message = strings.get(R.string.error_timeout),
                exception = e
            )
        } catch (e: IOException) {
            // Network errors (no internet, connection refused, etc.)
            Result.Error(
                message = strings.get(R.string.error_network, e.message ?: strings.get(R.string.error_network_fallback)),
                exception = e
            )
        } catch (e: Exception) {
            // Any other unexpected errors
            Result.Error(
                message = strings.get(R.string.error_unexpected, e.message ?: strings.get(R.string.error_unknown)),
                exception = e
            )
        }
    }
}
