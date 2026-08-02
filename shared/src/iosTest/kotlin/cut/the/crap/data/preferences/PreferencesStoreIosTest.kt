package cut.the.crap.data.preferences

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A first `iosTest` — proves the native (`iosSimulatorArm64Test`) source set is wired and can
 * exercise iosMain code. [preferencesPath] resolves an `NSDocumentDirectory`-based path, so this
 * asserts its shape without pinning the (per-simulator) absolute Documents location.
 */
class PreferencesStoreIosTest {

    @Test
    fun preferencesPathLandsUnderDatastore() {
        val path = preferencesPath("settings").toString()
        assertTrue(
            path.endsWith("/datastore/settings.preferences_pb"),
            "unexpected preferences path: $path",
        )
        assertTrue(path.contains("/Documents/"), "expected an NSDocumentDirectory path: $path")
    }
}
