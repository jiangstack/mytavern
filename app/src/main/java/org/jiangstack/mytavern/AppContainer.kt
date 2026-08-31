package org.jiangstack.mytavern

import android.content.Context
import androidx.room.Room
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.jiangstack.mytavern.data.remote.HttpLogInterceptor
import org.jiangstack.mytavern.data.repository.BackupRepository
import org.jiangstack.mytavern.data.repository.HttpLogRepository
import org.jiangstack.mytavern.data.local.AppDatabase
import org.jiangstack.mytavern.data.local.MIGRATION_1_2
import org.jiangstack.mytavern.data.local.MIGRATION_2_3
import org.jiangstack.mytavern.data.local.MIGRATION_3_4
import org.jiangstack.mytavern.data.local.MIGRATION_4_5
import org.jiangstack.mytavern.data.local.MIGRATION_5_6
import org.jiangstack.mytavern.data.local.MIGRATION_6_7
import org.jiangstack.mytavern.data.local.MIGRATION_7_8
import org.jiangstack.mytavern.data.local.MIGRATION_8_9
import org.jiangstack.mytavern.data.local.MIGRATION_9_10
import org.jiangstack.mytavern.data.local.MIGRATION_10_11
import org.jiangstack.mytavern.data.local.MIGRATION_11_12
import org.jiangstack.mytavern.data.local.MIGRATION_12_13
import org.jiangstack.mytavern.data.local.MIGRATION_13_14
import org.jiangstack.mytavern.data.local.MIGRATION_14_15
import org.jiangstack.mytavern.data.local.MIGRATION_15_16
import org.jiangstack.mytavern.data.remote.ImageApiService
import org.jiangstack.mytavern.data.remote.LlmApiService
import org.jiangstack.mytavern.data.repository.CharacterRepositoryImpl
import org.jiangstack.mytavern.data.repository.ChatRepositoryImpl
import org.jiangstack.mytavern.data.repository.ImageApiConfigRepositoryImpl
import org.jiangstack.mytavern.data.repository.InteractiveGameImageRepositoryImpl
import org.jiangstack.mytavern.data.repository.LlmConfigRepositoryImpl
import org.jiangstack.mytavern.data.repository.QuickReplyRepositoryImpl
import org.jiangstack.mytavern.data.repository.SessionCharacterRepositoryImpl
import org.jiangstack.mytavern.data.repository.UserPreferencesRepositoryImpl
import org.jiangstack.mytavern.data.repository.WorldBookRepositoryImpl
import org.jiangstack.mytavern.data.repository.NovelRepositoryImpl
import org.jiangstack.mytavern.data.repository.InteractiveGameRepositoryImpl
import org.jiangstack.mytavern.data.repository.TownRepositoryImpl
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.ChatRepository
import org.jiangstack.mytavern.domain.repository.ImageApiConfigRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameImageRepository
import org.jiangstack.mytavern.domain.repository.LlmConfigRepository
import org.jiangstack.mytavern.domain.repository.QuickReplyRepository
import org.jiangstack.mytavern.domain.repository.SessionCharacterRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository
import org.jiangstack.mytavern.domain.repository.NovelRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameRepository
import org.jiangstack.mytavern.domain.repository.TownRepository
import org.jiangstack.mytavern.domain.service.ImageGenerationService
import org.jiangstack.mytavern.domain.service.InteractiveStoryService
import org.jiangstack.mytavern.domain.service.LlmService
import org.jiangstack.mytavern.domain.service.NovelAgentService
import org.jiangstack.mytavern.domain.service.TownSimulationService
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "mytavern.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16).build()

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

    val imageApiConfigRepository: ImageApiConfigRepository =
        ImageApiConfigRepositoryImpl(database.imageApiConfigDao())

    val interactiveGameImageRepository: InteractiveGameImageRepository =
        InteractiveGameImageRepositoryImpl(database.interactiveGameImageDao())

    val quickReplyRepository: QuickReplyRepository =
        QuickReplyRepositoryImpl(database.quickReplyDao())

    val sessionStateRepository: org.jiangstack.mytavern.domain.repository.SessionStateRepository =
        org.jiangstack.mytavern.data.repository.SessionStateRepositoryImpl(database.sessionStateDao())

    val novelRepository: NovelRepository =
        NovelRepositoryImpl(database.novelDao(), database.novelChapterDao(), database.novelCharacterDao(), database.novelCharacterItemDao())

    val backupRepository: org.jiangstack.mytavern.data.repository.BackupRepository =
        org.jiangstack.mytavern.data.repository.BackupRepository(context, database)

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

    val imageApiService: ImageApiService = Retrofit.Builder()
        .baseUrl("https://api.kie.ai/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ImageApiService::class.java)

    val llmService: LlmService by lazy {
        LlmService(llmApiService, llmConfigRepository, userPreferencesRepository, okHttpClient, json)
    }

    val imageGenerationService: ImageGenerationService by lazy {
        ImageGenerationService(imageApiService, imageApiConfigRepository, userPreferencesRepository, interactiveGameImageRepository, interactiveGameRepository, context, okHttpClient, json)
    }

    val novelAgentService: NovelAgentService by lazy {
        NovelAgentService(llmService, novelRepository, json)
    }

    val interactiveGameRepository: InteractiveGameRepository by lazy {
        InteractiveGameRepositoryImpl(
            database.interactiveGameDao(),
            database.interactiveGameCharacterDao(),
            database.interactiveMessageDao(),
            database.interactiveGameStateDao(),
            database.interactiveCheckpointDao(),
            json
        )
    }

    val interactiveStoryService: InteractiveStoryService by lazy {
        InteractiveStoryService(llmService, interactiveGameRepository, characterRepository, worldBookRepository, userPreferencesRepository, json)
    }

    val townRepository: TownRepository by lazy {
        TownRepositoryImpl(
            database.townDao(),
            database.townLocationDao(),
            database.townMemberDao(),
            database.townRelationshipDao(),
            database.townSceneDao(),
            database.townLogDao(),
            database.townSnapshotDao(),
            json
        )
    }

    val townSimulationService: TownSimulationService by lazy {
        TownSimulationService(llmService, townRepository, characterRepository, userPreferencesRepository, json)
    }
}
