package cut.the.crap.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * WP5 spike: proves DataStore actually works **off Android**.
 *
 * The migration hinges on one question — `preferencesDataStore(name = …)` is a Context-bound
 * Android delegate, so is there a multiplatform equivalent that keeps the same Preferences API?
 * These tests answer it by writing to a real file on the JVM and reading it back through a
 * *second* store instance, which is the part that would fail if persistence were faked.
 */
class PreferencesStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val themeKey = stringPreferencesKey("theme_preference")
    private val devModeKey = booleanPreferencesKey("developer_mode")

    @Test
    fun `writes and reads back a preference`() = runBlocking {
        val path = "${tempFolder.newFolder().path}/settings.preferences_pb".toPath()
        val store = createPreferencesStore(name = "unused", path = path)

        store.edit { it[themeKey] = "DARK" }

        assertEquals("DARK", store.data.first()[themeKey])
    }

    @Test
    fun `persists across store instances`() = runBlocking {
        // The real test: a fresh DataStore over the same file must see what the first one wrote.
        // An in-memory fake would pass every other assertion here and fail this one.
        val path = "${tempFolder.newFolder().path}/settings.preferences_pb".toPath()

        // DataStore refuses two live instances over one file, so the first must be closed before
        // the second opens — hence the explicit scope. In the app this never comes up, because the
        // store is a DI singleton.
        val firstScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        createPreferencesStore(name = "unused", path = path, scope = firstScope).edit {
            it[themeKey] = "LIGHT"
            it[devModeKey] = true
        }
        firstScope.cancel()

        val reopened = createPreferencesStore(name = "unused", path = path)
        val prefs = reopened.data.first()

        assertEquals("LIGHT", prefs[themeKey])
        assertEquals(true, prefs[devModeKey])
    }

    @Test
    fun `refuses two live stores over the same file`() {
        // Documenting the constraint that drives the singleton requirement: this is DataStore
        // protecting its write lock, not a bug.
        val path = "${tempFolder.newFolder().path}/settings.preferences_pb".toPath()

        val first = createPreferencesStore(name = "unused", path = path)
        val second = createPreferencesStore(name = "unused", path = path)

        runBlocking { first.edit { it[devModeKey] = true } }

        val error = runCatching { runBlocking { second.data.first() } }.exceptionOrNull()
        assertEquals(IllegalStateException::class, error!!::class)
    }

    @Test
    fun `an absent key reads as null rather than throwing`() = runBlocking {
        val path = "${tempFolder.newFolder().path}/settings.preferences_pb".toPath()
        val store = createPreferencesStore(name = "unused", path = path)

        assertNull(store.data.first()[themeKey])
    }

    @Test
    fun `creates the file lazily, so a first run on a clean machine works`() = runBlocking {
        // Nothing exists yet — not even the parent directory's contents.
        val path = "${tempFolder.newFolder().path}/nested/settings.preferences_pb".toPath()
        val store = createPreferencesStore(name = "unused", path = path)

        store.edit { it[devModeKey] = false }

        assertEquals(false, store.data.first()[devModeKey])
    }

    @Test
    fun `the desktop path is absolute and namespaced per store`() {
        val settings = preferencesPath("app_settings")
        val backup = preferencesPath("backup_preferences")

        assert(settings.isAbsolute) { "expected an absolute path, got $settings" }
        assertEquals("app_settings.preferences_pb", settings.name)
        assertEquals("backup_preferences.preferences_pb", backup.name)
        // Distinct stores must not collide on one file.
        assert(settings != backup)
    }
}
