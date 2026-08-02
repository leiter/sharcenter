package cut.the.crap.ui.content.settings.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.data.rest.identity.DeviceKey
import cut.the.crap.platform.Notifier
import cut.the.crap.shared.resources.*
import cut.the.crap.tools.formatMediumDateTime
import cut.the.crap.ui.components.BottomNavigationBar
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The identity section of Settings (`doc/IDENTITY_SPEC.md` §5–§6): who this install is, which
 * devices share that identity, the recovery phrase, and the two destructive actions.
 *
 * Everything destructive is behind a confirmation that says what is actually lost — restoring a
 * phrase overwrites the current key, revoking is permanent, and reset strands every campaign the
 * identity owns. None of those can be undone from here or by the server (D4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityScreen(
    navController: NavHostController,
    viewModel: IdentityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val notifier: Notifier = koinInject()
    val clipboard = LocalClipboardManager.current
    val copiedMessage = stringResource(Res.string.identity_copied)

    val copy: (String) -> Unit = { text ->
        clipboard.setText(AnnotatedString(text))
        notifier.show(copiedMessage)
    }

    // Notices are one-shot: show, then clear, so re-entering the screen does not replay the last
    // outcome as if it just happened.
    state.notice?.let { notice ->
        LaunchedEffect(notice) {
            notifier.show(getString(notice.text()))
            viewModel.consumeNotice()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.settings_identity)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp).testTag(IDENTITY_LIST_TAG),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    !state.hasIdentity -> item { CreateIdentityCard(state, viewModel) }

                    else -> {
                        item { ServerStatusCard(state, viewModel, copy) }
                        (state.server as? ServerState.Registered)?.let { registered ->
                            item { DevicesCard(registered, state.keyId, viewModel) }
                            item { LinkDeviceCard(viewModel, state.busy) }
                        }
                        if (state.server !is ServerState.Registered) {
                            item { JoinIdentityCard(state, viewModel, copy) }
                        }
                        item { RecoveryPhraseCard(state, viewModel, copy) }
                        item { RestorePhraseCard(viewModel, state.busy) }
                        item { ResetCard(viewModel) }
                    }
                }
            }
        }
    }
}

/** Lets tests scroll the sections into view; the list is longer than any test window. */
const val IDENTITY_LIST_TAG: String = "identity_list"

/* ------------------------------------------------------------------ sections */

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun CreateIdentityCard(state: IdentityUiState, viewModel: IdentityViewModel) {
    var displayName by remember { mutableStateOf("") }
    var deviceLabel by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf(false) }

    SectionCard {
        Text(
            stringResource(Res.string.identity_none_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(Res.string.identity_none_body), style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(Res.string.identity_display_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = deviceLabel,
            onValueChange = { deviceLabel = it },
            label = { Text(stringResource(Res.string.identity_device_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.createIdentity(displayName, deviceLabel) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(Res.string.identity_create)) }

        HorizontalDivider()

        // The other way in: this device has no identity because it belongs to one that already
        // exists elsewhere. Same two paths as §5.4 and §6.1.
        TextButton(onClick = { joining = !joining }) {
            Text(stringResource(Res.string.identity_join_title))
        }
        if (joining) JoinFields(viewModel, state)
    }
}

@Composable
private fun ServerStatusCard(
    state: IdentityUiState,
    viewModel: IdentityViewModel,
    copy: (String) -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }

    SectionCard {
        when (val server = state.server) {
            is ServerState.Registered -> {
                LabelledValue(stringResource(Res.string.identity_user_id), server.userId, copy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        server.displayName ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { renaming = true }) {
                        Text(stringResource(Res.string.identity_rename))
                    }
                }
            }

            ServerState.Unregistered -> {
                Text(
                    stringResource(Res.string.identity_unregistered_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(Res.string.identity_unregistered_body))
                Button(onClick = { viewModel.createIdentity(null, null) }, enabled = !state.busy) {
                    Text(stringResource(Res.string.identity_create))
                }
            }

            ServerState.Revoked -> {
                Text(
                    stringResource(Res.string.identity_revoked_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(stringResource(Res.string.identity_revoked_body))
            }

            ServerState.Unreachable, ServerState.Unknown -> {
                Text(stringResource(Res.string.identity_unreachable))
                OutlinedButton(onClick = viewModel::refresh, enabled = !state.busy) {
                    Text(stringResource(Res.string.identity_retry))
                }
            }
        }

        HorizontalDivider()
        LabelledValue(stringResource(Res.string.identity_key_id), state.keyId.orEmpty(), copy)
    }

    if (renaming) {
        TextInputDialog(
            title = stringResource(Res.string.identity_rename_title),
            label = stringResource(Res.string.identity_display_name),
            initial = (state.server as? ServerState.Registered)?.displayName.orEmpty(),
            onDismiss = { renaming = false },
            onConfirm = { name ->
                renaming = false
                viewModel.rename(name)
            },
        )
    }
}

@Composable
private fun DevicesCard(
    server: ServerState.Registered,
    thisKeyId: String?,
    viewModel: IdentityViewModel,
) {
    var pendingRevoke by remember { mutableStateOf<DeviceKey?>(null) }

    SectionCard {
        Text(
            stringResource(Res.string.identity_devices),
            style = MaterialTheme.typography.titleMedium,
        )
        server.devices.forEach { device ->
            DeviceRow(
                device = device,
                isThisDevice = device.pubkey == thisKeyId,
                onRevoke = { pendingRevoke = device },
            )
        }
    }

    pendingRevoke?.let { device ->
        ConfirmDialog(
            title = stringResource(Res.string.identity_revoke_title),
            body = stringResource(Res.string.identity_revoke_body),
            confirm = stringResource(Res.string.identity_revoke),
            onDismiss = { pendingRevoke = null },
            onConfirm = {
                pendingRevoke = null
                viewModel.revokeDevice(device.pubkey)
            },
        )
    }
}

@Composable
private fun DeviceRow(device: DeviceKey, isThisDevice: Boolean, onRevoke: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                device.label ?: device.pubkey.take(12),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (isThisDevice) {
                Text(
                    stringResource(Res.string.identity_this_device),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                if (device.isActive) {
                    stringResource(Res.string.identity_device_added, device.addedAt.asDateTime())
                } else {
                    stringResource(
                        Res.string.identity_device_revoked,
                        (device.revokedAt ?: 0L).asDateTime(),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (device.isActive) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
        if (device.isActive) {
            TextButton(onClick = onRevoke) { Text(stringResource(Res.string.identity_revoke)) }
        }
    }
}

@Composable
private fun LinkDeviceCard(viewModel: IdentityViewModel, busy: Boolean) {
    var code by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    SectionCard {
        Text(
            stringResource(Res.string.identity_link_here_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(Res.string.identity_link_here_body))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text(stringResource(Res.string.identity_link_code)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(stringResource(Res.string.identity_device_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                viewModel.linkDevice(code, label)
                code = ""
                label = ""
            },
            enabled = !busy && code.isNotBlank(),
        ) { Text(stringResource(Res.string.identity_link_submit)) }
    }
}

@Composable
private fun JoinIdentityCard(
    state: IdentityUiState,
    viewModel: IdentityViewModel,
    copy: (String) -> Unit,
) {
    SectionCard {
        Text(
            stringResource(Res.string.identity_join_title),
            style = MaterialTheme.typography.titleMedium,
        )
        JoinFields(viewModel, state, copy)
    }
}

/** The "I already have an identity elsewhere" half of §5.4, shared by both entry points. */
@Composable
private fun JoinFields(
    viewModel: IdentityViewModel,
    state: IdentityUiState,
    copy: (String) -> Unit = {},
) {
    var userId by remember { mutableStateOf("") }

    Text(stringResource(Res.string.identity_join_body), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
        value = userId,
        onValueChange = { userId = it },
        label = { Text(stringResource(Res.string.identity_user_id)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { viewModel.createLinkCode(userId) },
        enabled = !state.busy && userId.isNotBlank(),
    ) { Text(stringResource(Res.string.identity_join_generate)) }

    state.linkCode?.let { code ->
        Text(stringResource(Res.string.identity_join_code_body), style = MaterialTheme.typography.bodySmall)
        Monospace(code, copy)
    }
}

@Composable
private fun RecoveryPhraseCard(
    state: IdentityUiState,
    viewModel: IdentityViewModel,
    copy: (String) -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }

    SectionCard {
        Text(
            stringResource(Res.string.identity_phrase_section),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(Res.string.identity_phrase_body))

        val phrase = state.recoveryPhrase
        if (phrase == null) {
            OutlinedButton(onClick = { confirming = true }, enabled = !state.busy) {
                Text(stringResource(Res.string.identity_phrase_show))
            }
        } else {
            Text(
                stringResource(Res.string.identity_phrase_warning),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Monospace(phrase.mapIndexed { i, w -> "${i + 1}. $w" }.joinToString("  "), copy)
            TextButton(onClick = viewModel::hideRecoveryPhrase) {
                Text(stringResource(Res.string.identity_phrase_hide))
            }
        }
    }

    if (confirming) {
        ConfirmDialog(
            title = stringResource(Res.string.identity_phrase_reveal_title),
            body = stringResource(Res.string.identity_phrase_reveal_body),
            confirm = stringResource(Res.string.identity_phrase_show),
            onDismiss = { confirming = false },
            onConfirm = {
                confirming = false
                viewModel.revealRecoveryPhrase()
            },
        )
    }
}

@Composable
private fun RestorePhraseCard(viewModel: IdentityViewModel, busy: Boolean) {
    var phrase by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }

    SectionCard {
        Text(
            stringResource(Res.string.identity_phrase_enter),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = phrase,
            onValueChange = { phrase = it },
            label = { Text(stringResource(Res.string.identity_phrase_enter_hint)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { confirming = true }, enabled = !busy && phrase.isNotBlank()) {
            Text(stringResource(Res.string.identity_phrase_restore))
        }
    }

    if (confirming) {
        ConfirmDialog(
            title = stringResource(Res.string.identity_phrase_replace_title),
            body = stringResource(Res.string.identity_phrase_replace_body),
            confirm = stringResource(Res.string.identity_phrase_restore),
            destructive = true,
            onDismiss = { confirming = false },
            onConfirm = {
                confirming = false
                viewModel.restore(phrase)
                phrase = ""
            },
        )
    }
}

@Composable
private fun ResetCard(viewModel: IdentityViewModel) {
    var confirming by remember { mutableStateOf(false) }

    SectionCard {
        Text(
            stringResource(Res.string.identity_reset_section),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(stringResource(Res.string.identity_reset_body))
        OutlinedButton(onClick = { confirming = true }) {
            Text(stringResource(Res.string.identity_reset))
        }
    }

    if (confirming) {
        ConfirmDialog(
            title = stringResource(Res.string.identity_reset_title),
            body = stringResource(Res.string.identity_reset_body_confirm),
            confirm = stringResource(Res.string.identity_reset),
            destructive = true,
            onDismiss = { confirming = false },
            onConfirm = {
                confirming = false
                viewModel.resetIdentity()
            },
        )
    }
}

/* ------------------------------------------------------------------- pieces */

@Composable
private fun LabelledValue(label: String, value: String, copy: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Monospace(value, copy)
}

/** Key ids and phrases are transcribed by hand, so they are monospaced, selectable and copyable. */
@Composable
private fun Monospace(value: String, copy: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        SelectionContainer(Modifier.weight(1f)) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
        IconButton(onClick = { copy(value) }) {
            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(Res.string.identity_copied))
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirm,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}

/** Server timestamps are unix **seconds**; the formatter takes milliseconds. */
private fun Long.asDateTime(): String = formatMediumDateTime(this * 1000)

private fun IdentityNotice.text(): StringResource = when (this) {
    IdentityNotice.REGISTERED -> Res.string.identity_notice_registered
    IdentityNotice.RENAMED -> Res.string.identity_notice_renamed
    IdentityNotice.DEVICE_LINKED -> Res.string.identity_notice_device_linked
    IdentityNotice.DEVICE_REVOKED -> Res.string.identity_notice_device_revoked
    IdentityNotice.LAST_KEY -> Res.string.identity_notice_last_key
    IdentityNotice.KEY_TAKEN -> Res.string.identity_notice_key_taken
    IdentityNotice.KEY_ALREADY_REVOKED -> Res.string.identity_notice_key_already_revoked
    IdentityNotice.INVALID_PROOF -> Res.string.identity_notice_invalid_proof
    IdentityNotice.INVALID_CODE -> Res.string.identity_notice_invalid_code
    IdentityNotice.PHRASE_RESTORED -> Res.string.identity_notice_phrase_restored
    IdentityNotice.PHRASE_WRONG_LENGTH -> Res.string.identity_notice_phrase_wrong_length
    IdentityNotice.PHRASE_UNKNOWN_WORD -> Res.string.identity_notice_phrase_unknown_word
    IdentityNotice.PHRASE_CHECKSUM -> Res.string.identity_notice_phrase_checksum
    IdentityNotice.IDENTITY_RESET -> Res.string.identity_notice_reset
    IdentityNotice.NO_IDENTITY -> Res.string.identity_notice_no_identity
    IdentityNotice.NOT_AUTHENTICATED -> Res.string.identity_notice_not_authenticated
    IdentityNotice.NETWORK_ERROR -> Res.string.identity_notice_network
    IdentityNotice.SERVER_ERROR -> Res.string.identity_notice_server
}
