package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 版本 10 到 11 没有新的 schema 变更，只是为了刷新 identity hash
        // 但实际上需要确保所有列都存在
        try {
            db.execSQL("ALTER TABLE chat_sessions ADD COLUMN novelId INTEGER")
        } catch (_: Exception) { }
        try {
            db.execSQL("ALTER TABLE chat_sessions ADD COLUMN agentSystemPrompt TEXT")
        } catch (_: Exception) { }
        try {
            db.execSQL("ALTER TABLE chat_messages ADD COLUMN role TEXT")
        } catch (_: Exception) { }
        try {
            db.execSQL("ALTER TABLE chat_messages ADD COLUMN messageType TEXT")
        } catch (_: Exception) { }
    }
}

// 处理从版本 9 直接升级到 11 的情况
val MIGRATION_9_11 = object : Migration(9, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_sessions ADD COLUMN novelId INTEGER")
        db.execSQL("ALTER TABLE chat_sessions ADD COLUMN agentSystemPrompt TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN role TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN messageType TEXT")
    }
}
