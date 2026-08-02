package cut.the.crap.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cut.the.crap.data.db.sql.ShareDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies the Room -> SQLDelight data migration on the JVM, using the JDBC driver —
 * the same driver desktop/macOS will use — so this runs without a device.
 *
 * Builds a database exactly as the Room build left it (v4 schema, `user_version = 4`,
 * seeded rows), runs the `.sqm` migrations, and asserts that existing data survives and
 * the v5 subject tables appear. This is the guarantee that protects existing installs.
 */
class SqlDelightMigrationTest {

    private lateinit var driver: SqlDriver

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        exec("PRAGMA foreign_keys=ON")
    }

    @After
    fun tearDown() = driver.close()

    private fun exec(sql: String) = driver.execute(null, sql, 0).value.let { }

    private fun userVersion(): Long =
        driver.executeQuery(null, "PRAGMA user_version", { c -> c.next(); app.cash.sqldelight.db.QueryResult.Value(c.getLong(0)!!) }, 0).value

    /** Recreates the v4 schema exactly as Room had it, seeds rows, stamps user_version = 4. */
    private fun createRoomV4Database() {
        exec(
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
        exec(
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
        exec(
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

        exec(
            "INSERT INTO tweets_table (id, link, added, position, description, favourite, hideItem) " +
                "VALUES (1, 'https://example.com', 1000, 0, 'seed link', 0, 0)"
        )
        exec(
            "INSERT INTO content_items_table (id, text, created, lastModified, sortOrder, isFavorite, category, isActive) " +
                "VALUES (1, 'seed post', 1000, 1000, 0, 1, NULL, 0)"
        )
        exec(
            "INSERT INTO handle_tag_table (id, text, type, created, lastUsed, usageCount, isFavorite, category, color, isArchived, sortOrder) " +
                "VALUES (1, '@seed', 0, 1000, 1000, 3, 0, NULL, NULL, 0, 0)"
        )

        exec("PRAGMA user_version = 4")
    }

    @Test
    fun schemaVersionIs6() {
        assertEquals(6L, ShareDatabase.Schema.version)
    }

    @Test
    fun migrate4To5_preservesExistingDataAndAddsSubjectTables() {
        createRoomV4Database()
        assertEquals(4L, userVersion())

        // Run the 4 -> 5 migration (the .sqm file mirroring Room's MIGRATION_4_5), then 5 -> 6
        // (adds the comment column) so the row is on the same schema the generated mapper expects.
        ShareDatabase.Schema.migrate(driver, 4, 5).value
        ShareDatabase.Schema.migrate(driver, 5, 6).value

        val db = createDatabase(driver)

        // --- v4 rows survived, read back through the generated queries + column adapters ---
        val link = db.contentLinkQueries.getById(1, ::ContentLinkDB).executeAsOneOrNull()
        assertNotNull(link)
        assertEquals("https://example.com", link!!.link)
        assertEquals(1, link.id)
        assertFalse(link.favourite)

        val post = db.contentItemQueries.getById(1, ::ContentItemDB).executeAsOneOrNull()
        assertNotNull(post)
        assertEquals("seed post", post!!.text)
        assertTrue(post.isFavorite) // Boolean adapter round-trip
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
        assertEquals(1, db.subjectQueries.getSubjectsForLink(1, ::SubjectDB).executeAsList().size)

        // --- ON DELETE CASCADE still enforced, as it was under Room ---
        db.contentItemQueries.delete(1)
        assertTrue(db.subjectQueries.getSubjectsForPost(1, ::SubjectDB).executeAsList().isEmpty())
    }

    @Test
    fun freshInstall_createsV5SchemaAndRoundTripsRows() {
        ShareDatabase.Schema.create(driver).value
        val db = createDatabase(driver)

        db.contentLinkQueries.insert(
            link = "https://fresh.example",
            added = 42,
            position = 1,
            description = "d",
            favourite = true,
            hideItem = false,
            comment = "a quote about this link",
        )
        val all = db.contentLinkQueries.getItems(
            includeFavourite = null,
            includeHidden = null,
            linkSubstring = null,
            startTime = null,
            endTime = null,
            sortByListPosition = false,
            sortByDate = true,
            mapper = ::ContentLinkDB,
        ).executeAsList()

        assertEquals(1, all.size)
        assertEquals("https://fresh.example", all.first().link)
        assertTrue(all.first().favourite)
        assertEquals("a quote about this link", all.first().comment)
        // AUTOINCREMENT assigned the id (Room's autoGenerate behaviour for id == 0)
        assertTrue(all.first().id > 0)
    }

    @Test
    fun migrate5To6_addsCommentColumn() {
        createRoomV4Database()
        ShareDatabase.Schema.migrate(driver, 4, 5).value
        ShareDatabase.Schema.migrate(driver, 5, 6).value

        val db = createDatabase(driver)

        // The v4 seed row survives with comment defaulting to null (no DEFAULT on the new column).
        val link = db.contentLinkQueries.getById(1, ::ContentLinkDB).executeAsOneOrNull()
        assertNotNull(link)
        assertNull(link!!.comment)

        // The new column round-trips.
        db.contentLinkQueries.insert(
            link = "https://example.org",
            added = 2000,
            position = 0,
            description = "",
            favourite = false,
            hideItem = false,
            comment = "reply text",
        )
        val inserted = db.contentLinkQueries.getItems(
            includeFavourite = null,
            includeHidden = null,
            linkSubstring = "example.org",
            startTime = null,
            endTime = null,
            sortByListPosition = false,
            sortByDate = true,
            mapper = ::ContentLinkDB,
        ).executeAsList().first()
        assertEquals("reply text", inserted.comment)
    }
}
