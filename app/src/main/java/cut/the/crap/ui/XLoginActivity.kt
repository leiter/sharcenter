package cut.the.crap.ui

import org.jetbrains.compose.resources.getString

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.shared.resources.settings_toast_x_login_success
import cut.the.crap.shared.resources.xlogin_logged_in_as
import cut.the.crap.shared.resources.xlogin_prompt
import cut.the.crap.shared.resources.xlogin_session_expired
import cut.the.crap.shared.resources.xlogin_title
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.ui.theme.MyAppTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "XLoginActivity"

class XLoginActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    companion object {
        const val EXTRA_REASON = "reason"
        const val REASON_INITIAL_SETUP = "initial_setup"
        const val REASON_AUTH_EXPIRED = "auth_expired"
        const val RESULT_LOGIN_SUCCESS = 1
        const val RESULT_LOGIN_CANCELLED = 0

        fun createIntent(context: Context, reason: String = REASON_INITIAL_SETUP): Intent {
            return Intent(context, XLoginActivity::class.java).apply {
                putExtra(EXTRA_REASON, reason)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reason = intent.getStringExtra(EXTRA_REASON) ?: REASON_INITIAL_SETUP

        setContent {
            MyAppTheme {
                XLoginScreen(
                    reason = reason,
                    onLoginSuccess = { authToken, ct0Token, screenName ->
                        handleLoginSuccess(authToken, ct0Token, screenName)
                    },
                    onCancel = {
                        setResult(RESULT_LOGIN_CANCELLED)
                        finish()
                    }
                )
            }
        }
    }

    private fun handleLoginSuccess(authToken: String, ct0Token: String, screenName: String?) {
        lifecycleScope.launch {
            settingsRepository.updateXCredentials(authToken, ct0Token)
            val message = if (screenName != null) {
                getString(Res.string.xlogin_logged_in_as, screenName)
            } else {
                getString(Res.string.settings_toast_x_login_success)
            }
            Toast.makeText(this@XLoginActivity, message, Toast.LENGTH_SHORT).show()
            setResult(RESULT_LOGIN_SUCCESS)
            finish()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun XLoginScreen(
    reason: String,
    onLoginSuccess: (authToken: String, ct0Token: String, screenName: String?) -> Unit,
    onCancel: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("") }
    val context = LocalContext.current

    val reasonMessage = when (reason) {
        XLoginActivity.REASON_AUTH_EXPIRED -> stringResource(Res.string.xlogin_session_expired)
        else -> stringResource(Res.string.xlogin_prompt)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.xlogin_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.dialog_cancel))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Info banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = reasonMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // WebView
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.setSupportMultipleWindows(false)
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                            // Enable cookies but don't clear them - X needs some for 2FA flow
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            CookieManager.getInstance().flush()

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    currentUrl = url ?: ""
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    currentUrl = url ?: ""

                                    // Check for successful login by looking for cookies
                                    checkForLoginCookies(url) { authToken, ct0Token, screenName ->
                                        onLoginSuccess(authToken, ct0Token, screenName)
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    // Stay within x.com/twitter.com
                                    return !(url.contains("x.com") || url.contains("twitter.com"))
                                }
                            }

                            loadUrl("https://x.com/i/flow/login")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun checkForLoginCookies(
    url: String?,
    onSuccess: (authToken: String, ct0Token: String, screenName: String?) -> Unit
) {
    if (url == null) return

    Log.d(TAG, "Checking URL: $url")

    // Only check on x.com/twitter.com pages
    if (!url.contains("x.com") && !url.contains("twitter.com")) return

    // Skip if still on login/auth flow pages - wait for full completion
    val loginFlowPatterns = listOf(
        "/i/flow/login",
        "/login",
        "/account/access",
        "/account/login_verification",
        "/i/flow/two_factor"
    )
    if (loginFlowPatterns.any { url.contains(it) }) {
        Log.d(TAG, "Still in login flow, waiting...")
        return
    }

    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie("https://x.com") ?: cookieManager.getCookie("https://twitter.com")

    if (cookies.isNullOrEmpty()) {
        Log.d(TAG, "No cookies found")
        return
    }

    Log.d(TAG, "Checking cookies for login state...")

    // Parse cookies
    val cookieMap = cookies.split(";")
        .map { it.trim() }
        .filter { it.contains("=") }
        .associate {
            val parts = it.split("=", limit = 2)
            parts[0].trim() to parts.getOrElse(1) { "" }.trim()
        }

    val authToken = cookieMap["auth_token"]
    val ct0Token = cookieMap["ct0"]

    // Extract screen_name from twid cookie if available
    val screenName: String? = null // Would need additional API call to get screen name

    if (!authToken.isNullOrEmpty() && !ct0Token.isNullOrEmpty()) {
        Log.d(TAG, "Login successful! auth_token and ct0 found")
        onSuccess(authToken, ct0Token, screenName)
    } else {
        Log.d(TAG, "Cookies incomplete - auth_token: ${authToken != null}, ct0: ${ct0Token != null}")
    }
}
