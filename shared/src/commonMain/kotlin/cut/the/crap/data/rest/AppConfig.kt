package cut.the.crap.data.rest

/**
 * Build/runtime configuration the shared networking layer needs, injected rather than read from a
 * platform global. `BuildConfig` is Android-only, so `networkModule` (commonMain) takes these
 * values through DI instead: Android provides them from `BuildConfig`, iOS from a constant/plist,
 * desktop from wherever WP8 decides.
 */
data class AppConfig(
    val apiBaseUrl: String,
    val isDebug: Boolean,
)
