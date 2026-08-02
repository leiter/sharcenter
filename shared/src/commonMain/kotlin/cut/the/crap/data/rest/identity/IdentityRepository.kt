package cut.the.crap.data.rest.identity

import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.Source
import cut.the.crap.identity.IdentityManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import okio.IOException

/**
 * Registers this install's public key with the campaign server and proves the signature protocol
 * works end to end.
 *
 * Signing happens in the shared client's `CtcSignature` plugin, so nothing here mentions keys.
 * [register] is the one call whose key is not yet known server-side — trust on first use.
 */
interface IdentityRepository {

    /**
     * Creates the identity if needed and registers it. Idempotent server-side: a key that is
     * already known returns its existing user rather than a second one.
     */
    suspend fun register(displayName: String? = null, keyLabel: String? = null): Result<RegisteredUser>

    /** The signed no-op. Returns the `user_id` the server resolved this install's key to. */
    suspend fun ping(): Result<String>

    /** This install's user, with every device key ever bound to it — including revoked ones. */
    suspend fun me(): Result<UserProfile>

    /** Renames the user. A blank name clears it; a user is allowed to be nameless. */
    suspend fun setDisplayName(displayName: String?): Result<UserProfile>

    /**
     * Binds a second device's key to this user.
     *
     * Called on an **already-registered** device — this request's signature is what vouches for
     * the new key — with the [cut.the.crap.identity.AddKeyProof] the new device produced.
     */
    suspend fun addKey(pubkey: String, proof: String, label: String? = null): Result<DeviceKey>

    /**
     * Revokes a device key. The server refuses to revoke the last active one (409 `last_key`),
     * because that would orphan the identity and everything it owns; "reset identity" is the
     * deliberate path for that.
     */
    suspend fun revokeKey(pubkey: String): Result<DeviceKey>
}

/** The server's view of this install's identity. */
data class RegisteredUser(
    val userId: String,
    val displayName: String?,
    val createdAt: Long,
)

/** [RegisteredUser] plus the device list — what the Settings identity section renders. */
data class UserProfile(
    val userId: String,
    val displayName: String?,
    val createdAt: Long,
    val keys: List<DeviceKey>,
)

/** One device bound to the user. Revoked keys stay listed, so a lockout is visible as such. */
data class DeviceKey(
    val pubkey: String,
    val label: String?,
    val addedAt: Long,
    val revokedAt: Long?,
) {
    val isActive: Boolean get() = revokedAt == null
}

class IdentityRepositoryImpl(
    private val client: HttpClient,
    private val config: AppConfig,
    private val identityManager: IdentityManager,
) : IdentityRepository {

    override suspend fun register(displayName: String?, keyLabel: String?): Result<RegisteredUser> {
        // The plugin can only sign once a seed exists, and registration is the first signed
        // request there is — so the identity has to be created before the call, not by it.
        identityManager.getOrCreate()

        return call {
            client.post(url(USERS_PATH)) {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(displayName, keyLabel))
            }
        }.map { response -> response.body<RegisterResponse>().toDomain() }
    }

    override suspend fun ping(): Result<String> =
        call { client.get(url(PING_PATH)) }
            .map { response -> response.body<PingResponse>().userId }

    override suspend fun me(): Result<UserProfile> =
        call { client.get(url(ME_PATH)) }
            .map { response -> response.body<UserResponse>().toDomain() }

    override suspend fun setDisplayName(displayName: String?): Result<UserProfile> =
        call {
            client.patch(url(ME_PATH)) {
                contentType(ContentType.Application.Json)
                setBody(PatchUserRequest(displayName ?: ""))
            }
        }.map { response -> response.body<UserResponse>().toDomain() }

    override suspend fun addKey(pubkey: String, proof: String, label: String?): Result<DeviceKey> =
        call {
            client.post(url(KEYS_PATH)) {
                contentType(ContentType.Application.Json)
                setBody(AddKeyRequest(pubkey, proof, label))
            }
        }.map { response -> response.body<KeyResponse>().toDomain() }

    override suspend fun revokeKey(pubkey: String): Result<DeviceKey> =
        call { client.delete(url("$KEYS_PATH/${pubkey.encodeURLPathPart()}")) }
            .map { response -> response.body<KeyResponse>().toDomain() }

    private fun url(path: String) = config.campaignBaseUrl.trimEnd('/') + path

    /**
     * Runs [block] and classifies the outcome.
     *
     * The shared client is built without `expectSuccess`, so Ktor raises nothing on 4xx/5xx and
     * `body<T>()` would fail with a confusing transformation error instead — the status has to be
     * classified here. Same reasoning as `CampaignRepositoryImpl`.
     */
    private suspend inline fun call(
        block: () -> io.ktor.client.statement.HttpResponse,
    ): Result<io.ktor.client.statement.HttpResponse> = try {
        val response = block()
        val status = response.status
        when {
            // 401 means the signature, the timestamp or the nonce was rejected. Never retry it
            // blindly: a replayed nonce would be rejected again, and a clock-skew retry needs the
            // clock fixed first.
            status.value == 401 -> Result.Error(
                AppError.Client(401, response.errorCode() ?: status.description),
                retryable = false,
            )
            status.value >= 500 -> Result.Error(AppError.SourceServer(Source.CAMPAIGN, status.value))
            !status.isSuccess() -> Result.Error(
                AppError.Client(status.value, response.errorCode() ?: status.description),
                retryable = false,
            )
            else -> Result.Success(response)
        }
    } catch (e: SocketTimeoutException) {
        Result.Error(AppError.SourceTimeout(Source.CAMPAIGN), e)
    } catch (e: ContentConvertException) {
        Result.Error(AppError.Unavailable(Source.CAMPAIGN), e, retryable = false)
    } catch (e: SerializationException) {
        Result.Error(AppError.ParseError, e, retryable = false)
    } catch (e: IOException) {
        Result.Error(AppError.Network(e.message), e)
    } catch (e: Exception) {
        Result.Error(AppError.FetchFailed(Source.CAMPAIGN, e.message), e)
    }

    /**
     * The machine-readable `error` code from the server's error body (`last_key`, `key_taken`,
     * `invalid_proof`, …), or null if the body is not one.
     *
     * Worth the extra read: "409" alone cannot tell the user whether they tried to revoke their
     * last device or to claim a key that already belongs to somebody else, and those need
     * different words on screen.
     */
    private suspend fun io.ktor.client.statement.HttpResponse.errorCode(): String? = try {
        body<ErrorResponse>().error.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val USERS_PATH = "/api/users"
        const val PING_PATH = "/api/ping"
        const val ME_PATH = "/api/users/me"
        const val KEYS_PATH = "/api/users/keys"
    }
}

private inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> try {
        Result.Success(transform(data))
    } catch (e: SerializationException) {
        Result.Error(AppError.ParseError, e, retryable = false)
    } catch (e: ContentConvertException) {
        Result.Error(AppError.Unavailable(Source.CAMPAIGN), e, retryable = false)
    }
    is Result.Error -> this
}

@Serializable
private data class RegisterRequest(
    val display_name: String? = null,
    val key_label: String? = null,
)

@Serializable
private data class RegisterResponse(
    val user_id: String = "",
    val display_name: String? = null,
    val created_at: Long = 0,
) {
    fun toDomain() = RegisteredUser(user_id, display_name, created_at)
}

@Serializable
private data class PingResponse(val user_id: String = "") {
    val userId: String get() = user_id
}

@Serializable
private data class UserResponse(
    val user_id: String = "",
    val display_name: String? = null,
    val created_at: Long = 0,
    val keys: List<KeyResponse> = emptyList(),
) {
    fun toDomain() = UserProfile(user_id, display_name, created_at, keys.map { it.toDomain() })
}

@Serializable
private data class KeyResponse(
    val pubkey: String = "",
    val label: String? = null,
    val added_at: Long = 0,
    val revoked_at: Long? = null,
) {
    fun toDomain() = DeviceKey(pubkey, label, added_at, revoked_at)
}

@Serializable
private data class PatchUserRequest(val display_name: String)

@Serializable
private data class AddKeyRequest(
    val pubkey: String,
    val proof: String,
    val label: String? = null,
)

@Serializable
private data class ErrorResponse(val error: String = "", val detail: String? = null)
