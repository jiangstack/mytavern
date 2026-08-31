package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TownRelationship(
    val id: Long = 0,
    val townId: Long,
    val memberAId: Long,
    val memberBId: Long,
    val affinity: Int = 0,
    val note: String = ""
) {
    companion object {
        fun ordered(aId: Long, bId: Long): Pair<Long, Long> =
            if (aId <= bId) aId to bId else bId to aId
    }
}
