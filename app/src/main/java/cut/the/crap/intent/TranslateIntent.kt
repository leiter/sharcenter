package cut.the.crap.intent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Sealed interface representing translation intents.
 * Provides functionality to translate text using installed translation apps.
 */
sealed interface TranslateIntent {
    fun createIntent(context: Context): Intent?

    /**
     * Translate text using any available translation app that supports ACTION_PROCESS_TEXT.
     * This will show a chooser if multiple apps are available.
     *
     * @param text The text to translate
     * @param showChooser If true, always show app chooser. If false, use default app if set.
     * @param chooserTitle Optional custom title for the chooser dialog
     */
    data class TranslateText(
        val text: String,
        val showChooser: Boolean = true,
        val chooserTitle: String = "Translate with"
    ) : TranslateIntent {
        override fun createIntent(context: Context): Intent? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                    putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                }

                if (showChooser) {
                    Intent.createChooser(intent, chooserTitle)
                } else {
                    intent
                }
            } else {
                null // ACTION_PROCESS_TEXT not available before API 23
            }
        }
    }

    /**
     * Translate text specifically with Google Translate app.
     * Falls back to ACTION_PROCESS_TEXT if Google Translate is not available.
     *
     * @param text The text to translate
     * @param sourceLang Optional source language code (e.g., "en", "es"). Auto-detect if null.
     * @param targetLang Optional target language code. Uses user's default if null.
     */
    data class TranslateWithGoogle(
        val text: String,
        val sourceLang: String? = null,
        val targetLang: String? = null
    ) : TranslateIntent {
        override fun createIntent(context: Context): Intent? {
            // Try Google Translate specific intent first
            val googleTranslateIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                setPackage("com.google.android.apps.translate")

                // Add language parameters if specified
                sourceLang?.let { putExtra("key_text_input_language", it) }
                targetLang?.let { putExtra("key_text_output_language", it) }
            }

            // Check if Google Translate is installed
            val packageManager = context.packageManager
            val isGoogleTranslateInstalled = try {
                packageManager.getPackageInfo("com.google.android.apps.translate", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            return if (isGoogleTranslateInstalled && googleTranslateIntent.resolveActivity(packageManager) != null) {
                googleTranslateIntent
            } else {
                // Fallback to generic translation intent
                TranslateText(text, showChooser = false).createIntent(context)
            }
        }
    }

    /**
     * Open Google Translate app directly without pre-filled text.
     * Useful for allowing users to manually enter or paste text.
     */
    data object OpenGoogleTranslate : TranslateIntent {
        override fun createIntent(context: Context): Intent? {
            return try {
                context.packageManager.getLaunchIntentForPackage("com.google.android.apps.translate")
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        /**
         * Query all apps that can handle text translation.
         * Returns a list of package names for apps that support ACTION_PROCESS_TEXT.
         *
         * @param context Android context
         * @return List of TranslationApp objects containing app details
         */
        fun getAvailableTranslationApps(context: Context): List<TranslationApp> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return emptyList()
            }

            val packageManager = context.packageManager
            val testIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, "test")
            }

            val queryFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PackageManager.MATCH_ALL
            } else {
                0
            }

            val resolveInfoList = packageManager.queryIntentActivities(testIntent, queryFlags)

            return resolveInfoList.map { resolveInfo ->
                TranslationApp(
                    packageName = resolveInfo.activityInfo.packageName,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    activityName = resolveInfo.activityInfo.name
                )
            }.distinctBy { it.packageName }
        }

        /**
         * Check if Google Translate is installed on the device.
         *
         * @param context Android context
         * @return true if Google Translate is installed
         */
        fun isGoogleTranslateInstalled(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo("com.google.android.apps.translate", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        /**
         * Check if any translation apps are available on the device.
         *
         * @param context Android context
         * @return true if at least one translation app is available
         */
        fun hasTranslationApps(context: Context): Boolean {
            return getAvailableTranslationApps(context).isNotEmpty()
        }
    }
}

/**
 * Data class representing a translation app installed on the device.
 *
 * @property packageName The app's package name
 * @property appName The human-readable app name
 * @property activityName The specific activity that handles translation
 */
data class TranslationApp(
    val packageName: String,
    val appName: String,
    val activityName: String
)

/**
 * Extension function to launch a translation intent.
 *
 * @param text The text to translate
 * @param showChooser If true, show app chooser for available translation apps
 * @return true if intent was successfully launched, false otherwise
 */
fun Context.launchTranslation(text: String, showChooser: Boolean = true): Boolean {
    val intent = TranslateIntent.TranslateText(text, showChooser).createIntent(this)
    return try {
        intent?.let {
            startActivity(it)
            true
        } ?: false
    } catch (e: Exception) {
        false
    }
}

/**
 * Extension function to launch Google Translate specifically.
 * Falls back to generic translation if Google Translate is not available.
 *
 * @param text The text to translate
 * @param sourceLang Optional source language code
 * @param targetLang Optional target language code
 * @return true if intent was successfully launched, false otherwise
 */
fun Context.launchGoogleTranslate(
    text: String,
    sourceLang: String? = null,
    targetLang: String? = null
): Boolean {
    val intent = TranslateIntent.TranslateWithGoogle(text, sourceLang, targetLang).createIntent(this)
    return try {
        intent?.let {
            startActivity(it)
            true
        } ?: false
    } catch (e: Exception) {
        false
    }
}
