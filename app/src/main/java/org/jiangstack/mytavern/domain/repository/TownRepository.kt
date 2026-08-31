package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.Town
import org.jiangstack.mytavern.domain.model.TownLocation
import org.jiangstack.mytavern.domain.model.TownLogEntry
import org.jiangstack.mytavern.domain.model.TownMember
import org.jiangstack.mytavern.domain.model.TownRelationship
import org.jiangstack.mytavern.domain.model.TownScene
import org.jiangstack.mytavern.domain.model.TownSnapshot
import org.jiangstack.mytavern.domain.model.TownSnapshotData

interface TownRepository {
    // 小镇
    fun getAllTowns(): Flow<List<Town>>
    fun getTownByIdFlow(id: Long): Flow<Town?>
    suspend fun getTownById(id: Long): Town?
    suspend fun insertTown(town: Town): Long
    suspend fun updateTown(town: Town)
    suspend fun deleteTown(town: Town)

    // 地点
    fun getLocationsByTownId(townId: Long): Flow<List<TownLocation>>
    suspend fun getLocationsByTownIdSync(townId: Long): List<TownLocation>
    suspend fun insertLocation(location: TownLocation): Long
    suspend fun updateLocation(location: TownLocation)
    suspend fun deleteLocation(location: TownLocation)

    // 成员
    fun getMembersByTownId(townId: Long): Flow<List<TownMember>>
    suspend fun getMembersByTownIdSync(townId: Long): List<TownMember>
    suspend fun getMemberById(id: Long): TownMember?
    suspend fun insertMember(member: TownMember): Long
    suspend fun updateMember(member: TownMember)

    /**
     * 按角色卡同步成员：更新已有成员的 persona/扮演标记（保留记忆与日程），
     * 删除被移除的成员，插入新增成员。返回 characterId -> memberId 映射。
     */
    suspend fun syncMembers(townId: Long, desired: List<TownMember>): Map<Long, Long>

    // 关系
    fun getRelationshipsByTownId(townId: Long): Flow<List<TownRelationship>>
    suspend fun getRelationshipsByTownIdSync(townId: Long): List<TownRelationship>
    suspend fun getRelationshipBetween(townId: Long, aId: Long, bId: Long): TownRelationship?
    suspend fun upsertRelationship(relationship: TownRelationship)

    // 场景
    fun getScenesByTownId(townId: Long): Flow<List<TownScene>>
    fun getSceneByIdFlow(id: Long): Flow<TownScene?>
    suspend fun getSceneById(id: Long): TownScene?
    suspend fun getFirstPendingScene(townId: Long): TownScene?
    suspend fun getActiveInteractiveScene(townId: Long): TownScene?
    suspend fun getRecentScenesSync(townId: Long, limit: Int): List<TownScene>
    suspend fun insertScene(scene: TownScene): Long
    suspend fun updateScene(scene: TownScene)

    // 日志
    fun getLogsByTownId(townId: Long): Flow<List<TownLogEntry>>
    suspend fun getRecentLogsSync(townId: Long, limit: Int): List<TownLogEntry>
    suspend fun insertLog(log: TownLogEntry): Long

    // 快照
    fun getSnapshotsByTownId(townId: Long): Flow<List<TownSnapshot>>
    suspend fun getSnapshotById(id: Long): TownSnapshot?
    suspend fun insertSnapshot(snapshot: TownSnapshot): Long
    suspend fun deleteSnapshot(snapshot: TownSnapshot)
    suspend fun restoreSnapshotData(data: TownSnapshotData)
}
