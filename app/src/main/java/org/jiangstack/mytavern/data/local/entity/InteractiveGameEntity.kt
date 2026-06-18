package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interactive_games")
data class InteractiveGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val narratorStyle: String = "",
    val storyBackground: String = "",
    val storyMainPlot: String = "",
    val windowWordCount: Int = 3000,
    val playCharacterId: Long,
    val worldBookId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
