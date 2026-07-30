package cut.the.crap.identity

import cut.the.crap.platform.CryptoProvider
import cut.the.crap.platform.ED25519_SEED_SIZE
import cut.the.crap.platform.IdentityKeyStore
import cut.the.crap.tools.encodeBase64Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This install's identity: the public half of the Ed25519 keypair it signs requests with.
 *
 * The private seed is never part of this object — it stays inside [IdentityManager], which is the
 * only thing that reads it, so no caller can accidentally log or serialise it.
 */
class Identity internal constructor(val publicKey: ByteArray) {

    /** The `keyid` sent in the `CTC-Sig` header, and the server's `user_key.pubkey`. */
    val keyId: String = publicKey.encodeBase64Url()

    override fun equals(other: Any?): Boolean =
        this === other || (other is Identity && keyId == other.keyId)

    override fun hashCode(): Int = keyId.hashCode()

    /** Deliberately prints the key id only — there is no private material here, but be obvious. */
    override fun toString(): String = "Identity(keyId=$keyId)"
}

/**
 * Creates, restores and uses this install's identity.
 *
 * The private seed lives in [IdentityKeyStore] and is read on demand rather than cached, so
 * "reset identity" takes effect immediately and no long-lived copy sits in the heap.
 *
 * Nothing here talks to the network. Registering the public key with the server, and signing
 * requests with [sign], is the next step (`doc/IDENTITY_SPEC.md` §9 step 2).
 */
class IdentityManager(
    private val keyStore: IdentityKeyStore,
    private val crypto: CryptoProvider,
) : RequestSigner {
    private val bip39 = Bip39(crypto)

    // Serialises create/restore/reset against each other: two concurrent getOrCreate() calls must
    // not generate two seeds and have the second silently overwrite the first.
    private val mutex = Mutex()

    /** The current identity, or null when this install has none yet. */
    suspend fun current(): Identity? = keyStore.loadSeed()?.let { Identity(crypto.ed25519PublicKey(it)) }

    /** The current identity, generating and persisting one on first use. */
    suspend fun getOrCreate(): Identity = mutex.withLock {
        current() ?: run {
            val seed = crypto.randomBytes(ED25519_SEED_SIZE)
            keyStore.storeSeed(seed)
            Identity(crypto.ed25519PublicKey(seed))
        }
    }

    /**
     * Replaces this install's identity with the one behind [phrase].
     *
     * On [MnemonicResult.Valid] the recovered seed is stored, **overwriting any existing
     * identity** — the caller is responsible for confirming that with the user first. Every other
     * result leaves storage untouched.
     */
    suspend fun restore(phrase: String): MnemonicResult = mutex.withLock {
        bip39.decode(phrase).also { result ->
            if (result is MnemonicResult.Valid) keyStore.storeSeed(result.seed)
        }
    }

    /**
     * The 24-word recovery phrase for the current identity, or null when there is none.
     *
     * The single intended exposure of the private seed, in the form a user can write down. Show it
     * once at creation and behind a confirmation thereafter; never log it.
     */
    suspend fun recoveryPhrase(): List<String>? = keyStore.loadSeed()?.let { bip39.encode(it) }

    /**
     * Signs [message] with the current identity, or returns null when this install has none.
     *
     * The 64-byte detached signature goes into the `sig` field of the `CTC-Sig` header.
     */
    suspend fun sign(message: ByteArray): ByteArray? =
        keyStore.loadSeed()?.let { crypto.ed25519Sign(it, message) }

    /**
     * [RequestSigner] — one seed read for both halves, so the plugin cannot sign with one identity
     * and attribute it to another if a reset lands between the two calls.
     */
    override suspend fun signRequest(message: ByteArray): SignedMessage? {
        val seed = keyStore.loadSeed() ?: return null
        return SignedMessage(
            keyId = crypto.ed25519PublicKey(seed).encodeBase64Url(),
            signature = crypto.ed25519Sign(seed, message),
        )
    }

    /**
     * Erases this install's identity. The server-side user is not deleted and its campaigns still
     * exist — they simply become unmanageable, which the UI must say plainly
     * (`doc/IDENTITY_SPEC.md` §6.3).
     */
    suspend fun reset() = mutex.withLock { keyStore.clear() }
}
