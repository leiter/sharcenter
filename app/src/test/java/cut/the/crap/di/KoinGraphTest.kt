package cut.the.crap.di

import kotlinx.coroutines.CoroutineDispatcher

import android.app.Application
import android.content.Context
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.data.domain.KeywordRepository
import cut.the.crap.data.rest.networkModule
import cut.the.crap.data.rest.repositoryModule
import cut.the.crap.identity.identityModule
import cut.the.crap.share.SharedUrlProcessor
import cut.the.crap.share.shareModule
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify

/**
 * Statically verifies that every Koin definition's constructor dependencies can be
 * resolved from the module set — the build-time replacement for Hilt's compile-time
 * graph validation. Reflection-only: it does not instantiate Android objects.
 *
 * `extraTypes` lists dependencies provided at runtime outside the modules
 * (the Android [Context]/[Application] from `androidContext()`, and framework types
 * Ktor pulls in) so they aren't reported as missing.
 */
class KoinGraphTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun koinGraphResolves() {
        module {
            includes(
                androidAppModule,
                platformModule,
                databaseModule,
                networkModule,
                repositoryModule,
                shareModule,
                viewModelModule,
                // Loaded by MainActivity on Android and by Main.kt on desktop; not on iOS
                // (doc/IDENTITY_SPEC.md §3.1). Verified here so a broken identity binding fails
                // the build rather than the identity screen.
                identityModule,
            )
        }.verify(
            extraTypes = listOf(
                Context::class,
                Application::class,
                // Supplied to the HttpClient factory lambda, not constructor-injected;
                // verify() reflects the constructor, so declare them as external.
                HttpClientEngine::class,
                HttpClientConfig::class,
                // LinksViewModel's dispatchers have Kotlin default values, which verify()
                // also cannot see for the same reason.
                CoroutineDispatcher::class,
                // AppConfig's fields are literal BuildConfig values (androidAppModule), not Koin
                // bindings; verify() reflects its constructor, so declare the field types external.
                // No other definition constructor-injects a raw String/Boolean, so nothing real is hidden.
                String::class,
                Boolean::class,
            ),
            // SharedUrlProcessor takes a List<SharedLinkHandler>. Constructor reflection carries the
            // generic arg (List<SharedLinkHandler>), but the shareModule binding is keyed under the
            // bare List::class, so verify() reports it missing. Declaring the params as bare KClasses
            // matches the binding. The two repositories are named so this stays a real check on them.
            injections = injectedParameters(
                definition<SharedUrlProcessor>(
                    ContentLinkRepository::class,
                    KeywordRepository::class,
                    List::class,
                ),
            ),
        )
    }
}
