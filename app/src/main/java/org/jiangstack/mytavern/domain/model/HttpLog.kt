package org.jiangstack.mytavern.domain.model

data class HttpLog(
    val id: Long,
    val timestamp: Long,
    val method: String,
    val url: String,
    val requestHeaders: String,
    val requestBody: String,
    val responseCode: Int,
    val responseHeaders: String,
    val responseBody: String,
    val durationMs: Long
)
