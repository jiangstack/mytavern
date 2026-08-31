package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TownSnapshot(
    val id: Long = 0,
    val townId: Long,
    val name: String,
    val day: Int,
    val hour: Int,
    val snapshotJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 小镇整包快照。恢复时删除该小镇全部数据后按原 id 重新插入，保证内部引用一致。
 */
@Serializable
data class TownSnapshotData(
    val town: Town,
    val locations: List<TownLocation> = emptyList(),
    val members: List<TownMember> = emptyList(),
    val relationships: List<TownRelationship> = emptyList(),
    val scenes: List<TownScene> = emptyList(),
    val logs: List<TownLogEntry> = emptyList()
)
