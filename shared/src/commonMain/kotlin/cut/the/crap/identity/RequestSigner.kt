package cut.the.crap.identity

/**
 * What the `CTC-Sig` Ktor plugin needs from the identity layer.
 *
 * A separate interface rather than the plugin depending on [IdentityManager] directly, so the
 * networking layer carries no dependency on key storage or crypto — and so a target that has no
 * identity implementation yet (iOS, see `doc/IDENTITY_SPEC.md` §3.2) can simply leave it unbound
 * and get an unsigned client instead of a crash at startup.
 */
interface RequestSigner {

    /**
     * Signs [message] — the canonical signing string of §4.2 — or returns null when this install
     * has no identity yet, in which case the request goes out unsigned.
     */
    suspend fun signRequest(message: ByteArray): SignedMessage?
}

/** A signature and the key id it should be attributed to. */
class SignedMessage(val keyId: String, val signature: ByteArray)
