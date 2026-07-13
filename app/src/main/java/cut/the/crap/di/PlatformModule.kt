package cut.the.crap.di

import cut.the.crap.platform.AndroidClipboard
import cut.the.crap.platform.AndroidFileAccess
import cut.the.crap.platform.AndroidNotifier
import cut.the.crap.platform.AndroidSharer
import cut.the.crap.platform.AndroidUrlOpener
import cut.the.crap.platform.Clipboard
import cut.the.crap.platform.FileAccess
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.Sharer
import cut.the.crap.platform.UrlOpener
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android bindings for the platform seams declared in `:shared/commonMain/platform`.
 *
 * These are the interfaces that let UI and ViewModel code stay Android-free: the common code
 * depends on [Notifier] and friends, and only this module knows they are really Toasts, Intents
 * and a ClipboardManager. The desktop app (WP8) supplies its own module against the same
 * interfaces.
 *
 * Every implementation here is handed the *application* Context, so the ones that start an
 * Activity must set `FLAG_ACTIVITY_NEW_TASK` — see [AndroidUrlOpener] and [AndroidSharer].
 *
 * `Log` is absent on purpose — it is `expect`/`actual`, so it needs no runtime binding.
 */
val platformModule = module {
    single<Notifier> { AndroidNotifier(androidContext()) }
    single<FileAccess> { AndroidFileAccess(androidContext()) }
    single<Clipboard> { AndroidClipboard(androidContext()) }
    single<UrlOpener> { AndroidUrlOpener(androidContext()) }
    single<Sharer> { AndroidSharer(androidContext()) }
}
