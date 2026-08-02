package cut.the.crap.platform

/**
 * The primitives the identity layer needs, behind a platform seam.
 *
 * Kept deliberately small and stateless: every operation takes its key material as a parameter, so
 * implementations hold nothing and are trivially testable. Callers get key material from
 * [IdentityKeyStore], not from here.
 *
 * ### Why a seam at all
 *
 * `minSdk = 26` rules out Android's own Ed25519, so an implementation has to be bundled, and the
 * bundled one differs per target (see `doc/IDENTITY_SPEC.md` §3.2 — Bouncy Castle on the two JVM
 * targets, CryptoKit on iOS, migrating to cryptography-kotlin once the toolchain reaches Kotlin
 * 2.3). Everything above this interface is pure common code and never learns which is in use.
 *
 * This is an interface bound in Koin rather than `expect`/`actual` — matching [Clipboard],
 * [Notifier] and [UrlOpener] — which is also what lets iOS compile while its implementation is
 * still outstanding.
 *
 * All byte arrays use the canonical raw Ed25519 encodings: 32-byte seeds, 32-byte public keys,
 * 64-byte detached signatures. Those encodings are fixed by `doc/IDENTITY_SPEC.md` §4.2 rather
 * than by any library, so swapping the implementation changes nothing on the wire or on disk.
 */
interface CryptoProvider {

    /** [size] cryptographically secure random bytes. */
    fun randomBytes(size: Int): ByteArray

    /** The SHA-256 digest of [data] — 32 bytes. */
    fun sha256(data: ByteArray): ByteArray

    /**
     * Derives the 32-byte Ed25519 public key from a 32-byte private [seed].
     *
     * @throws IllegalArgumentException if [seed] is not 32 bytes.
     */
    fun ed25519PublicKey(seed: ByteArray): ByteArray

    /**
     * Signs [message] with the private key derived from [seed], returning a 64-byte detached
     * signature.
     *
     * @throws IllegalArgumentException if [seed] is not 32 bytes.
     */
    fun ed25519Sign(seed: ByteArray, message: ByteArray): ByteArray

    /**
     * Verifies a 64-byte detached [signature] over [message] against a 32-byte [publicKey].
     *
     * Returns false — rather than throwing — for malformed input, so callers can treat every
     * rejection the same way.
     */
    fun ed25519Verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean
}

/** Length of an Ed25519 private seed, in bytes. */
const val ED25519_SEED_SIZE: Int = 32

/** Length of an Ed25519 public key, in bytes. */
const val ED25519_PUBLIC_KEY_SIZE: Int = 32

/** Length of an Ed25519 detached signature, in bytes. */
const val ED25519_SIGNATURE_SIZE: Int = 64
