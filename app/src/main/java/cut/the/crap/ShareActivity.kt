package cut.the.crap

import org.jetbrains.compose.resources.getString

import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.share_edit_dialog_cancel
import cut.the.crap.shared.resources.share_edit_dialog_keywords_label
import cut.the.crap.shared.resources.share_edit_dialog_link_label
import cut.the.crap.shared.resources.share_edit_dialog_save
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cut.the.crap.data.preferences.SettingsRepository
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
            insertAndFinish(url, keywords = emptyList(), wasResolved = wasResolved)
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
                    onSave = { editedUrl, keywords ->
                        lifecycleScope.launch {
                            insertAndFinish(editedUrl, keywords, wasResolved)
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
        wasResolved: Boolean
    ) {
        // Insert first and toast immediately, then enrich — enrichment does network I/O, so the
        // user sees "saved" without waiting on it. lifecycleScope is cancelled on finish(), so
        // enrichSaved must complete before finish().
        val result: SaveResult = processor.insertLink(url, keywords, wasResolved)

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
 * Dialog shown before saving a shared link when the user has opted in. Lets them edit
 * the link URL and add comma-separated keywords. [onSave] receives the edited URL and
 * the parsed (trimmed, non-blank) keyword list; [onCancel] discards without saving.
 */
@Composable
private fun ShareEditDialog(
    initialUrl: String,
    onSave: (url: String, keywords: List<String>) -> Unit,
    onCancel: () -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var keywordsText by remember { mutableStateOf("") }

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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(url.trim(), keywords)
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
