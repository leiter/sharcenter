package cut.the.crap.ui.content.settings.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.identity.DeviceKey
import cut.the.crap.data.rest.identity.IdentityRepository
import cut.the.crap.identity.IdentityManager
import cut.the.crap.identity.MnemonicResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the server currently says about this install's key.
 *
 * Modelled as states rather than a `registered: Boolean` because the interesting ones are not
 * opposites: [Unregistered] wants a "create" button, [Revoked] wants "this device was locked out,
 * restore a phrase or start over", and [Unreachable] must not be shown as either — a flaky
 * connection is not a lost identity.
 */
sealed interface ServerState {
    /** Not asked yet, or there is no local key to ask about. */
    data object Unknown : ServerState

    /** A key exists on this device, but the server has never seen it. */
    data object Unregistered : ServerState

    data class Registered(
        val userId: String,
        val displayName: String?,
        val devices: List<DeviceKey>,
    ) : ServerState {
        val activeDevices: List<DeviceKey> get() = devices.filter { it.isActive }
    }

    /** This device's key was revoked — by this user, from another device. */
    data object Revoked : ServerState

    data object Unreachable : ServerState
}

/**
 * One-shot outcomes the screen turns into text.
 *
 * The view model deliberately holds no strings: it runs in `commonMain` and must not reach for
 * Compose resources, and a test that asserts [LAST_KEY] keeps passing when the wording changes.
 */
enum class IdentityNotice {
    REGISTERED,
    RENAMED,
    DEVICE_LINKED,
    DEVICE_REVOKED,
    LAST_KEY,
    KEY_TAKEN,
    KEY_ALREADY_REVOKED,
    INVALID_PROOF,
    INVALID_CODE,
    PHRASE_RESTORED,
    PHRASE_WRONG_LENGTH,
    PHRASE_UNKNOWN_WORD,
    PHRASE_CHECKSUM,
    IDENTITY_RESET,
    NO_IDENTITY,
    NOT_AUTHENTICATED,
    NETWORK_ERROR,
    SERVER_ERROR,
}

data class IdentityUiState(
    val busy: Boolean = true,
    /** This install's public key, or null when it has no identity yet. */
    val keyId: String? = null,
    val server: ServerState = ServerState.Unknown,
    /** The 24 words, non-null only while the user has explicitly asked to see them. */
    val recoveryPhrase: List<String>? = null,
    /** `<pubkey>:<proof>` for this device to be pasted into an already-registered one. */
    val linkCode: String? = null,
    val notice: IdentityNotice? = null,
) {
    val hasIdentity: Boolean get() = keyId != null
}

/**
 * Backs the identity section of Settings (`doc/IDENTITY_SPEC.md` §5, §6).
 *
 * Every mutation goes through [IdentityManager] or [IdentityRepository]; nothing here touches key
 * material directly, and the seed never enters the UI state except as the recovery phrase the user
 * explicitly asked to see.
 */
class IdentityViewModel(
    private val identityManager: IdentityManager,
    private val repository: IdentityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(IdentityUiState())
    val state: StateFlow<IdentityUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads the local key and asks the server what it knows about it. */
    fun refresh() = launch {
        val keyId = identityManager.current()?.keyId
        if (keyId == null) {
            _state.value = IdentityUiState(busy = false, keyId = null, server = ServerState.Unknown)
            return@launch
        }
        _state.value = _state.value.copy(keyId = keyId, server = serverState())
    }

    /** Creates the identity if needed and registers it (trust on first use). */
    fun createIdentity(displayName: String?, deviceLabel: String?) = launch {
        // Explicitly, rather than leaving it to the repository: registration is signed by the key
        // it registers, so the key has to exist first, and that ordering is this screen's
        // business — not a detail of whichever IdentityRepository happens to be bound.
        identityManager.getOrCreate()
        when (val result = repository.register(displayName?.trim(), deviceLabel?.trim())) {
            is Result.Success -> _state.value = _state.value.copy(
                keyId = identityManager.current()?.keyId,
                server = serverState(),
                notice = IdentityNotice.REGISTERED,
            )
            is Result.Error -> _state.value = _state.value.copy(
                keyId = identityManager.current()?.keyId,
                notice = result.notice(),
            )
        }
    }

    fun rename(displayName: String) = launch {
        when (val result = repository.setDisplayName(displayName.trim().ifEmpty { null })) {
            is Result.Success -> _state.value = _state.value.copy(
                server = result.data.toRegistered(),
                notice = IdentityNotice.RENAMED,
            )
            is Result.Error -> _state.value = _state.value.copy(notice = result.notice())
        }
    }

    /**
     * Produces the code this (new) device hands to an already-registered one.
     *
     * [userId] is read off the other device by the user. It is part of the signed challenge, so a
     * typo produces a code the server rejects rather than one that works against someone else.
     */
    fun createLinkCode(userId: String) = launch {
        val trimmed = userId.trim()
        // A device that has never registered still has no key; make one, then prove we hold it.
        identityManager.getOrCreate()
        val claim = identityManager.addKeyProof(trimmed)
        _state.value = _state.value.copy(
            keyId = identityManager.current()?.keyId,
            linkCode = claim?.let { "${it.pubkey}$LINK_CODE_SEPARATOR${it.proof}" },
            notice = if (claim == null) IdentityNotice.NO_IDENTITY else null,
        )
    }

    fun clearLinkCode() {
        _state.value = _state.value.copy(linkCode = null)
    }

    /** Accepts the code produced by [createLinkCode] on another device and binds its key here. */
    fun linkDevice(code: String, label: String?) = launch {
        val parts = code.trim().split(LINK_CODE_SEPARATOR)
        if (parts.size != 2 || parts.any { it.isEmpty() }) {
            _state.value = _state.value.copy(notice = IdentityNotice.INVALID_CODE)
            return@launch
        }
        when (val result = repository.addKey(parts[0], parts[1], label?.trim()?.ifEmpty { null })) {
            is Result.Success -> _state.value = _state.value.copy(
                server = serverState(),
                notice = IdentityNotice.DEVICE_LINKED,
            )
            is Result.Error -> _state.value = _state.value.copy(notice = result.notice())
        }
    }

    fun revokeDevice(pubkey: String) = launch {
        when (val result = repository.revokeKey(pubkey)) {
            is Result.Success -> _state.value = _state.value.copy(
                server = serverState(),
                notice = IdentityNotice.DEVICE_REVOKED,
            )
            is Result.Error -> _state.value = _state.value.copy(notice = result.notice())
        }
    }

    /** Reveals the 24 words. The caller is expected to have confirmed with the user first. */
    fun revealRecoveryPhrase() = launch {
        val phrase = identityManager.recoveryPhrase()
        _state.value = _state.value.copy(
            recoveryPhrase = phrase,
            notice = if (phrase == null) IdentityNotice.NO_IDENTITY else null,
        )
    }

    fun hideRecoveryPhrase() {
        _state.value = _state.value.copy(recoveryPhrase = null)
    }

    /**
     * Replaces this install's identity with the one behind [phrase].
     *
     * Destructive: the caller must confirm first. A phrase that does not decode leaves the
     * existing identity untouched, which is why the failure notices are specific — "wrong length"
     * and "that is not a word" are things the user can act on.
     */
    fun restore(phrase: String) = launch {
        when (val result = identityManager.restore(phrase)) {
            is MnemonicResult.Valid -> _state.value = IdentityUiState(
                busy = false,
                keyId = identityManager.current()?.keyId,
                server = serverState(),
                notice = IdentityNotice.PHRASE_RESTORED,
            )
            is MnemonicResult.WrongLength ->
                _state.value = _state.value.copy(notice = IdentityNotice.PHRASE_WRONG_LENGTH)
            is MnemonicResult.UnknownWord ->
                _state.value = _state.value.copy(notice = IdentityNotice.PHRASE_UNKNOWN_WORD)
            MnemonicResult.ChecksumMismatch ->
                _state.value = _state.value.copy(notice = IdentityNotice.PHRASE_CHECKSUM)
        }
    }

    /**
     * Erases this install's identity (§6.3). The server-side user survives and keeps its
     * campaigns — they simply become unmanageable, which the screen says before asking.
     */
    fun resetIdentity() = launch {
        identityManager.reset()
        _state.value = IdentityUiState(busy = false, notice = IdentityNotice.IDENTITY_RESET)
    }

    fun consumeNotice() {
        _state.value = _state.value.copy(notice = null)
    }

    // --- internals ----------------------------------------------------------

    private suspend fun serverState(): ServerState = when (val result = repository.me()) {
        is Result.Success -> result.data.toRegistered()
        is Result.Error -> when {
            result.error.isCode("unknown_key") -> ServerState.Unregistered
            result.error.isCode("key_revoked") -> ServerState.Revoked
            else -> ServerState.Unreachable
        }
    }

    private fun cut.the.crap.data.rest.identity.UserProfile.toRegistered() =
        ServerState.Registered(userId, displayName, keys)

    /**
     * Every entry point sets [IdentityUiState.busy] and clears it again, so a slow round trip
     * cannot be double-submitted from the UI.
     */
    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, notice = null)
            try {
                block()
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    private companion object {
        const val LINK_CODE_SEPARATOR = ":"
    }
}

/** The server's machine-readable `error` code, which the repository puts in [AppError.Client]. */
private fun AppError.isCode(code: String): Boolean =
    this is AppError.Client && description == code

/**
 * Status *and* code, not code alone: `key_revoked` means two different things depending on where
 * it comes from. As a 401 it means this device was locked out; as a 409 it means the key someone
 * is trying to link was revoked earlier and will not be revived.
 */
private fun Result.Error.notice(): IdentityNotice {
    val client = error as? AppError.Client ?: return when {
        error is AppError.SourceServer -> IdentityNotice.SERVER_ERROR
        retryable -> IdentityNotice.NETWORK_ERROR
        else -> IdentityNotice.SERVER_ERROR
    }
    return when {
        client.status == 409 && client.description == "last_key" -> IdentityNotice.LAST_KEY
        client.status == 409 && client.description == "key_taken" -> IdentityNotice.KEY_TAKEN
        client.status == 409 && client.description == "key_revoked" ->
            IdentityNotice.KEY_ALREADY_REVOKED
        client.status == 400 && client.description == "invalid_proof" ->
            IdentityNotice.INVALID_PROOF
        client.status == 401 -> IdentityNotice.NOT_AUTHENTICATED
        else -> IdentityNotice.SERVER_ERROR
    }
}
