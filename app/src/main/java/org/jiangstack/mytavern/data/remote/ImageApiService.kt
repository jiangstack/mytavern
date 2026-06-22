package org.jiangstack.mytavern.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface ImageApiService {

    @POST
    suspend fun createTask(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: CreateImageTaskRequest
    ): ImageApiResponse<ImageTaskData>

    @GET
    suspend fun getRecordInfo(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Query("taskId") taskId: String
    ): ImageApiResponse<ImageRecordData>
}

@Serializable
data class CreateImageTaskRequest(
    val model: String,
    val input: JsonObject
)

@Serializable
data class ImageTaskData(
    val taskId: String,
    val recordId: String
)

@Serializable
data class ImageRecordData(
    val taskId: String,
    val model: String,
    val state: String,
    val param: String? = null,
    val resultJson: String? = null,
    val failCode: String? = null,
    val failMsg: String? = null
)

@Serializable
data class ImageApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T
)
