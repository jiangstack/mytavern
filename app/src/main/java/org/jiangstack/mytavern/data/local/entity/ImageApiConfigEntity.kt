package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_api_configs")
data class ImageApiConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String
)
