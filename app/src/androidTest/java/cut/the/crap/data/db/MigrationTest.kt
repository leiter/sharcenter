package cut.the.crap.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance gate for the Room -> SQLDelight migration.
 *
 * Builds a database exactly as the Room build left it (v4 schema, `user_version = 4`,
 * seeded rows), then opens it with the SQLDelight driver and asserts that:
 *  - the 4 -> 5 `.sqm` migration runs (subjects + cross-ref tables appear),
 *  - pre-existing v4 rows survive and are readable through the generated queries
 *    (which also exercises the Int/Boolean column adapters),
 *  - `ON DELETE CASCADE` still works, i.e. `PRAGMA foreign_keys` is on as it was under Room.
 *
 * This is the same guarantee the previous Room `MigrationTestHelper` test gave, and it is
 * what protects existing installs' data.
 *
 * Requires a device/emulator; runs on the non-minified `instrumentation` build type:
 *   ./gradlew connectedInstrumentationAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testDb = "migration-test.db"

    @Before
    fun setUp() = context.deleteDatabase(testDb).let { }

    @After
    fun tearDown() = context.deleteDatabase(testDb).let { }

    /** Recreates the v4 schema exactly as Room had it, seeds rows, and stamps user_version = 4. */
    private fun createRoomV4Database() {
        val path = context.getDatabasePath(testDb)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS tweets_table (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    link TEXT NOT NULL,
                    added INTEGER NOT NULL,
                    position INTEGER NOT NULL DEFAULT 0,
                    description TEXT NOT NULL DEFAULT '',
                    favourite INTEGER NOT NULL DEFAULT 0,
                    hideItem INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_items_table (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    text TEXT NOT NULL,
                    created INTEGER NOT NULL,
                    lastModified INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    category TEXT,
                    isActive INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS handle_tag_table (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    text TEXT NOT NULL,
                    type INTEGER NOT NULL,
                    created INTEGER NOT NULL,
                    lastUsed INTEGER NOT NULL,
                    usageCount INTEGER NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    category TEXT,
                    color TEXT,
                    isArchived INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                "INSERT INTO tweets_table (id, link, added, position, description, favourite, hideItem) " +
                    "VALUES (1, 'https://example.com', 1000, 0, 'seed link', 0, 0)"
            )
            db.execSQL(
                "INSERT INTO content_items_table (id, text, created, lastModified, sortOrder, isFavorite, category, isActive) " +
                    "VALUES (1, 'seed post', 1000, 1000, 0, 1, NULL, 0)"
            )
            db.execSQL(
                "INSERT INTO handle_tag_table (id, text, type, created, lastUsed, usageCount, isFavorite, category, color, isArchived, sortOrder) " +
                    "VALUES (1, '@seed', 0, 1000, 1000, 3, 0, NULL, NULL, 0, 0)"
            )

            db.version = 4 // Room's user_version
        }
    }

    @Test
    fun migrate4To5_preservesExistingDataAndAddsSubjectTables() = runBlockingTest {
        createRoomV4Database()

        // Opening with the SQLDelight driver must run the 4 -> 5 migration.
        val driver = createDriver(context, testDb)
        val db = createDatabase(driver)

        // --- v4 rows survived, and read back through the generated queries + adapters ---
        val link = db.contentLinkQueries.getById(1, ::ContentLinkDB).executeAsOneOrNull()
        assertNotNull(link)
        assertEquals("https://example.com", link!!.link)
        assertEquals(1, link.id)
        assertEquals(false, link.favourite)
        // The comment column added in the 5 -> 6 migration; the driver runs straight to the
        // current schema version, so the v4 seed row picks it up with no value.
        assertNull(link.comment)

        val post = db.contentItemQueries.getById(1, ::ContentItemDB).executeAsOneOrNull()
        assertNotNull(post)
        assertEquals("seed post", post!!.text)
        assertEquals(true, post.isFavorite) // Boolean adapter round-trip
        assertNull(post.category)

        val keyword = db.keywordQueries.getById(1, ::KeywordDB).executeAsOneOrNull()
        assertNotNull(keyword)
        assertEquals("@seed", keyword!!.text)
        assertEquals(3, keyword.usageCount) // Int adapter round-trip

        // --- new v5 tables exist and the many-to-many joins work ---
        db.subjectQueries.insertWithId(1, "Politics", "FF0000", 2000, 2000)
        db.subjectQueries.linkPost(1, 1)
        db.subjectQueries.linkLink(1, 1)

        val forPost = db.subjectQueries.getSubjectsForPost(1, ::SubjectDB).executeAsList()
        assertEquals(1, forPost.size)
        assertEquals("Politics", forPost.first().name)

        val forLink = db.subjectQueries.getSubjectsForLink(1, ::SubjectDB).executeAsList()
        assertEquals(1, forLink.size)

        // --- ON DELETE CASCADE still enforced (PRAGMA foreign_keys=ON, as under Room) ---
        db.contentItemQueries.delete(1)
        assertTrue(db.subjectQueries.getSubjectsForPost(1, ::SubjectDB).executeAsList().isEmpty())

        driver.close()
    }

    // Minimal blocking bridge so the test body can stay straight-line.
    private fun runBlockingTest(block: suspend () -> Unit) =
        kotlinx.coroutines.runBlocking { block() }
}
