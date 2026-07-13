package cut.the.crap

import org.jetbrains.compose.resources.getString

import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.backup_toast_failed
import cut.the.crap.shared.resources.backup_toast_success
import cut.the.crap.shared.resources.restore_toast_failed
import cut.the.crap.shared.resources.restore_toast_success
import cut.the.crap.shared.resources.share_chooser_title

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath
import cut.the.crap.data.backup.DatabaseBackupManager
import cut.the.crap.data.rest.YouTubeMetadataBackfiller
import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.data.rest.task.ShareLinksTask
import cut.the.crap.intent.FacebookIntent
import cut.the.crap.intent.TwitterIntent
import cut.the.crap.tools.copyToClipboard
import cut.the.crap.intent.extractTweetId
import cut.the.crap.tools.insertText
import android.widget.Toast
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ContentItemAction
import cut.the.crap.ui.components.api.ContentLinkAction
import cut.the.crap.ui.components.api.FileAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UploadAction
import cut.the.crap.ui.theme.MyAppTheme
import cut.the.crap.ui.content.NavigationGraph
import cut.the.crap.ui.content.Screen
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.posts.composePostFromLink
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.settings.SettingsViewModel
import cut.the.crap.data.rest.networkModule
import cut.the.crap.data.rest.repositoryModule
import cut.the.crap.di.platformModule
import cut.the.crap.di.databaseModule
import cut.the.crap.di.viewModelModule
import cut.the.crap.share.shareModule
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.startKoin

class MyApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            modules(
                platformModule,
                databaseModule,
                networkModule,
                repositoryModule,
                shareModule,
                viewModelModule,
            )
        }
    }

    /**
     * Provides the app-wide Coil [ImageLoader] used by every AsyncImage (currently the link
     * thumbnails). Explicitly wires a memory cache and a persistent disk cache so thumbnails
     * survive scrolling and app restarts.
     *
     * Coil 3 ships no network fetcher, so one is registered explicitly; the Ktor 2 fetcher
     * reuses the HTTP stack the app already depends on (and works on desktop later).
     *
     * Cache-header behaviour: Coil 2 needed `respectCacheHeaders(false)` here because some
     * thumbnail hosts (e.g. YouTube) send short-lived / no-store Cache-Control headers that
     * would otherwise force a re-download. Coil 3 ignores cache headers *by default* — you
     * opt in via the `coil-network-cache-control` artifact, which we deliberately do not add.
     * So omitting the old call preserves the previous behaviour.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

class MainActivity : ComponentActivity() {

    private val linksViewModel: LinksViewModel by viewModel()

    private val postsViewModel: PostsViewModel by viewModel()

    private val settingsViewModel: SettingsViewModel by viewModel()

    val databaseBackupManager: DatabaseBackupManager by inject()

    val jobQueueRepository: JobQueueRepository by inject()

    val youTubeMetadataBackfiller: YouTubeMetadataBackfiller by inject()

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // Perform daily database backup if needed (first app start of the day)
        lifecycleScope.launch {
            databaseBackupManager.performDailyBackupIfNeeded()
        }

        // Backfill YouTube metadata for older links that were saved without it
        lifecycleScope.launch {
            youTubeMetadataBackfiller.backfillMissing()
        }

        // DEV: Submit test job to job queue server on app start (debug builds only)
        if (BuildConfig.DEBUG) {
            lifecycleScope.launch {
                val testTask = ShareLinksTask(
                    links = listOf(
                        "https://example.com/article1",
                        "https://example.com/article2",
                        "https://twitter.com/test/status/123"
                    )
                )
                jobQueueRepository.submitTask(testTask)
            }
        }

        setContent {
            // Collect theme preference from settings
            val settings by settingsViewModel.settings.collectAsState()
            val themePreference = settings.themePreference

            MyAppTheme(themePreference = themePreference) {

                val navController = rememberNavController()

                val action: (Action) -> Unit = {
                    handleAction(it, linksViewModel, postsViewModel, navController, this)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationGraph(
                            navController = navController,
                            action = action,
                            linksViewModel = linksViewModel,
                            postsViewModel = postsViewModel,
                            settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

private fun handleAction(
    action: Action,
    linksViewModel: LinksViewModel,
    postsViewModel: PostsViewModel,
    navController: NavHostController,
    activity: ComponentActivity
) {
    when(action){
        // Handle Activity-level actions (type-safe, no casts!)
        is ContentLinkAction.Open -> activity.startActivity(
            Intent(Intent.ACTION_VIEW, action.item.link.toUri())
        )
        is ContentLinkAction.OpenProfile -> activity.startActivity(
            Intent(Intent.ACTION_VIEW, action.url.toUri())
        )
        is ContentLinkAction.CopyToClipboard -> copyToClipboard(activity, action.item.link)

        is ContentLinkAction.ComposePost -> {
            // Bridge Links → Posts: seed the editor with this link's URL + saved markers,
            // then switch to the Posts tab so the draft is ready to edit and send.
            postsViewModel.composePostFromLink(action.item)
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }

        is ContentItemAction.CopyToClipboard -> copyToClipboard(activity, action.contentItem.text)

        is FileAction.BackupDatabase -> {
            // Access DatabaseBackupManager from MainActivity
            val mainActivity = activity as? MainActivity
            mainActivity?.let { main ->
                main.lifecycleScope.launch {
                    val result = main.databaseBackupManager.performManualBackup()
                    result.onSuccess { fileName ->
                        Toast.makeText(
                            activity,
                            getString(Res.string.backup_toast_success, fileName),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    result.onFailure { error ->
                        Toast.makeText(
                            activity,
                            getString(Res.string.backup_toast_failed, error.message ?: ""),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        is FileAction.RestoreDatabase -> {
            val mainActivity = activity as? MainActivity
            mainActivity?.let { main ->
                main.lifecycleScope.launch {
                    val result = main.databaseBackupManager.restoreFromBackup(action.uri)
                    result.onSuccess { name ->
                        Toast.makeText(
                            activity,
                            getString(Res.string.restore_toast_success, name),
                            Toast.LENGTH_LONG
                        ).show()
                        // Room must reopen the replaced file, so restart the process.
                        restartApp(activity)
                    }
                    result.onFailure { error ->
                        Toast.makeText(
                            activity,
                            getString(Res.string.restore_toast_failed, error.message ?: ""),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        is TextAction.PasteFromClipboard -> {
            // Get text from clipboard
            val clipboardManager = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                if (pastedText.isNotEmpty()) {
                    // Insert the pasted text into the current content
                    val currentText = postsViewModel.screenState.value.focusedContentText
                    val updatedText = currentText.insertText(pastedText)
                    postsViewModel.consumeAction(TextAction.EditContentText(updatedText))
                }
            }
        }

        is TextAction.CopyContentText -> {
            // Copy the current editor content to the clipboard
            val currentText = postsViewModel.screenState.value.focusedContentText.newText
            if (currentText.isNotEmpty()) {
                copyToClipboard(activity, currentText)
            }
        }

        is TextAction.PostContentOnTwitter -> {
            // Post the current draft text as a new tweet on Twitter/X
            val currentText = postsViewModel.screenState.value.focusedContentText.newText
            if (currentText.isNotEmpty()) {
                val twitterIntent = TwitterIntent.PostTweet(text = currentText)
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, twitterIntent.url.toUri())
                )
            }
        }

        is TextAction.PostContentOnFacebook -> {
            // Share the current draft text as a new post on Facebook
            val currentText = postsViewModel.screenState.value.focusedContentText.newText
            if (currentText.isNotEmpty()) {
                val facebookIntent = FacebookIntent.SharePost(text = currentText)
                // Facebook's web sharer does not reliably prefill the post text, so copy it to the
                // clipboard first so the user can paste it into the composer.
                copyToClipboard(activity, currentText)
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, facebookIntent.url.toUri())
                )
            }
        }

        is TextAction.ShareContentViaSheet -> {
            // Hand the current draft text to the native Android share sheet (ACTION_SEND).
            val currentText = postsViewModel.screenState.value.focusedContentText.newText
            if (currentText.isNotEmpty()) {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, currentText)
                }
                activity.lifecycleScope.launch {
                    activity.startActivity(
                        Intent.createChooser(sendIntent, getString(Res.string.share_chooser_title))
                    )
                }
            }
        }

        is ContentItemAction.PostOnTwitter -> {
            // Post the content as a new tweet on Twitter/X
            // TwitterIntent.PostTweet automatically extracts any URLs from the content to use as link preview
            val twitterIntent = TwitterIntent.PostTweet(
                text = action.contentItem.text
            )

            // Launch the Twitter/X intent
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, twitterIntent.url.toUri())
            )
        }

        is ContentItemAction.PostOnFacebook -> {
            // Share the content as a new post on Facebook
            // FacebookIntent.SharePost automatically extracts any URLs from the content to use as link preview
            val facebookIntent = FacebookIntent.SharePost(
                text = action.contentItem.text
            )

            // Facebook's web sharer does not reliably prefill the post text, so copy it to the
            // clipboard first so the user can paste it into the composer.
            copyToClipboard(activity, action.contentItem.text)

            // Launch the Facebook intent
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, facebookIntent.url.toUri())
            )
        }

        is ContentItemAction.ShareViaSheet -> {
            // Hand the post text to the native Android share sheet (ACTION_SEND). This exposes
            // every installed app that accepts plain text — WhatsApp, LinkedIn, Bluesky,
            // Mastodon, Telegram, etc. — without a dedicated intent class per network.
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, action.contentItem.text)
            }
            activity.lifecycleScope.launch {
                activity.startActivity(
                    Intent.createChooser(sendIntent, getString(Res.string.share_chooser_title))
                )
            }
        }

        is ContentLinkAction.CreateComment -> {
            // Close the dialog first (better UX - dialog closes before leaving screen)
            linksViewModel.consumeAction(action)

            // For comments, extract tweet ID to reply directly if it's a Twitter/X URL
            val tweetId = action.item.link.extractTweetId()

            // Check if extraction was successful (tweet ID will be different from original URL)
            val twitterIntent = if (tweetId != action.item.link && tweetId.all { it.isDigit() }) {
                // Successfully extracted tweet ID - use Reply to comment on the tweet
                TwitterIntent.Reply(
                    tweetId = tweetId,
                    replyText = action.comment
                )
            } else {
                // Not a tweet URL - post as regular tweet with the link as preview
                TwitterIntent.PostTweet(
                    text = action.comment,
                    prominentUrl = action.item.link
                )
            }

            activity.startActivity(
                Intent(Intent.ACTION_VIEW, twitterIntent.url.toUri())
            )
        }

        is ContentLinkAction.CreateQuote -> {
            // Close the dialog first (better UX - dialog closes before leaving screen)
            linksViewModel.consumeAction(action)

            // For quotes, extract tweet ID and use QuoteTweet if it's a Twitter/X URL
            val tweetId = action.item.link.extractTweetId()

            // Check if extraction was successful (tweet ID will be different from original URL)
            val twitterIntent = if (tweetId != action.item.link && tweetId.all { it.isDigit() }) {
                // Successfully extracted tweet ID - use native quote tweet
                TwitterIntent.QuoteTweet(
                    tweetId = tweetId,
                    comment = action.quote
                )
            } else {
                // Not a tweet URL or extraction failed - post as regular tweet with link
                TwitterIntent.PostTweet(
                    text = action.quote,
                    prominentUrl = action.item.link
                )
            }

            activity.startActivity(
                Intent(Intent.ACTION_VIEW, twitterIntent.url.toUri())
            )
        }

        // Handle UploadAction with ContentResolver
        is UploadAction -> {
            postsViewModel.consumeActionWithResolver(action, activity.contentResolver)
        }

        // Route to appropriate ViewModel
        else -> {
            linksViewModel.consumeAction(action)
            postsViewModel.consumeAction(action)
        }
    }
}

/**
 * Relaunches the app from a cold start by killing the current process. Used after a
 * database restore so Room reopens the replaced database file from scratch.
 */
private fun restartApp(activity: ComponentActivity) {
    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
    activity.startActivity(intent)
    activity.finish()
    Runtime.getRuntime().exit(0)
}