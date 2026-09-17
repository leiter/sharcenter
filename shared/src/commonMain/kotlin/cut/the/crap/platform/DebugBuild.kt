package cut.the.crap.platform

/**
 * Whether this is a debug build of the shared module itself, so commonMain (`networkModule`) can
 * gate debug-only behavior (e.g. request logging) without `AppConfig` carrying the flag through DI.
 */
expect val isDebugBuild: Boolean
