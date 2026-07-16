package cut.the.crap.ui

import androidx.navigation.NavHostController
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.intent.FacebookIntent
import cut.the.crap.intent.TwitterIntent
import cut.the.crap.intent.extractTweetId
import cut.the.crap.platform.AppRestarter
import cut.the.crap.platform.Clipboard
import cut.the.crap.platform.NotificationDuration
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.Sharer
import cut.the.crap.platform.UrlOpener
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.backup_toast_failed
import cut.the.crap.shared.resources.backup_toast_success
import cut.the.crap.shared.resources.restore_toast_failed
import cut.the.crap.shared.resources.restore_toast_success
import cut.the.crap.shared.resources.share_chooser_title
import cut.the.crap.tools.insertText
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ContentItemAction
import cut.the.crap.ui.components.api.ContentLinkAction
import cut.the.crap.ui.components.api.FileAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.content.Screen
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.posts.PostsViewModel
import cut.the.crap.ui.content.posts.composePostFromLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * The app-root action router: platform-agnostic handling for the [Action]s that a single ViewModel
 * cannot serve on its own — cross-screen navigation, opening/sharing URLs, clipboard, and the
 * backup/restore flow. Everything it touches is a `commonMain` seam ([Clipboard], [UrlOpener],
 * [Sharer], [Notifier], [BackupManager], [AppRestarter]) so this compiles on every target; each
 * platform launcher (Android's `MainActivity`, later desktop/iOS) just supplies the bindings.
 *
 * @param scope a UI-scoped [CoroutineScope] for the suspending backup/restore/share work. Callers
 *   pass a `rememberCoroutineScope()` from composition.
 */
fun handleAction(
    action: Action,
    linksViewModel: LinksViewModel,
    postsViewModel: PostsViewModel,
    navController: NavHostController,
    scope: CoroutineScope,
    clipboard: Clipboard,
    urlOpener: UrlOpener,
    sharer: Sharer,
    notifier: Notifier,
    backupManager: BackupManager,
    appRestarter: AppRestarter,
) {
    when (action) {
        is ContentLinkAction.Open -> urlOpener.open(action.item.link)
        is ContentLinkAction.OpenProfile -> urlOpener.open(action.url)
        is ContentLinkAction.CopyToClipboard -> clipboard.copy(action.item.link)

        is ContentLinkAction.ComposePost -> {
            // Bridge Links → Posts: seed the editor with this link's URL + saved markers,
            // then switch to the Posts tab so the draft is ready to edit and send.
            postsViewModel.composePostFromLink(action.item)
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }

        is ContentItemAction.CopyToClipboard -> clipboard.copy(action.contentItem.text)

        is FileAction.BackupDatabase -> {
            scope.launch {
                val result = backupManager.performManualBackup()
                result.onSuccess { fileName ->
                    notifier.show(getString(Res.string.backup_toast_success, fileName), NotificationDuration.Long)
                }
                result.onFailure { error ->
                    notifier.show(getString(Res.string.backup_toast_failed, error.message ?: ""), NotificationDuration.Long)
                }
            }
        }

        is FileAction.RestoreDatabase -> {
            scope.launch {
                val result = backupManager.restoreFromBackup(action.uri)
                result.onSuccess { name ->
                    notifier.show(getString(Res.string.restore_toast_success, name), NotificationDuration.Long)
                    // The restore replaced the database file under the open driver, so relaunch
                    // to reopen it. Platforms that cannot restart (desktop/iOS) skip this.
                    if (appRestarter.isSupported) appRestarter.restart()
                }
                result.onFailure { error ->
                    notifier.show(getString(Res.string.restore_toast_failed, error.message ?: ""), NotificationDuration.Long)
                }
            }
        }

        is TextAction.PasteFromClipboard -> {
            val pastedText = clipboard.paste()
            if (!pastedText.isNullOrEmpty()) {
                val currentText = postsViewModel.screenState.value.focusedContentText
                val updatedText = currentText.insertText(pastedText)
                postsViewModel.consumeAction(TextAction.EditContentText(updatedText))
            }
        }

        is TextAction.CopyContentText -> {
            val currentText = postsViewModel.screenState.value.focusedContentText.newText
            if (currentText.isNotEmpty()) {
                clipboard.copy(currentText)
            }
        }

        is TextAction.PostContentOnTwitter -> {
            val currentText = postsViewModel.screenState.value.focusedContentText.newText
            if (currentText.isNotEmpty()) {
                urlOpener.open(TwitterIntent.PostTweet(text = currentText).url)
            }
        }

        is TextAction.PostContentOnFacebook -> {
            val currentText = postsViewModel.screenState.value.focusedContentText.newText
            if (currentText.isNotEmpty()) {
                // Facebook's web sharer does not reliably prefill the post text, so copy it to the
                // clipboard first so the user can paste it into the composer.
                clipboard.copy(currentText)
                urlOpener.open(FacebookIntent.SharePost(text = currentText).url)
            }
        }

        is TextAction.ShareContentViaSheet -> {
            val currentText = postsViewModel.screenState.value.focusedContentText.newText
            if (currentText.isNotEmpty()) {
                // The chooser title is a resource, and reading the catalogue suspends.
                scope.launch {
                    sharer.shareText(currentText, getString(Res.string.share_chooser_title))
                }
            }
        }

        is ContentItemAction.PostOnTwitter -> {
            // TwitterIntent.PostTweet automatically extracts any URLs from the content to use as link preview
            urlOpener.open(TwitterIntent.PostTweet(text = action.contentItem.text).url)
        }

        is ContentItemAction.PostOnFacebook -> {
            // Facebook's web sharer does not reliably prefill the post text, so copy it first.
            clipboard.copy(action.contentItem.text)
            urlOpener.open(FacebookIntent.SharePost(text = action.contentItem.text).url)
        }

        is ContentItemAction.ShareViaSheet -> {
            // Hand the post text to the platform share sheet. This exposes every installed app
            // that accepts plain text — WhatsApp, LinkedIn, Bluesky, Mastodon, Telegram, etc.
            scope.launch {
                sharer.shareText(
                    action.contentItem.text,
                    getString(Res.string.share_chooser_title),
                )
            }
        }

        is ContentLinkAction.CreateComment -> {
            // Close the dialog first (better UX - dialog closes before leaving screen)
            linksViewModel.consumeAction(action)

            // For comments, extract tweet ID to reply directly if it's a Twitter/X URL
            val tweetId = action.item.link.extractTweetId()

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

            urlOpener.open(twitterIntent.url)
        }

        is ContentLinkAction.CreateQuote -> {
            // Close the dialog first (better UX - dialog closes before leaving screen)
            linksViewModel.consumeAction(action)

            // For quotes, extract tweet ID and use QuoteTweet if it's a Twitter/X URL
            val tweetId = action.item.link.extractTweetId()

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

            urlOpener.open(twitterIntent.url)
        }

        // Route to appropriate ViewModel
        else -> {
            linksViewModel.consumeAction(action)
            postsViewModel.consumeAction(action)
        }
    }
}
