package cut.the.crap.data.rest

import cut.the.crap.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Shared JSON configuration, used both by the HTTP client's content negotiation and by
     * the schema-driven [cut.the.crap.data.rest.parser.JsonSourceParser], so they agree on
     * lenient/unknown-key handling.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(OkHttp) {
            // Default request configuration with base URL
            defaultRequest {
                url(BuildConfig.API_BASE_URL)
            }

            // Content Negotiation for JSON serialization
            install(ContentNegotiation) {
                json(json)
            }

            // Timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000  // 30 seconds
                connectTimeoutMillis = 10_000  // 10 seconds
                socketTimeoutMillis = 30_000   // 30 seconds
            }

            // Logging (only in debug builds)
            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }
            }
        }
    }
}
