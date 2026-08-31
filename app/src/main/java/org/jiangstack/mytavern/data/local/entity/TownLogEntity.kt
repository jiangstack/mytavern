package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "town_logs",
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
data class TownLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val townId: Long,
    val day: Int,
    val hour: Int,
    val kind: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
