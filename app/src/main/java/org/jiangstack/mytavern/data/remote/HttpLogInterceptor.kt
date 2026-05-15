package org.jiangstack.mytavern.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
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
                durationMs = System.currentTimeMillis() - startTime
            )
            httpLogRepository.add(log)
            throw e
        }

        val durationMs = System.currentTimeMillis() - startTime
        val responseHeaders = formatHeaders(response.headers, filterSensitive = false)
        val responseBody = try {
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
}
