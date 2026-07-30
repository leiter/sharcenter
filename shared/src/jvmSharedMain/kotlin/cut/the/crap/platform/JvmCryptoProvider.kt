package cut.the.crap.platform

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom

/**
 * [CryptoProvider] for the two JVM targets, on Bouncy Castle's *lightweight* API.
 *
 * Shared by Android and desktop through the `jvmSharedMain` source set. The lightweight API
 * (`org.bouncycastle.crypto.*`) is plain Java with no JCE provider registration, so the same code
 * runs at `minSdk = 26` and on the desktop JDK — which is why this is one implementation rather
 * than two. See `doc/IDENTITY_SPEC.md` §3.2 for why Bouncy Castle rather than cryptography-kotlin
 * (its EdDSA release ships klibs this project's Kotlin cannot consume) or libsodium (JNA and
 * per-ABI native libraries on Android, for a primitive available here in pure Java).
 */
class JvmCryptoProvider : CryptoProvider {

    // Seeded by the OS; on Android this is the same SecureRandom the platform uses everywhere.
    private val random = SecureRandom()

    override fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also(random::nextBytes)

    override fun sha256(data: ByteArray): ByteArray {
        val digest = SHA256Digest()
        digest.update(data, 0, data.size)
        return ByteArray(digest.digestSize).also { digest.doFinal(it, 0) }
    }

    override fun ed25519PublicKey(seed: ByteArray): ByteArray =
        privateKey(seed).generatePublicKey().encoded

    override fun ed25519Sign(seed: ByteArray, message: ByteArray): ByteArray =
        Ed25519Signer().run {
            init(true, privateKey(seed))
            update(message, 0, message.size)
            generateSignature()
        }

    override fun ed25519Verify(
        publicKey: ByteArray,
        message: ByteArray,
        signature: ByteArray,
    ): Boolean {
        // Malformed input is a rejection, not an error: callers treat every failure identically,
        // and Bouncy Castle throws on a wrong-sized key rather than returning false.
        if (publicKey.size != ED25519_PUBLIC_KEY_SIZE) return false
        if (signature.size != ED25519_SIGNATURE_SIZE) return false

        return Ed25519Signer().run {
            init(false, Ed25519PublicKeyParameters(publicKey, 0))
            update(message, 0, message.size)
            verifySignature(signature)
        }
    }

    private fun privateKey(seed: ByteArray): Ed25519PrivateKeyParameters {
        require(seed.size == ED25519_SEED_SIZE) {
            "Seed must be $ED25519_SEED_SIZE bytes, was ${seed.size}"
        }
        return Ed25519PrivateKeyParameters(seed, 0)
    }
}
