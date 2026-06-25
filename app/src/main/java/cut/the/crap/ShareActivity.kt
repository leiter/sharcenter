package cut.the.crap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.data.rest.YouTubeRepository
import cut.the.crap.data.rest.YouTubeUrlParser
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.UrlResolver
import cut.the.crap.tools.parseSocialMediaUrl
import kotlinx.coroutines.Dispatchers
import cut.the.crap.ui.XLoginActivity
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.content.settings.ThemePreference
import cut.the.crap.ui.theme.MyAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShareReceiver"

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject
    lateinit var contentRepository: ContentLinkRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var youTubeRepository: YouTubeRepository

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
                saveUrlAndFinish(url, wasResolved = false)
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
                    imageUri?.let {
                        Toast.makeText(this, "Received image: ${intent.dataString}", Toast.LENGTH_LONG).show()
                    }
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Unsupported type: ${intent.type}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private fun processUrl(url: String, isRetry: Boolean) {
        lifecycleScope.launch {
            // Check if this is an X URL that needs resolution
            if (!UrlResolver.isXRedirectUrl(url)) {
                // Not an X redirect URL, just save it
                saveUrlAndFinish(url, wasResolved = false)
                return@launch
            }

            // Get X credentials from settings
            val settings = settingsRepository.settingsFlow.first()
            val credentials = if (settings.xAuthToken != null && settings.xCt0Token != null) {
                Log.d(TAG, "X credentials found, will use GraphQL")
                UrlResolver.XCredentials(settings.xAuthToken, settings.xCt0Token)
            } else {
                Log.d(TAG, "No X credentials configured")
                null
            }

            // Use the new method that returns detailed status
            when (val result = UrlResolver.resolveXUrlWithStatus(url, credentials)) {
                is UrlResolver.ResolveResult.Success -> {
                    Log.d(TAG, "URL resolved successfully: ${result.url}")
                    saveUrlAndFinish(result.url, wasResolved = result.url != url)
                }

                is UrlResolver.ResolveResult.AuthRequired -> {
                    Log.d(TAG, "Auth required for URL resolution")
                    if (isRetry) {
                        // Already tried login, just save the unresolved URL
                        Log.w(TAG, "Auth still failing after login, saving unresolved")
                        saveUrlAndFinish(url, wasResolved = false)
                    } else {
                        // Prompt user to login
                        pendingUrl = url
                        val reason = if (credentials != null) {
                            XLoginActivity.REASON_AUTH_EXPIRED
                        } else {
                            XLoginActivity.REASON_INITIAL_SETUP
                        }
                        promptForLogin(reason)
                    }
                }

                is UrlResolver.ResolveResult.Failed -> {
                    Log.w(TAG, "URL resolution failed: ${result.reason}")
                    // Save the unresolved URL
                    saveUrlAndFinish(url, wasResolved = false)
                }
            }
        }
    }

    private fun promptForLogin(reason: String) {
        Toast.makeText(
            this,
            if (reason == XLoginActivity.REASON_AUTH_EXPIRED)
                "X session expired. Please log in again."
            else
                "Log in to X to resolve this URL",
            Toast.LENGTH_SHORT
        ).show()

        val loginIntent = XLoginActivity.createIntent(this, reason)
        xLoginLauncher.launch(loginIntent)
    }

    private fun saveUrlAndFinish(url: String, wasResolved: Boolean) {
        lifecycleScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            if (settings.editSharedLinkBeforeSave) {
                // Let the user review/edit the link (and add keywords) before it's saved.
                showEditDialog(url, wasResolved, settings.themePreference)
            } else {
                insertAndFinish(url, keywords = emptyList(), wasResolved = wasResolved)
            }
        }
    }

    /**
     * Renders an editable dialog over the transparent activity. On save the (possibly
     * edited) link and keywords are inserted; on cancel nothing is saved.
     */
    private fun showEditDialog(url: String, wasResolved: Boolean, themePreference: ThemePreference) {
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

    private suspend fun insertAndFinish(url: String, keywords: List<String>, wasResolved: Boolean) {
        var contentLink = ContentLink(link = url)
        if (keywords.isNotEmpty()) {
            contentLink = LinkMetadata.setTags(contentLink, keywords, ChipsType.KeyWords)
        }
        contentRepository.insert(contentLink)

        val message = if (wasResolved) {
            "Link resolved and saved"
        } else {
            "Link saved"
        }
        Toast.makeText(this@ShareReceiverActivity, message, Toast.LENGTH_SHORT).show()

        // Fetch YouTube metadata before finishing (lifecycleScope cancels on finish)
        if (YouTubeUrlParser.isYouTubeUrl(url)) {
            try {
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    youTubeRepository.getVideoMetadata(url)
                }
                if (result is cut.the.crap.data.rest.Result.Success) {
                    val recentItems = contentRepository.byTimeRange(
                        start = contentLink.added - 1000,
                        end = contentLink.added + 1000
                    )
                    val dbItem = recentItems.firstOrNull { it.link == url }
                    if (dbItem != null) {
                        val type = parseSocialMediaUrl(url)?.contentType ?: "video"
                        val updated = LinkMetadata.setYouTubeMetadata(
                            dbItem,
                            channelName = result.data.channelName,
                            videoTitle = result.data.title,
                            thumbnailUrl = result.data.thumbnailUrl,
                            contentType = type
                        )
                        contentRepository.update(updated)
                        Log.d(TAG, "YouTube metadata saved for: $url")
                    }
                } else if (result is cut.the.crap.data.rest.Result.Error) {
                    Log.e(TAG, "YouTube metadata fetch failed: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "YouTube metadata fetch error", e)
            }
        }

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
        title = { Text(stringResource(R.string.share_edit_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.share_edit_dialog_link_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text(stringResource(R.string.share_edit_dialog_keywords_label)) },
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
                Text(stringResource(R.string.share_edit_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.share_edit_dialog_cancel))
            }
        }
    )
}
