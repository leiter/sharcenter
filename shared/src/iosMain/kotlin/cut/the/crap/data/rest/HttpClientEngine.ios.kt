package cut.the.crap.data.rest

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

/**
 * Ktor's Darwin (NSURLSession) engine. Unlike Android/desktop (which share OkHttp for identical
 * redirect semantics), iOS has no OkHttp; UrlResolver's redirect handling is expressed through
 * Ktor's engine-independent `followRedirects` config, so Darwin is fine.
 */
actual fun httpClientEngine(): HttpClientEngineFactory<*> = Darwin
