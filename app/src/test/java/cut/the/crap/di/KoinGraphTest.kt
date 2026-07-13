package cut.the.crap.di

import android.app.Application
import android.content.Context
import cut.the.crap.data.rest.networkModule
import cut.the.crap.data.rest.repositoryModule
import cut.the.crap.share.shareModule
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
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
                platformModule,
                databaseModule,
                networkModule,
                repositoryModule,
                shareModule,
                viewModelModule,
            )
        }.verify(
            extraTypes = listOf(
                Context::class,
                Application::class,
                // Supplied to the HttpClient factory lambda, not constructor-injected;
                // verify() reflects the constructor, so declare them as external.
                HttpClientEngine::class,
                HttpClientConfig::class,
            ),
        )
    }
}
