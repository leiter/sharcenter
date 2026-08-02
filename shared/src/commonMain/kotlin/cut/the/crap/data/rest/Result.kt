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
     * @param error What went wrong, as a typed [AppError]. The data layer describes the
     *        failure; the UI decides how to phrase it (see `AppError.localized()` in :app).
     * @param exception The underlying exception (optional)
     * @param retryable Whether the failure is transient (network/server) and worth
     *        retrying later. False for permanent failures such as a deleted/private
     *        resource or a malformed request that will never succeed.
     */
    data class Error(
        val error: AppError,
        val exception: Throwable? = null,
        val retryable: Boolean = true
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
        is Error -> throw exception ?: Exception(error.debugText)
    }
}
