package org.jiangstack.mytavern.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import org.jiangstack.mytavern.data.repository.HttpLogRepository
import org.jiangstack.mytavern.domain.model.HttpLog

class HttpLogInterceptor(
    private val httpLogRepository: HttpLogRepository
) : Interceptor {

    companion object {
        private const val MAX_BODY_LENGTH = 2048
        private const val SENSITIVE_HEADER = "authorization"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val requestHeaders = formatHeaders(request.headers, filterSensitive = true)
        val requestBody = request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8().truncate()
        } ?: ""

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            val log = HttpLog(
                id = httpLogRepository.nextId(),
                timestamp = startTime,
                method = request.method,
                url = request.url.toString(),
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseCode = -1,
                responseHeaders = "",
                responseBody = e.message ?: "Unknown error",
                responseSummary = "",
                durationMs = System.currentTimeMillis() - startTime
            )
            httpLogRepository.add(log)
            throw e
        }

        val durationMs = System.currentTimeMillis() - startTime
        val responseHeaders = formatHeaders(response.headers, filterSensitive = false)
        val isEventStream = response.header("Content-Type")?.contains("text/event-stream") == true
        val responseBody = if (isEventStream) {
            "[SSE Stream]"
        } else try {
            response.peekBody(Long.MAX_VALUE).string().truncate()
        } catch (_: Exception) {
            ""
        }
        val log = HttpLog(
            id = httpLogRepository.nextId(),
            timestamp = startTime,
            method = request.method,
            url = request.url.toString(),
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            responseCode = response.code,
            responseHeaders = responseHeaders,
            responseBody = responseBody,
            responseSummary = extractSummary(responseBody),
            durationMs = durationMs
        )
        httpLogRepository.add(log)

        return response
    }

    private fun formatHeaders(headers: okhttp3.Headers, filterSensitive: Boolean): String {
        return headers.toMultimap().entries.joinToString("\n") { (name, values) ->
            val displayName = name
            val displayValues = if (filterSensitive && name.equals(SENSITIVE_HEADER, ignoreCase = true)) {
                listOf("***")
            } else {
                values
            }
            "$displayName: ${displayValues.joinToString(", ")}"
        }
    }

    private fun String.truncate(): String {
        return if (length > MAX_BODY_LENGTH) substring(0, MAX_BODY_LENGTH) + "\n... (${length - MAX_BODY_LENGTH} more chars)" else this
    }

    private fun extractSummary(responseBody: String): String {
        if (responseBody.isBlank()) return ""
        return try {
            val json = Json.parseToJsonElement(responseBody).jsonObject
            val parts = mutableListOf<String>()

            // OpenAI format: choices[0].finish_reason
            json["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("finish_reason")?.jsonPrimitive?.content?.let {
                parts.add(it)
            }

            // Anthropic format: stop_reason
            json["stop_reason"]?.jsonPrimitive?.content?.let {
                parts.add(it)
            }

            // Model name
            json["model"]?.jsonPrimitive?.content?.let { model ->
                // Truncate long model names (e.g., "deepseek/deepseek-v4-pro" -> "deepseek-v4-pro")
                parts.add(model.substringAfterLast("/"))
            }

            // Token usage
            json["usage"]?.jsonObject?.get("total_tokens")?.jsonPrimitive?.int?.let {
                parts.add("${it} tokens")
            }

            parts.joinToString(" | ")
        } catch (_: Exception) {
            ""
        }
    }
}
