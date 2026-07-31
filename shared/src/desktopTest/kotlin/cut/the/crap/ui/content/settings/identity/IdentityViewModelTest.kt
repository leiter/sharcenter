package cut.the.crap.ui.content.settings.identity

import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.fake.FakeIdentityKeyStore
import cut.the.crap.fake.FakeIdentityRepository
import cut.the.crap.identity.IdentityManager
import cut.the.crap.platform.JvmCryptoProvider
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.tools.decodeBase64Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The identity section of Settings.
 *
 * The value here is in the states the screen has to distinguish: "no key yet", "key the server has
 * never seen", "key this user revoked from another device" and "server unreachable" all look
 * similar from a failed request and mean entirely different things to the person holding the
 * phone. Getting them confused would tell someone their identity is gone because their train went
 * into a tunnel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IdentityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val crypto = JvmCryptoProvider()
    private val keyStore = FakeIdentityKeyStore()
    private val manager = IdentityManager(keyStore, crypto)

    private fun viewModel(repository: FakeIdentityRepository) =
        IdentityViewModel(manager, repository)

    // --- states -------------------------------------------------------------

    @Test
    fun `a fresh install has no identity and asks the server nothing`() = runTest(testDispatcher) {
        val repository = FakeIdentityRepository()
        val model = viewModel(repository)
        advanceUntilIdle()

        val state = model.state.value
        assertTrue(!state.hasIdentity)
        assertEquals(ServerState.Unknown, state.server)
        assertTrue(repository.calls.isEmpty(), "no key means nothing to ask about")
        assertTrue(!state.busy)
    }

    @Test
    fun `creating an identity registers it and reports the server view`() = runTest(testDispatcher) {
        val repository = FakeIdentityRepository()
        val model = viewModel(repository)
        advanceUntilIdle()

        model.createIdentity("Alice", "Pixel")
        advanceUntilIdle()

        val state = model.state.value
        assertEquals(IdentityNotice.REGISTERED, state.notice)
        assertEquals(manager.current()!!.keyId, state.keyId)
        val server = assertIs<ServerState.Registered>(state.server)
        assertEquals("user-1", server.userId)
        assertEquals("Alice", server.displayName)
    }

    @Test
    fun `a key the server has never seen reads as unregistered, not as broken`() =
        runTest(testDispatcher) {
            manager.getOrCreate()
            val model = viewModel(FakeIdentityRepository(profile = null))
            advanceUntilIdle()

            assertEquals(ServerState.Unregistered, model.state.value.server)
            assertTrue(model.state.value.hasIdentity)
        }

    @Test
    fun `a revoked key reads as revoked`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val repository = FakeIdentityRepository().apply {
            failure = Result.Error(AppError.Client(401, "key_revoked"), retryable = false)
        }
        val model = viewModel(repository)
        advanceUntilIdle()

        assertEquals(ServerState.Revoked, model.state.value.server)
    }

    @Test
    fun `an unreachable server is not mistaken for a lost identity`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val repository = FakeIdentityRepository().apply {
            failure = Result.Error(AppError.Network("no route"), retryable = true)
        }
        val model = viewModel(repository)
        advanceUntilIdle()

        assertEquals(ServerState.Unreachable, model.state.value.server)
        assertEquals(manager.current()!!.keyId, model.state.value.keyId, "the key is still here")
    }

    // --- profile and devices ------------------------------------------------

    @Test
    fun `renaming updates the shown name`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val model = viewModel(FakeIdentityRepository.registered(FakeIdentityRepository.key("k1")))
        advanceUntilIdle()

        model.rename("  Renamed  ")
        advanceUntilIdle()

        assertEquals(IdentityNotice.RENAMED, model.state.value.notice)
        assertEquals("Renamed", assertIs<ServerState.Registered>(model.state.value.server).displayName)
    }

    @Test
    fun `revoking the last device is refused with a specific reason`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val model = viewModel(FakeIdentityRepository.registered(FakeIdentityRepository.key("k1")))
        advanceUntilIdle()

        model.revokeDevice("k1")
        advanceUntilIdle()

        assertEquals(IdentityNotice.LAST_KEY, model.state.value.notice)
        val server = assertIs<ServerState.Registered>(model.state.value.server)
        assertEquals(1, server.activeDevices.size, "the device must still be usable")
    }

    @Test
    fun `revoking a second device leaves one active`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val model = viewModel(
            FakeIdentityRepository.registered(
                FakeIdentityRepository.key("k1", "Phone"),
                FakeIdentityRepository.key("k2", "Laptop"),
            )
        )
        advanceUntilIdle()

        model.revokeDevice("k2")
        advanceUntilIdle()

        assertEquals(IdentityNotice.DEVICE_REVOKED, model.state.value.notice)
        val server = assertIs<ServerState.Registered>(model.state.value.server)
        assertEquals(listOf("k1"), server.activeDevices.map { it.pubkey })
        assertEquals(2, server.devices.size, "a revoked device stays visible as revoked")
    }

    // --- linking ------------------------------------------------------------

    @Test
    fun `the pairing code is this device's key and a proof over the given user id`() =
        runTest(testDispatcher) {
            val model = viewModel(FakeIdentityRepository())
            advanceUntilIdle()

            model.createLinkCode("  user-1  ")
            advanceUntilIdle()

            val code = model.state.value.linkCode!!
            val (pubkey, proof) = code.split(":")
            assertEquals(manager.current()!!.keyId, pubkey)
            assertTrue(
                crypto.ed25519Verify(
                    publicKey = pubkey.decodeBase64Url(),
                    message = "add-key:user-1:$pubkey".encodeToByteArray(),
                    signature = proof.decodeBase64Url(),
                ),
                "the code must carry a proof the server will accept",
            )
        }

    @Test
    fun `linking splits the code and passes both halves through`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val repository = FakeIdentityRepository.registered(FakeIdentityRepository.key("k1"))
        val model = viewModel(repository)
        advanceUntilIdle()

        model.linkDevice("k2:the-proof", "Laptop")
        advanceUntilIdle()

        assertEquals(IdentityNotice.DEVICE_LINKED, model.state.value.notice)
        assertTrue(repository.calls.contains("addKey(k2,the-proof,Laptop)"))
        assertEquals(2, assertIs<ServerState.Registered>(model.state.value.server).activeDevices.size)
    }

    @Test
    fun `a malformed pairing code never reaches the server`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val repository = FakeIdentityRepository.registered(FakeIdentityRepository.key("k1"))
        val model = viewModel(repository)
        advanceUntilIdle()
        repository.calls.clear()

        model.linkDevice("not-a-code", null)
        advanceUntilIdle()

        assertEquals(IdentityNotice.INVALID_CODE, model.state.value.notice)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `a rejected proof is reported as such, not as a generic failure`() =
        runTest(testDispatcher) {
            manager.getOrCreate()
            val repository = FakeIdentityRepository.registered(FakeIdentityRepository.key("k1"))
            val model = viewModel(repository)
            advanceUntilIdle()
            repository.failure = Result.Error(AppError.Client(400, "invalid_proof"), retryable = false)

            model.linkDevice("k2:wrong", null)
            advanceUntilIdle()

            assertEquals(IdentityNotice.INVALID_PROOF, model.state.value.notice)
        }

    @Test
    fun `a key that belongs to someone else is reported as such`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val repository = FakeIdentityRepository.registered(FakeIdentityRepository.key("k1"))
        val model = viewModel(repository)
        advanceUntilIdle()
        repository.failure = Result.Error(AppError.Client(409, "key_taken"), retryable = false)

        model.linkDevice("k2:proof", null)
        advanceUntilIdle()

        assertEquals(IdentityNotice.KEY_TAKEN, model.state.value.notice)
    }

    // --- recovery phrase and reset ------------------------------------------

    @Test
    fun `the recovery phrase is only present after it is asked for`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val model = viewModel(FakeIdentityRepository.registered(FakeIdentityRepository.key("k1")))
        advanceUntilIdle()
        assertNull(model.state.value.recoveryPhrase, "must not be in state until requested")

        model.revealRecoveryPhrase()
        advanceUntilIdle()
        assertEquals(24, model.state.value.recoveryPhrase!!.size)

        model.hideRecoveryPhrase()
        assertNull(model.state.value.recoveryPhrase)
    }

    @Test
    fun `restoring a phrase replaces the identity`() = runTest(testDispatcher) {
        val other = IdentityManager(FakeIdentityKeyStore(), crypto)
        val otherIdentity = other.getOrCreate()
        val phrase = other.recoveryPhrase()!!.joinToString(" ")

        manager.getOrCreate()
        val model = viewModel(FakeIdentityRepository.registered(FakeIdentityRepository.key("k1")))
        advanceUntilIdle()

        model.restore(phrase)
        advanceUntilIdle()

        assertEquals(IdentityNotice.PHRASE_RESTORED, model.state.value.notice)
        assertEquals(otherIdentity.keyId, model.state.value.keyId)
    }

    @Test
    fun `a bad phrase says what is wrong and changes nothing`() = runTest(testDispatcher) {
        val original = manager.getOrCreate()
        val model = viewModel(FakeIdentityRepository.registered(FakeIdentityRepository.key("k1")))
        advanceUntilIdle()

        model.restore("abandon abandon abandon")
        advanceUntilIdle()
        assertEquals(IdentityNotice.PHRASE_WRONG_LENGTH, model.state.value.notice)

        model.restore(("abandon ".repeat(23) + "zoo").trim())
        advanceUntilIdle()
        assertEquals(IdentityNotice.PHRASE_CHECKSUM, model.state.value.notice)

        model.restore(("abandon ".repeat(23) + "notaword").trim())
        advanceUntilIdle()
        assertEquals(IdentityNotice.PHRASE_UNKNOWN_WORD, model.state.value.notice)

        assertEquals(original.keyId, manager.current()!!.keyId, "the identity must be untouched")
    }

    @Test
    fun `reset erases the identity and returns to the empty state`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val model = viewModel(FakeIdentityRepository.registered(FakeIdentityRepository.key("k1")))
        advanceUntilIdle()

        model.resetIdentity()
        advanceUntilIdle()

        assertEquals(IdentityNotice.IDENTITY_RESET, model.state.value.notice)
        assertNull(model.state.value.keyId)
        assertEquals(ServerState.Unknown, model.state.value.server)
        assertNull(manager.current())
    }

    @Test
    fun `a notice is consumed once`() = runTest(testDispatcher) {
        manager.getOrCreate()
        val model = viewModel(FakeIdentityRepository.registered(FakeIdentityRepository.key("k1")))
        advanceUntilIdle()
        model.rename("x")
        advanceUntilIdle()

        assertEquals(IdentityNotice.RENAMED, model.state.value.notice)
        model.consumeNotice()
        assertNull(model.state.value.notice)
    }
}
