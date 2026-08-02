package cut.the.crap.fake

import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.identity.DeviceKey
import cut.the.crap.data.rest.identity.IdentityRepository
import cut.the.crap.data.rest.identity.RegisteredUser
import cut.the.crap.data.rest.identity.UserProfile

/**
 * A server that behaves like `routes/identity.py` without the network.
 *
 * Errors are expressed the way the real repository expresses them — `AppError.Client(status,
 * <server error code>)` — so a test that pins the `409 last_key` handling is pinning the same
 * value the live server sends.
 */
class FakeIdentityRepository(
    var profile: UserProfile? = null,
) : IdentityRepository {

    /** Set to make the next calls fail. Cleared by hand, not automatically. */
    var failure: Result.Error? = null

    val calls = mutableListOf<String>()

    override suspend fun register(displayName: String?, keyLabel: String?): Result<RegisteredUser> {
        calls += "register"
        failure?.let { return it }
        val existing = profile
        profile = UserProfile(
            userId = existing?.userId ?: "user-1",
            displayName = displayName,
            createdAt = 100,
            keys = existing?.keys ?: listOf(DeviceKey("key-a", keyLabel, 100, null)),
        )
        return Result.Success(
            RegisteredUser(profile!!.userId, profile!!.displayName, profile!!.createdAt)
        )
    }

    override suspend fun ping(): Result<String> {
        calls += "ping"
        failure?.let { return it }
        return Result.Success(profile?.userId ?: "user-1")
    }

    override suspend fun me(): Result<UserProfile> {
        calls += "me"
        failure?.let { return it }
        return profile?.let { Result.Success(it) } ?: unknownKey()
    }

    override suspend fun setDisplayName(displayName: String?): Result<UserProfile> {
        calls += "setDisplayName"
        failure?.let { return it }
        profile = profile?.copy(displayName = displayName) ?: return unknownKey()
        return Result.Success(profile!!)
    }

    override suspend fun addKey(pubkey: String, proof: String, label: String?): Result<DeviceKey> {
        calls += "addKey($pubkey,$proof,$label)"
        failure?.let { return it }
        val key = DeviceKey(pubkey, label, 200, null)
        profile = profile?.copy(keys = profile!!.keys + key) ?: return unknownKey()
        return Result.Success(key)
    }

    override suspend fun revokeKey(pubkey: String): Result<DeviceKey> {
        calls += "revokeKey($pubkey)"
        failure?.let { return it }
        val current = profile ?: return unknownKey()
        if (current.keys.count { it.isActive } <= 1) {
            return Result.Error(AppError.Client(409, "last_key"), retryable = false)
        }
        val revoked = DeviceKey(pubkey, current.keys.find { it.pubkey == pubkey }?.label, 200, 300)
        profile = current.copy(keys = current.keys.map { if (it.pubkey == pubkey) revoked else it })
        return Result.Success(revoked)
    }

    private fun unknownKey() = Result.Error(AppError.Client(401, "unknown_key"), retryable = false)

    companion object {
        fun registered(vararg keys: DeviceKey) = FakeIdentityRepository(
            UserProfile("user-1", "Alice", 100, keys.toList())
        )

        fun key(pubkey: String, label: String? = null, revokedAt: Long? = null) =
            DeviceKey(pubkey, label, 100, revokedAt)
    }
}
