package cut.the.crap.identity

import cut.the.crap.fake.FakeIdentityKeyStore
import cut.the.crap.platform.JvmCryptoProvider
import cut.the.crap.tools.decodeBase64Url
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdentityManagerTest {

    private val crypto = JvmCryptoProvider()

    private fun manager(keyStore: FakeIdentityKeyStore = FakeIdentityKeyStore()) =
        IdentityManager(keyStore, crypto) to keyStore

    @Test
    fun `a fresh install has no identity`() = runTest {
        val (manager, _) = manager()
        assertNull(manager.current())
        assertNull(manager.recoveryPhrase())
        assertNull(manager.sign("m".encodeToByteArray()))
    }

    @Test
    fun `getOrCreate generates and persists once`() = runTest {
        val (manager, keyStore) = manager()

        val first = manager.getOrCreate()
        val second = manager.getOrCreate()

        assertEquals(first, second)
        assertEquals(1, keyStore.stores, "the seed must be written exactly once")
        assertEquals(first.keyId, manager.current()?.keyId)
    }

    @Test
    fun `concurrent getOrCreate calls agree on one identity`() = runTest {
        val (manager, keyStore) = manager()

        val identities = List(16) { async { manager.getOrCreate() } }.awaitAll()

        assertEquals(1, identities.map { it.keyId }.toSet().size, "must not generate two seeds")
        assertEquals(1, keyStore.stores)
    }

    @Test
    fun `separate installs get different identities`() = runTest {
        val (first, _) = manager()
        val (second, _) = manager()
        assertNotEquals(first.getOrCreate().keyId, second.getOrCreate().keyId)
    }

    @Test
    fun `signatures verify against the identity's public key`() = runTest {
        val (manager, _) = manager()
        val identity = manager.getOrCreate()
        val message = "GET\n/api/ping\n1753900000\nnonce\n".encodeToByteArray()

        val signature = requireNotNull(manager.sign(message))

        assertTrue(crypto.ed25519Verify(identity.publicKey, message, signature))
    }

    @Test
    fun `the recovery phrase restores the same identity on another install`() = runTest {
        val (original, _) = manager()
        val identity = original.getOrCreate()
        val phrase = requireNotNull(original.recoveryPhrase()).joinToString(" ")

        val (fresh, keyStore) = manager()
        val result = fresh.restore(phrase)

        assertIs<MnemonicResult.Valid>(result)
        assertEquals(identity.keyId, fresh.current()?.keyId)
        assertEquals(1, keyStore.stores)
    }

    @Test
    fun `a rejected phrase leaves the existing identity untouched`() = runTest {
        val (manager, keyStore) = manager()
        val original = manager.getOrCreate()
        val storesAfterCreate = keyStore.stores

        assertIs<MnemonicResult.WrongLength>(manager.restore("abandon abandon art"))
        assertIs<MnemonicResult.ChecksumMismatch>(
            manager.restore(("abandon ".repeat(23) + "zoo").trim())
        )

        assertEquals(original.keyId, manager.current()?.keyId)
        assertEquals(storesAfterCreate, keyStore.stores, "a bad phrase must not write")
    }

    @Test
    fun `reset erases the identity and the next use makes a new one`() = runTest {
        val (manager, keyStore) = manager()
        val original = manager.getOrCreate()

        manager.reset()

        assertNull(manager.current())
        assertEquals(1, keyStore.clears)
        assertNotEquals(original.keyId, manager.getOrCreate().keyId)
    }

    @Test
    fun `addKeyProof signs the exact challenge the server rebuilds`() = runTest {
        val (manager, _) = manager()
        val identity = manager.getOrCreate()

        val claim = manager.addKeyProof("user-42")!!

        assertEquals(identity.keyId, claim.pubkey)
        assertTrue(
            crypto.ed25519Verify(
                publicKey = identity.publicKey,
                // The server builds this string itself from the authenticated user_id and the
                // pubkey in the body; if either side's format drifts, the proof stops verifying.
                message = "add-key:user-42:${identity.keyId}".encodeToByteArray(),
                signature = claim.proof.decodeBase64Url(),
            ),
            "proof must verify against add-key:<user_id>:<pubkey>",
        )
    }

    @Test
    fun `a proof is bound to one user id and one key`() = runTest {
        val (device, _) = manager()
        device.getOrCreate()
        val identity = device.current()!!

        // Same device, different identity to join: a different signature. This is what stops a
        // proof captured elsewhere from being replayed against another user.
        assertNotEquals(device.addKeyProof("user-a")!!.proof, device.addKeyProof("user-b")!!.proof)

        val (other, _) = manager()
        other.getOrCreate()
        val otherClaim = other.addKeyProof("user-a")!!
        assertNotEquals(identity.keyId, otherClaim.pubkey)
        assertTrue(
            !crypto.ed25519Verify(
                publicKey = identity.publicKey,
                message = "add-key:user-a:${otherClaim.pubkey}".encodeToByteArray(),
                signature = otherClaim.proof.decodeBase64Url(),
            ),
            "one device's proof must not verify as another's",
        )
    }

    @Test
    fun `a fresh install cannot produce a proof`() = runTest {
        val (manager, _) = manager()
        assertNull(manager.addKeyProof("user-42"))
    }

    @Test
    fun `keyId is unpadded base64url of the public key`() = runTest {
        val (manager, _) = manager()
        val identity = manager.getOrCreate()

        // 32 bytes → 43 base64 characters plus one '=' of padding, which is stripped.
        assertEquals(43, identity.keyId.length)
        assertTrue(identity.keyId.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertEquals(identity.keyId, identity.toString().substringAfter("keyId=").dropLast(1))
    }
}
