package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "town_members",
    foreignKeys = [
        ForeignKey(
            entity = TownEntity::class,
            parentColumns = ["id"],
            childColumns = ["townId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["townId"])]
)
data class TownMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val townId: Long,
    val characterId: Long,
    val persona: String = "",
    val isPlayerControlled: Boolean = false,
    val currentLocationId: Long? = null,
    val currentActivity: String = "",
    val mood: String = "",
    val todayScheduleJson: String = "[]",
    val recentMemoryJson: String = "[]",
    val importantMemoryJson: String = "[]"
)
