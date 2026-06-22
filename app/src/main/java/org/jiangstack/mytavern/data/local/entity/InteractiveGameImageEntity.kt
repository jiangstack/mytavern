package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interactive_game_images",
    foreignKeys = [
        ForeignKey(
            entity = InteractiveGameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class InteractiveGameImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val remoteUrl: String,
    val localUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
