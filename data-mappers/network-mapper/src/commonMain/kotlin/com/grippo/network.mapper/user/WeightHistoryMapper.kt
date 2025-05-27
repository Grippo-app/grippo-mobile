package com.grippo.network.mapper.user

import com.grippo.database.entity.WeightHistoryEntity
import com.grippo.logger.AppLogger
import com.grippo.network.user.WeightHistoryResponse

public fun List<WeightHistoryResponse>.toEntities(): List<WeightHistoryEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun WeightHistoryResponse.toEntityOrNull(): WeightHistoryEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "WeightHistoryResponse.id is null"
    } ?: return null

    val entityWeight = AppLogger.checkOrLog(weight) {
        "WeightHistoryResponse.weight is null"
    } ?: return null

    val created = AppLogger.checkOrLog(createdAt) {
        "WeightHistoryResponse.createdAt is null"
    } ?: return null

    val updated = AppLogger.checkOrLog(updatedAt) {
        "WeightHistoryResponse.updatedAt is null"
    } ?: return null

    return WeightHistoryEntity(
        id = entityId,
        weight = entityWeight,
        createdAt = created,
        updatedAt = updated,
    )
}