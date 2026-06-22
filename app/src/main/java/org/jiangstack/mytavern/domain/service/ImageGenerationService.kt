package org.jiangstack.mytavern.domain.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.jiangstack.mytavern.data.remote.CreateImageTaskRequest
import org.jiangstack.mytavern.data.remote.ImageApiResponse
import org.jiangstack.mytavern.data.remote.ImageApiService
import org.jiangstack.mytavern.domain.model.ImageApiConfig
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.InteractiveGameImage
import org.jiangstack.mytavern.domain.model.InteractiveGameState
import org.jiangstack.mytavern.domain.repository.ImageApiConfigRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameImageRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import java.io.File

class ImageGenerationService(
    private val imageApiService: ImageApiService,
    private val imageApiConfigRepository: ImageApiConfigRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val imageRepository: InteractiveGameImageRepository,
    private val gameRepository: InteractiveGameRepository,
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    suspend fun submitAndPoll(
        game: InteractiveGame,
        gameState: InteractiveGameState?,
        prompt: String,
        paramsJson: String,
        onProgress: (suspend (attempt: Int, maxAttempts: Int) -> Unit)? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val config = getDefaultConfig()
                ?: return@withContext Result.failure(IllegalStateException("请先配置图像 API 默认配置"))

            val baseInput = parseParamsJson(paramsJson)
            val input = buildJsonObject {
                baseInput.entries.forEach { put(it.key, it.value) }
                put("prompt", JsonPrimitive(prompt))
            }

            val request = CreateImageTaskRequest(model = config.model, input = input)
            val createUrl = "${config.baseUrl.trimEnd('/')}/createTask"
            Log.d("ImageGenerationService", "createTask url=$createUrl request=$request")
            val createResponse = imageApiService.createTask(
                url = createUrl,
                authorization = "Bearer ${config.apiKey}",
                request = request
            )
            Log.d("ImageGenerationService", "createTask response: $createResponse")

            if (!createResponse.isSuccess()) {
                return@withContext Result.failure(IllegalStateException(mapApiError(createResponse.code, createResponse.msg)))
            }

            val taskData = createResponse.data
            val recordUrl = "${config.baseUrl.trimEnd('/')}/recordInfo"
            val maxAttempts = 32
            val pollIntervalMs = 10_000L
            for (attempt in 1..maxAttempts) {
                onProgress?.invoke(attempt, maxAttempts)
                delay(pollIntervalMs)

                val recordResponse = imageApiService.getRecordInfo(
                    url = recordUrl,
                    authorization = "Bearer ${config.apiKey}",
                    taskId = taskData.taskId
                )
                Log.d("ImageGenerationService", "recordInfo attempt $attempt: $recordResponse")

                if (!recordResponse.isSuccess()) {
                    return@withContext Result.failure(IllegalStateException(mapApiError(recordResponse.code, recordResponse.msg)))
                }

                val record = recordResponse.data
                when (record.state.lowercase()) {
                    "waiting", "queuing", "generating" -> { /* continue polling */ }
                    "success" -> {
                        val urls = parseResultUrls(record.resultJson)
                        return@withContext Result.success(urls)
                    }
                    "fail" -> {
                        return@withContext Result.failure(IllegalStateException(record.failMsg ?: "生成失败"))
                    }
                    else -> {
                        return@withContext Result.failure(IllegalStateException("未知任务状态: ${record.state}"))
                    }
                }
            }


            Result.failure(IllegalStateException("生成超时（x次轮询后仍未完成），请稍后再试"))
        } catch (e: Exception) {
            Log.e("ImageGenerationService", "submitAndPoll failed", e)
            Result.failure(Exception("图像生成请求失败: ${e.message ?: "未知错误"}。请在设置-http日志中查看详细请求信息。", e))
        }
    }

    suspend fun downloadImage(gameId: Long, url: String): Result<InteractiveGameImage> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "game_images").apply { mkdirs() }
            val file = File(dir, "${gameId}_${System.currentTimeMillis()}.jpg")

            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("下载图片失败: ${response.code}"))
                }
                val body = response.body ?: return@withContext Result.failure(IllegalStateException("下载图片失败: 空响应"))
                file.sink().buffer().use { sink ->
                    sink.writeAll(body.source())
                }
            }

            val image = InteractiveGameImage(
                gameId = gameId,
                remoteUrl = url,
                localUri = file.absolutePath
            )
            val id = imageRepository.insertImage(image)
            Result.success(image.copy(id = id))
        } catch (e: Exception) {
            Log.e("ImageGenerationService", "downloadImage failed", e)
            Result.failure(e)
        }
    }

    suspend fun setGameBackground(game: InteractiveGame, image: InteractiveGameImage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val localUri = image.localUri ?: return@withContext Result.failure(IllegalStateException("图片尚未下载到本地"))
            val updatedGame = game.copy(backgroundImageUri = localUri)
            gameRepository.updateGame(updatedGame)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ImageGenerationService", "setGameBackground failed", e)
            Result.failure(e)
        }
    }

    private suspend fun getDefaultConfig(): ImageApiConfig? {
        val defaultId = userPreferencesRepository.defaultImageApiConfigId.first()
        return defaultId?.let { imageApiConfigRepository.getConfigById(it) }
    }

    private fun parseParamsJson(paramsJson: String): JsonObject {
        return try {
            json.parseToJsonElement(paramsJson).jsonObject
        } catch (_: Exception) {
            JsonObject(emptyMap())
        }
    }

    private fun parseResultUrls(resultJson: String?): List<String> {
        if (resultJson.isNullOrBlank()) return emptyList()
        return try {
            val jsonElement = json.parseToJsonElement(resultJson)
            val array = jsonElement.jsonObject["resultUrls"]?.jsonArray
                ?: jsonElement.jsonObject["images"]?.jsonArray
                ?: jsonElement.jsonArray
            array.mapNotNull { it.jsonPrimitive.content }
        } catch (e: Exception) {
            Log.e("ImageGenerationService", "parseResultUrls failed: $resultJson", e)
            emptyList()
        }
    }

    private fun deriveRecordInfoUrl(baseUrl: String): String {
        val trimmed = baseUrl.removeSuffix("/")
        val lastSlash = trimmed.lastIndexOf("/")
        return if (lastSlash > 0) {
            trimmed.substring(0, lastSlash) + "/recordInfo"
        } else {
            trimmed + "/recordInfo"
        }
    }

    private fun ImageApiResponse<*>.isSuccess(): Boolean {
        return code == 200
    }

    private fun mapApiError(code: Int, msg: String): String {
        return when (code) {
            401 -> "未授权，请检查 API Key"
            402 -> "积分不足"
            429 -> "频率超限，请稍后再试"
            501 -> "生成失败"
            else -> msg.ifBlank { "请求失败 (code=$code)" }
        }
    }
}
