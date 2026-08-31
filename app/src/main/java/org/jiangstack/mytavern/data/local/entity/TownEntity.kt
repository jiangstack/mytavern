package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "towns")
data class TownEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val worldDescription: String = "",
    val currentDay: Int = 1,
    val currentHour: Int = 8,
    val playMemberId: Long? = null,
    val windowWordCount: Int = 3000,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
