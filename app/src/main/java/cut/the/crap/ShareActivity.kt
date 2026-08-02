package cut.the.crap

import org.jetbrains.compose.resources.getString

import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.share_edit_dialog_campaign_picker_empty
import cut.the.crap.shared.resources.share_edit_dialog_campaign_picker_title
import cut.the.crap.shared.resources.share_edit_dialog_cancel
import cut.the.crap.shared.resources.share_edit_dialog_comment_label
import cut.the.crap.shared.resources.share_edit_dialog_keywords_label
import cut.the.crap.shared.resources.share_edit_dialog_link_label
import cut.the.crap.shared.resources.share_edit_dialog_pick_campaign_text
import cut.the.crap.shared.resources.share_edit_dialog_save
import cut.the.crap.shared.resources.share_edit_dialog_save_as_post
import cut.the.crap.shared.resources.share_edit_dialog_save_to_link
import cut.the.crap.shared.resources.share_edit_dialog_title
import cut.the.crap.shared.resources.share_toast_handle_exists
import cut.the.crap.shared.resources.share_toast_handle_saved
import cut.the.crap.shared.resources.share_toast_link_resolved_saved
import cut.the.crap.shared.resources.share_toast_link_saved
import cut.the.crap.shared.resources.share_toast_received_image
import cut.the.crap.shared.resources.share_toast_unsupported_type
import cut.the.crap.shared.resources.share_toast_x_login_prompt
import cut.the.crap.shared.resources.share_toast_x_session_expired

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import cut.the.crap.platform.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.ContentItemRepository
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignPost
import cut.the.crap.data.rest.campaign.CampaignRepository
import cut.the.crap.share.HandleResult
import cut.the.crap.share.ResolveResult
import cut.the.crap.share.SaveResult
import cut.the.crap.share.SharedUrlProcessor
import cut.the.crap.ui.XLoginActivity
import cut.the.crap.ui.content.settings.ThemePreference
import cut.the.crap.ui.theme.MyAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "ShareReceiver"

class ShareReceiverActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    /**
     * The platform-agnostic share pipeline (resolve → save/enrich → handle pool). This activity is
     * now just the Android shell around it: intent parsing, the edit dialog, the X sign-in prompt,
     * and toast feedback. See [SharedUrlProcessor].
     */
    private val processor: SharedUrlProcessor by inject()

    // Backs the edit dialog's optional comment/quote: "Save as Post" and the campaign-text picker.
    private val contentItemRepository: ContentItemRepository by inject()
    private val campaignRepository: CampaignRepository by inject()

    // Store pending URL for retry after login
    private var pendingUrl: String? = null

    // Activity result launcher for X login
    private val xLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == XLoginActivity.RESULT_LOGIN_SUCCESS) {
            // Retry with the pending URL after successful login
            pendingUrl?.let { url ->
                Log.d(TAG, "Login successful, retrying URL resolution")
                processUrl(url, isRetry = true)
            } ?: finish()
        } else {
            // Login cancelled - save the unresolved URL anyway
            pendingUrl?.let { url ->
                Log.d(TAG, "Login cancelled, saving unresolved URL")
                lifecycleScope.launch {
                    saveUrlAndFinish(url, wasResolved = false)
                }
            } ?: finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingShare(intent)
    }

    private fun handleIncomingShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val extras = intent.extras
            if (extras != null) {
                for (key in extras.keySet()) {
                    Log.d(TAG, "$key : ${extras[key]}")
                }
            }
            when (intent.type) {
                "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    sharedText?.let { text ->
                        processUrl(text, isRetry = false)
                    } ?: finish()
                }
                "image/jpeg", "image/jpg" -> {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    Log.d(TAG, "Received image: $intent  $imageUri")
                    // finish() moved inside: reading the catalogue suspends, and finishing
                    // first would cancel lifecycleScope before the Toast is ever shown.
                    lifecycleScope.launch {
                        imageUri?.let {
                            Toast.makeText(
                                this@ShareReceiverActivity,
                                getString(Res.string.share_toast_received_image, intent.dataString.toString()),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        finish()
                    }
                }
                else -> {
                    lifecycleScope.launch {
                        Toast.makeText(
                            this@ShareReceiverActivity,
                            getString(Res.string.share_toast_unsupported_type, intent.type.toString()),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        } else {
            finish()
        }
    }

    private fun processUrl(url: String, isRetry: Boolean) {
        lifecycleScope.launch {
            when (val resolution = processor.resolve(url)) {
                is ResolveResult.Ready -> {
                    saveUrlAndFinish(resolution.url, wasResolved = resolution.wasResolved)
                }

                is ResolveResult.AuthRequired -> {
                    if (isRetry) {
                        // Already tried login, just save the unresolved URL
                        Log.w(TAG, "Auth still failing after login, saving unresolved")
                        saveUrlAndFinish(resolution.url, wasResolved = false)
                    } else {
                        // Prompt user to login, then retry via the activity result callback
                        pendingUrl = resolution.url
                        val reason = if (resolution.credentialsPresent) {
                            XLoginActivity.REASON_AUTH_EXPIRED
                        } else {
                            XLoginActivity.REASON_INITIAL_SETUP
                        }
                        promptForLogin(reason)
                    }
                }
            }
        }
    }

    private fun promptForLogin(reason: String) {
        lifecycleScope.launch {
            Toast.makeText(
                this@ShareReceiverActivity,
                if (reason == XLoginActivity.REASON_AUTH_EXPIRED)
                    getString(Res.string.share_toast_x_session_expired)
                else
                    getString(Res.string.share_toast_x_login_prompt),
                Toast.LENGTH_SHORT
            ).show()
        }

        val loginIntent = XLoginActivity.createIntent(this, reason)
        xLoginLauncher.launch(loginIntent)
    }

    private suspend fun saveUrlAndFinish(url: String, wasResolved: Boolean) {
        // Some shares save a handle to the keyword pool instead of storing the link — e.g. an
        // X/Twitter profile. The processor decides; when it returns a handle we skip the link insert.
        val handle = processor.handleToSaveInstead(url)
        if (handle != null) {
            saveHandleAndFinish(handle)
            return
        }
        val settings = settingsRepository.settingsFlow.first()
        if (settings.editSharedLinkBeforeSave) {
            // Let the user review/edit the link (and add keywords) before it's saved.
            showEditDialog(url, wasResolved, settings.themePreference)
        } else {
            insertAndFinish(
                url, keywords = emptyList(), wasResolved = wasResolved,
                comment = null, saveAsPost = false, saveToLink = false,
            )
        }
    }

    /**
     * Renders an editable dialog over the transparent activity. On save the (possibly
     * edited) link and keywords are inserted; on cancel nothing is saved.
     */
    private fun showEditDialog(
        url: String,
        wasResolved: Boolean,
        themePreference: ThemePreference
    ) {
        setContent {
            MyAppTheme(themePreference = themePreference) {
                ShareEditDialog(
                    initialUrl = url,
                    campaignRepository = campaignRepository,
                    onSave = { editedUrl, keywords, comment, saveAsPost, saveToLink ->
                        lifecycleScope.launch {
                            insertAndFinish(editedUrl, keywords, wasResolved, comment, saveAsPost, saveToLink)
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private suspend fun insertAndFinish(
        url: String,
        keywords: List<String>,
        wasResolved: Boolean,
        comment: String?,
        saveAsPost: Boolean,
        saveToLink: Boolean,
    ) {
        // Insert first and toast immediately, then enrich — enrichment does network I/O, so the
        // user sees "saved" without waiting on it. lifecycleScope is cancelled on finish(), so
        // enrichSaved must complete before finish().
        val result: SaveResult = processor.insertLink(
            url, keywords, wasResolved,
            comment = comment.takeIf { saveToLink },
        )

        if (saveAsPost && !comment.isNullOrBlank()) {
            contentItemRepository.insert(ContentItem(text = comment, isActive = false))
        }

        val message = if (result.wasResolved) {
            getString(Res.string.share_toast_link_resolved_saved)
        } else {
            getString(Res.string.share_toast_link_saved)
        }
        Toast.makeText(this@ShareReceiverActivity, message, Toast.LENGTH_SHORT).show()

        processor.enrichSaved(url, result.savedAt)
        finish()
    }

    /**
     * Saves [handle] to the `ACCOUNT` keyword pool (deduped case-insensitively by the processor)
     * and finishes, toasting whether it was newly added or already present.
     */
    private suspend fun saveHandleAndFinish(handle: String) {
        val result: HandleResult = processor.saveHandle(handle)
        val message = if (result.alreadyExisted) {
            getString(Res.string.share_toast_handle_exists, result.handle)
        } else {
            getString(Res.string.share_toast_handle_saved, result.handle)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShare(intent)
    }
}

/**
 * Dialog shown before saving a shared link when the user has opted in. Lets them edit the link URL,
 * add comma-separated keywords, and write (or pick from a campaign) a comment/quote about the link
 * — saved by default both as a new Posts draft and attached to the link itself, each toggleable.
 * [onSave] receives the edited URL, the parsed keyword list, the trimmed comment (or null), and the
 * two save-destination flags; [onCancel] discards without saving anything.
 */
@Composable
private fun ShareEditDialog(
    initialUrl: String,
    campaignRepository: CampaignRepository,
    onSave: (
        url: String,
        keywords: List<String>,
        comment: String?,
        saveAsPost: Boolean,
        saveToLink: Boolean,
    ) -> Unit,
    onCancel: () -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var keywordsText by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var saveAsPost by remember { mutableStateOf(true) }
    var saveToLink by remember { mutableStateOf(true) }
    var showCampaignPicker by remember { mutableStateOf(false) }

    if (showCampaignPicker) {
        CampaignPostPickerDialog(
            campaignRepository = campaignRepository,
            onPick = { post ->
                commentText = post.text
                showCampaignPicker = false
            },
            onDismiss = { showCampaignPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.share_edit_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(Res.string.share_edit_dialog_link_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text(stringResource(Res.string.share_edit_dialog_keywords_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text(stringResource(Res.string.share_edit_dialog_comment_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { showCampaignPicker = true }) {
                    Text(stringResource(Res.string.share_edit_dialog_pick_campaign_text))
                }
                if (commentText.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = saveAsPost, onCheckedChange = { saveAsPost = it })
                        Text(stringResource(Res.string.share_edit_dialog_save_as_post))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = saveToLink, onCheckedChange = { saveToLink = it })
                        Text(stringResource(Res.string.share_edit_dialog_save_to_link))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(url.trim(), keywords, commentText.trim().ifBlank { null }, saveAsPost, saveToLink)
                },
                enabled = url.isNotBlank()
            ) {
                Text(stringResource(Res.string.share_edit_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.share_edit_dialog_cancel))
            }
        }
    )
}

/**
 * Lets the user pick an existing campaign post's text to reuse as the comment/quote. Fetched lazily
 * — only when this dialog opens, not on every share — since [CampaignRepository] has no persistent
 * cache (it's a Koin `factory`, so this call gets a fresh instance either way).
 */
@Composable
private fun CampaignPostPickerDialog(
    campaignRepository: CampaignRepository,
    onPick: (CampaignPost) -> Unit,
    onDismiss: () -> Unit
) {
    var choices by remember { mutableStateOf<List<Pair<String, CampaignPost>>?>(null) }

    LaunchedEffect(Unit) {
        val summaries = campaignRepository.list().getOrNull().orEmpty()
        val campaigns: List<Campaign> = summaries.mapNotNull { summary ->
            campaignRepository.get(summary.id).getOrNull()
        }
        choices = campaigns.flatMap { campaign ->
            campaign.countries.flatMap { country -> country.posts }
                .map { post -> campaign.displayTitle to post }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.share_edit_dialog_campaign_picker_title)) },
        text = {
            val current = choices
            if (current == null) {
                CircularProgressIndicator()
            } else if (current.isEmpty()) {
                Text(stringResource(Res.string.share_edit_dialog_campaign_picker_empty))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(current) { (campaignTitle, post) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(post) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                "$campaignTitle — ${post.language.uppercase()} ${post.id}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                post.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.share_edit_dialog_cancel))
            }
        }
    )
}
