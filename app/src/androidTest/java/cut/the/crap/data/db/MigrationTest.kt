package cut.the.crap.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cut.the.crap.tools.MIGRATION_4_5
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 0 safety net for the KMP migration.
 *
 * Validates the v4 -> v5 Room migration (subjects + many-to-many cross-refs added by
 * the color-subjects feature) against the exported schemas, without needing a device
 * that already holds real data. Room additionally asserts that the migrated schema
 * matches `schemas/.../5.json` exactly.
 *
 * This is also the reference behaviour the SQLDelight port must reproduce in Phase 3.
 *
 * Requires a connected device or emulator. Runs against the non-minified
 * `instrumentation` build type (see `testBuildType` in build.gradle.kts):
 *   ./gradlew connectedInstrumentationAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate4To5_preservesExistingDataAndAddsSubjectTables() {
        // --- Given a v4 database seeded with a link and a post ---
        helper.createDatabase(testDb, 4).apply {
            execSQL(
                "INSERT INTO tweets_table " +
                    "(id, link, added, position, description, favourite, hideItem) " +
                    "VALUES (1, 'https://example.com', 1000, 0, 'seed link', 0, 0)"
            )
            execSQL(
                "INSERT INTO content_items_table " +
                    "(id, text, created, lastModified, sortOrder, isFavorite, isActive) " +
                    "VALUES (1, 'seed post', 1000, 1000, 0, 0, 1)"
            )
            close()
        }

        // --- When MIGRATION_4_5 runs (Room validates the result matches 5.json) ---
        val db = helper.runMigrationsAndValidate(testDb, 5, true, MIGRATION_4_5)

        // --- Then the pre-existing v4 rows survived ---
        db.query("SELECT link FROM tweets_table WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("https://example.com", c.getString(0))
        }
        db.query("SELECT text FROM content_items_table WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("seed post", c.getString(0))
        }

        // --- And the new v5 tables are present and usable, including the join rows ---
        db.execSQL(
            "INSERT INTO subjects_table (id, name, colorHex, createdAt, modifiedAt) " +
                "VALUES (1, 'Politics', '#FF0000', 2000, 2000)"
        )
        db.execSQL("INSERT INTO link_subject_cross_ref (linkId, subjectId) VALUES (1, 1)")
        db.execSQL("INSERT INTO post_subject_cross_ref (postId, subjectId) VALUES (1, 1)")

        db.query("SELECT COUNT(*) FROM subjects_table").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.query("SELECT subjectId FROM link_subject_cross_ref WHERE linkId = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        db.close()
    }
}
