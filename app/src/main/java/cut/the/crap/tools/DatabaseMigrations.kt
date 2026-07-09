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

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Subjects: a colour (optionally named) used to bundle posts and links.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS subjects_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT,
                colorHex TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                modifiedAt INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_subjects_table_modifiedAt ON subjects_table(modifiedAt)")

        // Many-to-many join: post -> subject.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS post_subject_cross_ref (
                postId INTEGER NOT NULL,
                subjectId INTEGER NOT NULL,
                PRIMARY KEY(postId, subjectId),
                FOREIGN KEY(postId) REFERENCES content_items_table(id) ON DELETE CASCADE,
                FOREIGN KEY(subjectId) REFERENCES subjects_table(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_post_subject_cross_ref_subjectId ON post_subject_cross_ref(subjectId)")

        // Many-to-many join: link -> subject.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS link_subject_cross_ref (
                linkId INTEGER NOT NULL,
                subjectId INTEGER NOT NULL,
                PRIMARY KEY(linkId, subjectId),
                FOREIGN KEY(linkId) REFERENCES tweets_table(id) ON DELETE CASCADE,
                FOREIGN KEY(subjectId) REFERENCES subjects_table(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_link_subject_cross_ref_subjectId ON link_subject_cross_ref(subjectId)")
    }
}
