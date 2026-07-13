package cut.the.crap.di

import cut.the.crap.platform.AndroidNotifier
import cut.the.crap.platform.Notifier
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android bindings for the platform seams declared in `:shared/commonMain/platform`.
 *
 * These are the interfaces that let UI and ViewModel code stay Android-free: the common code
 * depends on [Notifier] and friends, and only this module knows they are really Toasts. The
 * desktop app (WP8) supplies its own module against the same interfaces.
 *
 * `Log` is absent on purpose — it is `expect`/`actual`, so it needs no runtime binding.
 */
val platformModule = module {
    single<Notifier> { AndroidNotifier(androidContext()) }
}
