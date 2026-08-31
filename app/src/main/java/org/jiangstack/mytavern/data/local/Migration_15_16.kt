package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS towns (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                worldDescription TEXT NOT NULL,
                currentDay INTEGER NOT NULL,
                currentHour INTEGER NOT NULL,
                playMemberId INTEGER,
                windowWordCount INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS town_locations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                townId INTEGER NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                FOREIGN KEY(townId) REFERENCES towns(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_town_locations_townId
            ON town_locations(townId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS town_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                townId INTEGER NOT NULL,
                characterId INTEGER NOT NULL,
                persona TEXT NOT NULL,
                isPlayerControlled INTEGER NOT NULL,
                currentLocationId INTEGER,
                currentActivity TEXT NOT NULL,
                mood TEXT NOT NULL,
                todayScheduleJson TEXT NOT NULL,
                recentMemoryJson TEXT NOT NULL,
                importantMemoryJson TEXT NOT NULL,
                FOREIGN KEY(townId) REFERENCES towns(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_town_members_townId
            ON town_members(townId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS town_relationships (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                townId INTEGER NOT NULL,
                memberAId INTEGER NOT NULL,
                memberBId INTEGER NOT NULL,
                affinity INTEGER NOT NULL,
                note TEXT NOT NULL,
                FOREIGN KEY(townId) REFERENCES towns(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_town_relationships_townId
            ON town_relationships(townId)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_town_relationships_townId_memberAId_memberBId
            ON town_relationships(townId, memberAId, memberBId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS town_scenes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                townId INTEGER NOT NULL,
                day INTEGER NOT NULL,
                hour INTEGER NOT NULL,
                locationId INTEGER,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                participantIdsJson TEXT NOT NULL,
                linesJson TEXT NOT NULL,
                summary TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(townId) REFERENCES towns(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_town_scenes_townId
            ON town_scenes(townId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS town_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                townId INTEGER NOT NULL,
                day INTEGER NOT NULL,
                hour INTEGER NOT NULL,
                kind TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(townId) REFERENCES towns(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_town_logs_townId
            ON town_logs(townId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS town_snapshots (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                townId INTEGER NOT NULL,
                name TEXT NOT NULL,
                day INTEGER NOT NULL,
                hour INTEGER NOT NULL,
                snapshotJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(townId) REFERENCES towns(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_town_snapshots_townId
            ON town_snapshots(townId)
            """.trimIndent()
        )
    }
}
