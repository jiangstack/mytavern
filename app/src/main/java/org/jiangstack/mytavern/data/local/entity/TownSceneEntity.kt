package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "town_scenes",
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
data class TownSceneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val townId: Long,
    val day: Int,
    val hour: Int,
    val locationId: Long? = null,
    val type: String,
    val status: String,
    val participantIdsJson: String = "[]",
    val linesJson: String = "[]",
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
