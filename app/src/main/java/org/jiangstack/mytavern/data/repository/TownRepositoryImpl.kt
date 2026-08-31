package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jiangstack.mytavern.data.local.dao.TownDao
import org.jiangstack.mytavern.data.local.dao.TownLocationDao
import org.jiangstack.mytavern.data.local.dao.TownLogDao
import org.jiangstack.mytavern.data.local.dao.TownMemberDao
import org.jiangstack.mytavern.data.local.dao.TownRelationshipDao
import org.jiangstack.mytavern.data.local.dao.TownSceneDao
import org.jiangstack.mytavern.data.local.dao.TownSnapshotDao
import org.jiangstack.mytavern.data.local.entity.TownEntity
import org.jiangstack.mytavern.data.local.entity.TownLocationEntity
import org.jiangstack.mytavern.data.local.entity.TownLogEntity
import org.jiangstack.mytavern.data.local.entity.TownMemberEntity
import org.jiangstack.mytavern.data.local.entity.TownRelationshipEntity
import org.jiangstack.mytavern.data.local.entity.TownSceneEntity
import org.jiangstack.mytavern.data.local.entity.TownSnapshotEntity
import org.jiangstack.mytavern.domain.model.LogKind
import org.jiangstack.mytavern.domain.model.SceneLine
import org.jiangstack.mytavern.domain.model.SceneStatus
import org.jiangstack.mytavern.domain.model.SceneType
import org.jiangstack.mytavern.domain.model.ScheduleItem
import org.jiangstack.mytavern.domain.model.Town
import org.jiangstack.mytavern.domain.model.TownLocation
import org.jiangstack.mytavern.domain.model.TownLogEntry
import org.jiangstack.mytavern.domain.model.TownMember
import org.jiangstack.mytavern.domain.model.TownMemoryEntry
import org.jiangstack.mytavern.domain.model.TownRelationship
import org.jiangstack.mytavern.domain.model.TownScene
import org.jiangstack.mytavern.domain.model.TownSnapshot
import org.jiangstack.mytavern.domain.model.TownSnapshotData
import org.jiangstack.mytavern.domain.repository.TownRepository

class TownRepositoryImpl(
    private val townDao: TownDao,
    private val locationDao: TownLocationDao,
    private val memberDao: TownMemberDao,
    private val relationshipDao: TownRelationshipDao,
    private val sceneDao: TownSceneDao,
    private val logDao: TownLogDao,
    private val snapshotDao: TownSnapshotDao,
    private val json: Json
) : TownRepository {

    // ========== 小镇 ==========

    override fun getAllTowns(): Flow<List<Town>> =
        townDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getTownByIdFlow(id: Long): Flow<Town?> =
        townDao.getByIdFlow(id).map { it?.toDomain() }

    override suspend fun getTownById(id: Long): Town? = townDao.getById(id)?.toDomain()

    override suspend fun insertTown(town: Town): Long = townDao.insert(town.toEntity())

    override suspend fun updateTown(town: Town) = townDao.update(town.toEntity())

    override suspend fun deleteTown(town: Town) = townDao.delete(town.toEntity())

    // ========== 地点 ==========

    override fun getLocationsByTownId(townId: Long): Flow<List<TownLocation>> =
        locationDao.getByTownId(townId).map { list -> list.map { it.toDomain() } }

    override suspend fun getLocationsByTownIdSync(townId: Long): List<TownLocation> =
        locationDao.getByTownIdSync(townId).map { it.toDomain() }

    override suspend fun insertLocation(location: TownLocation): Long = locationDao.insert(location.toEntity())

    override suspend fun updateLocation(location: TownLocation) = locationDao.update(location.toEntity())

    override suspend fun deleteLocation(location: TownLocation) = locationDao.delete(location.toEntity())

    // ========== 成员 ==========

    override fun getMembersByTownId(townId: Long): Flow<List<TownMember>> =
        memberDao.getByTownId(townId).map { list -> list.map { it.toDomain() } }

    override suspend fun getMembersByTownIdSync(townId: Long): List<TownMember> =
        memberDao.getByTownIdSync(townId).map { it.toDomain() }

    override suspend fun getMemberById(id: Long): TownMember? = memberDao.getById(id)?.toDomain()

    override suspend fun insertMember(member: TownMember): Long = memberDao.insert(member.toEntity())

    override suspend fun updateMember(member: TownMember) = memberDao.update(member.toEntity())

    override suspend fun syncMembers(townId: Long, desired: List<TownMember>): Map<Long, Long> {
        val existing = memberDao.getByTownIdSync(townId)
        val desiredByCharacterId = desired.associateBy { it.characterId }
        val result = mutableMapOf<Long, Long>()

        // 删除被移除的成员
        existing.filter { it.characterId !in desiredByCharacterId }.forEach { memberDao.delete(it) }

        // 更新已有成员（仅 persona 与扮演标记，保留记忆/日程/位置等运行时状态）
        existing.filter { it.characterId in desiredByCharacterId }.forEach { entity ->
            val desiredMember = desiredByCharacterId.getValue(entity.characterId)
            memberDao.update(
                entity.copy(
                    persona = desiredMember.persona,
                    isPlayerControlled = desiredMember.isPlayerControlled
                )
            )
            result[entity.characterId] = entity.id
        }

        // 插入新增成员
        desired.filter { member -> existing.none { it.characterId == member.characterId } }.forEach { member ->
            val newId = memberDao.insert(member.copy(townId = townId).toEntity())
            result[member.characterId] = newId
        }
        return result
    }

    // ========== 关系 ==========

    override fun getRelationshipsByTownId(townId: Long): Flow<List<TownRelationship>> =
        relationshipDao.getByTownId(townId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRelationshipsByTownIdSync(townId: Long): List<TownRelationship> =
        relationshipDao.getByTownIdSync(townId).map { it.toDomain() }

    override suspend fun getRelationshipBetween(townId: Long, aId: Long, bId: Long): TownRelationship? =
        relationshipDao.findBetween(townId, aId, bId)?.toDomain()

    override suspend fun upsertRelationship(relationship: TownRelationship) {
        val (a, b) = TownRelationship.ordered(relationship.memberAId, relationship.memberBId)
        val normalized = relationship.copy(memberAId = a, memberBId = b)
        val existing = relationshipDao.findBetween(relationship.townId, a, b)
        if (existing == null) {
            relationshipDao.insert(normalized.toEntity())
        } else {
            relationshipDao.update(normalized.copy(id = existing.id).toEntity())
        }
    }

    // ========== 场景 ==========

    override fun getScenesByTownId(townId: Long): Flow<List<TownScene>> =
        sceneDao.getByTownId(townId).map { list -> list.map { it.toDomain() } }

    override fun getSceneByIdFlow(id: Long): Flow<TownScene?> =
        sceneDao.getByIdFlow(id).map { it?.toDomain() }

    override suspend fun getSceneById(id: Long): TownScene? = sceneDao.getById(id)?.toDomain()

    override suspend fun getFirstPendingScene(townId: Long): TownScene? =
        sceneDao.getFirstPendingSync(townId)?.toDomain()

    override suspend fun getActiveInteractiveScene(townId: Long): TownScene? =
        sceneDao.getActiveInteractiveSync(townId)?.toDomain()

    override suspend fun getRecentScenesSync(townId: Long, limit: Int): List<TownScene> =
        sceneDao.getRecentSync(townId, limit).map { it.toDomain() }

    override suspend fun insertScene(scene: TownScene): Long = sceneDao.insert(scene.toEntity())

    override suspend fun updateScene(scene: TownScene) = sceneDao.update(scene.toEntity())

    // ========== 日志 ==========

    override fun getLogsByTownId(townId: Long): Flow<List<TownLogEntry>> =
        logDao.getByTownId(townId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRecentLogsSync(townId: Long, limit: Int): List<TownLogEntry> =
        logDao.getRecentSync(townId, limit).map { it.toDomain() }.reversed()

    override suspend fun insertLog(log: TownLogEntry): Long = logDao.insert(log.toEntity())

    // ========== 快照 ==========

    override fun getSnapshotsByTownId(townId: Long): Flow<List<TownSnapshot>> =
        snapshotDao.getByTownId(townId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSnapshotById(id: Long): TownSnapshot? = snapshotDao.getById(id)?.toDomain()

    override suspend fun insertSnapshot(snapshot: TownSnapshot): Long = snapshotDao.insert(snapshot.toEntity())

    override suspend fun deleteSnapshot(snapshot: TownSnapshot) = snapshotDao.delete(snapshot.toEntity())

    override suspend fun restoreSnapshotData(data: TownSnapshotData) {
        val townId = data.town.id
        locationDao.deleteByTownId(townId)
        memberDao.deleteByTownId(townId)
        relationshipDao.deleteByTownId(townId)
        sceneDao.deleteByTownId(townId)
        logDao.deleteByTownId(townId)

        data.locations.forEach { locationDao.insert(it.toEntity()) }
        data.members.forEach { memberDao.insert(it.toEntity()) }
        data.relationships.forEach { relationshipDao.insert(it.toEntity()) }
        data.scenes.forEach { sceneDao.insert(it.toEntity()) }
        data.logs.forEach { logDao.insert(it.toEntity()) }
        townDao.update(data.town.toEntity())
    }

    // ========== 映射 ==========

    private fun TownEntity.toDomain() = Town(
        id = id,
        name = name,
        worldDescription = worldDescription,
        currentDay = currentDay,
        currentHour = currentHour,
        playMemberId = playMemberId,
        windowWordCount = windowWordCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Town.toEntity() = TownEntity(
        id = id,
        name = name,
        worldDescription = worldDescription,
        currentDay = currentDay,
        currentHour = currentHour,
        playMemberId = playMemberId,
        windowWordCount = windowWordCount,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )

    private fun TownLocationEntity.toDomain() = TownLocation(
        id = id, townId = townId, name = name, description = description
    )

    private fun TownLocation.toEntity() = TownLocationEntity(
        id = id, townId = townId, name = name, description = description
    )

    private fun TownMemberEntity.toDomain(): TownMember {
        val schedule = try {
            json.decodeFromString(ListSerializer(ScheduleItem.serializer()), todayScheduleJson)
        } catch (_: Exception) { emptyList() }
        val recent = try {
            json.decodeFromString(ListSerializer(TownMemoryEntry.serializer()), recentMemoryJson)
        } catch (_: Exception) { emptyList() }
        val important = try {
            json.decodeFromString(ListSerializer(TownMemoryEntry.serializer()), importantMemoryJson)
        } catch (_: Exception) { emptyList() }
        return TownMember(
            id = id,
            townId = townId,
            characterId = characterId,
            persona = persona,
            isPlayerControlled = isPlayerControlled,
            currentLocationId = currentLocationId,
            currentActivity = currentActivity,
            mood = mood,
            todaySchedule = schedule,
            recentMemory = recent,
            importantMemory = important
        )
    }

    private fun TownMember.toEntity() = TownMemberEntity(
        id = id,
        townId = townId,
        characterId = characterId,
        persona = persona,
        isPlayerControlled = isPlayerControlled,
        currentLocationId = currentLocationId,
        currentActivity = currentActivity,
        mood = mood,
        todayScheduleJson = json.encodeToString(ListSerializer(ScheduleItem.serializer()), todaySchedule),
        recentMemoryJson = json.encodeToString(ListSerializer(TownMemoryEntry.serializer()), recentMemory),
        importantMemoryJson = json.encodeToString(ListSerializer(TownMemoryEntry.serializer()), importantMemory)
    )

    private fun TownRelationshipEntity.toDomain() = TownRelationship(
        id = id, townId = townId, memberAId = memberAId, memberBId = memberBId,
        affinity = affinity, note = note
    )

    private fun TownRelationship.toEntity() = TownRelationshipEntity(
        id = id, townId = townId, memberAId = memberAId, memberBId = memberBId,
        affinity = affinity, note = note
    )

    private fun TownSceneEntity.toDomain(): TownScene {
        val participants = try {
            json.decodeFromString(ListSerializer(Long.serializer()), participantIdsJson)
        } catch (_: Exception) { emptyList() }
        val lines = try {
            json.decodeFromString(ListSerializer(SceneLine.serializer()), linesJson)
        } catch (_: Exception) { emptyList() }
        return TownScene(
            id = id,
            townId = townId,
            day = day,
            hour = hour,
            locationId = locationId,
            type = try { SceneType.valueOf(type) } catch (_: Exception) { SceneType.ENCOUNTER },
            status = try { SceneStatus.valueOf(status) } catch (_: Exception) { SceneStatus.DONE },
            participantIds = participants,
            lines = lines,
            summary = summary,
            createdAt = createdAt
        )
    }

    private fun TownScene.toEntity() = TownSceneEntity(
        id = id,
        townId = townId,
        day = day,
        hour = hour,
        locationId = locationId,
        type = type.name,
        status = status.name,
        participantIdsJson = json.encodeToString(ListSerializer(Long.serializer()), participantIds),
        linesJson = json.encodeToString(ListSerializer(SceneLine.serializer()), lines),
        summary = summary,
        createdAt = createdAt
    )

    private fun TownLogEntity.toDomain() = TownLogEntry(
        id = id,
        townId = townId,
        day = day,
        hour = hour,
        kind = try { LogKind.valueOf(kind) } catch (_: Exception) { LogKind.TICK },
        text = text,
        createdAt = createdAt
    )

    private fun TownLogEntry.toEntity() = TownLogEntity(
        id = id,
        townId = townId,
        day = day,
        hour = hour,
        kind = kind.name,
        text = text,
        createdAt = createdAt
    )

    private fun TownSnapshotEntity.toDomain() = TownSnapshot(
        id = id,
        townId = townId,
        name = name,
        day = day,
        hour = hour,
        snapshotJson = snapshotJson,
        createdAt = createdAt
    )

    private fun TownSnapshot.toEntity() = TownSnapshotEntity(
        id = id,
        townId = townId,
        name = name,
        day = day,
        hour = hour,
        snapshotJson = snapshotJson,
        createdAt = createdAt
    )
}
