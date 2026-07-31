package cut.the.crap.ui.content.settings.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cut.the.crap.identity.IdentityManager
import org.koin.compose.getKoin

/**
 * Whether this platform can hold an identity at all.
 *
 * `identityModule` is loaded on Android and desktop only — iOS has no `CryptoProvider` or
 * `IdentityKeyStore` implementation yet (`doc/IDENTITY_SPEC.md` §3.1). The UI lives in
 * `commonMain` and is therefore compiled for iOS too, so it has to ask rather than assume:
 * offering an identity screen that cannot resolve its view model would crash on tap.
 *
 * Asking Koin, rather than an `expect`/`actual` platform flag, keeps this true by construction —
 * the answer is "is the thing actually bound", which is the condition that matters. When the
 * Keychain and CryptoKit implementations land, iOS starts loading the module and this turns true
 * with no change here.
 */
@Composable
fun rememberIdentitySupported(): Boolean {
    val koin = getKoin()
    return remember(koin) { koin.getOrNull<IdentityManager>() != null }
}
