package cut.the.crap.data.rest

/**
 * A generic wrapper for API responses that can represent success or failure
 */
sealed class Result<out T> {
    /**
     * Represents a successful API response
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed API response
     * @param message Human-readable error message
     * @param exception The underlying exception (optional)
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : Result<Nothing>()

    /**
     * Returns true if this is a Success result
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Returns true if this is an Error result
     */
    fun isError(): Boolean = this is Error

    /**
     * Returns the data if Success, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Returns the data if Success, throws exception if Error
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception ?: Exception(message)
    }
}
