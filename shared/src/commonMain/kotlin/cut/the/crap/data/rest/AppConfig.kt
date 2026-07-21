package cut.the.crap.data.rest

/**
 * Build/runtime configuration the shared networking layer needs, injected rather than read from a
 * platform global. `BuildConfig` is Android-only, so `networkModule` (commonMain) takes these
 * values through DI instead: Android provides them from `BuildConfig`, iOS from a constant/plist,
 * desktop from wherever WP8 decides.
 */
data class AppConfig(
    val apiBaseUrl: String,
    /**
     * Origin of the action-campaign site (`/api/abu-safiya`). A separate host from [apiBaseUrl]:
     * that one is the job-queue backend, this one is the public campaign site.
     */
    val campaignBaseUrl: String,
    val isDebug: Boolean,
)
