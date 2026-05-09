package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_sessions ADD COLUMN aiCharacterId INTEGER")
        db.execSQL("ALTER TABLE chat_sessions ADD COLUMN worldBookId INTEGER")
    }
}
