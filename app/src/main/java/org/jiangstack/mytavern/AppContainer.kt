package org.jiangstack.mytavern

import android.content.Context
import androidx.room.Room
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.jiangstack.mytavern.data.remote.HttpLogInterceptor
import org.jiangstack.mytavern.data.repository.HttpLogRepository
import org.jiangstack.mytavern.data.local.AppDatabase
import org.jiangstack.mytavern.data.local.MIGRATION_1_2
import org.jiangstack.mytavern.data.local.MIGRATION_2_3
import org.jiangstack.mytavern.data.local.MIGRATION_3_4
import org.jiangstack.mytavern.data.local.MIGRATION_4_5
import org.jiangstack.mytavern.data.remote.LlmApiService
import org.jiangstack.mytavern.data.repository.CharacterRepositoryImpl
import org.jiangstack.mytavern.data.repository.ChatRepositoryImpl
import org.jiangstack.mytavern.data.repository.LlmConfigRepositoryImpl
import org.jiangstack.mytavern.data.repository.SessionCharacterRepositoryImpl
import org.jiangstack.mytavern.data.repository.UserPreferencesRepositoryImpl
import org.jiangstack.mytavern.data.repository.WorldBookRepositoryImpl
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.ChatRepository
import org.jiangstack.mytavern.domain.repository.LlmConfigRepository
import org.jiangstack.mytavern.domain.repository.SessionCharacterRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository
import org.jiangstack.mytavern.domain.service.LlmService
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "mytavern.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()

    val userPreferencesRepository: UserPreferencesRepository =
        UserPreferencesRepositoryImpl(context)

    val characterRepository: CharacterRepository =
        CharacterRepositoryImpl(database.characterDao())

    val worldBookRepository: WorldBookRepository =
        WorldBookRepositoryImpl(database.worldBookDao(), database.worldBookRuleDao())

    val chatRepository: ChatRepository =
        ChatRepositoryImpl(database.chatSessionDao(), database.chatMessageDao())

    val sessionCharacterRepository: SessionCharacterRepository =
        SessionCharacterRepositoryImpl(database.sessionCharacterDao())

    val llmConfigRepository: LlmConfigRepository =
        LlmConfigRepositoryImpl(database.llmConfigDao())

    val httpLogRepository: HttpLogRepository = HttpLogRepository()

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLogInterceptor(httpLogRepository))
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val llmApiService: LlmApiService = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(LlmApiService::class.java)

    val llmService: LlmService by lazy {
        LlmService(llmApiService, llmConfigRepository, userPreferencesRepository, okHttpClient, json)
    }
}
