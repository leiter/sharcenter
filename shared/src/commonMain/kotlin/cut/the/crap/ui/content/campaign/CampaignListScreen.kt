package cut.the.crap.ui.content.campaign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.data.rest.AppConfig
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.campaign.CampaignRepository
import cut.the.crap.data.rest.campaign.CampaignSummary
import cut.the.crap.platform.FileAccess
import cut.the.crap.platform.rememberFilePicker
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.action_back
import cut.the.crap.shared.resources.campaign_badge_featured
import cut.the.crap.shared.resources.campaign_badge_member
import cut.the.crap.shared.resources.campaign_badge_owner
import cut.the.crap.shared.resources.campaign_create_dialog_cd
import cut.the.crap.shared.resources.campaign_create_dialog_error
import cut.the.crap.shared.resources.campaign_create_dialog_invalid_json
import cut.the.crap.shared.resources.campaign_create_dialog_paste_label
import cut.the.crap.shared.resources.campaign_create_dialog_open_builder
import cut.the.crap.shared.resources.campaign_create_dialog_pick_file
import cut.the.crap.shared.resources.campaign_create_dialog_submit
import cut.the.crap.shared.resources.campaign_create_dialog_terms
import cut.the.crap.shared.resources.campaign_create_dialog_title
import cut.the.crap.shared.resources.campaign_join_dialog_cd
import cut.the.crap.shared.resources.campaign_join_dialog_code_label
import cut.the.crap.shared.resources.campaign_join_dialog_error
import cut.the.crap.shared.resources.campaign_join_dialog_submit
import cut.the.crap.shared.resources.campaign_join_dialog_title
import cut.the.crap.shared.resources.campaign_list_contacts
import cut.the.crap.shared.resources.campaign_list_counts
import cut.the.crap.shared.resources.campaign_list_empty_body
import cut.the.crap.shared.resources.campaign_list_empty_title
import cut.the.crap.shared.resources.campaign_list_failed
import cut.the.crap.shared.resources.campaign_list_retry
import cut.the.crap.shared.resources.campaign_list_title
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.ui.components.BottomNavigationBar
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Exposed for [CampaignScreensTest] — the checkbox has no unique text of its own to find it by. */
const val CAMPAIGN_CREATE_TERMS_TAG = "campaign_create_terms_checkbox"

/**
 * The campaigns this install can act on.
 *
 * Replaces the country list that used to sit behind the Posts top bar. That screen showed one
 * hardcoded campaign's countries with no `onClick` anywhere; this one is a list of campaigns, and
 * every row goes somewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignListScreen(
    navController: NavHostController,
    viewModel: CampaignListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }

    if (showCreate) {
        CreateCampaignDialog(
            onDismiss = { showCreate = false },
            onCreated = { showCreate = false; viewModel.refresh() },
        )
    }
    if (showJoin) {
        JoinCampaignDialog(
            onDismiss = { showJoin = false },
            onJoined = { showJoin = false; viewModel.refresh() },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.campaign_list_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showJoin = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = stringResource(Res.string.campaign_join_dialog_cd),
                        )
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(Res.string.campaign_list_retry),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.campaign_create_dialog_cd))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            when {
                // An error with nothing to show. A failed refresh over an existing list falls
                // through to the list instead — blanking what the user was reading would be worse
                // than briefly showing something slightly stale.
                state.error != null && state.campaigns.isEmpty() -> Message(
                    title = stringResource(Res.string.campaign_list_failed),
                    body = null,
                    onRetry = viewModel::refresh,
                )

                state.isEmpty -> Message(
                    title = stringResource(Res.string.campaign_list_empty_title),
                    body = stringResource(Res.string.campaign_list_empty_body),
                    onRetry = null,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.campaigns, key = { it.id }) { campaign ->
                        CampaignRow(campaign) {
                            navController.navigate("campaign_detail/${campaign.id}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampaignRow(campaign: CampaignSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    campaign.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Badge(campaign)
            }
            campaign.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(
                    Res.string.campaign_list_counts,
                    campaign.countryCount,
                    campaign.postCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (campaign.contactCount > 0) {
                Text(
                    stringResource(Res.string.campaign_list_contacts, campaign.contactCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Yours", "joined" or "included with the app" — three different relationships, three labels. */
@Composable
private fun Badge(campaign: CampaignSummary) {
    val label = when {
        campaign.role == "owner" -> stringResource(Res.string.campaign_badge_owner)
        campaign.isMine -> stringResource(Res.string.campaign_badge_member)
        campaign.featured -> stringResource(Res.string.campaign_badge_featured)
        else -> return
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun Message(title: String, body: String?, onRetry: (() -> Unit)?) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            body?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            onRetry?.let {
                OutlinedButton(onClick = it) {
                    Text(stringResource(Res.string.campaign_list_retry))
                }
            }
        }
    }
}

/**
 * Pastes or picks the JSON built by `/campaign-builder` (or hand-written) and creates a campaign
 * from it, with the caller as owner. Sent to the server mostly as-is — see
 * [CampaignRepository.create] — except [confirmedTerms] is always patched into the outgoing body
 * from this dialog's own checkbox, so the app is the one source of truth for that confirmation
 * regardless of what the pasted text already contains.
 */
@Composable
private fun CreateCampaignDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val repository: CampaignRepository = koinInject()
    val fileAccess: FileAccess = koinInject()
    val config: AppConfig = koinInject()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val invalidJsonMessage = stringResource(Res.string.campaign_create_dialog_invalid_json)
    val genericErrorMessage = stringResource(Res.string.campaign_create_dialog_error)

    var json by remember { mutableStateOf("") }
    var confirmedTerms by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberFilePicker(mimeTypes = listOf("application/json", "text/plain")) { uris ->
        uris.firstOrNull()?.let { uri ->
            scope.launch { fileAccess.readText(uri)?.let { json = it } }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.campaign_create_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it; error = null },
                    label = { Text(stringResource(Res.string.campaign_create_dialog_paste_label)) },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { filePicker.launch() }) {
                    Text(stringResource(Res.string.campaign_create_dialog_pick_file))
                }
                TextButton(
                    onClick = {
                        uriHandler.openUri(config.campaignBaseUrl.trimEnd('/') + "/campaign-builder")
                    },
                ) {
                    Text(stringResource(Res.string.campaign_create_dialog_open_builder))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = confirmedTerms,
                        onCheckedChange = { confirmedTerms = it },
                        modifier = Modifier.testTag(CAMPAIGN_CREATE_TERMS_TAG),
                    )
                    Text(stringResource(Res.string.campaign_create_dialog_terms))
                }
                if (submitting) CircularProgressIndicator()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = json.isNotBlank() && confirmedTerms && !submitting,
                onClick = {
                    val patched = patchConfirmedTerms(json, confirmedTerms)
                    if (patched == null) {
                        error = invalidJsonMessage
                        return@TextButton
                    }
                    submitting = true
                    error = null
                    scope.launch {
                        when (repository.create(patched)) {
                            is Result.Success -> onCreated()
                            is Result.Error -> {
                                submitting = false
                                error = genericErrorMessage
                            }
                        }
                    }
                },
            ) { Text(stringResource(Res.string.campaign_create_dialog_submit)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}

/** Parses [rawJson] as an object and overwrites/adds `confirmedTerms`; null if it isn't valid JSON. */
private fun patchConfirmedTerms(rawJson: String, confirmedTerms: Boolean): String? = try {
    val obj = Json.parseToJsonElement(rawJson).jsonObject
    val patched = JsonObject(obj + ("confirmedTerms" to JsonPrimitive(confirmedTerms)))
    Json.encodeToString(JsonObject.serializer(), patched)
} catch (e: Exception) {
    null
}

@Composable
private fun JoinCampaignDialog(onDismiss: () -> Unit, onJoined: () -> Unit) {
    val repository: CampaignRepository = koinInject()
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.campaign_join_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; failed = false },
                    label = { Text(stringResource(Res.string.campaign_join_dialog_code_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (submitting) CircularProgressIndicator()
                if (failed) {
                    Text(
                        stringResource(Res.string.campaign_join_dialog_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank() && !submitting,
                onClick = {
                    submitting = true
                    failed = false
                    scope.launch {
                        when (repository.join(code.trim())) {
                            is Result.Success -> onJoined()
                            is Result.Error -> {
                                submitting = false
                                failed = true
                            }
                        }
                    }
                },
            ) { Text(stringResource(Res.string.campaign_join_dialog_submit)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}
