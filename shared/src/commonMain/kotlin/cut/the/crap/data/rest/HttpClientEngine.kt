package cut.the.crap.data.rest

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * The platform's Ktor engine.
 *
 * `commonMain` cannot name a concrete engine (`OkHttp`, `Darwin`, …) — they live in the platform
 * source sets — so any client built in shared code gets its engine through this seam. Android and
 * desktop both return OkHttp, deliberately: `UrlResolver` depends on precise redirect semantics,
 * and using one engine everywhere means those behave identically on both targets. A future iOS
 * target would return `Darwin` here and nothing else would change.
 */
expect fun httpClientEngine(): HttpClientEngineFactory<*>
