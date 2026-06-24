package cut.the.crap.tools

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Adds a new column 'position' to the 'tweets_table' table
        db.execSQL("ALTER TABLE tweets_table ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Creates the new handle_tag_table
        db.execSQL("""
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
        """)
        // Create indices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_handle_tag_table_type ON handle_tag_table(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_handle_tag_table_lastUsed ON handle_tag_table(lastUsed)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_handle_tag_table_text_type ON handle_tag_table(text, type)")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Creates the new content_items_table
        db.execSQL("""
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
        """)
        // Create indices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_content_items_table_created ON content_items_table(created)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_content_items_table_lastModified ON content_items_table(lastModified)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_content_items_table_sortOrder ON content_items_table(sortOrder)")
    }
}
