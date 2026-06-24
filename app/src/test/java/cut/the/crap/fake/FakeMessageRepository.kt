package cut.the.crap.fake

import cut.the.crap.data.rest.Message
import cut.the.crap.data.rest.MessageRepository
import cut.the.crap.data.rest.Result

/**
 * Fake implementation of MessageRepository for testing.
 * Allows configuring success/failure responses for tests.
 */
class FakeMessageRepository : MessageRepository {

    private var nextResult: Result<String> = Result.Success("OK")
    private val sentMessages = mutableListOf<Message>()

    override suspend fun postMessage(message: Message): Result<String> {
        sentMessages.add(message)
        return nextResult
    }

    // Test helpers
    fun setNextResult(result: Result<String>) {
        nextResult = result
    }

    fun setSuccessResponse(response: String) {
        nextResult = Result.Success(response)
    }

    fun setErrorResponse(message: String, exception: Throwable? = null) {
        nextResult = Result.Error(message, exception)
    }

    fun getSentMessages(): List<Message> = sentMessages.toList()

    fun getLastSentMessage(): Message? = sentMessages.lastOrNull()

    fun reset() {
        nextResult = Result.Success("OK")
        sentMessages.clear()
    }
}
