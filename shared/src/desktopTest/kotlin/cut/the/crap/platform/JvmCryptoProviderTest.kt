package cut.the.crap.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Ed25519 implementation.
 *
 * The RFC 8032 §7.1 vectors are the load-bearing part. `doc/IDENTITY_SPEC.md` §3.2 plans to swap
 * Bouncy Castle for cryptography-kotlin once the toolchain reaches Kotlin 2.3, and these vectors
 * are what proves the swap changes no byte of any key, signature or stored seed.
 */
class JvmCryptoProviderTest {

    private val crypto = JvmCryptoProvider()

    @Test
    fun `derives the RFC 8032 public keys`() {
        rfc8032.forEach { vector ->
            assertEquals(vector.publicKey, crypto.ed25519PublicKey(vector.seed.hexToBytes()).toHex())
        }
    }

    @Test
    fun `produces the RFC 8032 signatures`() {
        rfc8032.forEach { vector ->
            val signature = crypto.ed25519Sign(vector.seed.hexToBytes(), vector.message.hexToBytes())
            assertEquals(vector.signature, signature.toHex())
        }
    }

    @Test
    fun `verifies its own signatures`() {
        val seed = crypto.randomBytes(ED25519_SEED_SIZE)
        val publicKey = crypto.ed25519PublicKey(seed)
        val message = "the canonical signing string".encodeToByteArray()

        assertTrue(crypto.ed25519Verify(publicKey, message, crypto.ed25519Sign(seed, message)))
    }

    @Test
    fun `rejects a signature over a different message`() {
        val seed = crypto.randomBytes(ED25519_SEED_SIZE)
        val publicKey = crypto.ed25519PublicKey(seed)
        val signature = crypto.ed25519Sign(seed, "one".encodeToByteArray())

        assertFalse(crypto.ed25519Verify(publicKey, "two".encodeToByteArray(), signature))
    }

    @Test
    fun `rejects a signature from a different key`() {
        val message = "replayed".encodeToByteArray()
        val signature = crypto.ed25519Sign(crypto.randomBytes(ED25519_SEED_SIZE), message)
        val otherKey = crypto.ed25519PublicKey(crypto.randomBytes(ED25519_SEED_SIZE))

        assertFalse(crypto.ed25519Verify(otherKey, message, signature))
    }

    @Test
    fun `treats malformed key and signature sizes as a rejection, not an error`() {
        val seed = crypto.randomBytes(ED25519_SEED_SIZE)
        val publicKey = crypto.ed25519PublicKey(seed)
        val message = "m".encodeToByteArray()
        val signature = crypto.ed25519Sign(seed, message)

        assertFalse(crypto.ed25519Verify(ByteArray(31), message, signature))
        assertFalse(crypto.ed25519Verify(publicKey, message, ByteArray(63)))
        assertFalse(crypto.ed25519Verify(ByteArray(0), message, ByteArray(0)))
    }

    @Test
    fun `signing rejects a wrong-sized seed`() {
        assertFailsWith<IllegalArgumentException> { crypto.ed25519Sign(ByteArray(31), ByteArray(0)) }
        assertFailsWith<IllegalArgumentException> { crypto.ed25519PublicKey(ByteArray(33)) }
    }

    @Test
    fun `produces the expected key and signature sizes`() {
        val seed = crypto.randomBytes(ED25519_SEED_SIZE)
        assertEquals(ED25519_PUBLIC_KEY_SIZE, crypto.ed25519PublicKey(seed).size)
        assertEquals(ED25519_SIGNATURE_SIZE, crypto.ed25519Sign(seed, ByteArray(0)).size)
    }

    @Test
    fun `hashes the standard SHA-256 vectors`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            crypto.sha256(ByteArray(0)).toHex(),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            crypto.sha256("abc".encodeToByteArray()).toHex(),
        )
    }

    @Test
    fun `random bytes are the requested length and do not repeat`() {
        assertEquals(32, crypto.randomBytes(32).size)
        val draws = List(50) { crypto.randomBytes(32).toHex() }
        assertEquals(draws.size, draws.toSet().size)
    }

    private class Vector(
        val seed: String,
        val publicKey: String,
        val message: String,
        val signature: String,
    )

    private companion object {
        /** RFC 8032 §7.1 — Ed25519 test vectors 1, 2 and 3. */
        val rfc8032 = listOf(
            Vector(
                seed = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
                publicKey = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
                message = "",
                signature = "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e0652249015" +
                    "55fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
            ),
            Vector(
                seed = "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
                publicKey = "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
                message = "72",
                signature = "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69d" +
                    "a085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00",
            ),
            Vector(
                seed = "c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7",
                publicKey = "fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025",
                message = "af82",
                signature = "6291d657deec24024827e69c3abe01a30ce548a284743a445e3680d7db5ac3a" +
                    "c18ff9b538d16f290ae67f760984dc6594a7c15e9716ed28dc027beceea1ec40a",
            ),
        )
    }
}

private fun String.hexToBytes(): ByteArray =
    ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }

private fun ByteArray.toHex(): String = joinToString("") { byte ->
    val value = byte.toInt() and 0xFF
    "0123456789abcdef"[value shr 4].toString() + "0123456789abcdef"[value and 0x0F]
}
