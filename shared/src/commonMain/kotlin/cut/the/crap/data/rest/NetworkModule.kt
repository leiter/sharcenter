package cut.the.crap.data.rest

import cut.the.crap.identity.RequestSigner
import cut.the.crap.platform.CryptoProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Shared Ktor [HttpClient], on the per-platform [httpClientEngine] and the injected [AppConfig]
 * (base URL + debug flag) — no `BuildConfig`, so this lives in commonMain. `single` to mirror the
 * previous `@Singleton` scope.
 */
val networkModule = module {
    single {
        val config = get<AppConfig>()
        // Optional: a target without an identity implementation (iOS, see IDENTITY_SPEC §3.2)
        // does not load identityModule, and then every request simply goes out unsigned.
        val signer = getOrNull<RequestSigner>()
        val crypto = getOrNull<CryptoProvider>()
        HttpClient(httpClientEngine()) {
            // Default request configuration with base URL
            defaultRequest {
                url(config.apiBaseUrl)
            }

            // Identity signatures, for the campaign host only — never for the job-queue backend.
            install(CtcSignature) {
                this.signer = signer
                this.crypto = crypto
                signedHost = Url(config.campaignBaseUrl).host
            }

            // Content Negotiation for JSON serialization
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }

            // Timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000  // 30 seconds
                connectTimeoutMillis = 10_000  // 10 seconds
                socketTimeoutMillis = 30_000   // 30 seconds
            }

            // Logging (only in debug builds)
            if (config.isDebug) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }
            }
        }
    }
}
