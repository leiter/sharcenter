package cut.the.crap.platform

/** No build-type concept on plain JVM; desktop is dev-only for now (see WP8 notes). */
actual val isDebugBuild: Boolean = true
