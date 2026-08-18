package cut.the.crap.ui.content.campaign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.data.rest.campaign.CampaignRepository
import cut.the.crap.platform.FileAccess
import cut.the.crap.platform.UrlOpener
import cut.the.crap.platform.rememberFilePicker
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.action_back
import cut.the.crap.shared.resources.campaign_create_dialog_pick_file
import cut.the.crap.shared.resources.campaign_create_posts_cd
import cut.the.crap.shared.resources.campaign_detail_countries
import cut.the.crap.shared.resources.campaign_detail_disabled_body
import cut.the.crap.shared.resources.campaign_detail_disabled_title
import cut.the.crap.shared.resources.campaign_detail_failed
import cut.the.crap.shared.resources.campaign_detail_locate
import cut.the.crap.shared.resources.campaign_detail_open_country
import cut.the.crap.shared.resources.campaign_detail_publish
import cut.the.crap.shared.resources.campaign_detail_publish_desc
import cut.the.crap.shared.resources.campaign_detail_reach_out
import cut.the.crap.shared.resources.campaign_detail_reach_out_desc
import cut.the.crap.shared.resources.campaign_list_retry
import cut.the.crap.shared.resources.campaign_manage_generate_invite
import cut.the.crap.shared.resources.campaign_manage_invite_copy
import cut.the.crap.shared.resources.campaign_manage_invite_error
import cut.the.crap.shared.resources.campaign_manage_invite_generated
import cut.the.crap.shared.resources.campaign_manage_invite_role_editor
import cut.the.crap.shared.resources.campaign_manage_invite_role_member
import cut.the.crap.shared.resources.campaign_manage_delete
import cut.the.crap.shared.resources.campaign_manage_delete_confirm_action
import cut.the.crap.shared.resources.campaign_manage_delete_confirm_body
import cut.the.crap.shared.resources.campaign_manage_delete_confirm_title
import cut.the.crap.shared.resources.campaign_manage_delete_error
import cut.the.crap.shared.resources.campaign_manage_leave
import cut.the.crap.shared.resources.campaign_manage_leave_confirm_body
import cut.the.crap.shared.resources.campaign_manage_leave_confirm_title
import cut.the.crap.shared.resources.campaign_manage_leave_error
import cut.the.crap.shared.resources.campaign_manage_replace_items
import cut.the.crap.shared.resources.campaign_manage_replace_items_error
import cut.the.crap.shared.resources.campaign_parliament_badge
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.shared.resources.share_edit_dialog_save
import cut.the.crap.ui.components.BottomNavigationBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * One campaign: what to publish, whom to write to, and which countries it covers.
 *
 * This is where `CampaignCountry.url`, `hasParliamentAction` and `Campaign.locateUrl` stop being
 * parsed-and-ignored. All three were in the payload from the start and had no behaviour anywhere;
 * here a country row opens its page, the parliament chip opens the contact action, and locate
 * opens the geolocating entry point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailScreen(
    navController: NavHostController,
    campaignId: String,
    viewModel: CampaignDetailViewModel,
) {
    val state by viewModel.state.collectAsState()
    val urlOpener: UrlOpener = koinInject()
    val campaign = state.campaign

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(campaign?.displayTitle ?: campaignId) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = {
                    if (campaign != null && campaign.countriesWithPosts.isNotEmpty()) {
                        IconButton(onClick = { navController.navigate("campaign_composer/$campaignId") }) {
                            Icon(
                                imageVector = Icons.Filled.Campaign,
                                contentDescription = stringResource(Res.string.campaign_create_posts_cd),
                            )
                        }
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
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            when {
                state.isDisabled -> Message(
                    title = stringResource(Res.string.campaign_detail_disabled_title),
                    body = stringResource(Res.string.campaign_detail_disabled_body),
                    onRetry = null,
                )

                campaign == null && state.error != null -> Message(
                    title = stringResource(Res.string.campaign_detail_failed),
                    body = null,
                    onRetry = { viewModel.load(forceRefresh = true) },
                )

                campaign != null -> Body(
                    campaign, campaignId, navController, urlOpener,
                    onChanged = { viewModel.load(forceRefresh = true) },
                )

                else -> Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun Body(
    campaign: Campaign,
    campaignId: String,
    navController: NavHostController,
    urlOpener: UrlOpener,
    onChanged: () -> Unit,
) {
    var filter by remember { mutableStateOf(CountryFilter.ALL) }
    var sort by remember { mutableStateOf(CountrySort.REGISTRY) }
    val visibleCountries = remember(campaign, filter, sort) {
        campaign.countries.applyFilter(filter).applySort(sort)
    }
    val onFilter: (CountryFilter) -> Unit = { filter = it }
    val onSort: (CountrySort) -> Unit = { sort = it }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        campaign.description?.takeIf { it.isNotBlank() }?.let { description ->
            item { Text(description, style = MaterialTheme.typography.bodyMedium) }
        }

        // "Managed from the app" (CAMPAIGN_SCHEMA_SPEC.md §8 step 3): owner/editor can push a
        // revised item set, generate invites; any member can leave.
        if (campaign.role == "owner" || campaign.role == "editor") {
            item { ManageSection(campaignId, campaign.role, navController, onChanged) }
        } else if (campaign.role == "member") {
            item { LeaveSection(campaignId, navController) }
        }

        // "Find my country": the geolocating entry point. Parsed since the first version of the
        // payload, never once opened.
        campaign.locateUrl?.takeIf { it.isNotBlank() }?.let { locateUrl ->
            item {
                OutlinedButton(
                    onClick = { urlOpener.open(locateUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.campaign_detail_locate)) }
            }
        }

        if (campaign.countriesWithPosts.isNotEmpty()) {
            item {
                LaneCard(
                    title = stringResource(Res.string.campaign_detail_publish),
                    body = stringResource(Res.string.campaign_detail_publish_desc),
                    action = stringResource(Res.string.campaign_detail_publish),
                    onAction = { navController.navigate("campaign_composer/$campaignId") },
                )
            }
        }

        if (campaign.countriesWithContacts.isNotEmpty()) {
            item {
                LaneCard(
                    title = stringResource(Res.string.campaign_detail_reach_out),
                    body = stringResource(Res.string.campaign_detail_reach_out_desc),
                    action = null,
                    onAction = null,
                )
            }
        }

        item {
            Text(
                stringResource(Res.string.campaign_detail_countries),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // The filter and sort chips come over from the old country screen. Forty-two countries is
        // too many to scan, and "parliament action" as a filter finally selects for something the
        // user can act on rather than for a decorative chip.
        item { CountryFilterChips(campaign.countries, filter, onFilter) }
        item { CountrySortChips(sort, onSort) }

        items(visibleCountries, key = { it.countryCode }) { country ->
            CountryRow(country, urlOpener)
        }
    }
}

@Composable
private fun LaneCard(title: String, body: String, action: String?, onAction: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (action != null && onAction != null) {
                Button(onClick = onAction) { Text(action) }
            }
        }
    }
}

/** Owner/editor management: push a revised item set, generate an invite (CAMPAIGN_SCHEMA_SPEC §8 step 3). */
@Composable
private fun ManageSection(
    campaignId: String,
    role: String?,
    navController: NavHostController,
    onChanged: () -> Unit,
) {
    var showReplaceItems by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    if (showReplaceItems) {
        ReplaceItemsDialog(
            campaignId = campaignId,
            onDismiss = { showReplaceItems = false },
            onReplaced = { showReplaceItems = false; onChanged() },
        )
    }
    if (showInvite) {
        InviteDialog(campaignId = campaignId, onDismiss = { showInvite = false })
    }
    if (showDelete) {
        DeleteConfirmDialog(
            campaignId = campaignId,
            onDismiss = { showDelete = false },
            onDeleted = { navController.popBackStack() },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { showReplaceItems = true }) {
                Text(stringResource(Res.string.campaign_manage_replace_items))
            }
            OutlinedButton(onClick = { showInvite = true }) {
                Text(stringResource(Res.string.campaign_manage_generate_invite))
            }
            // Deleting is unrecoverable and there is no ownership transfer, so only the owner —
            // never an editor — can do it; an editor deleting the owner's campaign would be a
            // privilege escalation the server itself refuses (403), but the button shouldn't even
            // dangle there for someone who'll just get an error.
            if (role == "owner") {
                OutlinedButton(
                    onClick = { showDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(Res.string.campaign_manage_delete))
                }
            }
        }
    }
}

/** Pastes/picks the same campaign JSON the builder page produces; only its `items` are used. */
@Composable
private fun ReplaceItemsDialog(campaignId: String, onDismiss: () -> Unit, onReplaced: () -> Unit) {
    val repository: CampaignRepository = koinInject()
    val fileAccess: FileAccess = koinInject()
    val scope = rememberCoroutineScope()

    var json by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    val filePicker = rememberFilePicker(mimeTypes = listOf("application/json", "text/plain")) { uris ->
        uris.firstOrNull()?.let { uri ->
            scope.launch { fileAccess.readText(uri)?.let { json = it } }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.campaign_manage_replace_items)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it; error = false },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { filePicker.launch() }) {
                    Text(stringResource(Res.string.campaign_create_dialog_pick_file))
                }
                if (submitting) CircularProgressIndicator()
                if (error) {
                    Text(
                        stringResource(Res.string.campaign_manage_replace_items_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = json.isNotBlank() && !submitting,
                onClick = {
                    submitting = true
                    error = false
                    scope.launch {
                        when (repository.replaceItems(campaignId, json)) {
                            is Result.Success -> onReplaced()
                            is Result.Error -> { submitting = false; error = true }
                        }
                    }
                },
            ) { Text(stringResource(Res.string.share_edit_dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}

/** Generates an invite code, then shows it with a copy button. */
@Composable
private fun InviteDialog(campaignId: String, onDismiss: () -> Unit) {
    val repository: CampaignRepository = koinInject()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var role by remember { mutableStateOf("member") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.campaign_manage_generate_invite)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (code == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = role == "member",
                            onClick = { role = "member" },
                            label = { Text(stringResource(Res.string.campaign_manage_invite_role_member)) },
                        )
                        FilterChip(
                            selected = role == "editor",
                            onClick = { role = "editor" },
                            label = { Text(stringResource(Res.string.campaign_manage_invite_role_editor)) },
                        )
                    }
                    if (submitting) CircularProgressIndicator()
                    if (error) {
                        Text(
                            stringResource(Res.string.campaign_manage_invite_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text(stringResource(Res.string.campaign_manage_invite_generated, code!!))
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(code!!)) }) {
                        Text(stringResource(Res.string.campaign_manage_invite_copy))
                    }
                }
            }
        },
        confirmButton = {
            if (code == null) {
                TextButton(
                    enabled = !submitting,
                    onClick = {
                        submitting = true
                        error = false
                        scope.launch {
                            when (val result = repository.invite(campaignId, role)) {
                                is Result.Success -> { submitting = false; code = result.data }
                                is Result.Error -> { submitting = false; error = true }
                            }
                        }
                    },
                ) { Text(stringResource(Res.string.campaign_manage_generate_invite)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}

/** A member (not owner) can leave; the server itself blocks an owner from leaving their own campaign. */
@Composable
private fun LeaveSection(campaignId: String, navController: NavHostController) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        LeaveConfirmDialog(
            campaignId = campaignId,
            onDismiss = { showConfirm = false },
            onLeft = { navController.popBackStack() },
        )
    }

    OutlinedButton(onClick = { showConfirm = true }) {
        Text(stringResource(Res.string.campaign_manage_leave))
    }
}

@Composable
private fun LeaveConfirmDialog(campaignId: String, onDismiss: () -> Unit, onLeft: () -> Unit) {
    val repository: CampaignRepository = koinInject()
    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.campaign_manage_leave_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.campaign_manage_leave_confirm_body))
                if (submitting) CircularProgressIndicator()
                if (error) {
                    Text(
                        stringResource(Res.string.campaign_manage_leave_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = {
                    submitting = true
                    error = false
                    scope.launch {
                        when (repository.leave(campaignId)) {
                            is Result.Success -> onLeft()
                            is Result.Error -> { submitting = false; error = true }
                        }
                    }
                },
            ) { Text(stringResource(Res.string.campaign_manage_leave)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}

/** Owner-only, permanent — the server has nothing that walks this back. */
@Composable
private fun DeleteConfirmDialog(campaignId: String, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val repository: CampaignRepository = koinInject()
    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.campaign_manage_delete_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.campaign_manage_delete_confirm_body))
                if (submitting) CircularProgressIndicator()
                if (error) {
                    Text(
                        stringResource(Res.string.campaign_manage_delete_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = {
                    submitting = true
                    error = false
                    scope.launch {
                        when (repository.delete(campaignId)) {
                            is Result.Success -> onDeleted()
                            is Result.Error -> { submitting = false; error = true }
                        }
                    }
                },
            ) { Text(stringResource(Res.string.campaign_manage_delete_confirm_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun CountryRow(country: CampaignCountry, urlOpener: UrlOpener) {
    val hasPage = country.url.isNotBlank()
    val openCountry = stringResource(Res.string.campaign_detail_open_country, country.countryName)
    val contacts = country.contacts

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    // Only clickable when there is something to open — a row that reacts to a tap
                    // by doing nothing is exactly what made the old screen feel broken.
                    if (hasPage) Modifier.clickable { urlOpener.open(country.url) } else Modifier
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${country.flag} ${country.countryName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (country.hasPosts) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CountrySubtitle(country)
            }

            // A single contact (abu-safiya's one-MdB-lookup-link-per-country shape) still fits
            // inline, as before. More than one is rendered as its own list below instead — see
            // the block after this Row.
            if (contacts.size == 1) {
                AssistChip(
                    onClick = { urlOpener.open(contacts[0].url) },
                    label = { Text(contacts[0].label ?: stringResource(Res.string.campaign_parliament_badge)) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            if (hasPage) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = openCountry,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        // More than one contact (e.g. gaza-politik's per-target list of Fraktionen/parties,
        // unlike abu-safiya's single MdB-lookup link) doesn't fit in the header row. The old
        // code took contacts.firstOrNull() here, which silently dropped every contact past the
        // first — for a country with 19 contacts, 18 were simply unreachable in the app.
        if (contacts.size > 1) {
            Column(modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                contacts.forEach { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { urlOpener.open(contact.url) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            contact.label ?: contact.url,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = contact.label ?: contact.url,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
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
