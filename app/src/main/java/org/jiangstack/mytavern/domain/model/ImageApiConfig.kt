package org.jiangstack.mytavern.domain.model

data class ImageApiConfig(
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String
)
